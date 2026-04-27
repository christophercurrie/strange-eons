#!/usr/bin/env python3
"""
Assemble the release deploy bundle from CI build artifacts.

Reads installers downloaded from the build matrix (one subdirectory per
matrix entry) and produces an output tree mirroring what the OpalStack
host should serve:

    <output>/
        index.html                          downloads landing page
        manifest.json                       per-channel published-build state
        strange-eons-<ver>[-<suffix>]-...   installers (channel-aware naming)
        updates/
            catalog.txt                     update catalog (this build only;
                                            cross-channel merge tracked in #39)

Stable releases use bare version filenames (strange-eons-3.5.0-...).
Pre-release builds carry the tag suffix in the filename
(strange-eons-3.5.0-test3-...) so multiple in-flight pre-releases coexist.

The deploy job pulls the existing manifest.json from the server before
running this script and passes it via --existing-manifest. The script
replaces this channel's entry in the manifest, preserving the other
channel, and writes the merged manifest into the output for upload.

The output directory is rsynced to ~/apps/strangeeons/ on the OpalStack
host as the final deploy step (without --delete-after, so the other
channel's installers stay put).
"""
import argparse
import datetime
import hashlib
import json
import shutil
import sys
from pathlib import Path

CHANNEL_UUIDS = {
    "stable":       "c8d1620e-5eeb-47f4-9ef2-49e9947faa90",
    "experimental": "1b7ef4bd-f63a-4884-9979-830d4feb18b8",
}

