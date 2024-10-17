# This Dockerfile is designed to set up a Java development environment with JDK 17 and Git,
# specifically tailored for building Jenkins plugins. It includes the installation of Maven,
# cloning of two specific Jenkins plugin repositories, and building these plugins using Maven.

# Start from a base image that includes JDK 17 and Git. This serves as the foundation for the development environment.
FROM gitpod/workspace-java-17

# Define the versions of the JDKs to install. This is used by the `sdk install java` command to install the specified JDK.
ENV JDK8_PACKAGE=8.0.422-tem
ENV JDK11_PACKAGE=11.0.24-tem
ENV JDK17_PACKAGE=17.0.12-tem
ENV JDK21_PACKAGE=21.0.5-tem

# Update the package list and install Maven. Maven is required for building Java projects, including Jenkins plugins.
# The `apt-get update` command updates the list of available packages and their versions, but it does not install or
# upgrade any packages. The `apt-get install -y maven` command installs Maven without requiring confirmation from the user.
# Finally, `rm -rf /var/lib/apt/lists/*` cleans up the package list files to keep the Docker image size down.
RUN sudo apt-get update && \
    sudo apt-get install -y maven && \
    sudo rm -rf /var/lib/apt/lists/*

# See https://www.gitpod.io/docs/configure/authentication#how-to-get-scm-api-token-from-gitpods-gitlab-github-or-bitbucket-integration-as-environment-variable \
# How to get SCM API token from Gitpod’s GitHub integration as environment variable
RUN file="$HOME/.bashrc.d/770-scm_token.sh" \
    && printf '%s\n' 'if [[ "${GITPOD_WORKSPACE_CONTEXT_URL:-}" == *gitlab* ]]; then : "gitlab"; else : "github"; fi; scm_name="$_"' > "${file}" \
    && printf 'export SCM_TOKEN="$(%s)"\n' "gp credential-helper get <<<host=\${scm_name}.com | sed -n 's/^password=//p'" >> "${file}"

# Install the gh cli tool 
RUN (type -p wget >/dev/null || (sudo apt update && sudo apt-get install wget -y)) \
    && sudo mkdir -p -m 755 /etc/apt/keyrings \
    && wget -qO- https://cli.github.com/packages/githubcli-archive-keyring.gpg | sudo tee /etc/apt/keyrings/githubcli-archive-keyring.gpg > /dev/null \
    && sudo chmod go+r /etc/apt/keyrings/githubcli-archive-keyring.gpg \
    && echo "deb [arch=$(dpkg --print-architecture) signed-by=/etc/apt/keyrings/githubcli-archive-keyring.gpg] https://cli.github.com/packages stable main" | sudo tee /etc/apt/sources.list.d/github-cli.list > /dev/null \
    && sudo apt update \
    && sudo apt install gh -y
    
 # Initialize SDKMAN and install the various JDKs thanks to SDKMAN
RUN /bin/bash -c "source \"$HOME/.sdkman/bin/sdkman-init.sh\" && \
    yes | sdk install java \"$JDK8_PACKAGE\" \
    && yes | sdk install java \"$JDK11_PACKAGE\" \
    && yes | sdk install java \"$JDK21_PACKAGE\" \
    && yes | sdk install java \"$JDK17_PACKAGE\""
