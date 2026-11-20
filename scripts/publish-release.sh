#!/usr/bin/env bash
# Oculo: publish the current version (smarttubetv/oculo.gradle « oculoVersion ») as a GitHub Release, the source of
# the in-app updater (OCULO.md « In-app updates » and « Releases »).
#
#   scripts/publish-release.sh            # build, check, tag v<version>, create the release, verify the download URLs
#   scripts/publish-release.sh --dry-run  # build, check and write the manifest only (no tag, no release)
#
# Refuses when: not on branch oculo, tracked changes, HEAD not pushed to origin/oculo, submodules not at their pinned
# commits, tag v<version> or its release already exists, no CHANGELOG.md section for the version, no signing config,
# an APK not signed with the Oculo key or not carrying the expected package / versionCode.
set -euo pipefail

REPO="francoisauger/oculo"
BRANCH="oculo"
PACKAGE="com.francoisauger.oculo"
ABIS=(armeabi-v7a arm64-v8a x86)
MANIFEST_NAME="oculo_stable.json"

DRY_RUN=0
for arg in "$@"; do
    case "$arg" in
        --dry-run) DRY_RUN=1 ;;
        -h|--help) sed -n '2,11p' "$0"; exit 0 ;;
        *) echo "Unknown argument: $arg" >&2; exit 2 ;;
    esac
done

die() { echo "publish-release: $*" >&2; exit 1; }
step() { echo; echo "== $*"; }

ROOT="$(git rev-parse --show-toplevel)"
cd "$ROOT"

# --- Preconditions -----------------------------------------------------------------------------------------------
step "Checks"
[ "$(git branch --show-current)" = "$BRANCH" ] || die "not on branch $BRANCH"
[ -z "$(git status --porcelain --untracked-files=no)" ] || die "tracked changes in the working tree; commit them first"
if git submodule status | grep -q '^[-+U]'; then
    die "submodules not at their pinned commits (git submodule update --init --recursive)"
fi
git fetch -q origin "$BRANCH"
[ "$(git rev-parse HEAD)" = "$(git rev-parse "origin/$BRANCH")" ] || die "HEAD is not pushed to origin/$BRANCH"

VERSION="$(sed -n "s/^def oculoVersion = '\([0-9]*\.[0-9]*\.[0-9]*\)'.*/\1/p" smarttubetv/oculo.gradle)"
[ -n "$VERSION" ] || die "cannot read oculoVersion from smarttubetv/oculo.gradle"
TAG="v$VERSION"
echo "Version $VERSION, tag $TAG, commit $(git rev-parse --short HEAD)"

if [ "$DRY_RUN" = 0 ]; then
    git rev-parse -q --verify "refs/tags/$TAG" >/dev/null && die "tag $TAG already exists locally"
    [ -z "$(git ls-remote --tags origin "refs/tags/$TAG")" ] || die "tag $TAG already exists on origin"
    gh release view "$TAG" --repo "$REPO" >/dev/null 2>&1 && die "release $TAG already exists on GitHub"
fi

SIGNING="${OCULO_SIGNING_CONFIG:-$HOME/.config/chuck/oculo/signing.json}"
[ -f "$SIGNING" ] || die "no signing config at $SIGNING (OCULO.md « Signing »)"

SDK_DIR="$(sed -n 's/^sdk.dir=//p' local.properties 2>/dev/null || true)"
SDK_DIR="${SDK_DIR:-${ANDROID_HOME:-$HOME/Library/Android/sdk}}"
BUILD_TOOLS="$(ls -d "$SDK_DIR"/build-tools/* | sort -V | tail -1)"
APKSIGNER="$BUILD_TOOLS/apksigner"
AAPT2="$BUILD_TOOLS/aapt2"
[ -x "$APKSIGNER" ] && [ -x "$AAPT2" ] || die "apksigner / aapt2 not found in $BUILD_TOOLS"

