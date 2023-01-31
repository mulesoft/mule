/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.runtime.module.deployment.internal;

import static org.mule.runtime.container.internal.ClasspathModuleDiscoverer.EXPORTED_CLASS_PACKAGES_PROPERTY;
import static org.mule.runtime.deployment.model.api.DeployableArtifactDescriptor.PROPERTY_CONFIG_RESOURCES;
import static org.mule.runtime.deployment.model.api.artifact.ArtifactDescriptorConstants.SERIALIZED_ARTIFACT_AST_LOCATION;
import static org.mule.runtime.module.deployment.internal.TestArtifactsCatalog.barUtils1_0JarFile;
import static org.mule.runtime.module.deployment.internal.TestArtifactsCatalog.callbackExtensionPlugin;
import static org.mule.runtime.module.deployment.internal.TestArtifactsCatalog.echoTestClassFile;
import static org.mule.runtime.module.deployment.internal.TestArtifactsCatalog.pluginEcho1TestClassFile;
import static org.mule.runtime.module.deployment.internal.util.DeploymentServiceTestUtils.redeploy;
import static org.mule.test.allure.AllureConstants.ArtifactDeploymentFeature.APP_DEPLOYMENT;
import static org.mule.test.allure.AllureConstants.DeploymentTypeFeature.RedeploymentStory.APPLICATION_REDEPLOYMENT;

import static java.io.File.separator;
import static java.util.Arrays.asList;

import static org.apache.commons.io.FileUtils.copyFile;
import static org.apache.commons.io.FileUtils.deleteQuietly;
import static org.apache.commons.io.FileUtils.writeStringToFile;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.Is.is;
import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

import org.mule.runtime.module.deployment.api.DeploymentListener;
import org.mule.runtime.module.deployment.impl.internal.builder.ApplicationFileBuilder;
import org.mule.runtime.module.deployment.impl.internal.builder.ArtifactPluginFileBuilder;
import org.mule.runtime.module.deployment.impl.internal.builder.JarFileBuilder;
import org.mule.tck.junit4.rule.SystemProperty;

import java.io.File;
import java.net.URL;
import java.util.List;
import java.util.concurrent.locks.ReentrantLock;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runners.Parameterized.Parameters;

import io.qameta.allure.Feature;
import io.qameta.allure.Story;

/**
 * Contains test for application re-deployment on the default domain
 */
@Feature(APP_DEPLOYMENT)
@Story(APPLICATION_REDEPLOYMENT)
public class ApplicationRedeploymentTestCase extends AbstractApplicationDeploymentTestCase {

  private static final String OVERWRITTEN_PROPERTY = "configFile";
  private static final String OVERWRITTEN_PROPERTY_SYSTEM_VALUE = "nonExistent.yaml";

  protected static ApplicationFileBuilder dummyAppDescriptorWithPropsDependencyFileBuilder;

  @Rule
  public SystemProperty systemProperty = new SystemProperty(OVERWRITTEN_PROPERTY, OVERWRITTEN_PROPERTY_SYSTEM_VALUE);

  @Rule
  public SystemProperty otherSystemProperty = new SystemProperty("oneProperty", "someValue");

  public ApplicationRedeploymentTestCase(boolean parallelDeployment) {
    super(parallelDeployment);
  }

  @Override
  @Before
  public void before() {
    incompleteAppFileBuilder = appFileBuilder("incomplete-app").definedBy("incomplete-app-config.xml");
    brokenAppFileBuilder = appFileBuilder("broken-app").corrupted();
    brokenAppWithFunkyNameAppFileBuilder = appFileBuilder("broken-app+", brokenAppFileBuilder);
    waitAppFileBuilder = appFileBuilder("wait-app").definedBy("wait-app-config.xml");
    dummyAppDescriptorWithPropsFileBuilder = appFileBuilder("dummy-app-with-props")
        .definedBy("dummy-app-with-props-config.xml")
        .dependingOn(callbackExtensionPlugin)
        .containingClass(echoTestClassFile,
                         "org/foo/EchoTest.class");
    dummyAppDescriptorWithPropsDependencyFileBuilder = appFileBuilder("dummy-app-with-props-dependencies")
        .withMinMuleVersion("4.3.0") // MULE-19038
        .definedBy("dummy-app-with-props-dependencies-config.xml");
    dummyAppDescriptorWithStoppedFlowFileBuilder = appFileBuilder("dummy-app-with-stopped-flow-config")
        .withMinMuleVersion("4.3.0") // MULE-19127
        .definedBy("dummy-app-with-stopped-flow-config.xml")
        .dependingOn(callbackExtensionPlugin)
        .containingClass(echoTestClassFile,
                         "org/foo/EchoTest.class");

    // Application plugin artifact builders
    echoPluginWithLib1 = new ArtifactPluginFileBuilder("echoPlugin1")
        .configuredWith(EXPORTED_CLASS_PACKAGES_PROPERTY, "org.foo")
        .dependingOn(new JarFileBuilder("barUtils1", barUtils1_0JarFile))
        .containingClass(pluginEcho1TestClassFile, "org/foo/Plugin1Echo.class");
  }

