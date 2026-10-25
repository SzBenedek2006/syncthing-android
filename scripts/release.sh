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

# Checks

## Current working directory
if test "$(basename $PWD)" != "$PROJECT_ROOT"; then
	echo "Please run this in directory $PROJECT_ROOT"
	exit 0
fi

## Clean workspace
if ! git diff-index --exit-code --quiet HEAD; then
	echo "Creating a release must happen on a clean git workspace"
	exit 1
fi

## Version check
APP_VERSION=$(awk -F'"' '/versionName =/ {print $2}' app/build.gradle.kts)
GIT_TAG=$(git describe --tags --abbrev=0)

if test "$APP_VERSION" != "${GIT_TAG#v}"; then
	echo "Version $APP_VERSION in gradle and tag $GIT_TAG doesn't match"
	exit 1
fi

prefix="${APP_VERSION%.*}"

suffix="${APP_VERSION##*.}"

suffix=$((suffix + 1))

NEW_VERSION="${prefix}.${suffix}"

echo "New suggested version is $NEW_VERSION"
