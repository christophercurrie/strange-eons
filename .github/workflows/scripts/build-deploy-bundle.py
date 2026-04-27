#!/usr/bin/env python3
"""
Assemble the release deploy bundle from CI build artifacts.

Reads installers downloaded from the build matrix (one subdirectory per
matrix entry) and produces an output tree mirroring what the OpalStack
host should serve:

    <output>/
        index.html                          downloads landing page
        strange-eons-<ver>-<os-arch>.<ext>  installers (renamed for stable URLs)
        updates/
            catalog.txt                     update catalog (one app listing)

The output directory is rsynced to ~/apps/strangeeons/ on the OpalStack
host as the final deploy step.
"""
import argparse
import datetime
import hashlib
import shutil
import sys
from pathlib import Path

CHANNEL_UUIDS = {
    "stable":       "c8d1620e-5eeb-47f4-9ef2-49e9947faa90",
    "experimental": "1b7ef4bd-f63a-4884-9979-830d4feb18b8",
}

# Map (artifact-dir-name, file-suffix) -> (canonical filename suffix, label).
# artifact-dir-name matches the matrix `artifact:` field in release.yml.
# The {ver} placeholder is filled per release.
INSTALLER_RULES = [
    # macOS
    (("strange-eons-macos-aarch64", ".dmg"),
     "strange-eons-{ver}-macos-aarch64.dmg",
     "macOS (Apple Silicon)"),
    (("strange-eons-macos-x64", ".dmg"),
     "strange-eons-{ver}-macos-x64.dmg",
     "macOS (Intel)"),
    # Windows
    (("strange-eons-windows", ".msi"),
     "strange-eons-{ver}-windows.msi",
     "Windows"),
    # Linux
    (("strange-eons-linux", ".deb"),
     "strange-eons-{ver}.deb",
     "Linux (Debian/Ubuntu, .deb)"),
    (("strange-eons-linux", ".rpm"),
     "strange-eons-{ver}.rpm",
     "Linux (RHEL/Fedora, .rpm)"),
    (("strange-eons-linux", ".tar.gz"),
     "strange-eons-{ver}-linux.tar.gz",
     "Linux (portable tarball)"),
]


def sha256(path: Path) -> str:
    h = hashlib.sha256()
    with path.open("rb") as f:
        for chunk in iter(lambda: f.read(1 << 16), b""):
            h.update(chunk)
    return h.hexdigest()


def md5(path: Path) -> str:
    h = hashlib.md5()
    with path.open("rb") as f:
        for chunk in iter(lambda: f.read(1 << 16), b""):
            h.update(chunk)
    return h.hexdigest()


def human_size(n: int) -> str:
    for unit in ("B", "KB", "MB", "GB"):
        if n < 1024:
            return f"{n:.1f} {unit}" if unit != "B" else f"{n} {unit}"
        n /= 1024
    return f"{n:.1f} TB"


def find_installer(artifacts_dir: Path, dirname: str, suffix: str):
    """Return the first file under artifacts/<dirname>/ matching suffix."""
    base = artifacts_dir / dirname
    if not base.exists():
        return None
    # rglob handles nested layouts (e.g. linux app-image tarball wrapper)
    candidates = sorted(base.rglob(f"*{suffix}"))
    return candidates[0] if candidates else None


def collect(artifacts_dir: Path, output_dir: Path, version: str):
    output_dir.mkdir(parents=True, exist_ok=True)
    collected = []
    for (dirname, suffix), name_tmpl, label in INSTALLER_RULES:
        src = find_installer(artifacts_dir, dirname, suffix)
        if src is None:
            print(f"warn: no {suffix} found under {dirname}", file=sys.stderr)
            continue
        dest_name = name_tmpl.format(ver=version)
        dest = output_dir / dest_name
        shutil.copy2(src, dest)
        collected.append({
            "label": label,
            "filename": dest_name,
            "size": dest.stat().st_size,
            "sha256": sha256(dest),
            "md5": md5(dest),
        })
        print(f"  {label}: {dest_name} ({human_size(dest.stat().st_size)})")
    return collected


def catalog_id(channel: str, when: datetime.datetime) -> str:
    """CATALOGUEID{uuid:Y-M-D-H-M-S-MS}. Java Calendar months are 0-indexed."""
    uuid = CHANNEL_UUIDS[channel]
    date = (
        f"{when.year}-{when.month - 1}-{when.day}-"
        f"{when.hour}-{when.minute}-{when.second}-"
        f"{when.microsecond // 1000}"
    )
    return f"CATALOGUEID{{{uuid}:{date}}}"


