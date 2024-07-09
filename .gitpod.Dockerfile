FROM gitpod/workspace-java

# Remove JDK 11 if it exists (use specific package name if installed)
# RUN sudo apt-get remove -y <jdk11-package-name>

# Install JDK 17
RUN sudo apt-get update && \
    sudo apt-get install -y openjdk-17-jdk && \
    sudo apt-get clean && \
    sudo rm -rf /var/lib/apt/lists/* /tmp/* /var/tmp/*

# Set JAVA_HOME environment variable for JDK 17
ENV JAVA_HOME /usr/lib/jvm/java-17-openjdk-amd64
