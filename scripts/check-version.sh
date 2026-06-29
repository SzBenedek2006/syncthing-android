#!/bin/sh
# This is supposed to be called from bump-version.sh

set -e

printf "%b" \
	"\n\n" \
	"Checking Syncthing Version is compatible\n" \
	"-----------------------------\n"

NEW_VERSION_NAME=$1
if [ -z "$NEW_VERSION_NAME" ]
then
    echo "New version name is empty. Please set a new version. Current version: $OLD_VERSION_NAME"
    exit 1
fi

cd "syncthing/src/github.com/syncthing/syncthing/"
CURRENT_TAG=$(git describe)
if [ "${NEW_VERSION_NAME#${CURRENT_TAG#v}}" = "$NEW_VERSION_NAME" ]; then
    printf "New version name %s is not compatible with Syncthing version %s\n" \
           "$NEW_VERSION_NAME" "$CURRENT_TAG"
    exit 1
fi
