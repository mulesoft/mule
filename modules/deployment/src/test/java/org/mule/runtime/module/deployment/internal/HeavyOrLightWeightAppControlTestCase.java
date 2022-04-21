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
import static org.mule.test.allure.AllureConstants.DeploymentTypeFeature.DeploymentTypeStory.HEAVYWEIGHT;
import static org.mule.test.allure.AllureConstants.DeploymentTypeFeature.DeploymentTypeStory.LIGHTWEIGHT;

import static java.util.Arrays.asList;
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

import com.github.valfirst.slf4jtest.LoggingEvent;
import com.github.valfirst.slf4jtest.TestLogger;
import io.qameta.allure.Feature;
import io.qameta.allure.Issues;
import io.qameta.allure.Issue;
import io.qameta.allure.Story;
import org.junit.Test;
import org.junit.runners.Parameterized;

@Feature(DEPLOYMENT_TYPE)
public class HeavyOrLightWeightAppControlTestCase extends AbstractApplicationDeploymentTestCase {

  private static final int ERROR_LEVEL = 1;
  private static final String EXCEPTION_ERROR_MESSAGE =
      "org.eclipse.aether.transfer.ArtifactNotFoundException: Could not find artifact org.mule.connectors:mule-sockets-connector";
  private static final String APP_XML_FILE = "simple.xml";
  private static final String HEAVYWEIGHT_APP = "heavyweight";
  private static final String LIGHTWEIGHT_APP = "lightweight";
  private static final String MULE_CONNECTORS_GROUP_ID = "org.mule.connectors";
  private static final String MULE_PLUGIN_CLASSIFIER = "mule-plugin";
  private static final String MULE_PLUGIN_EXTENSION_NAME = "-mule-plugin.jar";
  private static final String MULE_PLUGIN_NAME = "mule-sockets-connector";
  private static final String MULESOFT_PUBLIC_REPOSITORY = "https://repository.mulesoft.org/nexus/content/repositories/public/";
  private static final TestLogger logger = getTestLogger(DefaultArchiveDeployer.class);

  @Parameterized.Parameters(name = "Parallel: {0}")
  public static List<Boolean> params() {
    return asList(false);
  }

  public HeavyOrLightWeightAppControlTestCase(boolean parallelDeployment) {
    super(parallelDeployment);
  }

  @Test
  @Story(LIGHTWEIGHT)
  public void lightweightAppDeploymentDownloadsDependenciesFromRemoteRepo() throws Exception {
    final File muleRepository = Paths.get(getMuleBaseFolder().getAbsolutePath(), "repository").toFile();
    setMavenConfiguration(muleRepository);

    final ApplicationFileBuilder applicationFileBuilder = getApplicationFileBuilder(LIGHTWEIGHT_APP);

    addPackedAppFromBuilder(applicationFileBuilder);
    startDeployment();

    final String applicationName = applicationFileBuilder.getDeployedPath();
    final File applicationRepository = Paths.get(getAppFolder(applicationName).toString(), "repository").toFile();
    final Collection<File> muleRepositoryContents = getRepositoryContents(muleRepository);
    final List<String> muleRepositoryContentNames = muleRepositoryContents.stream().map(File::getName).collect(toList());

    assertDeploymentSuccess(applicationDeploymentListener, applicationFileBuilder.getId());
    assertApplicationAnchorFileExists(applicationName);
    assertThat(applicationRepository.exists(), is(false));
    assertThat(muleRepositoryContentNames, hasItem(allOf(startsWith(MULE_PLUGIN_NAME), endsWith(MULE_PLUGIN_EXTENSION_NAME))));
    assertThat(muleRepositoryContentNames, not(hasItem(allOf(startsWith("mule-core"), endsWith(".jar")))));
  }

