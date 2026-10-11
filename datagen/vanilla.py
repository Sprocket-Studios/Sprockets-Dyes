"""Read-only view of a Minecraft client jar's assets/ and data/ (no extraction needed)."""
import fnmatch
import zipfile
from pathlib import Path, PurePosixPath

LOOM_CACHE = Path.home() / ".gradle/caches/fabric-loom"


def client_jar(version):
    jar = LOOM_CACHE / version / "minecraft-client.jar"
    if not jar.exists():
        raise SystemExit(f"missing {jar}: configure the {version}-fabric node once (any ./gradlew task does)")
    return jar


class VanillaJar:
    def __init__(self, version, jar=None):
        self.version = version
        self.zip = zipfile.ZipFile(jar or client_jar(version))
        self.names = [n for n in self.zip.namelist() if n.startswith(("assets/", "data/")) and not n.endswith("/")]
        self._set = set(self.names)
        self._by_dir = {}   # directory -> [file names]
        self._by_name = {}  # file name -> [full paths]
        for n in self.names:
            d, _, f = n.rpartition("/")
            self._by_dir.setdefault(d, []).append(f)
            self._by_name.setdefault(f, []).append(n)

    def exists(self, path):
        return str(path) in self._set

    def read_text(self, path):
        return self.zip.read(str(path)).decode("utf-8")

    def read_bytes(self, path):
        return self.zip.read(str(path))

    def glob(self, directory, pattern="*"):
        """Files directly inside `directory` whose name matches `pattern`, as PurePosixPaths."""
        d = str(directory).rstrip("/")
        names = self._by_dir.get(d, [])
        if not any(ch in pattern for ch in "*?["):
            return [PurePosixPath(f"{d}/{pattern}")] if pattern in names else []
        return [PurePosixPath(f"{d}/{f}") for f in fnmatch.filter(names, pattern)]

    def rglob(self, directory, pattern="*"):
        prefix = str(directory).rstrip("/") + "/"
        if not any(ch in pattern for ch in "*?["):
            return [PurePosixPath(n) for n in self._by_name.get(pattern, []) if n.startswith(prefix)]
        return [PurePosixPath(n) for n in self.names if n.startswith(prefix) and fnmatch.fnmatch(n.rsplit("/", 1)[-1], pattern)]