# Changelog bullets of every CHANGELOG.md section up to this version (the updater shows all the entries newer than
# the installed version, so someone skipping versions sees everything).
OUT="smarttubetv/build/oculo-release/$TAG"
rm -rf "$OUT"
mkdir -p "$OUT"
python3 - "$VERSION" "$OUT/changelog.json" <<'PY'
import json, re, sys
version, out = sys.argv[1], sys.argv[2]
key = lambda v: tuple(int(x) for x in v.split('.'))
sections, current = {}, None
for line in open('CHANGELOG.md', encoding='utf-8'):
    m = re.match(r'^## (\d+\.\d+\.\d+)\s*$', line)
    if m:
        current = m.group(1)
        sections[current] = []
    elif re.match(r'^#', line):
        current = None if line.startswith('# ') or line.startswith('## ') else current
    elif current and line.startswith('- '):
        sections[current].append(line[2:].strip())
if not sections.get(version):
    sys.exit(f'publish-release: no bullets under « ## {version} » in CHANGELOG.md')
kept = {v: b for v, b in sections.items() if key(v) <= key(version) and b}
json.dump(kept, open(out, 'w', encoding='utf-8'), ensure_ascii=False, indent=2)
PY

# --- Build -------------------------------------------------------------------------------------------------------
step "Build (assembleOculoRelease)"
export JAVA_HOME="${JAVA_HOME_17:-/opt/homebrew/opt/openjdk@17}"
./gradlew -q :smarttubetv:assembleOculoRelease
APK_DIR="smarttubetv/build/outputs/apk/oculo/release"

# --- Check the APKs ----------------------------------------------------------------------------------------------
step "APK checks"
# Expected signer: the Oculo release key's certificate (SHA-256 of the DER), read from the keystore in place.
EXPECTED_CERT="$(python3 - "$SIGNING" <<'PY'
import json, os, subprocess, hashlib, sys
c = json.load(open(sys.argv[1]))
env = dict(os.environ, OCULO_STOREPASS=c['storePassword'])
der = subprocess.run(['keytool', '-exportcert', '-keystore', c['storeFile'], '-alias', c['keyAlias'],
                      '-storepass:env', 'OCULO_STOREPASS'], env=env, check=True, capture_output=True).stdout
print(hashlib.sha256(der).hexdigest())
PY
)"
[ -n "$EXPECTED_CERT" ] || die "cannot read the release certificate"

EXPECTED_CODE="$(python3 -c "import sys; a,b,c=map(int,sys.argv[1].split('.')); print(1000000+a*1000000+b*1000+c)" "$VERSION")"
: > "$OUT/SHA256SUMS.txt"
for abi in "${ABIS[@]}"; do
    name="Oculo_${VERSION}_${abi}.apk"
    src="$APK_DIR/$name"
    [ -f "$src" ] || die "missing $src"
    cert="$("$APKSIGNER" verify --print-certs "$src" 2>/dev/null | sed -n 's/^.*Signer.*certificate SHA-256 digest: //p' | sort -u)"
    [ "$cert" = "$EXPECTED_CERT" ] || die "$name is not signed with the Oculo release key"
    badging="$("$AAPT2" dump badging "$src" | sed -n 1p)"
    echo "$badging" | grep -q "name='$PACKAGE' versionCode='$EXPECTED_CODE' versionName='$VERSION'" \
        || die "$name: unexpected package line: $badging"
    cp "$src" "$OUT/$name"
    (cd "$OUT" && shasum -a 256 "$name" >> SHA256SUMS.txt)
    echo "ok  $name  $(du -h "$src" | cut -f1)"
done

# --- Manifest (SmartTube updater format, OCULO.md « In-app updates ») -------------------------------------------
step "Manifest $MANIFEST_NAME"
python3 - "$REPO" "$TAG" "$VERSION" "$OUT" "${ABIS[@]}" <<'PY'
import json, sys
repo, tag, version, out, *abis = sys.argv[1:]
base = f'https://github.com/{repo}/releases/download/{tag}'
url = lambda abi: f'{base}/Oculo_{version}_{abi}.apk'
code = lambda v: (lambda a, b, c: 1000000 + a * 1000000 + b * 1000 + c)(*map(int, v.split('.')))
package = {'downloadUrlList': [url('armeabi-v7a')]}  # fallback for an ABI without its own key: 32-bit ARM
for abi in abis:
    package[f'downloadUrlList_{abi}'] = [url(abi)]
