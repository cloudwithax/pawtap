#!/usr/bin/env bash
# Builds a signed release APK. Needs keystore.properties in the repo root (not committed).
set -e
cd "$(dirname "$0")"
./gradlew assembleRelease -q
cp app/build/outputs/apk/release/app-release.apk pawtap-release.apk
echo "-> pawtap-release.apk"
