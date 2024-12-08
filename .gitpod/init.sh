#!/bin/bash
# This script is designed to set up the environment for a Java project using Maven.
# It performs a clean install of the project dependencies and sets up a directory
# for Jenkins plugin development by cloning necessary repositories.

# Install dependencies and perform initial setup

# Run Maven clean install to ensure all project dependencies are correctly installed
# and the project is built successfully.
mvn clean install -DskipTests

# Create a directory for the test plugins and move into it. This directory will serve
# as a workspace for cloning and building Jenkins plugin repositories.
mkdir -p test-plugins

# Check if the terminal supports colors
if tput colors > /dev/null 2>&1; then
  color_cyan="\033[36m"
  color_blue="\033[34m"
  color_green="\033[01;32m"
  color_reset="\033[0m"
else
  color_cyan=""
  color_blue=""
  color_green=""
  color_reset=""
fi

echo -e "\nHello ${color_green}$GITPOD_GIT_USER_NAME${color_reset} 👋"
echo -e "\nThis tool needs a GitHub token to work."
echo -e "GitPod created a temporary GitHub token for you: ${color_cyan}$SCM_TOKEN${color_reset}"
echo -e "We stored it in the ${color_cyan}GH_TOKEN${color_reset} environment variable."
echo -e "If you want to use a different token, please set it in the ${color_cyan}GH_TOKEN${color_reset} environment variable."
echo -e "If you want to know more about GitHub tokens, please visit ${color_blue}https://docs.github.com/en/authentication/keeping-your-account-and-data-secure/managing-your-personal-access-tokens${color_reset}"
GH_TOKEN="$SCM_TOKEN"
export GH_TOKEN
# Make a GET request to the GitHub API to fetch user information
response=$(curl -sH "Authorization: token $SCM_TOKEN" https://api.github.com/user)
# Extract the GitHub handle from the response
GH_OWNER=$(echo $response | jq -r '.login')
export GH_OWNER
echo -e "\nThis tool also needs a GitHub handle to clone and fork plugin repositories"
echo -e "We stored \"${color_green}$GH_OWNER${color_reset}\" in the ${color_cyan}GH_OWNER${color_reset} environment variable."
echo -e "If you want to use another GitHub handle, please set the ${color_green}GH_OWNER${color_reset} variable accordingly."
echo -e "\nWe propose you to work with two Jenkins plugin repositories: ${color_cyan}badge-plugin${color_reset} and ${color_cyan}build-timestamp-plugin${color_reset}."
echo -e "You could, though, use other plugin repositories as well (like ${color_cyan}plot-plugin${color_reset}) by entering their name in the following ${color_cyan}java${color_reset} command."
echo -e "\nYou can now proceed with the modernizer tool thanks to the following commands:"
echo -e "${color_cyan}java -jar plugin-modernizer-cli/target/jenkins-plugin-modernizer-999999-SNAPSHOT.jar --dry-run --plugins badge,build-timestamp --recipe AddCodeOwner --export-datatables${color_reset}"

echo -e "\nBy the way, you can copy/paste from/to the terminal to execute the commands. Enjoy! 🚀"
echo -e "See ${color_blue}https://www.gitpod.io/docs/configure/user-settings/browser-settings#browser-settings${color_reset} to know more.\n"
