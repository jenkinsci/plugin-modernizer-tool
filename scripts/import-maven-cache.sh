#!/bin/bash

# This script copies the Maven local repository to the current directory.

echo "Determining Maven local repository path..."

# Get the Maven local repository path using Maven's help:evaluate goal.
MAVEN_REPO=$(mvn help:evaluate -Dexpression=settings.localRepository -q -DforceStdout)

# Check if the Maven repository path was found.
if [ -z "$MAVEN_REPO" ]; then
  echo "Failed to determine Maven local repository path."
  exit 1
fi

echo "Maven local repository path determined: $MAVEN_REPO"

# Copy the Maven repository to the current directory.
echo "Copying Maven repository to the current directory..."
mkdir -p .m2/repository
cp -vraxu "$MAVEN_REPO" .m2/repository

# Verify the copy operation.
if [ $? -eq 0 ]; then
  echo "Maven repository copied successfully to .m2"
else
  echo "Failed to copy Maven repository."
  exit 1
fi
