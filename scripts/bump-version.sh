#!/bin/sh

set -e

if ! git diff-index --exit-code --quiet HEAD; then
    echo "Bumping version aka cutting a release must happen on a clean git workspace"
    exit 1
fi

NEW_VERSION_NAME="$1"
OLD_VERSION_NAME=$(awk '/versionName/ {print $3}' "app/build.gradle.kts")
if [ -z "$NEW_VERSION_NAME" ]
then
    echo "New version name is empty. Please set a new version. Current version: $OLD_VERSION_NAME"
    exit 1
fi

./scripts/check-version.sh "$NEW_VERSION_NAME" 

printf "%b" \
	"\n\n" \
	"Running Lint\n" \
	"-----------------------------\n" \
	"\n"

./gradlew clean lintVitalRelease

printf "%b" \
	"\n\n" \
	"Enter Changelog for $NEW_VERSION_NAME\n" \
	"-----------------------------\n"

CHANGELOG=app/src/main/play/release-notes/en-GB/default.txt
if command -v edit >/dev/null; then
    editor=edit
elif [ -n "$EDITOR" ]; then
    editor="$EDITOR"
else
    echo "No editor found - need either 'edit' binary or EDITOR env var set"
    exit 1
fi
"$editor" "$CHANGELOG"

printf "%b" \
	"\n\n" \
	"Updating Version\n" \
	"-----------------------------\n"

OLD_VERSION_CODE=$(awk '/versionCode/ {print $3; exit}' "app/build.gradle.kts")
NEW_VERSION_CODE=$(($OLD_VERSION_CODE + 1))
BUILD_TMP="app/build.gradle.kts.tmp"

sed -e "s/versionCode = $OLD_VERSION_CODE/versionCode = $NEW_VERSION_CODE/" \
    -e "s/$OLD_VERSION_NAME/\"$NEW_VERSION_NAME\"/" \
	"app/build.gradle.kts" > "$BUILD_TMP"

mv "$BUILD_TMP" "app/build.gradle.kts"

git add "app/build.gradle.kts" "$CHANGELOG"
git commit -m "Bumped version to $NEW_VERSION_NAME"

git tag -a "$NEW_VERSION_NAME" -m "
$NEW_VERSION_NAME

$(cat "$CHANGELOG")
"
