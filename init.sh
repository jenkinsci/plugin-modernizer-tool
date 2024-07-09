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
mkdir test-plugins && cd test-plugins || exit

# Clone the first Jenkins plugin repository. The badge-plugin is a Jenkins plugin that
# adds badges to build pages, indicating various conditions like coverage or build status.
git clone https://github.com/jenkinsci/badge-plugin.git

# Clone the second Jenkins plugin repository. The build-timestamp-plugin adds the ability
# to display build timestamps in the Jenkins UI. This plugin will also be built as part
# of the Docker image setup.
git clone https://github.com/jenkinsci/build-timestamp-plugin.git
