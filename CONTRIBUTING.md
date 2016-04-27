
# Contributing to Mule Kernel

Thank you! Really, thank you for taking some of your precious time
helping improve the **Kernel** project.

This guide will help you get started with the Mule Kernel's development
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
    - [Working with Eclipse](#working-with-eclipse)
    - [Working with IntelliJ IDEA](#working-with-intellij-idea) 
- [Developing Your Contribution](#developing-your-contribution)
    - [Creating Your Feature Branch](#creating-your-feature-branch)
    - [Updating Your Feature Branch](#updating-your-feature-branch)
    - [Submitting a Pull Request](#submitting-a-pull-request)
- [Summary](#summary)


-----------------------------------------------------------------------------

 
## Before You Begin 

Mule is a powerful and complex project. Before contributing to the Mule source code, it is important to understand the domain of the Enterprise Integration, the Mule Kernel from the user point of view and the different mechanisms to extend Mule.

### Starting Points

There are many resources available to help you become familiar with Mule and its features:

- MuleSoft's [blog](http://blogs.mulesoft.com/). The fastest way of knowing about new features in Mule. 
- [Mule User Guide](https://docs.mulesoft.com/mule-user-guide), the official documentation around usage of Mule Runtime.
- The books [Mule in Action (2nd Edition)](http://www.manning.com/dossot2/) and [Getting Started with Mule Cloud Connect](http://shop.oreilly.com/product/0636920025726.do). Both are excellent resources for learning how to use and extend Mule.
- [@MuleSoft](https://twitter.com/MuleSoft)'s twitter account.

### Visiting the Community Meeting Points

If you have gotten this far, you probably already have
a new feature or a bug fix in mind to work on. This is great! Note, however, that there
could be other members of the Community with the same idea.

Before you begin, please take a few minutes to review community meeting
points to make sure someone else hasn't already taken on your challenge:

1. Review [existing JIRAs](http://www.mulesoft.org/jira/browse/MULE) to see if a bug has already been logged.
2. Follow the [MuleSoft Forum](https://forums.mulesoft.com/) chatter to see if anyone else has started resolving the problem or initiated an improvement.
3. Scan [StackOverflow](http://stackoverflow.com/questions/tagged/mule) to see if there is already a proposed solution to your problem.

### Understanding the Extension Mechanisms

**Mule Kernel** has two different extension mechanisms for writing modules and connectors.
Avoiding adding functionality to the project. Rather, using one of the mechanisms to extend Mule is probably the more favorable option.
It is important to understand both.

- The **first** and more intuitive mechanism for extensions is the [Anypoint Connector DevKit](https://docs.mulesoft.com/anypoint-connector-devkit), an **annotation-based framework** for easy construction of extensions.

- The **second** choice is to extend Mule through the [Extensions](https://docs.mulesoft.com/mule-fundamentals) mechanism.

## Setting Up the Development Environment

While getting ready to contribute to any piece of software, there are
several prerequisite components you will have to install. Plus, you
will need to obtain the preexisting source code there may exist.

This section provides some guidelines for installing those components and downloading the source code.

### Installing Prerequisites

Before you get started, you need to set yourself up with an environment in which to develop Mule. Your **dev** environment needs the following elements:

* A **Java SDK**.
* A recent version of **Maven**.
* A development environment tool or **IDE**.
* A new **branch** of code to work on.

#### Java

1. If you are working with *Windows* or **Linux**, install one of the following [Java Development Kits](http://www.oracle.com/technetwork/java/javase/downloads/index.html) on your local drive.
2. If you are working on a **Mac**, simply confirm that the JDK shipped with your Mac OS X is _Java SE Development Kit 8 (also known as Java SE)_ or newer using the command
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

   These two files prevent any problems regarding cryptography. If not present, download the [Java Cryptography Extension (JCE) Unlimited Strength Jurisdiction Policy Files 8.0](http://www.oracle.com/technetwork/java/javase/downloads/jce8-download-2133166.html), then copy the files into the security directory identified above.

#### Maven

1.  Download the Maven distribution from the [official web site](http://maven.apache.org/download.cgi), then unpack it to a convenient folder on your local drive.
2.  Create an environment variable called `M2_HOME` and set it to the folder into which you unpacked Maven.
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

**Mule** source code lives on **GitHub**. Perform the following procedure to locate the code and get it onto your local drive.

If you are new to Git, consider reading [Pro Git](http://git-scm.com/book) to learn the basics. Want a read-only version of the source code?

1.  [Create](https://help.github.com/articles/signing-up-for-a-new-github-account) or log in to your GitHub account.
2.  If you haven't already done so, [set up Git](https://help.github.com/articles/set-up-git) on your local drive.
3.  Navigate to Mule's GitHub page located at [https://github.com/mulesoft/mule.git](https://github.com/mulesoft).
4.  Click the **Fork** button at the top right corner of the page, then select your own Git repository into which GitHub will insert a copy.
5.  Prepare to **Clone** your forked repository from your GitHub account to your local drive through a secure file transfer connection. As per GitHub recommendation, we encourage  using HTTPS to transfer the source code files to your local drive. However, if you prefer to establish a secure connection for transferring the files via SSH, follow GitHub procedure to [generate SSH keys](https://help.github.com/articles/generating-an-ssh-key/).
6.  From the **command line**, create or navigate to an existing folder on your local drive into which you wish to store your forked clone of the connector source code.
7.  Then, execute one of the following:

    For **HTTPS**
    ```
    git clone https://github.com/<your-repo-name>/mule
    ```

    For **SSH**
    ```
    git clone git@github.com:<username>/<repo-name>.git
    ```

8.  Add the *upstream repository* so that you can pull changes and stay updated with changes to the connector code branch. From the command line, execute one of the following:

    For **HTTPS**
    ```
    git remote add upstream https://github.com/mulesoft/mule.git
    ```

    For **SSH**
    ```
    git remote add upstream git@github.com:mulesoft/mule.git
    ```

> Read more about how to [Fork a Repo](https://help.github.com/articles/fork-a-repo) on GitHub.

### Understanding the Build

This is an excellent moment to read the [Guide to Build Mule](BUILD.md). A correct understanding of how the Mule project is organized and built is key for a productive development.

We are ready to develop our improvements. But, instead of doing it manually, we may want to configure an IDE for better productivity. We will expand on this in the next section.

## Configuring the IDE

This section offers tips for importing and working on Mule source code in  **Eclipse** or **IntelliJ IDEA**. There are no restrictions on the type of integration development environment you use to develop Mule, we simply chose to discuss the above-listed as common IDEs.

### Working with Eclipse

An open-source integration development platform, use Eclipse to modify or add to your cloned version of Mule source code.

#### Importing

1. Download and install [Eclipse](http://www.eclipse.org/downloads/) on your local drive.
2. From the command line, in the directory into which you downloaded the Mule source code, enter the following command to generate the classpath and project files for each sub-project:
    ```
    mvn eclipse:eclipse
    ```
3. Before launching Eclipse, make the Maven repository known in Eclipse using the following command: 
    ```
    mvn -Declipse.workspace=/path/to/eclipse/workspace eclipse:configure-workspace
    ```
4. Launch Eclipse, selecting the workspace you just "mavenized".
5. Select **File > Import**.
6. In the **Import** wizard, click to expand the **General** folder, then select **Existing Projects into Workspace**, then click **Next**.
7. In the **Select root directory** field, use the **Browse** button to navigate to the directory into which you downloaded the cloned fork of Mule source code from your Github account.
8. Ensure all **Projects** are checked, then click **Finish**. Eclipse imports the mule source code. 
9. Open source code files as you need to edit or add content.
10. Click the **Save icon to save locally.

#### Debugging

You can debug following these steps. There is also a more in-depth guide available in the [Mule documentation site](https://developer.mulesoft.com/docs/display/current/Debugging).

1. In Eclipse, select **Window >  Open Perspective > Other...**, then select **Java** to open the Java Perspective.
2. Select **File > New > Java Project**. You are creating a new project just for launching Mule.
3. In the **New Java Project wizard**, select a **Name** for your project, such as Mule Launcher, then click **Next**.
4. In the **Java Settings** panel of the wizard, select the **Projects** tab, then click **Add**.
5. Click **Select All**, then click **OK**, then **Finish**.
6. In the Package Explorer, right click your launcher project's name, then select **Debug As > Debug Configurations...**
7. In the **Debug Configurations** wizard, double-click **Java Application**.
8. In the **Main class** field, enter the following content: `org.mule.MuleServer`
9. Click the **Arguments** tab. In the **Program Arguments** field, enter the following content: 
    ```
    -config <path to a Mule config>
    ```
10. Click **Apply**, then click **Debug**.
11. Eclipse requests permission to switch to the **Debug Perspective**.
12. Click **Yes** to accept and open.

You can now set breakpoints anywhere in any of the Java files in any of the Mule projects in the workspace.  

> When you change Java files and rebuild, the debugger hot swaps the rebuilt Java class file to the running Mule server. 
Note that you only need to follow the procedure above to set your Debug configurations once. Thereafter, simply right-click the project name, then select **Debug As > Java Application** for subsequent debugging. 

#### Debugging Remotely

1. From the command line, edit the `JPDA_OPTS` variable in the Mule startup script and specify the debugger port.
2. Start the Mule server with the `-debug` switch. The server waits until a debugger attaches.
3. In the Package Explorer in studio, right-click your Mule source code project's name, then select **Debug > Debug Configurations...**
4. Double-click **Remote Java Application**.
5. Under **Connection Properties**, enter a value for **Host** and **Port**, then click **Apply**.
6. Click Debug. Eclipse requests permission to switch to the **Debug Perspective**.
7. Click **Yes** to accept and open.

#### Testing

Use Maven to run unit tests on your project using the command below:

```
mvn test
```

In addition to the unit tests for each sub-project, the Mule **parent project** has a separate sub-project containing integration tests. These tests verify *macroscopic* functionality that could not be tested by any single sub-project alone.

#### Setting Eclipse Startup Parameters

The table below lists a number of command-line parameters you can use to alter Eclipse's startup behavior, if you wish to.

| Parameter         | Action                                             |  
|:------------------|:---------------------------------------------------|  
| `-clean`          | Enables clean registration of plug-in (some plug-ins do not always register themselves properly after a restart). |
| `-nosplash`       | Does not show Eclipse or plug-in splash screens.    |  
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
5. Set the correct source for the JDK on your local drive. Right click the **mule-transport-jdbc** directory, then select  **Module Settings > Sources > src > main > jdk7** or **jdk8**. Repeat this step for test sources, as tests.

> **Troubleshooting** : if IntelliJ presents any compilation errors in test classes from the CXF module when you create the project, it is safe to ignore them. Those classes depend on some test classes generated by the Maven build during execution. Right click on the error message, then select **Exclude from compile**.  Alternatively, you can run `mvn install` from the command line to fix the errors.

#### Debugging Remotely

You can debug following these steps. There is also a more in-depth guide available in the [Mule documentation site](https://developer.mulesoft.com/docs/display/current/Debugging).

1. Start the Mule server with the `-debug` switch. The server waits until a debugger attaches.
2. In IDEA, select **Run > Edit Configurations...** to open the **Run/Debug Configurations** window.
3. Click **Add New Configuration** (plus sign), then select **Remote**.
4. Enter a **name** for the configuration, then update the **host** and **port** values if required. You can use the default values, such as localhost:5005, for debugging a local mule instance.
5. Click **OK** to start the debugging session.

#### Testing

Use Maven to run unit tests on your project using the following command: 

```
mvn test
```

In addition to the unit tests for each sub-project, the Mule parent project has a separate sub-project containing integration tests. These tests verify *macroscopic* functionality that could not be tested by any single sub-project alone.

We finally have everything ready to start writing code. Lets start with the code and also learn how to commit it in the next section.

## Developing Your Contribution

Working directly on the `master` version of Mule source code is **strongly discouraged**, since it would
likely result in **merge conflicts** with the original `master` repository. Instead, the recommended approach for contributing to any source code is to host your changes in a 
`feature` branch. Mule code complies with the **GitFlow** branching model.

> Articles [A successful Git branching model](http://nvie.com/posts/a-successful-git-branching-model) (by Vincent Driessen) and [Release Management with Gitflow](http://www.clock.co.uk/blog/release-management-with-gitflow-the-clock-way) (by Paul Serby) provide excellent starting points for getting familiar with **GitFlow**.

### Creating Your Feature Branch

Open a command line window and perform the following steps:

* From your local drive, create a new branch in which you can work on your bug fix or improvement using the command:

    ```
    git branch yourJIRAIssueNumber
    ```

* Switch to the new branch using the command: 

    ```
    git checkout yourJIRAissuenumber
    ```

* Now you should be able to perform your very first compilation of the Mule Runtime source code. You only need to instruct Maven to download all the dependent libraries and compile the project. You can do by executing the following command in the directory into which you cloned the Mule source code:

    ```
    mvn install -DskipTests
    ```

If this is your first time using Maven, the download may take several minutes to complete.

> ** Windows and the Local Maven Repository **  
> In Windows, Maven stores the libraries in the .m2 repository in your home directory. For example, `C:\Documents and Settings\<username>\.m2\repository`.  Because Java RMI tests fail where a directory name includes spaces, you must move the Maven local repository to a directory with a name that does not include spaces, such as `%M2_HOME%/conf` or `%USERPROFILE%/.m2`

You are all set with a local development environment and your own branch of the Mule source code. You're ready. Get kicking!

The following steps briefly outline the development life-cycle to implement and commit your changes in **preparation for submission**.

1. If you are using an IDE, make sure you read the previous section about [IDE configuration](#configuring-the-ide).
2. Review the [Mule Coding Style](STYLE.md) documentation to ensure you adhere to source code standards, thus increasing the likelihood that your changes will be merged with the `mule-3.x` (i.e. master) source code.
3. **Import** the Mule source code project into your IDE (if you are using one), then work on your changes, fixes or improvements.
4. **Debug** and test your local version, resolving any issues that arise.
5. **Save** your changes locally.
6. **Prepare** your changes for a **Pull Request** by first squashing your changes into a single commit on your branch using the command:
    ```
    git rebase -i mule-3.x
    ```
7. **Push** your squashed commit to your branch on your GitHub repository. Refer to [Git's documentation](http://git-scm.com/book/en/v2/Git-Basics-Recording-Changes-to-the-Repository) for details on how to commit your changes.
8. **Regularly update your branch** with any changes or fixes applied to the `develop` branch (remember this is the repository that will contain the latest changes of the connector). Refer to details below.

### Updating Your Feature Branch

To ensure that your cloned version of Mule source code remains **up-to-date** with any changes uploaded to the `mule-3.x` (i.e. master) branch, you should regularly update the latter to **rebase** off the latest version of the `master`.

1. **Pull** the latest changes from the *upstream* **master** branch `mule-3.x` using the following commands:
    ```
    git fetch upstream
    git fetch upstream --tags
    ```

2. **Ensure** you are working with the master branch using the following command:
    ```
    git checkout mule-3.x
    ```

23. **Merge** the latest changes and updates from the **master** branch to your feature branch:
    ```
    git merge upstream/mule-3.x
    ```

3. **Push** any changes to the **master** to your forked clone:

    ```
    git push origin mule-3.x
    git push origin --tags
    ```

4. Access your **feature** branch once again (to continue coding) by running:

    ```
    git checkout dev/yourRepoName/bug/yourJIRAissuenumber
    ```

5. **Rebase** your branch from the latest version of the **master** branch with:

    ```
    git rebase mule-3.x
    ```

6. **Resolve** any conflicts on your **feature** branch that may appear as a result of the changes to `mule-3.x` (i.e. master).

7. **Push** the newly-rebased branch back to your fork on your Git repository using the following command:

    ```
    git push origin dev/yourRepoName/bug/yourJIRAissuenumber -f
    ```

###  Submitting a Pull Request

Ready to submit your patch for review and merging? Initiate a **Pull Request (PR)** on GitHub!

1. Review the [MuleSoft Contributor's Agreement](http://www.mulesoft.org/legal/contributor-agreement.html). Before any contribution is accepted, we need you to **run** the following notebook [script](https://api-notebook.anypoint.mulesoft.com/notebooks#bc1cf75a0284268407e4). This script will ask you to login to GitHub and **accept** our Contributor's Agreement, thus resulting in the creation of an issue in our contributors project with your name.
> Once you access the **MuleSoft Contributor's Agreement web site**, perform these actions:
    * Hit the **Play notebook** button (or the Run link) and follow the
    instructions as you are prompted through the screens.
    * Authenticate.
    * Register Your Name.
    * Accept the Agreement.
    * That's it. You're ready to contribute!
2. From the repository of your branch, click the **Pull Request** button.
3. In the **Pull Request Preview** dialog, provide a title and an optional description of your changes.
4. Review the commits that are part of your PR, then click **Send Pull Request**.
> Refer to GitHub's [detailed instructions](https://help.github.com/articles/using-pull-requests) for submitting a pull request.
5. **MuleSoft's Core Developer Team** will review the PR and may initiate discussion or ask questions about your changes in a **Pull Request Discussion**. If appropriate, the team will then merge your commits within the **master** branch. Acceptance of the Agreement will be validated at this stage.
> If you made **changes or corrections** to your commit **after** having submitted the PR, go back to the PR page and update the **Commit Range** (via the Commits tab), rather than submitting a new Pull Request.

## Summary

This guide started with pointing to different [sources of information](#starting-points) around MuleSoft and the Mule's [community meeting points](#visiting-the-community-meeting-points) on the web. These were useful for understanding where Mule is going and to have contact mechanisms with the rest of the Community for help or to spark discussion.

In order to set up our [development environment](#setting-up-the-development-environment), we got to [install some prerequisites](#installing-prerequisites). Once we had them ready, we downloaded the [source code](#getting-the-source-code).

At that point we were almost ready to develop our improvements. We just needed to [configure our favorite IDE](#configuring-the-ide) to develop or debug Mule code.

Afterwards, we were finally ready to [develop our contribution](#developing-your-contribution): 

* we created our very own [feature branch](#creating-your-feature-branch) to develop the improvement,
* we learnt how to [keep it updated](#updating-your-feature-branch) in order to
* submit a [pull request](#submitting-a-pull-request) to the main MuleSoft Mule Runtime repository.

---
#### **Thank you**, once again, for taking the time to contribute to **MuleSoft's Runtime**.
