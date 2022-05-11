/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.runtime.module.deployment.internal;

import static org.mule.maven.client.api.model.MavenConfiguration.newMavenConfigurationBuilder;
import static org.mule.maven.client.api.model.RemoteRepository.newRemoteRepositoryBuilder;
import static org.mule.runtime.container.api.MuleFoldersUtil.getAppFolder;
import static org.mule.runtime.container.api.MuleFoldersUtil.getMuleBaseFolder;
import static org.mule.runtime.container.internal.ClasspathModuleDiscoverer.EXPORTED_CLASS_PACKAGES_PROPERTY;
import static org.mule.runtime.globalconfig.api.GlobalConfigLoader.setMavenConfig;
import static org.mule.test.allure.AllureConstants.DeploymentTypeFeature.DEPLOYMENT_TYPE;

import static java.lang.Boolean.FALSE;
import static java.lang.Boolean.TRUE;
import static java.util.Arrays.asList;
import static java.util.Optional.ofNullable;
import static java.util.stream.Collectors.toList;

import static com.github.valfirst.slf4jtest.TestLoggerFactory.getTestLogger;
import static org.apache.commons.io.FileUtils.iterateFiles;
import static org.apache.commons.io.filefilter.HiddenFileFilter.VISIBLE;
import static org.hamcrest.Matchers.allOf;
import static org.hamcrest.Matchers.endsWith;
import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.not;
import static org.hamcrest.Matchers.startsWith;
import static org.junit.Assert.assertThat;

import org.mule.maven.client.api.model.MavenConfiguration.MavenConfigurationBuilder;
import org.mule.runtime.module.deployment.impl.internal.builder.ApplicationFileBuilder;
import org.mule.runtime.module.deployment.impl.internal.builder.ArtifactPluginFileBuilder;

import java.io.File;
import java.net.MalformedURLException;
import java.net.URL;
import java.nio.file.Paths;
import java.util.Collection;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.Optional;

import com.github.valfirst.slf4jtest.LoggingEvent;
import com.github.valfirst.slf4jtest.TestLogger;
import io.qameta.allure.Description;
import io.qameta.allure.Feature;
import io.qameta.allure.Issues;
import io.qameta.allure.Issue;
import io.qameta.allure.Stories;
import io.qameta.allure.Story;
import org.junit.Test;
import org.junit.runners.Parameterized;

@Feature(DEPLOYMENT_TYPE)
public class DeploymentTypeAppControlTestCase extends AbstractApplicationDeploymentTestCase {

  private static final String APP_XML_FILE = "simple.xml";
  private static final String HEAVYWEIGHT_APP = "heavyweight";
  private static final String LIGHTWEIGHT_APP = "lightweight";
  private static final String JAR_EXTENSION = ".jar";
  private static final String MAVEN_REPOSITORY_ID = "mulesoft-public";
  private static final String MULE_CORE_DEPENDENCY = "mule-core";
  private static final String MULE_PLUGIN_CLASSIFIER = "mule-plugin";
  private static final String MULE_PLUGIN_EXTENSION_NAME = "-mule-plugin.jar";
  private static final String MULE_PLUGIN_GROUP_ID = "org.mule.connectors";
  private static final String MULE_PLUGIN_NAME = "mule-sockets-connector";
  private static final String MULE_PLUGIN_PROPERTY_VALUE = "org.foo";
  private static final String MULE_PLUGIN_VERSION = "1.2.0";
  private static final String MULESOFT_PUBLIC_REPOSITORY = "https://repository.mulesoft.org/nexus/content/repositories/public/";
  private static final String REPOSITORY_PATH = "repository";
  private static final int ERROR_LEVEL = 1;
  private static final String EXCEPTION_ERROR_MESSAGE =
      "org.eclipse.aether.transfer.ArtifactNotFoundException: Could not find artifact org.mule.connectors:mule-sockets-connector";
  private static final TestLogger logger = getTestLogger(DefaultArchiveDeployer.class);

  private final String applicationWeight;
  private final boolean mulePluginMustBeResolved;
  private final boolean applicationRepositoryMustExist;
  private final boolean muleRepositoryMustExist;

  @Parameterized.Parameters(
      name = "Parallel: {0}, App Weight: {1}, Mule Plugin Must be Resolved: {2}, Application Repository Must Exist: {3}, Mule Repository Must Exist: {4}")
  public static List<Object[]> parameters() {
    return asList(new Object[][] {
        {FALSE, HEAVYWEIGHT_APP, TRUE, TRUE, FALSE},
        {FALSE, LIGHTWEIGHT_APP, TRUE, FALSE, TRUE},
        {FALSE, LIGHTWEIGHT_APP, FALSE, FALSE, FALSE},
    });
  }

  public DeploymentTypeAppControlTestCase(boolean parallelDeployment, String applicationWeight, boolean mulePluginMustBeResolved,
                                          boolean applicationRepositoryMustExist, boolean muleRepositoryMustExist) {
    super(parallelDeployment);
    this.applicationWeight = applicationWeight;
    this.mulePluginMustBeResolved = mulePluginMustBeResolved;
    this.applicationRepositoryMustExist = applicationRepositoryMustExist;
    this.muleRepositoryMustExist = muleRepositoryMustExist;
  }

