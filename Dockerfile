# Use the official Maven image with Eclipse Temurin JDK 21
FROM maven:3.9.9-eclipse-temurin-21

# Update package lists and install necessary packages
RUN apt-get update && \
    apt-get install -y curl zip unzip \
    && rm -rf /var/lib/apt/lists/*

# Set environment variables for JDK versions managed by SDKMAN
ENV JDK8_PACKAGE=8.0.422-tem
ENV JDK11_PACKAGE=11.0.24-tem
ENV JDK17_PACKAGE=17.0.12-tem
ENV JDK21_PACKAGE=21.0.4-tem
ENV MVN_INSTALL_PLUGIN_VERSION=3.1.2

# Install SDKMAN and respective JDKs
RUN rm /bin/sh && ln -s /bin/bash /bin/sh
RUN curl -s "https://get.sdkman.io" | bash
RUN source "/root/.sdkman/bin/sdkman-init.sh" && \
    sdk install java $JDK8_PACKAGE && \
    sdk install java $JDK11_PACKAGE && \
    sdk install java $JDK17_PACKAGE && \
    sdk install java $JDK21_PACKAGE

# Set the version for the plugin-modernizer
ENV VERSION=999999-SNAPSHOT

# Add the plugin-modernizer JAR files to the image
ADD plugin-modernizer-cli/target/jenkins-plugin-modernizer-${VERSION}.jar /jenkins-plugin-modernizer.jar
ADD plugin-modernizer-core/target/plugin-modernizer-core-${VERSION}.jar /jenkins-plugin-modernizer-core.jar

# Install the core dependency using the Maven install plugin
RUN mvn org.apache.maven.plugins:maven-install-plugin:${MVN_INSTALL_PLUGIN_VERSION}:install-file  \
    -Dfile=/jenkins-plugin-modernizer-core.jar \
    -DgroupId=io.jenkins.plugin-modernizer \
    -DartifactId=plugin-modernizer-core \
    -Dversion=${VERSION} \
    -Dpackaging=jar

# Set the entry point for the Docker container
ENTRYPOINT ["java", "-jar", "/jenkins-plugin-modernizer.jar"]do
