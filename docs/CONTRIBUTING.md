# Contributing to Plugin Modernizer Tool

To learn more about the architecture of the tool, see the [ARCHITECTURE](ARCHITECTURE.md) file.

## Getting Started

1) Clone the repository

2) Install tools

   i. Java 21

   ii. Maven 3.9.6

3) Open the project in your favorite IDE.

## Building the application locally

Once you have installed the tools listed before, you can generate the jar by running the command,

```shell
mvn verify
```

The JAR file will be available at the path `plugin-modernizer-cli/target/jenkins-plugin-modernizer-999999-SNAPSHOT.jar`.

This will run all the tests of the application. You can speed up the process by running the command:

```shell
 mvn package -DskipTests
```

## Run on Gitpod

This project includes a .gitpod.yml configuration file, which allows to easily work on it using Gitpod.
When the Gitpod workspace starts, the required versions of Java and Maven are automatically installed. 
Also, the following command is executed at startup:

```shell
mvn clean install -DskipTests
```

## Testing Changes

To test the changes, execute the following command:

```shell
mvn test
```

## Proposing Changes

All proposed changes are submitted and reviewed through a GitHub pull request. To submit a pull request:

1) Make sure your changeset is not introducing a regression by running mvn verify

2) Create a branch prefixed with:

   i. feature/ for a new enhancement

   ii. fix/ for a bugfix

   iii. docs/ for documentation changes

3) Commit your changes to this new branch

4) Push the branch to your fork of the repository.

5) In the GitHub Web UI, select the New pull request option

6) Follow the pull request template

7) Click the Create pull request button.

The pull request will be built in [ci.jenkins.io](https://ci.jenkins.io/)