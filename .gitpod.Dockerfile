# This Dockerfile is designed to set up a Java development environment with JDK 17 and Git,
# specifically tailored for building Jenkins plugins. It includes the installation of Maven,
# cloning of two specific Jenkins plugin repositories, and building these plugins using Maven.

# Start from a base image that includes JDK 17 and Git. This serves as the foundation for the development environment.
FROM gitpod/workspace-java-17

# Update the package list and install Maven. Maven is required for building Java projects, including Jenkins plugins.
# The `apt-get update` command updates the list of available packages and their versions, but it does not install or
# upgrade any packages. The `apt-get install -y maven` command installs Maven without requiring confirmation from the user.
# Finally, `rm -rf /var/lib/apt/lists/*` cleans up the package list files to keep the Docker image size down.
RUN sudo apt-get update && \
    sudo apt-get install -y maven && \
    sudo rm -rf /var/lib/apt/lists/*