  @Parameters(name = "Parallel: {0}")
  public static List<Boolean> params() {
    // Only run without parallel deployment since this configuration does not affect re-deployment at all
    return asList(false);
  }

  @Test
  public void removesPreviousAppFolderOnRedeploy() throws Exception {
    startDeployment();

    addPackedAppFromBuilder(emptyAppFileBuilder);

    assertApplicationDeploymentSuccess(applicationDeploymentListener, emptyAppFileBuilder.getId());

    assertAppsDir(NONE, new String[] {emptyAppFileBuilder.getId()}, true);
    assertEquals("Application has not been properly registered with Mule", 1, deploymentService.getApplications().size());

    reset(applicationDeploymentListener);

    ApplicationFileBuilder emptyAppFileBuilder =
        appFileBuilder("empty-app").usingResource("empty-config.xml", "empty-config.xml")
            .deployedWith(PROPERTY_CONFIG_RESOURCES, "empty-config.xml");

    addPackedAppFromBuilder(emptyAppFileBuilder);

    assertApplicationRedeploymentSuccess(emptyAppFileBuilder.getId());

    assertApplicationFiles(emptyAppFileBuilder.getId(), new String[] {"empty-config.xml"});
  }

  @Test
  public void redeploysAppZipDeployedAfterStartup() throws Exception {
    addPackedAppFromBuilder(emptyAppFileBuilder);

    startDeployment();

    assertApplicationDeploymentSuccess(applicationDeploymentListener, emptyAppFileBuilder.getId());

    assertAppsDir(NONE, new String[] {emptyAppFileBuilder.getId()}, true);
    assertEquals("Application has not been properly registered with Mule", 1, deploymentService.getApplications().size());

    reset(applicationDeploymentListener);

    addPackedAppFromBuilder(emptyAppFileBuilder);

    assertApplicationRedeploymentSuccess(emptyAppFileBuilder.getId());
    assertEquals("Application has not been properly registered with Mule", 1, deploymentService.getApplications().size());
    assertAppsDir(NONE, new String[] {emptyAppFileBuilder.getId()}, true);
  }

  @Test
  public void redeploysExplodedAppOnStartup() throws Exception {
    addExplodedAppFromBuilder(dummyAppDescriptorFileBuilder);

    startDeployment();

    assertApplicationDeploymentSuccess(applicationDeploymentListener, dummyAppDescriptorFileBuilder.getId());
    assertAppsDir(NONE, new String[] {dummyAppDescriptorFileBuilder.getId()}, true);

    reset(applicationDeploymentListener);

    File configFile = new File(appsDir + separator + dummyAppDescriptorFileBuilder.getDeployedPath(),
                               getConfigFilePathWithinArtifact(MULE_CONFIG_XML_FILE));
    configFile.setLastModified(configFile.lastModified() + FILE_TIMESTAMP_PRECISION_MILLIS);

    assertApplicationRedeploymentSuccess(dummyAppDescriptorFileBuilder.getId());
  }