  @Test
  @Story(LIGHTWEIGHT)
  public void lightweightAppDeploymentFailsIfNoRemoteRepoConfigured() throws Exception {
    final ApplicationFileBuilder applicationFileBuilder = getApplicationFileBuilder(LIGHTWEIGHT_APP);

    addPackedAppFromBuilder(applicationFileBuilder);
    startDeployment();

    final String applicationName = applicationFileBuilder.getDeployedPath();
    final File applicationRepository = Paths.get(getAppFolder(applicationName).toString(), "repository").toFile();
    final File muleRepository = Paths.get(getMuleBaseFolder().getAbsolutePath(), "repository").toFile();
    final List<String> logCauseMessages = getLogCauseMessages(logger.getAllLoggingEvents());

    assertDeploymentFailure(applicationDeploymentListener, applicationFileBuilder.getId());
    assertApplicationAnchorFileDoesNotExists(applicationName);
    assertThat(logCauseMessages, hasItem(startsWith(EXCEPTION_ERROR_MESSAGE)));
    assertThat(applicationRepository.exists(), is(false));
    assertThat(muleRepository.exists(), is(false));
  }

  @Test
  @Story(HEAVYWEIGHT)
  @Issues({@Issue("MULE-12298"), @Issue("MULE-12317")})
  public void heavyweightAppDeploymentDoesntDownloadDependenciesFromRemoteRepo() throws Exception {
    final ApplicationFileBuilder applicationFileBuilder = getApplicationFileBuilder(HEAVYWEIGHT_APP);

    addPackedAppFromBuilder(applicationFileBuilder);
    startDeployment();

    final String applicationName = applicationFileBuilder.getDeployedPath();
    final File applicationRepository = Paths.get(getAppFolder(applicationName).toString(), "repository").toFile();
    final File muleRepository = Paths.get(getMuleBaseFolder().getAbsolutePath(), "repository").toFile();
    final List<String> applicationRepositoryContentNames =
        getRepositoryContents(applicationRepository).stream().map(File::getName).collect(toList());

    assertDeploymentSuccess(applicationDeploymentListener, applicationFileBuilder.getId());
    assertApplicationAnchorFileExists(applicationName);
    assertThat(applicationRepository.exists(), is(true));
    assertThat(applicationRepositoryContentNames, hasItem(allOf(startsWith(MULE_PLUGIN_NAME), endsWith(MULE_PLUGIN_EXTENSION_NAME))));
    assertThat(muleRepository.exists(), is(false));
  }

  private ApplicationFileBuilder getApplicationFileBuilder(String applicationWeight) {
    ApplicationFileBuilder applicationFileBuilder = appFileBuilder(applicationWeight)
        .definedBy(APP_XML_FILE)
        .dependingOn(getMulePlugin());

    if (applicationWeight.equals(LIGHTWEIGHT_APP)) {
      applicationFileBuilder.usingLightWeightPackage();
    }

    return applicationFileBuilder;
  }

  private ArtifactPluginFileBuilder getMulePlugin() {
    return new ArtifactPluginFileBuilder(MULE_PLUGIN_NAME)
        .withGroupId(MULE_CONNECTORS_GROUP_ID)
        .withVersion("1.2.0")
        .withClassifier(MULE_PLUGIN_CLASSIFIER)
        .configuredWith(EXPORTED_CLASS_PACKAGES_PROPERTY, "org.foo");
  }

  private void setMavenConfiguration(File repository) throws MalformedURLException {
    MavenConfigurationBuilder mavenConfigurationBuilder = newMavenConfigurationBuilder();
    mavenConfigurationBuilder.remoteRepository(newRemoteRepositoryBuilder()
        .id("mulesoft-public")
        .url(new URL(MULESOFT_PUBLIC_REPOSITORY))
        .build());
    mavenConfigurationBuilder.localMavenRepositoryLocation(repository);
    setMavenConfig(mavenConfigurationBuilder.build());
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
    Throwable logCause = loggingEvents.get(ERROR_LEVEL).getThrowable().get();

    while (logCause != null) {
      logCauseMessages.add(logCause.toString());
      logCause = logCause.getCause();
    }

    return logCauseMessages;
  }
}
