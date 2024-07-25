#!/bin/bash

# Store the current directory
current_dir=$(pwd)

# Check if exactly one argument is provided
if [ "$#" -ne 1 ]; then
    echo "Usage: $0 <major_java_version>"
    exit 1
fi

major_version=$1

# Change to the directory where the script is located
cd "$(dirname "$0")" || exit

# Call install-sdk.sh and redirect output
# For silence:
./install-sdk.sh > /dev/null 2>&1
# Or, to redirect all output to stderr:
#./install-sdk.sh 2>&1 >&2

# Initialize SDKMAN
if [[ -s "$HOME/.sdkman/bin/sdkman-init.sh" ]]; then
    source "$HOME/.sdkman/bin/sdkman-init.sh"
    jdk-versions.sh > /dev/null 2>&1
else
    echo "SDKMAN is not installed or not found."
    exit 2
fi

# Change back to the original directory
cd "$current_dir" || exit

# Retrieve the version identifier for the requested major version of Temurin JDK
identifier=$(sdk list java | grep -E " $major_version\\.0.*-tem" | awk -v ver="$major_version" '$0 ~ " " ver "\\.0.*-tem" {print $NF}' | head -n 1)

if [ -z "$identifier" ]; then
    echo "No Temurin JDK version found for Java $major_version"
    exit 3
fi

# Get JAVA_HOME for the specified version
java_home=$(sdk home java "$identifier")

# Replace the expanded $HOME path with the literal $HOME string
java_home_modified=$(echo "$java_home" | sed "s|${HOME}|\\\$HOME|g")

if [ -z "$java_home_modified" ]; then
    echo "JAVA_HOME could not be found for Java $major_version"
    exit 4
fi

echo "$java_home_modified"
