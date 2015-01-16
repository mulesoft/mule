# Building Mule

[Gradle](http://www.gradle.org) is used to build mule ESB. To modify or add to Mule source code, it's specially relevant to understand the hierarchy of maven projects that represent the Mule source code.

To be able to build Mule you will need Maven among other things. Refer to the [CONTRIBUTE.md](CONTRIBUTE.md) file to find the installation instructions and prerequisites necessary to build Mule.

In the following sections you will learn how Mule is organized, how to build Mule and how to troubleshoot problems we might find.


### Mule Source Code Sub-projects

The Mule source code project is neatly organized into a hierarchy of sub-projects. (Sub-projects are sometimes referred to as "modules," but to avoid confusion with Mule modules, we use "sub-projects".) Each sub-project generally produces one Maven artifact, usually a JAR file. The Mule build is structured using intermediate project object models (POMs) to group common sub-projects together. These intermediate sub-projects are sub-folders of the source code's mule folder, and are described in the table below.

| Sub-project       | Description  |
|-------------------|--------------|
| `core`            | Mule's core API and functionality that is required by all transports distributions       |
| `modules`         | non-transport extensions to Mule, such as XML processing, Spring extras, or scripting support
| `tests`           | tests that can be run as part of the Mule build |
| `transports`      | Mule transports such as the JMS, VM, and TCP transports |

### Building Mule

To build Mule ESB, run Gradle wrapper (gradlew) found in the root of the project. The script will take care of downloading the correct Gradl version if
it is not already available.


|Command | Description |
|:----------|:-------------|
| `gradlew clean`	       | purges any built artifacts or intermediate files (such as .class) from the target directory. Note that Gradle takes care of incremental changes, and a full clean is not necessary needed |
| `gradlew build`	       | builds and test the module |
| `gradlew install`        | installs each module artifact into the local maven repository (no tests are run) |
| `gradlew test`           | runs any unit tests for this sub-project |
| `gradlew build -x test`  | Builds the module skipping tests |
| `gradlew distribution`   | builds and verifies the distribution |

You can scope the commands to a specific sub project by either running the command on that folder, or refering to the task via it's absolute name, for example:
`gradle :module-db:build`


#### Distributions

When you package Mule for distribution, the target files are created in the buid/distributions project. While historically there were several distribution types,
Mule ESB has only one distribution (th former Full Standalone Server).
To use Mule in embedded scenarios, either take jars from the distribution, or use maven dependencies to bring the required artifacts into your porject.


### Uploading artifacts

In order to upload artifacts, you need to have a gpg key to sign files, and provide credentials
to the nexus repository. Define these in GRADLE_HOME/gradle.properties:

    signing.keyId=...
    signing.password=...
    signing.secretKeyRingFile=...

    nexusUser=...
    nexusPassword=...

Then the following tasks can be used:
|Command | Description |
|:----------|:-------------|
| `gradlew uploadDistribution` | uploads the distribution to the repository |
| `gradlew uploadArchives`	   | uploads all the artifacts to the repository |

If no nexus username and password are defined, the distributions will get uploaded
to a temporal repo in the `build` folder.