if 'x86' in abis:
    package['downloadUrlList_x86_64'] = [url('x86')]  # x86_64 devices run the x86 APK
manifest = {'package': package}
changelog = json.load(open(f'{out}/changelog.json', encoding='utf-8'))
for v in sorted(changelog, key=lambda v: tuple(map(int, v.split('.'))), reverse=True):
    manifest[v] = {'versionCode': code(v), 'changelog': changelog[v]}
json.dump(manifest, open(f'{out}/oculo_stable.json', 'w', encoding='utf-8'), ensure_ascii=False, indent=2)
with open(f'{out}/notes.md', 'w', encoding='utf-8') as f:
    f.write('\n'.join(f'- {b}' for b in changelog[version]) + '\n\n')
    f.write(f'APK : `Oculo_{version}_armeabi-v7a.apk` (Android TV 32 bits, dont la TCL), `arm64-v8a`, `x86`. '
            f'Oculo se met à jour tout seul à partir de ce manifeste (`oculo_stable.json`).\n')
PY
python3 -m json.tool "$OUT/$MANIFEST_NAME" >/dev/null
cat "$OUT/$MANIFEST_NAME"

if [ "$DRY_RUN" = 1 ]; then
    step "Dry run: nothing tagged or published. Files in $OUT"
    exit 0
fi

# --- Tag and release ---------------------------------------------------------------------------------------------
step "Tag $TAG and GitHub Release"
git tag -a "$TAG" -m "Oculo $VERSION"
git push -q origin "refs/tags/$TAG"
assets=("$OUT/$MANIFEST_NAME" "$OUT/SHA256SUMS.txt")
for abi in "${ABIS[@]}"; do assets+=("$OUT/Oculo_${VERSION}_${abi}.apk"); done
gh release create "$TAG" --repo "$REPO" --verify-tag --latest --title "Oculo $VERSION" \
    --notes-file "$OUT/notes.md" "${assets[@]}"

# --- Verify what the TVs will download ---------------------------------------------------------------------------
step "Verification (releases/latest/download, redirects followed)"
VERIFY="$OUT/verify"
mkdir -p "$VERIFY"
LATEST="https://github.com/$REPO/releases/latest/download/$MANIFEST_NAME"
fetch() { # url dest: retries while GitHub settles the new release
    local i
    for i in 1 2 3 4 5 6 7 8 9 10 11 12; do
        if curl -sSfL --retry 2 -o "$2" -w '%{http_code}' "$1" | grep -q '^200$'; then return 0; fi
        sleep 5
    done
    return 1
}
fetch "$LATEST" "$VERIFY/$MANIFEST_NAME" || die "$LATEST does not answer 200"
cmp -s "$VERIFY/$MANIFEST_NAME" "$OUT/$MANIFEST_NAME" || die "$LATEST is not this release's manifest"
echo "ok  $LATEST"
for abi in "${ABIS[@]}"; do
    name="Oculo_${VERSION}_${abi}.apk"
    url="$(python3 -c "import json,sys; print(json.load(open(sys.argv[1]))['package']['downloadUrlList_'+sys.argv[2]][0])" \
        "$VERIFY/$MANIFEST_NAME" "$abi")"
    fetch "$url" "$VERIFY/$name" || die "$url does not answer 200"
    [ "$(shasum -a 256 < "$VERIFY/$name" | cut -d' ' -f1)" = "$(shasum -a 256 < "$OUT/$name" | cut -d' ' -f1)" ] \
        || die "$name downloaded from GitHub differs from the local build"
    echo "ok  $url (sha256 matches)"
done
echo
echo "Hosts in the redirect chain (what a TV must be able to reach):"
{ curl -sSIL "$LATEST"; curl -sSIL "$(python3 -c "import json,sys; print(json.load(open(sys.argv[1]))['package']['downloadUrlList'][0])" "$VERIFY/$MANIFEST_NAME")"; } \
    | sed -n 's/^[Ll]ocation: *https\{0,1\}:\/\/\([^/]*\).*/\1/p' | { cat; echo github.com; } | sort -u | sed 's/^/  /'
rm -rf "$VERIFY"
echo
echo "Published: https://github.com/$REPO/releases/tag/$TAG"
cat "$OUT/SHA256SUMS.txt"