  @Test
  @Issues({@Issue("MULE-12298"), @Issue("MULE-12317")})
  @Stories({@Story(HEAVYWEIGHT_APP), @Story(LIGHTWEIGHT_APP)})
  @Description("Verifies that the parameterized app has the correct deployment result based on its configuration and deployment type.")
  public void appDeploymentTypeControlTest() throws Exception {
    // Creates the app.
    final ApplicationFileBuilder applicationFileBuilder = getApplicationFileBuilder();
    addPackedAppFromBuilder(applicationFileBuilder);

    // Configures the maven repository.
    setMavenConfiguration();

    // Deploys the app.
    startDeployment();

    // Asserts the expected deployment result.
    assertDeploymentTypeCorrectlyManaged(applicationFileBuilder);
  }

  private void assertDeploymentTypeCorrectlyManaged(ApplicationFileBuilder applicationFileBuilder) {
    final String applicationId = applicationFileBuilder.getId();
    final String applicationName = applicationFileBuilder.getDeployedPath();
    final File applicationRepository = Paths.get(getAppFolder(applicationName).toString(), REPOSITORY_PATH).toFile();
    final File muleRepository = Paths.get(getMuleBaseFolder().getAbsolutePath(), REPOSITORY_PATH).toFile();

    assertExpectedDeploymentResult(applicationName, applicationId);
    assertExpectedRepositoryExistence(applicationRepository, muleRepository);
    assertExpectedRepositoryContent(applicationRepository, muleRepository);
  }

  private void assertExpectedDeploymentResult(String applicationName, String applicationId) {
    if (mulePluginMustBeResolved) {
      assertDeploymentSuccess(applicationDeploymentListener, applicationId);
      assertApplicationAnchorFileExists(applicationName);
    } else {
      assertDeploymentFailure(applicationDeploymentListener, applicationId);
      assertApplicationAnchorFileDoesNotExists(applicationName);
      assertThat(getLogCauseMessages(logger.getAllLoggingEvents()), hasItem(startsWith(EXCEPTION_ERROR_MESSAGE)));
    }
  }

  private void assertExpectedRepositoryExistence(File applicationRepository, File muleRepository) {
    assertThat(applicationRepository.exists(), is(applicationRepositoryMustExist));
    assertThat(muleRepository.exists(), is(muleRepositoryMustExist));
  }

  private void assertExpectedRepositoryContent(File applicationRepository, File muleRepository) {
    if (mulePluginMustBeResolved) {
      final List<String> repositoryContentsNames;

      if (applicationWeight.equals(LIGHTWEIGHT_APP)) {
        repositoryContentsNames = getRepositoryContents(muleRepository).stream().map(File::getName).collect(toList());
        assertThat(repositoryContentsNames, not(hasItem(allOf(startsWith(MULE_CORE_DEPENDENCY), endsWith(JAR_EXTENSION)))));
      } else {
        repositoryContentsNames = getRepositoryContents(applicationRepository).stream().map(File::getName).collect(toList());
      }

      assertThat(repositoryContentsNames, hasItem(allOf(startsWith(MULE_PLUGIN_NAME), endsWith(MULE_PLUGIN_EXTENSION_NAME))));
    }
  }

  private void setMavenConfiguration() throws MalformedURLException {
    if (applicationWeight.equals(LIGHTWEIGHT_APP) && mulePluginMustBeResolved) {
      final File muleRepository = Paths.get(getMuleBaseFolder().getAbsolutePath(), REPOSITORY_PATH).toFile();

      MavenConfigurationBuilder mavenConfigurationBuilder = newMavenConfigurationBuilder();
      mavenConfigurationBuilder.remoteRepository(newRemoteRepositoryBuilder()
          .id(MAVEN_REPOSITORY_ID)
          .url(new URL(MULESOFT_PUBLIC_REPOSITORY))
          .build());
      mavenConfigurationBuilder.localMavenRepositoryLocation(muleRepository);
      setMavenConfig(mavenConfigurationBuilder.build());
    }
  }

  private ApplicationFileBuilder getApplicationFileBuilder() {
    ArtifactPluginFileBuilder pluginBuilder = new ArtifactPluginFileBuilder(MULE_PLUGIN_NAME)
        .withGroupId(MULE_PLUGIN_GROUP_ID)
        .withVersion(MULE_PLUGIN_VERSION)
        .withClassifier(MULE_PLUGIN_CLASSIFIER)
        .configuredWith(EXPORTED_CLASS_PACKAGES_PROPERTY, MULE_PLUGIN_PROPERTY_VALUE);

    ApplicationFileBuilder applicationFileBuilder = appFileBuilder(applicationWeight)
        .definedBy(APP_XML_FILE)
        .dependingOn(pluginBuilder);

    if (applicationWeight.equals(LIGHTWEIGHT_APP)) {
      applicationFileBuilder.usingLightWeightPackage();
    }

    return applicationFileBuilder;
  }

  private Collection<File> getRepositoryContents(File repository) {
    if (repository.exists() && repository.isDirectory()) {
      List<File> repositoryFiles = new LinkedList<>();

      final Iterator<File> iterateFiles = iterateFiles(repository, VISIBLE, VISIBLE);
      while (iterateFiles.hasNext()) {
        File currentFile = iterateFiles.next();
        if (currentFile.isFile()) {
          repositoryFiles.add(currentFile);
        }
      }

      return repositoryFiles;
    } else {
      throw new NoSuchElementException("No internal repository found");
    }
  }

  private List<String> getLogCauseMessages(List<LoggingEvent> loggingEvents) {
    List<String> logCauseMessages = new LinkedList<>();
    Optional<Throwable> logCause = loggingEvents.get(ERROR_LEVEL).getThrowable();

    while (logCause.isPresent()) {
      logCauseMessages.add(logCause.get().toString());
      logCause = ofNullable(logCause.get().getCause());
    }

    return logCauseMessages;
  }
}
