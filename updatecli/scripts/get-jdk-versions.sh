#!/bin/bash

# Store the current directory
current_dir=$(pwd)

# Check if a version argument was provided
if [ "$#" -ne 1 ]; then
    echo "Usage: $0 <major_java_version>" >&2
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

# Initialize SDKMAN
source "$HOME/.sdkman/bin/sdkman-init.sh"

# Find and output the Temurin JDK version identifier for the given major version
identifier=$(sdk list java | grep -E " $major_version\\.0.*-tem" | awk -v ver="$major_version" '$0 ~ " " ver "\\.0.*-tem" {print $NF}' | head -n 1)

if [ -n "$identifier" ]; then
    echo "$identifier"
else
    echo "No Temurin JDK version found for Java $major_version" >&2
    exit 2
fi