# Map (artifact-dir-name, file-suffix) -> (canonical filename template, label).
# artifact-dir-name matches the matrix `artifact:` field in release.yml.
# Templates use {ver} and {dash_suffix} placeholders. dash_suffix expands to
# either "" (stable) or "-<tag-suffix>" (pre-release).
INSTALLER_RULES = [
    # macOS
    (("strange-eons-macos-aarch64", ".dmg"),
     "strange-eons-{ver}{dash_suffix}-macos-aarch64.dmg",
     "macOS (Apple Silicon)"),
    (("strange-eons-macos-x64", ".dmg"),
     "strange-eons-{ver}{dash_suffix}-macos-x64.dmg",
     "macOS (Intel)"),
    # Windows
    (("strange-eons-windows", ".msi"),
     "strange-eons-{ver}{dash_suffix}-windows.msi",
     "Windows"),
    # Linux
    (("strange-eons-linux", ".deb"),
     "strange-eons-{ver}{dash_suffix}.deb",
     "Linux (Debian/Ubuntu, .deb)"),
    (("strange-eons-linux", ".rpm"),
     "strange-eons-{ver}{dash_suffix}.rpm",
     "Linux (RHEL/Fedora, .rpm)"),
    (("strange-eons-linux", ".tar.gz"),
     "strange-eons-{ver}{dash_suffix}-linux.tar.gz",
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
    candidates = sorted(base.rglob(f"*{suffix}"))
    return candidates[0] if candidates else None


def collect(artifacts_dir: Path, output_dir: Path, version: str, dash_suffix: str):
    output_dir.mkdir(parents=True, exist_ok=True)
    collected = []
    for (dirname, suffix), name_tmpl, label in INSTALLER_RULES:
        src = find_installer(artifacts_dir, dirname, suffix)
        if src is None:
            print(f"warn: no {suffix} found under {dirname}", file=sys.stderr)
            continue
        dest_name = name_tmpl.format(ver=version, dash_suffix=dash_suffix)
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
    # hidden = yes keeps the listing out of CatalogDialog (which iterates
    # catalog.size(), excluding hidden entries) while leaving it visible to
    # AutomaticUpdater.findListingByUUID, which scans the full list.
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
        f"hidden = yes\n"
    )
    cat.write_text(body, encoding="utf-8")
    print(f"  catalog: {cat.relative_to(output_dir)}")


def load_existing_manifest(path):
    if path is None or not Path(path).exists():
        return {}
    try:
        return json.loads(Path(path).read_text(encoding="utf-8"))
    except Exception as e:
        print(f"warn: failed to parse existing manifest ({e}); starting fresh", file=sys.stderr)
        return {}


def merge_manifest(existing: dict, channel: str, entry: dict) -> dict:
    out = dict(existing)
    out[channel] = entry
    return out


def write_manifest(output_dir: Path, manifest: dict):
    (output_dir / "manifest.json").write_text(
        json.dumps(manifest, indent=2, sort_keys=True) + "\n",
        encoding="utf-8",
    )
    print(f"  manifest: manifest.json ({len(manifest)} channel{'s' if len(manifest) != 1 else ''})")


INDEX_TEMPLATE = """<!doctype html>
<html lang="en">
<head>
  <meta charset="utf-8">
  <title>Strange Eons</title>
  <meta name="viewport" content="width=device-width, initial-scale=1">
  <style>
    body {{ font-family: system-ui, -apple-system, sans-serif; max-width: 48rem; margin: 2rem auto; padding: 0 1rem; line-height: 1.5; }}
    h1 {{ margin-bottom: 0.25rem; }}
    h2 {{ margin-top: 2rem; }}
    .ver {{ color: #666; margin-bottom: 0.5rem; }}
    table {{ border-collapse: collapse; width: 100%; margin-top: 0.5rem; }}
    th, td {{ text-align: left; padding: 0.5rem; border-bottom: 1px solid #eee; vertical-align: top; }}
    th {{ background: #f7f7f7; }}
    code {{ font-family: ui-monospace, monospace; font-size: 0.85em; word-break: break-all; }}
    .note {{ color: #666; font-size: 0.9em; }}
    .empty {{ color: #888; font-style: italic; }}
    a.docs {{ display: inline-block; margin-top: 1rem; }}
  </style>
</head>
<body>
  <h1>Strange Eons</h1>
{sections}
  <a class="docs" href="https://strangeeons.cgjennings.ca/">Documentation (upstream)</a>
</body>
</html>
"""

SECTION_TEMPLATE = """  <h2>{heading}</h2>
  <p class="ver">Version {version_label} &middot; build {build} &middot; released {date}</p>
  <table>
    <thead><tr><th>Platform</th><th>File</th><th>Size</th><th>SHA-256</th></tr></thead>
    <tbody>
{rows}
    </tbody>
  </table>
"""

EMPTY_SECTION_TEMPLATE = """  <h2>{heading}</h2>
  <p class="empty">No {heading_lower} release published yet.</p>
"""


def render_section(heading: str, entry: dict | None) -> str:
    if not entry or not entry.get("files"):
        return EMPTY_SECTION_TEMPLATE.format(
            heading=heading,
            heading_lower=heading.lower(),
        )
    rows = []
    for it in entry["files"]:
        rows.append(
            f"      <tr>"
            f"<td>{it['label']}</td>"
            f"<td><a href=\"{it['filename']}\">{it['filename']}</a></td>"
            f"<td>{human_size(it['size'])}</td>"
            f"<td><code>{it['sha256']}</code></td>"
            f"</tr>"
        )
    suffix = entry.get("suffix") or ""
    version_label = f"{entry['version']}{('-' + suffix) if suffix else ''}"
    return SECTION_TEMPLATE.format(
        heading=heading,
        version_label=version_label,
        build=entry["build"],
        date=entry["released"],
        rows="\n".join(rows),
    )


def write_index(output_dir: Path, manifest: dict):
    sections = (
        render_section("Stable", manifest.get("stable"))
        + render_section("Pre-release", manifest.get("experimental"))
    )
    html = INDEX_TEMPLATE.format(sections=sections)
    (output_dir / "index.html").write_text(html, encoding="utf-8")
    stable_n = len(manifest.get("stable", {}).get("files", []))
    exp_n = len(manifest.get("experimental", {}).get("files", []))
    print(f"  index: index.html (stable: {stable_n}, pre-release: {exp_n})")


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
    ap.add_argument("--suffix",        default="",
                    help="Tag suffix (e.g. alpha, test3). Ignored for stable channel.")
    ap.add_argument("--existing-manifest", type=Path, default=None,
                    help="Path to existing manifest.json from server, for cross-channel merge.")
    args = ap.parse_args()

    when = datetime.datetime.now(datetime.timezone.utc).replace(microsecond=0)
    # Stable releases never carry a suffix in filenames; pre-release builds do.
    suffix = "" if args.channel == "stable" else args.suffix
    dash_suffix = f"-{suffix}" if suffix else ""

    print(f"Building deploy bundle for {args.version}{dash_suffix} build {args.build} ({args.type}, {args.channel})")

    installers = collect(args.artifacts_dir, args.output_dir, args.version, dash_suffix)

    existing = load_existing_manifest(args.existing_manifest)
    entry = {
        "version": args.version,
        "build": args.build,
        "type": args.type,
        "released": when.strftime("%Y-%m-%d"),
        "suffix": suffix,
        "files": installers,
    }
    manifest = merge_manifest(existing, args.channel, entry)

    write_manifest(args.output_dir, manifest)
    write_catalog(args.output_dir, args.version, args.build, args.channel, when)
    write_index(args.output_dir, manifest)
    print(f"Done. Output: {args.output_dir}")


if __name__ == "__main__":
    main()
