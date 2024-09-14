# Define the VERSION argument with a default value
ARG VERSION=999999-SNAPSHOT

# First stage: Build the project using Maven and Eclipse Temurin JDK 21
FROM maven:3.9.9-eclipse-temurin-21 AS builder

# Re-define the VERSION argument for the builder stage
ARG VERSION

# Set the VERSION environment variable
ENV VERSION=${VERSION}


# Add the current directory to the /plugin-modernizer directory in the container
ADD . /plugin-modernizer
RUN mkdir -p /plugin-modernizer
WORKDIR /plugin-modernizer

# Define a build argument for the Maven cache location
ARG MAVEN_CACHE=/root/.m2

# Print the Maven local repository path
RUN echo "Maven local repository path: $(mvn help:evaluate -Dexpression=settings.localRepository -q -DforceStdout)"

# List the Maven cache directory itself
RUN ls -ld $(mvn help:evaluate -Dexpression=settings.localRepository -q -DforceStdout)

# Use the build argument to set the Maven cache location with a bind mount
RUN --mount=type=bind,source=${MAVEN_CACHE},target=/root/.m2 \
    cd /plugin-modernizer && \
    mvn clean install -DskipTests

# Second stage: Create the final image using Maven and Eclipse Temurin JDK 21
FROM maven:3.9.9-eclipse-temurin-21 AS result-image

# Update package lists and install necessary packages
RUN apt-get update && \
    apt-get install -y curl zip unzip && \
    rm -rf /var/lib/apt/lists/*

# Set environment variables for JDK versions managed by SDKMAN
ENV JDK8_PACKAGE=8.0.422-tem
ENV JDK11_PACKAGE=11.0.24-tem
ENV JDK17_PACKAGE=17.0.12-tem
ENV JDK21_PACKAGE=21.0.4-tem
ENV MVN_INSTALL_PLUGIN_VERSION=3.1.3

# Replace the default shell with bash
RUN rm /bin/sh && ln -s /bin/bash /bin/sh

# Install SDKMAN and respective JDKs
RUN curl -s "https://get.sdkman.io" | bash
RUN source "/root/.sdkman/bin/sdkman-init.sh" && \
    sdk install java $JDK8_PACKAGE && \
    sdk install java $JDK11_PACKAGE && \
    sdk install java $JDK17_PACKAGE && \
    sdk install java $JDK21_PACKAGE

# Re-define the VERSION argument for the result-image stage
ARG VERSION

# Set the VERSION environment variable
ENV VERSION=${VERSION}

# Copy the built JAR files from the builder stage to the final image
COPY --from=builder /plugin-modernizer/plugin-modernizer-cli/target/jenkins-plugin-modernizer-${VERSION}.jar /jenkins-plugin-modernizer.jar
COPY --from=builder /plugin-modernizer/plugin-modernizer-core/target/plugin-modernizer-core-${VERSION}.jar /jenkins-plugin-modernizer-core.jar

# Install the core dependency using the Maven install plugin
RUN mvn org.apache.maven.plugins:maven-install-plugin:${MVN_INSTALL_PLUGIN_VERSION}:install-file \
    -Dfile=/jenkins-plugin-modernizer-core.jar \
    -DgroupId=io.jenkins.plugin-modernizer \
    -DartifactId=plugin-modernizer-core \
    -Dversion=${VERSION} \
    -Dpackaging=jar

# Set the entry point for the Docker container to run the main JAR file
ENTRYPOINT ["java", "-jar", "/jenkins-plugin-modernizer.jar"]
