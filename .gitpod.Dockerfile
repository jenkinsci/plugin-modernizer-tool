# Start from a base image that includes JDK 17 and Git
FROM openjdk:17-jdk

# Install Git
RUN apt-get update && \
    apt-get install -y git && \
    apt-get clean && \
    rm -rf /var/lib/apt/lists/* /tmp/* /var/tmp/*

# Clone the first repository
RUN git clone https://github.com/example/repo1.git /path/to/local/repo1

# Clone the second repository
RUN git clone https://github.com/example/repo2.git /path/to/local/repo2

# Set environment variables if necessary
ENV JAVA_HOME /usr/lib/jvm/java-17-openjdk-amd64

# Continue with any other setup you need
