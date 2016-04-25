
# Contributing to Anypoint&trade; Community Connectors

Thank you! Really, thank you for taking some of your precious time
helping improve the **MuleSoft Anypoint Connectors**.

This guide will help you get started with the Anypoint Connector development
environment. You will also learn the set of rules you are expected to
follow in order to submit improvements and/or fixes to these projects.

- [Before you begin](#before-you-begin) 
    - [Starting Points](#starting-points) 
    - [Visiting the Community Meeting Points](#visiting-the-community-meeting-points)
    - [Understanding the Extension Mechanisms](#understanding-the-extension-mechanisms)
- [Setting Up the Development Environment](#setting-up-the-development-environment) 
    - [Installing Prerequisites](#installing-prerequisites) 
    - [Getting the Source Code](#getting-the-source-code) 
- [Configuring the IDE](#configuring-the-ide)
    - [Working with Anypoint Studio](#working-with-anypoint-studio)
    - [Working with IntelliJ IDEA](#working-with-intellij-idea) 
- [Developing Your Contribution](#developing-your-contribution)
    - [Creating Your Feature Branch](#creating-your-feature-branch)
    - [Updating Your Feature Branch](#updating-your-feature-branch)
    - [Submitting a Pull Request](#submitting-a-pull-request)
- [Summary](#summary)


-----------------------------------------------------------------------------

 
## Before You Begin 

MuleSoft's Anypoint Connectors can range from very simple to very complex
projects. Before contributing code to them, it is important to
**understand the domain of the target system** that the connector is going
to communicate with.

Also, it is essential to have some knowledge of the
[Anypoint Connector DevKit](https://docs.mulesoft.com/anypoint-connector-devkit/v/3.7/index), which is the backbone of the connector. We advise you to become familiar with the [Connector Testing Framework](https://docs.mulesoft.com/anypoint-connector-devkit/v/3.7/connector-testing-framework), especially if you are creating new functionality that needs to be tested using that framework.

Lastly, having some **experience working with the Anypoint platform** is a big advantage. We will give you some pointers to get started on each of these topics.

### Starting Points

There are many resources available to help you become familiar with Mule and its features:

* [MuleSoft Cookbook Tutorial](http://mulesoft.github.io/mule-cookbook-tutorial), a step-by-step guide for getting started with Connector
Development.
* [Mule User Guide](https://docs.mulesoft.com/mule-user-guide/v/3.7/index), the official documentation for Mule ESB.
* The books [MuleSoft in Action (2nd Edition)](http://www.manning.com/dossot2) and [Getting Started with MuleSoft Cloud Connect](http://shop.oreilly.com/product/0636920025726.do). Both are excellent resources for learning how to use and extend Mule.
* MuleSoft's [blog](http://blogs.mulesoft.com), the fastest way to learn about the new features in Anypoint Platform.

### Visiting the Community Meeting Points

If you have gotten this far, you probably already have
a new feature or a bug fix in mind to work on. This is great! Note, however, that there
could be other members of the community with the same idea.

Before you begin, please take a few minutes to review community meeting
points to make sure someone else hasn't already taken on your challenge:

* Search for **existing GitHub issues** to see if a bug or feature request has already been submitted.
* Follow the [MuleSoft Forum](http://forums.mulesoft.com/topics/anypoint%20connector%20devkit.html)
chatter to see if anyone else has started resolving the problem or initiated an improvement.
* Scan [StackOverflow](http://stackoverflow.com/questions/tagged/mule) to see if there is already a proposed solution to your problem.

If no one else has made your improvement or fix in the above-listed resources, log the issue by creating a **GitHub tracking issue**.

### Understanding the Extension Mechanisms

**Mule ESB** has two different extension mechanisms for writing modules and connectors.
Avoiding adding functionality to the project. Rather, using one of the mechanisms to extend Mule is probably the more favorable option.
It is important to understand both.

The **first** and more intuitive mechanism for extensions is the [Anypoint Connector DevKit](https://docs.mulesoft.com/anypoint-connector-devkit/v/3.7/index), an **annotation-based framework** for easy construction
of extensions. This is the one we will be focusing on in this document.

The **second** choice is to extend Mule through the [Extensions](https://docs.mulesoft.com/mule-fundamentals/v/3.7/extending-mule) mechanism.

## Setting Up the Development Environment

While getting ready to contribute to any piece of software, there are
several **prerequisite components** you will have to install. Plus, you
will need to obtain the *preexisting source code* there may exist.

This section provides some guidelines for installing those components and downloading the source code.

### Installing Prerequisites

Before you get started, you need to set yourself up with an environment in which to develop Anypoint Connectors. Your dev environment needs a few things:

* A **Java SDK**.
* A recent version of **Maven**.
* A development environment tool or *IDE* (most preferably Anypoint Studio with DevKit plugin installed).
* A new **branch** of code to work on.

#### Java

1. If you are working with *Windows* or **Linux**, install one of the
following [Java Development Kits](http://www.oracle.com/technetwork/java/javase/downloads/index.html) on your local drive.

2. If you are working on a **Mac**,
simply confirm that the JDK shipped with your Mac OS X is _Java SE
Development Kit 7 (also known as Java SE)_ or newer using the command
`java -version`. Then skip to step 4 below.

3. Create an environment variable called `JAVA_HOME`, setting it to the directory in which you installed the JDK.

4. Update the `PATH` environment variable so that it includes the path to JDK binaries. Add the following to the `PATH` variable:

    On **Windows**
    ```
    %JAVA_HOME%/bin
    ```

    On **Linux or Mac OS X**
    ```
    $JAVA_HOME/bin
    ```

5. If you are using a Mac OS X, examine the contents of the `$JAVA_HOME/jre/lib/` security directory to confirm that the following two files are present:  
    - local_policy.jar
    - US_export_policy.jar 

   These two files prevent any problems regarding cryptography. If not present, download the [Java Cryptography Extension (JCE) Unlimited Strength Jurisdiction Policy Files 6.0](http://www.oracle.com/technetwork/java/javase/downloads/jce-6-download-429243.html), then copy the files into the security directory identified above.

#### Maven

1.  Download the Maven distribution from the [Maven web site](http://maven.apache.org/download.cgi), then unpack it to a convenient folder on your local drive.
2.  Create an environment variable called `M2_HOME`, then set it to the folder into which you unpacked Maven.
3.  Update the `PATH` environment variable to include the path to Maven binaries:

    On **Windows**
    ```
    %M2_HOME%/bin
    ```
    On **Linux or Mac OS X**
    ```
    $M2_HOME/bin
    ```
       
### Getting the Source Code

**MuleSoft Community Connectors** source code lives on **GitHub**. Perform
the following procedure to locate the code and get it onto your local drive.

If you are new to Git, consider reading [Pro Git](http://git-scm.com/book) to learn the basics. Want a read-only version of the source code?

1.  [Create](https://help.github.com/articles/signing-up-for-a-new-github-account) or log in to your GitHub account.
2.  If you haven't already done so, [set up Git](https://help.github.com/articles/set-up-git) on your local
drive.
3.  Navigate to [MuleSoft's GitHub Connector](https://github.com/mulesoft) page and select one of the repositories listed. For example, the [Twitter Connector](https://github.com/mulesoft/twitter-connector.git).
4.  Click the **Fork** button at the top right corner of the page, then select your own git repository into which GitHub inserts a copy.
5.  Prepare to **Clone** your forked repository from your GitHub account to your local drive via a secure file transfer connection. As per GitHub recommendation, we recommend using HTTPS to transfer the source code files
to your local drive. However, if you prefer to establish a secure connection for transferring the files via SSH, follow GitHub procedure to [generate SSH keys](https://help.github.com/articles/generating-ssh-keys).
6.  From the **command line**, create or navigate to an existing folder on your local drive into which you wish to store your forked clone of the connector source code.
7.  Then, execute one of the following:

    For **HTTPS**
    ```
    git clone https://github.com/<username>/<repo-name>
    ```

    For **SSH**
    ```
    git clone git@github.com:<username>/<repo-name>.git
    ```

8.  Add the *upstream repository* so that you can pull changes and stay updated with changes to the connector code branch. From the command line, execute one of the following:

    For **HTTPS**
    ```
    git remote add upstream https://github.com/mulesoft/<repo-name>.git
    ```

    For **SSH**
    ```
    git remote add upstream git@github.com:mulesoft/<repo-name>.git
    ```

> Read more about how to [Fork a Repo](https://help.github.com/articles/fork-a-repo) on GitHub.

### Understanding the Build

This is a great moment to stop and read the [MuleSoft Cookbook Tutorial](http://mulesoft.github.io/mule-cookbook-tutorial). A correct understanding of **how a Anypoint Connector project
is organized and built** is key to a productive development process.

We are ready to develop and implement our improvements. But, instead of doing it manually, we may want to configure an IDE for enhanced productivity. We will expand on this in the next section.

## Configuring the IDE

This section offers tips for importing and working on connector source code in **Anypoint Studio**. There are no restrictions on the type of integration development environment you use to develop Anypoint Connectors. We simply opted for the **Anypoint Studio with DevKit plugin installed**, which is the preferred choice since it adds a lot of support tools.

### Working with Anypoint Studio

Use Anypoint Studio to modify or add to your cloned version of the connector source code.

#### Importing

1. Download and install [Anypoint Studio](https://www.mulesoft.com/platform/studio) on your local drive.
2. Launch Studio.
3. Install **Anypoint DevKit Plugin**.
    * From the *Help* menu in Anypoint Studio, click **Install New Software**.
    * Open the *Work with* drop-down menu and select the **Anypoint Addons Update Site**.
    > If the Anypoint Addons Update Site is not available in the dropdown list, click *Add* and then *copy* the following URL to the repository location: **http://studio.mulesoft.org/r4/addons/beta**.
    * Check the *Anypoint DevKit Plugin* option and click **Next**.
    * Proceed through the remaining steps in the wizard to install.
    * Restart Studio.

4. Select **File > Import**.
5. In the **Import** wizard, click to expand the **Anypoint Studio** folder, then select **Anypoint Connector Project from External Location**, then click **Next**.
6. In the **Select root directory** field, use the *Browse* button to
navigate to the directory into which you downloaded the cloned fork of the source code from your GitHub account.
7. Ensure the project is checked, then click **Finish** and wait for Studio to import the source code. This step may take a while as it needs to download all dependencies from the Maven repository.
8. Open source code files when you need to edit or add content.
9. Click the **Save** icon to save locally.

#### Debugging

You can debug following these steps. There is also a more in-depth guide available in the [Mule documentation site](https://developer.mulesoft.com/docs/display/current/Debugging).

1. In Anypoint Studio, select **Window >  Open Perspective > Other...**, then select **Java** to open the Java Perspective.
2. Select **File > New > Java Project**. You are creating a new project just for launching Mule.
3. In the **New Java Project wizard**, select a **Name** for your project, such as Mule Launcher, then click **Next**.
4. In the **Java Settings** panel of the wizard, select the **Projects** tab, then click **Add**.
5. Click **Select All**, then click **OK**, then **Finish**.
6. In the Package Explorer, right click your launcher project's name, then select **Debug As > Debug Configurations...**
7. In the **Debug Configurations** wizard, double-click **Java Application**.
8. In the **Main class** field, enter the following content: `org.mule.MuleServer`
9. Click the **Arguments** tab. In the **Program Arguments** field, enter the following content: `-config <path to a Mule config>`
10. Click **Apply**, then click **Debug**.
11. Anypoint Studio requests permission to switch to the **Debug Perspective**; click **Yes** to accept and open.

You can now set breakpoints anywhere in any of the Java files in any of the Mule projects in the workspace.  When you change java files and rebuild, the debugger hot swaps the rebuilt Java class file to the running Mule server. Note that you only need to follow the procedure above to set your Debug configurations once; thereafter, simply right-click the project name, then select **Debug As > Java Application** for subsequent debugging. 

#### Debugging Remotely

1. From the command line, edit the `JPDA_OPTS` variable in the Mule startup script and specify the debugger port.
2. Start the Mule server with the `-debug` switch. The server waits until a debugger attaches.
3. In the Package Explorer in studio, right-click your Mule source code project's name, then select **Debug > Debug Configurations...**
4. Double-click **Remote Java Application**.
5. Under **Connection Properties**, enter a value for **Host** and **Port**, then click **Apply**.
6. Click Debug. Anypoint Studio requests permission to switch to the **Debug Perspective**; click **Yes** to accept and open.

#### Testing

Use Maven to run unit tests on your project using the command:

```
mvn test
```

In addition to the unit tests, the Anypoint Connector project is
normally packaged with a handful of functional tests. These tests are
part of a *JUnit Test Suite*, called __FunctionalTestSuite__.

To run these tests, you can use the following command:

```
mvn test -Dtest=FunctionalTestSuite
```

On occasion, some tests (that are not testing processors on the connector) require connectivity to the external system. These would be bundled together in a special test suite called SystemTestSuite. You can run those tests with:

```
mvn test -Dtest=SystemTestSuite
```

In these last two cases, you need to provide a file containing the
credentials that allow the connector to talk to the remote system.
Place a file named `automation-credentials.properties` in the
`src/test/resources` folder in your project. This file is "Git-ignored" and
should never be added to the repository.

> Read more about [MuleSoft's Testing Conventions](http://mulesoft.github.io/connector-certification-docs/advanced/index.html#_functional_tests_automation).

#### Setting Startup Parameters

The table below lists a number of command-line parameters you can use to alter Anypoint Studio startup behavior, if you wish to. Simply edit the bundled file `AnypointStudio.ini`.

| Parameter         | Action                                             |  
|:------------------|:---------------------------------------------------|  
| `-clean`          | Enables clean registration of plug-in (some plug-ins do not always register themselves properly after a restart). |
| `-nosplash`       | Does not show Anypoint Studio or plug-in splash screens.    |  
| `-showLocation`   | Allows you to explicitly set which JDK to use.      |  
| `-vm`             | Examples that come with the full Mule distribution. |
| `-vmargs`         | Allows you to pass in standard VM arguments.       |
 
### Working with IntelliJ IDEA

Use IntelliJ's IDEA integration platform to modify or add to your cloned Mule source code.

#### Importing

1. [Download](https://www.jetbrains.com/idea/download/) and install IntelliJ IDEA.
2. Open IDEA, then select **File > Open...**
3. Browse to the directory into which you downloaded the Mule source code, then select the `pom.xml` file. 
4. Click **OK**. IDEA takes awhile to process all the pom.xml files.
5. Set the correct source for the JDK on your local drive. Right click the **mule-transport-jdbc** directory, then select  **Module Settings > Sources > src > main > jdk6** or **jdk7**. Repeat this step for test sources, as tests.

> **Troubleshooting** : if IntelliJ presents any compilation errors in test classes from the CXF module when you create the project, it is safe to ignore them. Those classes depend on some test classes generated by the Maven build during execution. Right click on the error message, then select **Exclude from compile**.  Alternatively, you can run `mvn install` from the command line to fix the errors.

#### Debugging Remotely

You can debug following these steps. There is also a more in-depth guide available in the [Mule documentation site](https://developer.mulesoft.com/docs/display/current/Debugging).

1. Start the Mule server with the `-debug` switch. The server waits until a debugger attaches.
2. In IDEA, select **Run > Edit Configurations...** to open the **Run/Debug Configurations** window.
3. Click **Add New Configuration** (plus sign), then select **Remote**.
4. Enter a **name** for the configuration, then update the **host** and **port** values if required (You can use the default values, localhost:5005, for debugging a local mule instance).
5. Click **OK** to start the debugging session.

#### Testing

Use Maven to run unit tests on your project using the following command: `mvn test`.

In addition to the unit tests for each sub-project, the Mule parent project has a separate sub-project containing integration tests. These tests verify "macroscopic" functionality that could not be tested by any single sub-project alone.

We finally have everything ready to start writing code. Lets start with the code and also learn how to commit it in the next section.

## Developing Your Contribution

Working directly on the `master` version of source code is **strongly discouraged**, since it would
likely result in *merge conflicts* with the original `master` repository. Instead, the recommended approach for contributing to any source code is to host your changes in the
`feature` branch. Anypoint Connector code is developed following the
**GitFlow** branching model.

> Online articles [A successful Git branching model](http://nvie.com/posts/a-successful-git-branching-model) (by Vincent Driessen) and [Release Management with Gitflow](http://www.clock.co.uk/blog/release-management-with-gitflow-the-clock-way) (by Paul Serby) provide excellent starting points for getting familiar with **GitFlow**.

### Creating Your Feature Branch

Open a command line window and perform the following steps:

* From your local drive, position on `develop` branch:

    ```
    git checkout develop
    ```

* Create a new branch in which you can work on your bug fix or improvement using the command:

    ```
    git checkout -b feature/yourGitHubIssueNumber
    ```

* Now you should be able to compile your very first version of the connector source code. Unless you are using Anypoint Studio, you need only to instruct Maven to download all the libraries that are dependencies
and compile the project, which can be achieved by executing the following command in the directory into which you cloned the source code:

    ```
    mvn -DskipTests package
    ```

> If this is your first time using Maven, the download may take several minutes to complete.

You are all set with a local development environment and your own branch of the source code. You're ready. Get going!

The following steps briefly outline the development life-cycle to follow in order to develop and commit your changes in **preparation for submission**.

1. If you are using Anypoint Studio, make sure you read the previous section about [IDE configuration](#configuring-the-ide).
2. Make sure you *format* your source code using the [Formatting settings](https://github.com/mulesoft/connector-certification-docs/blob/develop/docs/02-coding-standards-best-practices/files/connectors-style-convention.xml). This will ensure you **adhere to source code standards**, thus increasing the likelihood that your
changes will be merged into the connector source code.
3. **Import** the source code project into Anypoint Studio (or your IDE of
choice), then work on your changes, fixes or improvements.
4. **Debug** and test your local version, resolving any issues that arise.
5. **Save** your changes locally.
6. **Prepare** your changes for a _Pull Request_ by first squashing your changes into a single commit on your branch using the command:

    ```
    git rebase -i
    ```
7. **Push** your squashed commit to your branch on your GitHub repository.
Refer to [Git's documentation](http://git-scm.com/book/en/v2/Git-Basics-Recording-Changes-to-the-Repository) for details on how to commit your changes.
8. **Regularly update your branch** with any changes or fixes applied to
the `develop` branch (remember this is the repository that will contain the latest changes of the connector). Refer to details below.

### Updating Your feature Branch

To ensure that your cloned version of source code remains **up-to-date**
with any changes uploaded to the connector branch, you
should regularly update your branch to **rebase** off of the latest version
of the `develop`.

1. **Pull** the latest changes from the upstream `develop` branch using the following commands:

    ```
    git fetch upstream
    git fetch upstream --tags
    ```

2. **Merge** the latest changes and updates from the `develop` branch to
your feature branch using the following command:

    ```
    git merge upstream/develop
    ```

3. **Push** any changes of `develop` branch to your forked clone using the
following commands:

    ```
    git push origin feature/yourGitHubIssueNumber
    git push origin --tags
    ```

4. Access your `feature` branch once again (to continue coding), using the command:

    ```
    git checkout dev/yourRepoName/bug/yourGitHubIssueNumber
    ```

5. **Rebase** your branch from the latest version of `develop` branch using
the following command:

    ```
    git rebase develop
    ```

6. **Resolve** any conflicts on your feature branch that may appear.

7. **Push** the newly-rebased branch back to your fork on your git repository using the following command:

    ```
    git push origin dev/yourRepoName/feature/yourGitHubIssueNumber -f
    ```

###  Submitting a Pull Request

Ready to submit your patch for review and merging? Initiate a **Pull Request (PR)** on GitHub!

1. Review the [MuleSoft Contributor's Agreement](http://www.mulesoft.org/legal/contributor-agreement.html). Before any contribution is accepted, we need you to **run** the following notebook
[script](https://api-notebook.anypoint.mulesoft.com/notebooks#bc1cf75a0284268407e4). This script will ask you to login to GitHub and **accept** our Contributor's Agreement, resulting in the creation of an issue in our
contributors project with your name.
> Once you access the **MuleSoft Contributor's Agreement web site**, perform these actions:
    * Hit the **Play notebook** button (or the Run link) and follow the
    instructions as you are prompted through the screens.
    * Authenticate.
    * Register Your Name.
    * Accept the Agreement.
    * That's it. You're ready to contribute!

2. From the repository of your branch, click the **Pull Request** button.
3. In the **Pull Request Preview** dialog, provide a title and an optional
description of your changes.
4. Review the commits that are part of your PR, then click **Send Pull
Request**.

    > Refer to GitHub's [detailed instructions](https://help.github.com/articles/using-pull-requests) for submitting a pull request.

5. **MuleSoft's Connectors Developer Team** will review the PR and may
initiate discussion or ask questions about your changes in a **Pull
Request Discussion**. If appropriate, the team will then merge your
commits within the `master` branch. We will validate acceptance of the
agreement at this step.
    > If you made **changes or corrections** to your commit **after**
having submitted the PR, go back to the PR page and update the **Commit
Range** (via the Commits tab), rather than submitting a new pull request.

# Summary

This guide started with pointing to different [sources of information](#starting-points) around
MuleSoft and the Mule's [community meeting points](#visiting-the-community-meeting-points) on
the web. These were useful for understanding where MuleSoft is going and to have contact with the rest of the community for help or to spark discussion.

In order to set up our [development environment](#setting-up-the-development-environment),
we went to [install some prerequisites](#installing-prerequisites).
Once we had those ready, we downloaded the [source code](#getting-the-source-code).

At that point we were almost ready to develop our improvements. We just
needed to [configure Anypoint Studio](#configuring-the-ide) to
develop or debug MuleSoft Anypoint Connector code.

Afterwards, we were ready to [develop our contribution](#developing-your-contribution): 

* we created our very own [feature branch](#creating-your-feature-branch) to develop the
improvement,
* we learned how to [keep it updated](#updating-your-feature-branch) in order to
* submit a [pull request](#submitting-a-pull-request) to the main MuleSoft Anypoint Conntectors repository.

---
#### **Thank you**, once again, for taking the time to  to contribute to **MuleSoft's Anypoint Connectors**._