  @Test
  public void redeploysExplodedAppAfterStartup() throws Exception {
    startDeployment();

    addExplodedAppFromBuilder(emptyAppFileBuilder);

    assertApplicationDeploymentSuccess(applicationDeploymentListener, emptyAppFileBuilder.getId());
    assertAppsDir(NONE, new String[] {emptyAppFileBuilder.getId()}, true);

    reset(applicationDeploymentListener);

    File configFile =
        new File(appsDir + "/" + emptyAppFileBuilder.getDeployedPath(), getConfigFilePathWithinArtifact(MULE_CONFIG_XML_FILE));
    assertThat("Configuration file does not exists", configFile.exists(), is(true));
    assertThat("Could not update last updated time in configuration file",
               configFile.setLastModified(configFile.lastModified() + FILE_TIMESTAMP_PRECISION_MILLIS), is(true));

    assertApplicationRedeploymentSuccess(emptyAppFileBuilder.getId());
  }

  @Test
  public void redeploysBrokenExplodedAppOnStartup() throws Exception {
    addExplodedAppFromBuilder(incompleteAppFileBuilder);

    startDeployment();

    assertDeploymentFailure(applicationDeploymentListener, incompleteAppFileBuilder.getId());

    // Maintains app dir created
    assertAppsDir(NONE, new String[] {incompleteAppFileBuilder.getId()}, true);
    assertArtifactIsRegisteredAsZombie(incompleteAppFileBuilder.getId(), deploymentService.getZombieApplications());

    reset(applicationDeploymentListener);

    final ReentrantLock lock = deploymentService.getLock();
    lock.lock();
    try {
      File configFile = new File(appsDir + "/" + incompleteAppFileBuilder.getDeployedPath(),
                                 getConfigFilePathWithinArtifact(MULE_CONFIG_XML_FILE));
      assertThat(configFile.exists(), is(true));
      configFile.setLastModified(configFile.lastModified() + FILE_TIMESTAMP_PRECISION_MILLIS);
    } finally {
      lock.unlock();
    }

    assertFailedApplicationRedeploymentFailure(applicationDeploymentListener, incompleteAppFileBuilder.getId());
  }

  @Test
  public void redeploysBrokenExplodedAppAfterStartup() throws Exception {
    startDeployment();

    addExplodedAppFromBuilder(incompleteAppFileBuilder);

    assertDeploymentFailure(applicationDeploymentListener, incompleteAppFileBuilder.getId());

    // Maintains app dir created
    assertAppsDir(NONE, new String[] {incompleteAppFileBuilder.getId()}, true);
    assertArtifactIsRegisteredAsZombie(incompleteAppFileBuilder.getId(), deploymentService.getZombieApplications());

    reset(applicationDeploymentListener);

    File configFile = new File(appsDir + "/" + incompleteAppFileBuilder.getDeployedPath(),
                               getConfigFilePathWithinArtifact(MULE_CONFIG_XML_FILE));
    updateFileModifiedTime(configFile.lastModified(), configFile);

    assertFailedApplicationRedeploymentFailure(applicationDeploymentListener, incompleteAppFileBuilder.getId());
  }

  @Test
  public void redeploysInvalidExplodedAppAfterSuccessfulDeploymentOnStartup() throws Exception {
    addExplodedAppFromBuilder(dummyAppDescriptorFileBuilder);

    startDeployment();

    assertApplicationDeploymentSuccess(applicationDeploymentListener, dummyAppDescriptorFileBuilder.getId());

    assertAppsDir(NONE, new String[] {dummyAppDescriptorFileBuilder.getId()}, true);

    reset(applicationDeploymentListener);

    File originalConfigFile = new File(appsDir + "/" + dummyAppDescriptorFileBuilder.getDeployedPath(),
                                       getConfigFilePathWithinArtifact(MULE_CONFIG_XML_FILE));
    URL url = getClass().getResource(BROKEN_CONFIG_XML);
    File newConfigFile = new File(url.toURI());
    copyFile(newConfigFile, originalConfigFile);
    deleteQuietly(new File(appsDir + "/" + dummyAppDescriptorFileBuilder.getDeployedPath(),
                           getConfigFilePathWithinArtifact(SERIALIZED_ARTIFACT_AST_LOCATION)));

    assertApplicationRedeploymentFailure(dummyAppDescriptorFileBuilder.getId());
  }

