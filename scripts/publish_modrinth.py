#!/usr/bin/env python3
"""
Uploads the jars in build/libs/<mod version>/ to Modrinth, one version per jar (stdlib only).

  python3 scripts/publish_modrinth.py <project slug or id>            # dry run: validate, print what it would do
  python3 scripts/publish_modrinth.py <project slug or id> --publish  # upload

The token is read from $MODRINTH_TOKEN or ~/.config/modrinth/token (needs the "Create versions" scope;
"Read projects" too if the project is still a draft). Versions that already exist (same version number)
are skipped, so a failed run can simply be repeated.

Per jar: game versions = the node's `mod.mc_releases`, loader = fabric/neoforge, environment = client and
server, dependencies = Architectury API (required) and, on Fabric, Fabric API (required). Existing versions
get their environment corrected on --publish runs. The minimum Architectury version is enforced by the
jar's own metadata; here we check Modrinth has an Architectury release at least that new for every game
version the jar claims.
"""
import json
import os
import re
import sys
import time
import tomllib
import urllib.error
import urllib.parse
import urllib.request
import uuid
from pathlib import Path

ROOT = Path(__file__).resolve().parent.parent
API = "https://api.modrinth.com/v2"
API3 = "https://api.modrinth.com/v3"  # per-version `environment` only exists in v3
ENVIRONMENT = "client_and_server"
USER_AGENT = "Sprocket-Studios/SprocketDyes publish script (github.com/Sprocket-Studios/SprocketDyes)"
LOADER_NAMES = {"fabric": "Fabric", "neoforge": "NeoForge"}
LOADER_LETTERS = {"fabric": "f", "neoforge": "n"}
DEPENDENCIES = {"fabric": ["architectury-api", "fabric-api"], "neoforge": ["architectury-api"]}

CHANGELOG = """\
- 132 dyes
- Slabs, stairs, and walls for colored blocks
- Mixing via crafting table
"""


def token():
    if os.environ.get("MODRINTH_TOKEN"):
        return os.environ["MODRINTH_TOKEN"].strip()
    path = Path.home() / ".config/modrinth/token"
    return path.read_text().strip() if path.exists() else None


class ApiError(Exception):
    pass


def request(method, path, tok=None, body=None, content_type=None, api=API):
    req = urllib.request.Request(api + path, data=body, method=method)
    req.add_header("User-Agent", USER_AGENT)
    if tok:
        req.add_header("Authorization", tok)
    if content_type:
        req.add_header("Content-Type", content_type)
    try:
        with urllib.request.urlopen(req) as resp:
            return json.loads(resp.read() or b"null")
    except urllib.error.HTTPError as e:
        raise ApiError(f"{method} {path}: HTTP {e.code} {e.read().decode(errors='replace')}")


def vkey(v):
    return tuple(int(x) for x in re.findall(r"\d+", v.split("+")[0]))


def jars():
    props = tomllib.loads((ROOT / "stonecutter.properties.toml").read_text())
    mod_version = props["mod"]["version"]
    out = []
    for mc, table in props.items():
        if not re.match(r"^\d", mc):
            continue
        for loader in ("fabric", "neoforge"):
            jar = ROOT / f"build/libs/{mod_version}/sprocket_dyes-{loader}-{mod_version}+{mc}.jar"
            out.append({
                "mc": mc,
                "loader": loader,
                "jar": jar,
                "game_versions": table["mod"]["mc_releases"],
                "arch_min": table["mod"]["arch_compat"],
                "version_number": f"{mod_version}-{LOADER_LETTERS[loader]}{mc}",
                "name": f"Sprocket Dyes {mod_version} ({LOADER_NAMES[loader]} {mc})",
            })
    return sorted(out, key=lambda j: (vkey(j["mc"]), j["loader"]))


def multipart(fields, files):
    boundary = uuid.uuid4().hex
    parts = []
    for name, value in fields.items():
        parts.append(f'--{boundary}\r\nContent-Disposition: form-data; name="{name}"\r\n\r\n{value}\r\n'.encode())
    for name, path in files.items():
        parts.append(f'--{boundary}\r\nContent-Disposition: form-data; name="{name}"; filename="{path.name}"\r\n'
                     f"Content-Type: application/java-archive\r\n\r\n".encode() + path.read_bytes() + b"\r\n")
    parts.append(f"--{boundary}--\r\n".encode())
    return b"".join(parts), f"multipart/form-data; boundary={boundary}"


