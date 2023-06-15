#!/usr/bin/env bash

set -exo pipefail

# Helper function to increment a semantic version string.
function increment_version() {
  local delimiter=.
  # shellcheck disable=SC2207
  local array=($(echo "$1" | tr $delimiter '\n'))
  local version_type=$2
  local major=${array[0]}
  local minor=${array[1]}
  local patch=${array[2]}

  if [ "$version_type" = "--major" ]; then
    major=$((major + 1))
    minor=0
    patch=0
  elif [ "$version_type" = "--minor" ]; then
    minor=$((minor + 1))
    patch=0
  elif [ "$version_type" = "--patch" ]; then
    patch=$((patch + 1))
  else
    echo "Invalid version type. Must be one of: '--major', '--minor', '--patch'"
    exit 1
  fi

  incremented_version="$major.$minor.$patch"

  echo "Incremented version: $incremented_version"
}

# Gets a property out of a .properties file
# usage: getProperty $key $filename
function getProperty() {
  grep "${1}" "$2" | cut -d'=' -f2
}

NEW_VERSION=$1
NEXT_VERSION="$(increment_version "$NEW_VERSION" --minor)-SNAPSHOT"
SNAPSHOT_VERSION=$(getProperty 'VERSION_NAME' gradle.properties)

echo "Publishing $NEW_VERSION"

# Prepare release
sed -i '' "s/${SNAPSHOT_VERSION}/${NEW_VERSION}/g" gradle.properties
git commit -am "Prepare for release $NEW_VERSION."
git tag -a "$NEW_VERSION" -m "Version $NEW_VERSION"

# Publish
./gradlew publish -x dokkaHtml

# Prepare next snapshot
echo "Updating snapshot version to $NEXT_VERSION"
sed -i '' "s/${NEW_VERSION}/${NEXT_VERSION}/g" gradle.properties
git commit -am "Prepare next development version."

# Push it all up
git push && git push --tags
