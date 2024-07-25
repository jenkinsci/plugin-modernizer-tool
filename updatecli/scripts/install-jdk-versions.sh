#!/bin/bash

# Initialize SDKMAN
source "$HOME/.sdkman/bin/sdkman-init.sh"

# Declare the JDK versions you're interested in
declare -a jdk_versions=("8" "11" "17" "21")

# Loop through each JDK version
for version in "${jdk_versions[@]}"; do
  # Use sdk list java with --no-pager or pipe through cat to avoid pager behavior
  # Adjust the grep pattern to match the current output format
identifier=$(PAGER=cat sdk list java | grep -E " $version\\.0.*-tem" | awk -v ver="$version" '$0 ~ " " ver "\\.0.*-tem" {print $NF}' | head -n 1)
if [ -n "$identifier" ]; then
    echo "Installing Temurin JDK version $version with identifier $identifier"
    # Install the JDK version using SDKMAN
    yes | sdk install java "$identifier"
  else
    echo "No Temurin JDK version found for $version"
  fi
done
