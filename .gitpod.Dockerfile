# This Dockerfile is designed to set up a Java development environment with JDK 17 and Git,
# specifically tailored for building Jenkins plugins. It includes the installation of Maven,
# cloning of two specific Jenkins plugin repositories, and building these plugins using Maven.

# Start from a base image that includes JDK 17 and Git. This serves as the foundation for the development environment.
FROM gitpod/workspace-java-17

# Update the package list and install Maven. Maven is required for building Java projects, including Jenkins plugins.
RUN sudo apt-get update && \
    sudo apt-get install -y maven && \
    sudo rm -rf /var/lib/apt/lists/*

# Create a directory for the test plugins and move into it. This directory will hold the cloned repositories.
RUN mkdir test-plugins && cd test-plugins

# Clone the first Jenkins plugin repository. This plugin will be built as part of the Docker image.
RUN git clone https://github.com/jenkinsci/badge-plugin.git

# Clone the second Jenkins plugin repository. This is another plugin that will be built.
RUN git clone https://github.com/jenkinsci/build-timestamp-plugin.git
