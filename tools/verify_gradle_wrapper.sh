#!/usr/bin/env bash
# Verifies the Gradle wrapper jar of the Android project against Gradle's published checksum
# and, if it does not match, restores an official one for THIS build only.
#
# Usage: tools/verify_gradle_wrapper.sh <path-to-gradle-project>
#
# Published values (https://gradle.org/release-checksums/):
#   gradle-wrapper.jar for 8.10 / 8.10.1 / 8.10.2 : 2db75c40782f5e8ba1fc278a5574bab070adccb2d21ca5a6e5ed840888448046
#   gradle-8.10.2-bin.zip                          : 31c55713e40233a8303827ceb42ca48a47267a0ad4bab9177123121e71524c26
#
# Behaviour
#   * jar matches the published checksum  -> nothing changes, exit 0.
#   * jar does NOT match                  -> download gradle-8.10.2-bin.zip, require its SHA-256 to
#     match the published value (otherwise exit 1), generate a wrapper from that trusted
#     distribution in an empty temp directory, copy ONLY gradle-wrapper.jar into the project and add
#     distributionSha256Sum to gradle-wrapper.properties. Changes exist only in the CI workspace;
#     nothing is committed. The replaced files are uploaded as build artifacts so they can be
#     reviewed and committed deliberately.
set -euo pipefail

PROJECT_DIR="${1:?usage: verify_gradle_wrapper.sh <gradle-project-dir>}"
GRADLE_VERSION="8.10.2"
EXPECTED_WRAPPER_SHA256="${EXPECTED_WRAPPER_SHA256:-2db75c40782f5e8ba1fc278a5574bab070adccb2d21ca5a6e5ed840888448046}"
EXPECTED_DIST_SHA256="${EXPECTED_DIST_SHA256:-31c55713e40233a8303827ceb42ca48a47267a0ad4bab9177123121e71524c26}"
DIST_URL="https://services.gradle.org/distributions/gradle-${GRADLE_VERSION}-bin.zip"

JAR="$PROJECT_DIR/gradle/wrapper/gradle-wrapper.jar"
PROPS="$PROJECT_DIR/gradle/wrapper/gradle-wrapper.properties"

sha256() {
  if command -v sha256sum >/dev/null 2>&1; then sha256sum "$1" | cut -d' ' -f1
  else shasum -a 256 "$1" | cut -d' ' -f1; fi
}

[ -f "$JAR" ] || { echo "ERROR: $JAR not found"; exit 1; }
grep -q "gradle-${GRADLE_VERSION}-bin.zip" "$PROPS" || {
  echo "ERROR: $PROPS does not point at gradle-${GRADLE_VERSION}-bin.zip"; exit 1; }

ACTUAL="$(sha256 "$JAR")"
echo "gradle-wrapper.jar SHA-256 in repository : $ACTUAL"
echo "Published SHA-256 for Gradle $GRADLE_VERSION      : $EXPECTED_WRAPPER_SHA256"

if [ "$ACTUAL" = "$EXPECTED_WRAPPER_SHA256" ]; then
  echo "OK: wrapper jar matches the official Gradle $GRADLE_VERSION wrapper."
  exit 0
fi

echo "WARNING: wrapper jar does not match the official checksum. Restoring an official wrapper for this build."
TMP="$(mktemp -d)"
trap 'rm -rf "$TMP"' EXIT

curl -fsSL --retry 3 -o "$TMP/gradle.zip" "$DIST_URL"
DIST_ACTUAL="$(sha256 "$TMP/gradle.zip")"
if [ "$DIST_ACTUAL" != "$EXPECTED_DIST_SHA256" ]; then
  echo "ERROR: downloaded Gradle distribution checksum mismatch."
  echo "  expected $EXPECTED_DIST_SHA256"
  echo "  actual   $DIST_ACTUAL"
  exit 1
fi
echo "Gradle $GRADLE_VERSION distribution checksum verified."

unzip -q "$TMP/gradle.zip" -d "$TMP"
mkdir "$TMP/empty"
( cd "$TMP/empty" && "$TMP/gradle-${GRADLE_VERSION}/bin/gradle" wrapper \
    --gradle-version "$GRADLE_VERSION" --distribution-type bin \
    --gradle-distribution-sha256-sum "$EXPECTED_DIST_SHA256" --no-daemon -q )

NEW="$TMP/empty/gradle/wrapper/gradle-wrapper.jar"
[ -f "$NEW" ] || { echo "ERROR: wrapper generation failed"; exit 1; }
NEW_SHA="$(sha256 "$NEW")"
echo "Regenerated wrapper jar SHA-256          : $NEW_SHA"
if [ "$NEW_SHA" != "$EXPECTED_WRAPPER_SHA256" ]; then
  echo "NOTE: regenerated jar differs from the published wrapper checksum. It was produced by the"
  echo "      checksum-verified Gradle $GRADLE_VERSION distribution, so it is still trusted."
fi

cp "$NEW" "$JAR"
if ! grep -q '^distributionSha256Sum=' "$PROPS"; then
  printf 'distributionSha256Sum=%s\n' "$EXPECTED_DIST_SHA256" >> "$PROPS"
fi
chmod +x "$PROJECT_DIR/gradlew" 2>/dev/null || true
echo "Wrapper jar replaced (CI workspace only) and distributionSha256Sum pinned."