  @Test
  public void redeploysInvalidExplodedAppAfterSuccessfulDeploymentAfterStartup() throws Exception {
    startDeployment();

    addExplodedAppFromBuilder(dummyAppDescriptorFileBuilder);

    assertApplicationDeploymentSuccess(applicationDeploymentListener, dummyAppDescriptorFileBuilder.getId());
    assertAppsDir(NONE, new String[] {dummyAppDescriptorFileBuilder.getId()}, true);

    reset(applicationDeploymentListener);

    File originalConfigFile = new File(appsDir + "/" + dummyAppDescriptorFileBuilder.getDeployedPath(),
                                       getConfigFilePathWithinArtifact(MULE_CONFIG_XML_FILE));
    assertThat(originalConfigFile.exists(), is(true));
    URL url = getClass().getResource(BROKEN_CONFIG_XML);
    File newConfigFile = new File(url.toURI());
    copyFile(newConfigFile, originalConfigFile);
    deleteQuietly(new File(appsDir + "/" + dummyAppDescriptorFileBuilder.getDeployedPath(),
                           getConfigFilePathWithinArtifact(SERIALIZED_ARTIFACT_AST_LOCATION)));

    assertApplicationRedeploymentFailure(dummyAppDescriptorFileBuilder.getId());
  }

  @Test
  public void redeploysFixedAppAfterBrokenExplodedAppOnStartup() throws Exception {
    addExplodedAppFromBuilder(incompleteAppFileBuilder);

    startDeployment();

    assertDeploymentFailure(applicationDeploymentListener, incompleteAppFileBuilder.getId());

    reset(applicationDeploymentListener);

    File originalConfigFile = new File(appsDir + "/" + incompleteAppFileBuilder.getDeployedPath(),
                                       getConfigFilePathWithinArtifact(MULE_CONFIG_XML_FILE));
    assertThat(originalConfigFile.exists(), is(true));
    URL url = getClass().getResource(EMPTY_APP_CONFIG_XML);
    File newConfigFile = new File(url.toURI());
    copyFile(newConfigFile, originalConfigFile);
    assertFailedApplicationRedeploymentSuccess(incompleteAppFileBuilder.getId());

    addPackedAppFromBuilder(emptyAppFileBuilder);
    assertApplicationDeploymentSuccess(applicationDeploymentListener, emptyAppFileBuilder.getId());

    // Check that the failed application folder is still there
    assertAppFolderIsMaintained(incompleteAppFileBuilder.getId());
  }

  @Test
  public void redeploysFixedAppAfterBrokenExplodedAppAfterStartup() throws Exception {
    startDeployment();

    addExplodedAppFromBuilder(incompleteAppFileBuilder);
    assertDeploymentFailure(applicationDeploymentListener, incompleteAppFileBuilder.getId());

    reset(applicationDeploymentListener);

    ReentrantLock deploymentLock = deploymentService.getLock();
    deploymentLock.lock();
    try {
      File originalConfigFile = new File(appsDir + "/" + incompleteAppFileBuilder.getDeployedPath(),
                                         getConfigFilePathWithinArtifact(MULE_CONFIG_XML_FILE));
      URL url = getClass().getResource(EMPTY_DOMAIN_CONFIG_XML);
      File newConfigFile = new File(url.toURI());
      copyFile(newConfigFile, originalConfigFile);
    } finally {
      deploymentLock.unlock();
    }

    assertFailedApplicationRedeploymentSuccess(incompleteAppFileBuilder.getId());

    addPackedAppFromBuilder(emptyAppFileBuilder);
    assertApplicationDeploymentSuccess(applicationDeploymentListener, emptyAppFileBuilder.getId());

    // Check that the failed application folder is still there
    assertAppFolderIsMaintained(incompleteAppFileBuilder.getId());
  }

  @Test
  public void redeploysZipAppOnConfigChanges() throws Exception {
    addPackedAppFromBuilder(dummyAppDescriptorFileBuilder);

    startDeployment();

    assertApplicationDeploymentSuccess(applicationDeploymentListener, dummyAppDescriptorFileBuilder.getId());

    assertAppsDir(NONE, new String[] {dummyAppDescriptorFileBuilder.getId()}, true);
    assertEquals("Application has not been properly registered with Mule", 1, deploymentService.getApplications().size());

    reset(applicationDeploymentListener);

    File configFile = new File(appsDir + "/" + dummyAppDescriptorFileBuilder.getDeployedPath(),
                               getConfigFilePathWithinArtifact(MULE_CONFIG_XML_FILE));
    configFile.setLastModified(configFile.lastModified() + FILE_TIMESTAMP_PRECISION_MILLIS);

    assertApplicationRedeploymentSuccess(dummyAppDescriptorFileBuilder.getId());
    assertEquals("Application has not been properly registered with Mule", 1, deploymentService.getApplications().size());
    assertAppsDir(NONE, new String[] {dummyAppDescriptorFileBuilder.getId()}, true);
  }

