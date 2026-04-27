#!/usr/bin/env python3
"""
Add or update a plugin entry in manifest.json.

Pulls a .seext bundle from a URL, extracts catalog metadata from its
embedded eons-plugin properties file, computes size + checksums, and
merges an entry into the manifest's `plugins` map keyed by the
plugin's CATALOGUEID UUID.

Typical usage:

    curl -fsSL https://strangeeons.fizmo.org/manifest.json -o manifest.json

    python3 add-plugin.py \\
        --url https://github.com/tokeeto/StrangeEonsAHLCG/raw/refs/heads/master/ArkhamHorrorLCG/ArkhamHorrorLCG.seext \\
        --filename ArkhamHorrorLCG.seext \\
        --manifest manifest.json \\
        --bundle-output upload/updates/ArkhamHorrorLCG.seext

    python3 build_deploy_bundle.py \\
        --output-dir upload \\
        --regenerate-catalog-only \\
        --existing-manifest manifest.json

    cp manifest.json upload/manifest.json
    rsync -avz upload/ strangeeons@opal20.opalstack.com:~/apps/strangeeons/

The manifest is updated in place. The bundle is written to --bundle-output
so it can be rsynced alongside.
"""
import argparse
import hashlib
import json
import re
import sys
import urllib.request
import zipfile
from pathlib import Path


def fetch_bundle(url: str, dest: Path) -> Path:
    print(f"Fetching {url}")
    dest.parent.mkdir(parents=True, exist_ok=True)
    with urllib.request.urlopen(url) as resp, dest.open("wb") as f:
        while True:
            chunk = resp.read(1 << 16)
            if not chunk:
                break
            f.write(chunk)
    print(f"  wrote {dest} ({dest.stat().st_size} bytes)")
    return dest


def hash_file(path: Path):
    sha = hashlib.sha256()
    md5 = hashlib.md5()
    with path.open("rb") as f:
        for chunk in iter(lambda: f.read(1 << 16), b""):
            sha.update(chunk)
            md5.update(chunk)
    return sha.hexdigest(), md5.hexdigest()


def extract_eons_plugin(seext: Path) -> str:
    """Read the eons-plugin properties file from the bundle's root."""
    with zipfile.ZipFile(seext) as z:
        return z.read("eons-plugin").decode("utf-8")


_CATALOG_ID_RE = re.compile(
    r'^id\s*[=:]\s*(CATALOGUEID\{([0-9a-fA-F-]+):[\d-]+\})', re.M)


def parse_catalog_id(eons_plugin: str):
    """Returns (full CATALOGUEID string, lowercase UUID)."""
    m = _CATALOG_ID_RE.search(eons_plugin)
    if not m:
        raise ValueError("no CATALOGUEID found in eons-plugin file")
    return m.group(1), m.group(2).lower()


_KEY_RE = re.compile(r'^(\s*)catalog-(\S+?)(\s*[=:]\s*)(.*)$')


def extract_catalog_block(eons_plugin: str) -> str:
    """Extract `catalog-*` lines (and their continuation lines) from the
    eons-plugin file with the `catalog-` prefix stripped from each key.

    Values, line continuations, escapes, and localized variants
    (catalog-name_en_US, etc.) are preserved verbatim. The catalog parser
    is the same Properties-style parser as the bundle's, so what we copy
    out reads back identically.
    """
    out = []
    lines = eons_plugin.splitlines()
    i = 0
    while i < len(lines):
        line = lines[i]
        m = _KEY_RE.match(line)
        if m:
            prefix, key, sep, rest = m.groups()
            out.append(f"{prefix}{key}{sep}{rest}")
            # Consume continuation lines verbatim (a `\` at end-of-line, not
            # itself escaped, indicates continuation).
            while lines[i].rstrip().endswith("\\") and not lines[i].rstrip().endswith("\\\\"):
                i += 1
                if i < len(lines):
                    out.append(lines[i])
            i += 1
        else:
            i += 1
    return "\n".join(out)


def main():
    ap = argparse.ArgumentParser(
        description=__doc__,
        formatter_class=argparse.RawDescriptionHelpFormatter,
    )
    ap.add_argument("--url",            required=True,
                    help="URL of the .seext bundle to fetch.")
    ap.add_argument("--filename",       required=True,
                    help="Filename to publish on the server "
                         "(e.g. ArkhamHorrorLCG.seext). The catalog listing "
                         "uses this as a relative URL alongside catalog.txt.")
    ap.add_argument("--manifest",       required=True, type=Path,
                    help="Path to manifest.json to update in place.")
    ap.add_argument("--bundle-output",  required=True, type=Path,
                    help="Where to write the downloaded bundle locally for "
                         "subsequent rsync upload.")
    args = ap.parse_args()

    fetch_bundle(args.url, args.bundle_output)
    sha256, md5 = hash_file(args.bundle_output)
    eons_plugin = extract_eons_plugin(args.bundle_output)
    catalog_id, uuid = parse_catalog_id(eons_plugin)
    catalog_block = extract_catalog_block(eons_plugin)
    if not catalog_block:
        print("warn: no catalog-* keys found in eons-plugin; "
              "catalog entry will only have id/url/size/md5.", file=sys.stderr)
    size = args.bundle_output.stat().st_size

    manifest = {}
    if args.manifest.exists():
        manifest = json.loads(args.manifest.read_text(encoding="utf-8"))
    plugins = manifest.setdefault("plugins", {})

    plugins[uuid] = {
        "filename": args.filename,
        "size": size,
        "md5": md5,
        "sha256": sha256,
        "catalog_id": catalog_id,
        "catalog_block": catalog_block,
    }

    args.manifest.write_text(
        json.dumps(manifest, indent=2, sort_keys=True) + "\n",
        encoding="utf-8",
    )

    print(f"Plugin {uuid} added/updated in {args.manifest}.")
    print(f"  filename:   {args.filename}")
    print(f"  size:       {size}")
    print(f"  md5:        {md5}")
    print(f"  sha256:     {sha256}")
    print(f"  catalog_id: {catalog_id}")


if __name__ == "__main__":
    main()
