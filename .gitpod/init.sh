#!/bin/bash
# This script is designed to set up the environment for a Java project using Maven.
# It performs a clean install of the project dependencies and sets up a directory
# for Jenkins plugin development by cloning necessary repositories.

# Install dependencies and perform initial setup

# Run Maven clean install to ensure all project dependencies are correctly installed
# and the project is built successfully.
mvn clean install

# Create a directory for the test plugins and move into it. This directory will serve
# as a workspace for cloning and building Jenkins plugin repositories.
mkdir -p test-plugins && cd test-plugins || exit

# Clone the first Jenkins plugin repository. The badge-plugin is a Jenkins plugin that
# adds badges to build pages, indicating various conditions like coverage or build status.
# Define the repository URL and the directory name
REPO_URL="https://github.com/jenkinsci/badge-plugin.git"
DIR_NAME="badge-plugin"

# Check if the directory exists and is not empty
if [ -d "$DIR_NAME" ] && [ "$(ls -A $DIR_NAME)" ]; then
  echo "The directory $DIR_NAME already exists and is not empty. Skipping clone."
else
  # If the directory does not exist or is empty, clone the repository
  echo "Cloning $REPO_URL into $DIR_NAME..."
  git clone $REPO_URL $DIR_NAME
fi

# Clone the second Jenkins plugin repository. The build-timestamp-plugin adds the ability
# to display build timestamps in the Jenkins UI. This plugin will also be built as part
# of the Docker image setup.
REPO_URL="https://github.com/jenkinsci/build-timestamp-plugin.git"
DIR_NAME="build-timestamp-plugin"

# Check if the directory exists and is not empty
if [ -d "$DIR_NAME" ] && [ "$(ls -A $DIR_NAME)" ]; then
  echo "The directory $DIR_NAME already exists and is not empty. Skipping clone."
else
  # If the directory does not exist or is empty, clone the repository
  echo "Cloning $REPO_URL into $DIR_NAME..."
  git clone $REPO_URL $DIR_NAME
fi

# Check if the terminal supports colors
if tput colors > /dev/null 2>&1; then
  color_cyan="\033[36m"
  color_reset="\033[0m"
else
  color_cyan=""
  color_reset=""
fi

echo -e "As a gentle reminder, we have already cloned two Jenkins plugin repositories: ${color_cyan}badge-plugin${color_reset} and ${color_cyan}build-timestamp-plugin${color_reset}."
echo -e "You can now proceed with the modernizer tool thanks to the following commands:"
echo -e "${color_cyan}java -jar plugin-modernizer-cli/target/jenkins-plugin-modernizer-999999-SNAPSHOT.jar --plugins badge-plugin,build-timestamp-plugin --recipes AddPluginsBom,AddCodeOwner${color_reset}"