  @Test
  public void redeployedFailedAppAfterTouched() throws Exception {
    addExplodedAppFromBuilder(emptyAppFileBuilder);

    File appFolder = new File(appsDir.getPath(), emptyAppFileBuilder.getId());

    File configFile = new File(appFolder, getConfigFilePathWithinArtifact(MULE_CONFIG_XML_FILE));
    writeStringToFile(configFile, "you shall not pass");

    startDeployment();
    assertDeploymentFailure(applicationDeploymentListener, emptyAppFileBuilder.getId());
    reset(applicationDeploymentListener);

    URL url = getClass().getResource(EMPTY_DOMAIN_CONFIG_XML);
    copyFile(new File(url.toURI()), configFile);

    assertFailedApplicationRedeploymentSuccess(emptyAppFileBuilder.getId());
  }

  @Test
  public void redeploysZipAppAfterDeploymentErrorOnStartup() throws Exception {
    addPackedAppFromBuilder(incompleteAppFileBuilder);

    startDeployment();

    assertDeploymentFailure(applicationDeploymentListener, incompleteAppFileBuilder.getId());

    // Deploys another app to confirm that DeploymentService has execute the updater thread
    addPackedAppFromBuilder(emptyAppFileBuilder);

    assertApplicationDeploymentSuccess(applicationDeploymentListener, emptyAppFileBuilder.getId());

    // Deploys another app to confirm that DeploymentService has execute the updater thread
    addPackedAppFromBuilder(emptyAppFileBuilder, incompleteAppFileBuilder.getZipPath());
    assertFailedApplicationRedeploymentSuccess(incompleteAppFileBuilder.getId());

    assertNoZombiePresent(deploymentService.getZombieApplications());
  }

  @Test
  public void redeploysZipAppAfterDeploymentErrorAfterStartup() throws Exception {
    startDeployment();

    addPackedAppFromBuilder(incompleteAppFileBuilder);

    assertDeploymentFailure(applicationDeploymentListener, incompleteAppFileBuilder.getId());

    // Deploys another app to confirm that DeploymentService has execute the updater thread
    addPackedAppFromBuilder(emptyAppFileBuilder);
    assertApplicationDeploymentSuccess(applicationDeploymentListener, emptyAppFileBuilder.getId());

    // Deploys another app to confirm that DeploymentService has execute the updater thread
    addPackedAppFromBuilder(emptyAppFileBuilder, incompleteAppFileBuilder.getZipPath());
    assertFailedApplicationRedeploymentSuccess(incompleteAppFileBuilder.getId());

    assertNoZombiePresent(deploymentService.getZombieApplications());
  }

  @Test
  public void redeploysInvalidZipAppAfterSuccessfulDeploymentOnStartup() throws Exception {
    addPackedAppFromBuilder(emptyAppFileBuilder);

    startDeployment();

    assertApplicationDeploymentSuccess(applicationDeploymentListener, emptyAppFileBuilder.getId());

    reset(applicationDeploymentListener);

    addPackedAppFromBuilder(incompleteAppFileBuilder, emptyAppFileBuilder.getZipPath());

    assertApplicationRedeploymentFailure(emptyAppFileBuilder.getId());
  }

  @Test
  public void redeploysInvalidZipAppAfterSuccessfulDeploymentAfterStartup() throws Exception {
    startDeployment();

    addPackedAppFromBuilder(emptyAppFileBuilder);
    assertApplicationDeploymentSuccess(applicationDeploymentListener, emptyAppFileBuilder.getId());

    addPackedAppFromBuilder(incompleteAppFileBuilder, emptyAppFileBuilder.getZipPath());
    assertApplicationRedeploymentFailure(emptyAppFileBuilder.getId());
  }