def write_catalog(output_dir: Path, version: str, build: int,
                  channel: str, when: datetime.datetime):
    updates_dir = output_dir / "updates"
    updates_dir.mkdir(exist_ok=True)
    cat = updates_dir / "catalog.txt"
    body = (
        f"# Strange Eons {channel} catalog\n"
        f"# Generated {when.isoformat()}\n"
        f"\n"
        f"name = Strange Eons {version}\n"
        f"description = Update to Strange Eons {version} (build {build})\n"
        f"version = {build}\n"
        f"url = https://strangeeons.fizmo.org/\n"
        f"homepage = https://strangeeons.fizmo.org/\n"
        f"date = {when.strftime('%Y-%m-%d')}\n"
        f"id = {catalog_id(channel, when)}\n"
    )
    cat.write_text(body, encoding="utf-8")
    print(f"  catalog: {cat.relative_to(output_dir)}")


INDEX_TEMPLATE = """<!doctype html>
<html lang="en">
<head>
  <meta charset="utf-8">
  <title>Strange Eons {version}</title>
  <meta name="viewport" content="width=device-width, initial-scale=1">
  <style>
    body {{ font-family: system-ui, -apple-system, sans-serif; max-width: 48rem; margin: 2rem auto; padding: 0 1rem; line-height: 1.5; }}
    h1 {{ margin-bottom: 0.25rem; }}
    .ver {{ color: #666; }}
    table {{ border-collapse: collapse; width: 100%; margin-top: 1rem; }}
    th, td {{ text-align: left; padding: 0.5rem; border-bottom: 1px solid #eee; }}
    th {{ background: #f7f7f7; }}
    code {{ font-family: ui-monospace, monospace; font-size: 0.85em; word-break: break-all; }}
    .note {{ color: #666; font-size: 0.9em; }}
    a.docs {{ display: inline-block; margin-top: 1rem; }}
  </style>
</head>
<body>
  <h1>Strange Eons</h1>
  <p class="ver">Version {version} &middot; build {build} &middot; {type_label}</p>

  <h2>Downloads</h2>
  <table>
    <thead><tr><th>Platform</th><th>File</th><th>Size</th><th>SHA-256</th></tr></thead>
    <tbody>
{rows}
    </tbody>
  </table>

  <p class="note">Released {date}. Verify each download with its SHA-256 sum.</p>

  <a class="docs" href="https://strangeeons.cgjennings.ca/">Documentation (upstream)</a>
</body>
</html>
"""


def write_index(output_dir: Path, version: str, build: int,
                release_type: str, when: datetime.datetime, installers):
    rows = []
    for it in installers:
        rows.append(
            f"      <tr>"
            f"<td>{it['label']}</td>"
            f"<td><a href=\"{it['filename']}\">{it['filename']}</a></td>"
            f"<td>{human_size(it['size'])}</td>"
            f"<td><code>{it['sha256']}</code></td>"
            f"</tr>"
        )
    html = INDEX_TEMPLATE.format(
        version=version,
        build=build,
        type_label=release_type.lower(),
        date=when.strftime("%Y-%m-%d"),
        rows="\n".join(rows) if rows else "      <tr><td colspan=4>No installers found.</td></tr>",
    )
    (output_dir / "index.html").write_text(html, encoding="utf-8")
    print(f"  index:   index.html ({len(installers)} installer{'s' if len(installers) != 1 else ''})")


def main():
    ap = argparse.ArgumentParser(description=__doc__)
    ap.add_argument("--artifacts-dir", required=True, type=Path)
    ap.add_argument("--output-dir",    required=True, type=Path)
    ap.add_argument("--version",       required=True, help="e.g. 3.5.0")
    ap.add_argument("--build",         required=True, type=int)
    ap.add_argument("--type",          required=True,
                    choices=["GENERAL", "ALPHA", "BETA", "DEVELOPMENT"])
    ap.add_argument("--channel",       required=True,
                    choices=["stable", "experimental"])
    args = ap.parse_args()

    when = datetime.datetime.now(datetime.timezone.utc).replace(microsecond=0)
    print(f"Building deploy bundle for {args.version} build {args.build} ({args.type}, {args.channel})")
    installers = collect(args.artifacts_dir, args.output_dir, args.version)
    write_catalog(args.output_dir, args.version, args.build, args.channel, when)
    write_index(args.output_dir, args.version, args.build, args.type, when, installers)
    print(f"Done. Output: {args.output_dir}")


if __name__ == "__main__":
    main()
