#!/bin/bash

# Check if a version argument was provided
if [ "$#" -ne 1 ]; then
    echo "Usage: $0 <major_java_version>"
    exit 1
fi

major_version=$1

# Initialize SDKMAN
source "$HOME/.sdkman/bin/sdkman-init.sh"

# Find and output the Temurin JDK version identifier for the given major version
identifier=$(sdk list java | grep -E " $major_version\\.0.*-tem" | awk -v ver="$major_version" '$0 ~ " " ver "\\.0.*-tem" {print $NF}' | head -n 1)

if [ -n "$identifier" ]; then
    echo "$identifier"
else
    echo "No Temurin JDK version found for Java $major_version"
fi
