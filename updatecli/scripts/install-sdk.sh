#!/bin/bash

# This script installs SDKMAN, a tool for managing parallel versions of multiple Software Development Kits on most Unix-based systems.

# Define SDKMAN installation URL
SDKMAN_URL="https://get.sdkman.io"

# Check if SDKMAN is already installed by checking if the sdkman-init.sh script exists
if [ -f "$HOME/.sdkman/bin/sdkman-init.sh" ]; then
    echo "SDKMAN is already installed."
else
    # Download and run the SDKMAN installation script
    echo "Installing SDKMAN..."
    curl -s "$SDKMAN_URL" | bash

    # Source the SDKMAN initialization script to make the `sdk` command available in the current shell session
    source "$HOME/.sdkman/bin/sdkman-init.sh"

    echo "SDKMAN installation completed."
fi
[[ -s "$HOME/.sdkman/bin/sdkman-init.sh" ]] && source "$HOME/.sdkman/bin/sdkman-init.sh"
# Verify SDKMAN installation by checking the sdk version
sdk version