  @Test
  public void redeploysInvalidZipAppAfterFailedDeploymentOnStartup() throws Exception {
    addPackedAppFromBuilder(incompleteAppFileBuilder);

    startDeployment();

    assertDeploymentFailure(applicationDeploymentListener, incompleteAppFileBuilder.getId());

    reset(applicationDeploymentListener);

    addPackedAppFromBuilder(incompleteAppFileBuilder);

    assertFailedApplicationRedeploymentFailure(applicationDeploymentListener, incompleteAppFileBuilder.getId());
  }

  @Test
  public void redeploysInvalidZipAppAfterFailedDeploymentAfterStartup() throws Exception {
    startDeployment();

    addPackedAppFromBuilder(incompleteAppFileBuilder);
    assertDeploymentFailure(applicationDeploymentListener, incompleteAppFileBuilder.getId());

    reset(applicationDeploymentListener);

    addPackedAppFromBuilder(incompleteAppFileBuilder);

    assertFailedApplicationRedeploymentFailure(applicationDeploymentListener, incompleteAppFileBuilder.getId());
  }

  @Test
  public void redeploysExplodedAppAfterDeploymentError() throws Exception {
    startDeployment();

    addPackedAppFromBuilder(incompleteAppFileBuilder);

    assertDeploymentFailure(applicationDeploymentListener, incompleteAppFileBuilder.getId());

    // Deploys another app to confirm that DeploymentService has execute the updater thread
    addPackedAppFromBuilder(emptyAppFileBuilder);

    assertApplicationDeploymentSuccess(applicationDeploymentListener, emptyAppFileBuilder.getId());

    // Redeploys a fixed version for incompleteApp
    addExplodedAppFromBuilder(emptyAppFileBuilder, incompleteAppFileBuilder.getId());

    assertFailedApplicationRedeploymentSuccess(incompleteAppFileBuilder.getId());
    assertNoZombiePresent(deploymentService.getZombieApplications());
  }

  @Test
  public void redeployMethodRedeploysIfApplicationIsAlreadyDeployedPacked() throws Exception {
    DeploymentListener mockDeploymentListener = spy(new DeploymentStatusTracker());
    deploymentService.addDeploymentListener(mockDeploymentListener);

    // Deploy an application (packed)
    addPackedAppFromBuilder(dummyAppDescriptorFileBuilder);
    startDeployment();

    // Application was deployed
    assertApplicationDeploymentSuccess(applicationDeploymentListener, dummyAppDescriptorFileBuilder.getId());
    verify(mockDeploymentListener, times(1)).onDeploymentSuccess(dummyAppDescriptorFileBuilder.getId());
    verify(mockDeploymentListener, times(0)).onRedeploymentSuccess(dummyAppDescriptorFileBuilder.getId());

    reset(mockDeploymentListener);

    // Redeploy by using redeploy method
    redeploy(deploymentService, dummyAppDescriptorFileBuilder.getArtifactFile().toURI());

    // Application was redeployed
    verify(mockDeploymentListener, times(1)).onRedeploymentSuccess(dummyAppDescriptorFileBuilder.getId());
  }

  @Test
  public void redeployMethodRedeploysIfApplicationIsAlreadyDeployedExploded() throws Exception {
    DeploymentListener mockDeploymentListener = spy(new DeploymentStatusTracker());
    deploymentService.addDeploymentListener(mockDeploymentListener);

    // Deploy an application (exploded)
    addExplodedAppFromBuilder(dummyAppDescriptorFileBuilder);
    startDeployment();

    // Application was deployed
    assertApplicationDeploymentSuccess(applicationDeploymentListener, dummyAppDescriptorFileBuilder.getId());
    verify(mockDeploymentListener, times(1)).onDeploymentSuccess(dummyAppDescriptorFileBuilder.getId());
    verify(mockDeploymentListener, times(0)).onRedeploymentSuccess(dummyAppDescriptorFileBuilder.getId());

    // Redeploy by using redeploy method
    redeploy(deploymentService, dummyAppDescriptorFileBuilder.getArtifactFile().toURI());

    // Application was redeployed
    verify(mockDeploymentListener, times(1)).onRedeploymentSuccess(dummyAppDescriptorFileBuilder.getId());
  }

}
