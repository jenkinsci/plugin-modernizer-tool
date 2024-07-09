#!/bin/bash
# Install dependencies and perform initial setup

# Run Maven clean install
mvn clean install

# Create a directory for the test plugins and move into it. This directory will hold the cloned repositories.
mkdir test-plugins && cd test-plugins || exit

# Clone the first Jenkins plugin repository. This plugin will be built as part of the Docker image.
git clone https://github.com/jenkinsci/badge-plugin.git

# Clone the second Jenkins plugin repository. This is another plugin that will be built.
git clone https://github.com/jenkinsci/build-timestamp-plugin.git
