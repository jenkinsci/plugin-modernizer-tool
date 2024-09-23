# This script copies the Maven local repository to the current directory.

Write-Output "Determining Maven local repository path..."

# Get the Maven local repository path using Maven's help:evaluate goal.
$mavenRepo = mvn help:evaluate -Dexpression=settings.localRepository -q -DforceStdout

# Check if the Maven repository path was found.
if (-not $mavenRepo) {
    Write-Output "Failed to determine Maven local repository path."
    exit 1
}

Write-Output "Maven local repository path determined: $mavenRepo"

# Copy the Maven repository to the current directory.
Write-Output "Copying Maven repository to the current directory..."
New-Item -ItemType Directory -Path .m2\repository -Force
Copy-Item -Recurse -Force "$mavenRepo\*" .m2\repository

# Verify the copy operation.
if ($?) {
    Write-Output "Maven repository copied successfully to .m2"
} else {
    Write-Output "Failed to copy Maven repository."
    exit 1
}