def main():
    args = [a for a in sys.argv[1:] if not a.startswith("--")]
    publish = "--publish" in sys.argv
    if len(args) != 1:
        raise SystemExit(__doc__)
    tok = token()
    if publish and not tok:
        raise SystemExit("no token: set MODRINTH_TOKEN or write it to ~/.config/modrinth/token")

    try:
        project = request("GET", f"/project/{urllib.parse.quote(args[0])}", tok)
        print(f"project: {project['title']} ({project['slug']}, {project['id']}, {project['status']})")
        existing = {v["version_number"]: v["id"] for v in request("GET", f"/project/{project['id']}/version", tok)}
    except ApiError as e:
        if publish:
            raise SystemExit(str(e))
        print(f"WARNING project {args[0]!r} not found ({'no token' if not tok else 'check slug/token scopes'}); validating the rest")
        project, existing = {"slug": args[0], "id": "?"}, {}
    deps = {slug: request("GET", f"/project/{slug}")["id"] for slug in ("architectury-api", "fabric-api")}
    known_versions = {t["version"] for t in request("GET", "/tag/game_version")}

    problems = []
    plan = jars()
    for j in plan:
        if not j["jar"].exists():
            problems.append(f"missing jar {j['jar']}")
        for gv in j["game_versions"]:
            if gv not in known_versions:
                problems.append(f"{j['version_number']}: Modrinth has no game version {gv!r}")
            arch = request("GET", "/project/architectury-api/version?" + urllib.parse.urlencode(
                {"loaders": json.dumps([j["loader"]]), "game_versions": json.dumps([gv])}))
            if not any(vkey(a["version_number"]) >= vkey(j["arch_min"]) for a in arch):
                problems.append(f"{j['version_number']}: no Architectury >= {j['arch_min']} for {j['loader']} {gv} on Modrinth")
    if problems:
        print("\n".join("PROBLEM " + p for p in problems))
        raise SystemExit(1)

    def set_environment(version_id):
        current = request("GET", f"/version/{version_id}", tok, api=API3).get("environment")
        if current != ENVIRONMENT:
            request("PATCH", f"/version/{version_id}", tok, json.dumps({"environment": ENVIRONMENT}).encode(),
                    "application/json", api=API3)
            print(f"    environment: {current} -> {ENVIRONMENT}")

    for j in plan:
        if publish and j["version_number"] in existing:
            set_environment(existing[j["version_number"]])
        state = "exists, skip" if j["version_number"] in existing else ("upload" if publish else "would upload")
        print(f"{j['version_number']:<18} {j['name']:<40} {', '.join(j['game_versions']):<22} "
              f"deps: {', '.join(DEPENDENCIES[j['loader']])} (Architectury >= {j['arch_min']})  [{state}]")
        if not publish or j["version_number"] in existing:
            continue
        data = {
            "project_id": project["id"],
            "name": j["name"],
            "version_number": j["version_number"],
            "changelog": CHANGELOG,
            "dependencies": [{"project_id": deps[s], "dependency_type": "required"} for s in DEPENDENCIES[j["loader"]]],
            "game_versions": j["game_versions"],
            "version_type": "release",
            "loaders": [j["loader"]],
            "featured": False,
            "status": "listed",
            "file_parts": ["file"],
            "primary_file": "file",
        }
        body, ctype = multipart({"data": json.dumps(data)}, {"file": j["jar"]})
        for attempt in range(5):
            try:
                created = request("POST", "/version", tok, body, ctype)
                break
            except ApiError as e:
                # Modrinth sometimes answers 500 (database deadlock); retry unless the version got created anyway.
                if "HTTP 5" not in str(e) or attempt == 4:
                    raise
                print(f"    {e}; retrying in {15 * (attempt + 1)}s")
                time.sleep(15 * (attempt + 1))
                now = {v["version_number"]: v for v in request("GET", f"/project/{project['id']}/version", tok)}
                if j["version_number"] in now:
                    created = now[j["version_number"]]
                    break
        set_environment(created["id"])
        print(f"    -> https://modrinth.com/mod/{project['slug']}/version/{created['id']}")
    if not publish:
        print("\ndry run - nothing uploaded. Add --publish to upload.")


if __name__ == "__main__":
    try:
        main()
    except ApiError as e:
        raise SystemExit(str(e))
