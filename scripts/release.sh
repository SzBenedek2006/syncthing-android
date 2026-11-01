#!/bin/sh

set -e
PROJECT_ROOT="syncthing-android"

# TODO
# 
# 1. Run checks
# 2. Print store changelog
# 3. Increment version
# 4. New tag with that version
# 5. Git push
# 6. GPP release


prompt() {
	read -r answer
	case "$answer" in
		[yY] | "")
			;;
		*)
			echo "Aborted."
			exit 1
			;;
	esac
}


# Checks

## Current working directory
printf "Current working dir: "
if test "$(basename $PWD)" != "$PROJECT_ROOT"; then
	echo "Please run this in directory $PROJECT_ROOT"
	exit 1
fi
printf "[OK]\n"

## Clean workspace

printf "Clean git workspace: "
if ! git diff-index --exit-code --quiet HEAD; then
	echo "Creating a release must happen on a clean git workspace"
	exit 1
fi
printf "[OK]\n"

## Version check
APP_VERSION=$(awk -F'"' '/versionName =/ {print $2}' app/build.gradle.kts)
VERSION_CODE=$(awk '/versionCode =/ {print $3}' app/build.gradle.kts)
GIT_TAG=$(git describe --tags --abbrev=0)

printf "Versions match: "
if test "$APP_VERSION" != "${GIT_TAG#v}"; then
	echo "Version $APP_VERSION in gradle and tag $GIT_TAG doesn't match"
	exit 1
fi
printf "[OK]\n"
## Env vars
printf "Testing env vars: "
if test -z "$SYNCTHING_RELEASE_STORE_FILE"; then
	echo "SYNCTHING_RELEASE_STORE_FILE is empty!"
	exit 1
fi
if test -z "$SIGNING_PASSWORD"; then
	echo "SIGNING_PASSWORD is empty!"
	exit 1
fi
if test -z "$SYNCTHING_RELEASE_KEY_ALIAS"; then
	echo "SYNCTHING_RELEASE_KEY_ALIAS is empty!"
	exit 1
fi
printf "[OK]\n"

# Store changelog

echo "Current changelog for beta release:"
for file in app/src/main/play/release-notes/*/beta.txt; do
	echo "$file:"
	cat "$file";
	echo
done

printf "Accept the changelogs? [Y/n] "
prompt


# New version
prefix="${APP_VERSION%.*}"

suffix="${APP_VERSION##*.}"

suffix=$((suffix + 1))

NEW_VERSION="${prefix}.${suffix}"
NEW_VERSION_CODE=$((VERSION_CODE + 1))

echo "New suggested version is $NEW_VERSION ($NEW_VERSION_CODE)"
printf "Accept the new version? [Y/n] "

prompt

echo "Accepted $NEW_VERSION"

## Updating build.gradle.kts (TODO BSD sed compatibility)
printf "Updating version in build.gradle.kts: "
sed -i "s/versionName = \"$APP_VERSION\"/versionName = \"$NEW_VERSION\"/" app/build.gradle.kts
sed -i "s/versionCode = $VERSION_CODE/versionCode = $NEW_VERSION_CODE/" app/build.gradle.kts
printf "[OK]\n"

# GitHub (and F-Droid) release
printf "Creating new version on git"
git add app/build.gradle.kts

git commit -m "Release $NEW_VERSION"

git tag "v$NEW_VERSION"

git push origin main --tags

# Play release
./gradlew publishReleaseBundle
