#!/usr/bin/env bash
#
# Sign macOS native libraries (.dylib/.jnilib) embedded inside a JAR with the
# given Developer ID identity, then re-inject them. Apple notarization scans
# inside JARs and rejects any unsigned Mach-O it finds; jpackage does not
# descend into JARs, so this runs between maven-shade and jpackage in the
# build pipeline.
#
# Usage:
#   sign-jar-natives.sh <jar-path> <signing-identity-fragment> <entitlements-plist>
#
# No-op when:
#   - the host is not macOS (Linux/Windows runners can call this freely);
#   - the signing identity argument is empty (signing not requested).
#
# Idempotent: re-signing already-signed binaries is safe because codesign
# --force replaces any existing signature.
#
set -euo pipefail

if [[ $# -ne 3 ]]; then
    echo "usage: $(basename "$0") <jar-path> <signing-identity-fragment> <entitlements-plist>" >&2
    exit 64
fi

JAR_PATH="$1"
SIGNING_FRAGMENT="$2"
ENTITLEMENTS="$3"

# Skip silently on non-Mac hosts: the deploy profile that calls us is shared
# with Linux/Windows runners, and there are no Mach-O natives to sign there.
if [[ "$(uname)" != "Darwin" ]]; then
    exit 0
fi

# Skip silently when no signing identity was requested. Two cases:
#   - Empty string (the property was declared empty somewhere, or the user
#     explicitly passed -Dstrange-eons.apple.signing.identity= ).
#   - Literal "${strange-eons.apple.signing.identity}" — Maven's antrun
#     forwards an unresolved property reference verbatim when the property
#     is not defined at all, and we deliberately don't declare a default to
#     keep the jpackage-mac-sign profile from auto-activating without -D.
case "$SIGNING_FRAGMENT" in
    ''|'${strange-eons.apple.signing.identity}')
        exit 0
        ;;
esac

if [[ ! -f "$JAR_PATH" ]]; then
    echo "error: jar not found at $JAR_PATH" >&2
    exit 66
fi
if [[ ! -f "$ENTITLEMENTS" ]]; then
    echo "error: entitlements not found at $ENTITLEMENTS" >&2
    exit 66
fi

# jpackage's --mac-signing-key-user-name takes the fragment; codesign wants the
# full canonical identity name. Resolve "Developer ID Application: <fragment>"
# in the keychain so the user can pass either form.
if [[ "$SIGNING_FRAGMENT" == "Developer ID Application: "* ]]; then
    CODESIGN_IDENTITY="$SIGNING_FRAGMENT"
else
    CODESIGN_IDENTITY="Developer ID Application: $SIGNING_FRAGMENT"
fi

# Collect the dylibs/jnilibs to sign. Skip non-mac natives (.so, .dll).
DYLIB_ENTRIES=$(unzip -Z1 "$JAR_PATH" 2>/dev/null \
    | grep -E '\.(dylib|jnilib)$' || true)

if [[ -z "$DYLIB_ENTRIES" ]]; then
    echo "no .dylib/.jnilib entries in $JAR_PATH; nothing to sign"
    exit 0
fi

WORK_DIR=$(mktemp -d -t sign-jar-natives.XXXXXX)
trap 'rm -rf "$WORK_DIR"' EXIT

ABS_JAR=$(cd "$(dirname "$JAR_PATH")" && pwd)/$(basename "$JAR_PATH")
ABS_ENT=$(cd "$(dirname "$ENTITLEMENTS")" && pwd)/$(basename "$ENTITLEMENTS")

cd "$WORK_DIR"

# Extract each native, sign in place, then update the jar entry.
# Updating one path at a time keeps directory layout intact inside the jar.
while IFS= read -r entry; do
    [[ -z "$entry" ]] && continue
    echo "::group::sign $entry"
    unzip -q -o "$ABS_JAR" "$entry"
    codesign --force \
        --options runtime \
        --timestamp \
        --entitlements "$ABS_ENT" \
        --sign "$CODESIGN_IDENTITY" \
        "$entry"
    codesign --verify --strict --verbose=2 "$entry"
    # `jar uf` preserves the entry path relative to the jar root.
    jar uf "$ABS_JAR" "$entry"
    rm -f "$entry"
    echo "::endgroup::"
done <<< "$DYLIB_ENTRIES"

echo "signed $(echo "$DYLIB_ENTRIES" | grep -c .) native binar(y|ies) in $(basename "$JAR_PATH")"
