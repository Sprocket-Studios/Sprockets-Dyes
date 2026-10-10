#!/usr/bin/env python3
"""Renders palette/colors.json into palette/preview.html (a self-contained review page)."""
import json
from pathlib import Path

root = Path(__file__).resolve().parent
data = json.loads((root / "colors.json").read_text())
template = (root / "preview_template.html").read_text()
html = template.replace("/*__PALETTE__*/null", json.dumps(data, separators=(",", ":")))
(root / "preview.html").write_text(html)
print(f"wrote {root / 'preview.html'}")
