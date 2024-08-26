# Plugin modernizer Tool Architecture

**Architecture Diagram:**

![Architecture Diagram](./images/architecture.svg)

The tool starts by retrieving the list of recipes and plugins from the CLI arguments provided by the user.

It utilizes the GHService for various GitHub-related operations such as cloning repositories, forking, syncing with upstream, creating branches, committing changes, pushing to the repository, and creating pull requests.

After cloning the plugins locally, the tool gathers metadata about each plugin.

The MavenInvoker is then used to execute various Maven goals on the plugins, including compilation, running OpenRewrite recipes, and verification.