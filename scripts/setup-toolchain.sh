#!/usr/bin/env bash
# Recreates the toolchain used to build the APK (JDK 17 + Android SDK 34 + Gradle 8.7).
set -euo pipefail
cd "$(dirname "$0")/.."
mkdir -p .tooling && cd .tooling

if [ ! -d jdk-17.0.21+9 ]; then
  curl -sL -o jdk17.tar.gz "https://api.adoptium.net/v3/binary/latest/17/ga/linux/x64/jdk/hotspot/normal/eclipse"
  tar xzf jdk17.tar.gz && rm jdk17.tar.gz
fi

if [ ! -d sdk/platforms ]; then
  curl -sL -o cmdline.zip "https://dl.google.com/android/repository/commandlinetools-linux-11076708_latest.zip"
  python3 -c "import zipfile;zipfile.ZipFile('cmdline.zip').extractall('.')"
  mkdir -p sdk/cmdline-tools && mv cmdline-tools sdk/cmdline-tools/latest
  chmod -R +x sdk/cmdline-tools/latest/bin
  export JAVA_HOME="$PWD/jdk-17.0.21+9"
  yes | sdk/cmdline-tools/latest/bin/sdkmanager --sdk_root="$PWD/sdk" --licenses >/dev/null || true
  sdk/cmdline-tools/latest/bin/sdkmanager --sdk_root="$PWD/sdk" \
      "platform-tools" "platforms;android-34" "build-tools;34.0.0"
fi

if [ ! -d gradle-8.7 ]; then
  curl -sL -o gradle.zip "https://services.gradle.org/distributions/gradle-8.7-bin.zip"
  python3 -c "import zipfile;zipfile.ZipFile('gradle.zip').extractall('.')" && rm gradle.zip
fi

cd .. && printf 'sdk.dir=%s\n' "$PWD/.tooling/sdk" > local.properties
echo "ok - now run: JAVA_HOME=$PWD/.tooling/jdk-17.0.21+9 .tooling/gradle-8.7/bin/gradle :app:assembleRelease"
