/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.runtime.module.deployment.internal;

import static java.lang.String.format;
import static java.lang.System.currentTimeMillis;
import static java.lang.Thread.currentThread;
import static java.util.Arrays.asList;
import static java.util.Collections.emptyMap;
import static java.util.Optional.empty;
import static org.apache.commons.io.FileUtils.deleteDirectory;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.not;
import static org.hamcrest.core.Is.is;
import static org.hamcrest.core.IsNull.notNullValue;
import static org.hamcrest.core.IsNull.nullValue;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;
import static org.junit.Assume.assumeThat;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mule.runtime.api.deployment.meta.Product.MULE;
import static org.mule.runtime.container.internal.ClasspathModuleDiscoverer.EXPORTED_CLASS_PACKAGES_PROPERTY;
import static org.mule.runtime.container.internal.ClasspathModuleDiscoverer.EXPORTED_RESOURCE_PROPERTY;
import static org.mule.runtime.core.api.config.MuleProperties.OBJECT_MULE_CONFIGURATION;
import static org.mule.runtime.core.internal.config.bootstrap.ClassLoaderRegistryBootstrapDiscoverer.BOOTSTRAP_PROPERTIES;
import static org.mule.runtime.core.internal.context.ArtifactStoppedPersistenceListener.ARTIFACT_STOPPED_LISTENER;
import static org.mule.runtime.deployment.model.api.DeployableArtifactDescriptor.PROPERTY_CONFIG_RESOURCES;
import static org.mule.runtime.deployment.model.api.application.ApplicationStatus.CREATED;
import static org.mule.runtime.deployment.model.api.application.ApplicationStatus.DESTROYED;
import static org.mule.runtime.deployment.model.api.application.ApplicationStatus.STARTED;
import static org.mule.runtime.deployment.model.api.application.ApplicationStatus.STOPPED;
import static org.mule.runtime.deployment.model.api.artifact.ArtifactDescriptorConstants.EXPORTED_PACKAGES;
import static org.mule.runtime.deployment.model.api.artifact.ArtifactDescriptorConstants.EXPORTED_RESOURCES;
import static org.mule.runtime.deployment.model.api.artifact.ArtifactDescriptorConstants.MULE_LOADER_ID;
import static org.mule.runtime.extension.api.loader.xml.XmlExtensionModelLoader.RESOURCE_XML;
import static org.mule.runtime.module.deployment.impl.internal.policy.PropertiesBundleDescriptorLoader.PROPERTIES_BUNDLE_DESCRIPTOR_LOADER_ID;
import static org.mule.runtime.module.deployment.impl.internal.util.DeploymentPropertiesUtils.resolveDeploymentProperties;
import static org.mule.runtime.module.deployment.internal.DefaultArchiveDeployer.START_ARTIFACT_ON_DEPLOYMENT_PROPERTY;
import static org.mule.runtime.module.deployment.internal.DeploymentDirectoryWatcher.DEPLOYMENT_APPLICATION_PROPERTY;
import static org.mule.runtime.module.deployment.internal.MuleDeploymentService.findSchedulerService;
import static org.mule.runtime.module.deployment.internal.TestApplicationFactory.createTestApplicationFactory;
import static org.mule.tck.MuleTestUtils.testWithSystemProperty;
import static org.mule.test.allure.AllureConstants.DeploymentConfiguration.DEPLOYMENT_CONFIGURATION;
import static org.mule.test.allure.AllureConstants.DeploymentConfiguration.FlowStatePersistenceStory.FLOW_STATE_PERSISTENCE;

import org.mule.runtime.api.component.ConfigurationProperties;
import org.mule.runtime.api.deployment.meta.MuleArtifactLoaderDescriptor;
import org.mule.runtime.api.deployment.meta.MuleArtifactLoaderDescriptorBuilder;
import org.mule.runtime.api.deployment.meta.MulePluginModel;
import org.mule.runtime.api.exception.MuleException;
import org.mule.runtime.core.api.config.MuleConfiguration;
import org.mule.runtime.core.api.config.MuleProperties;
import org.mule.runtime.core.api.construct.Flow;
import org.mule.runtime.core.api.extension.ExtensionManager;
import org.mule.runtime.core.api.util.func.CheckedRunnable;
import org.mule.runtime.deployment.model.api.application.Application;
import org.mule.runtime.deployment.model.api.application.ApplicationStatus;
import org.mule.runtime.deployment.model.internal.application.MuleApplicationClassLoaderFactory;
import org.mule.runtime.deployment.model.internal.nativelib.DefaultNativeLibraryFinderFactory;
import org.mule.runtime.extension.api.loader.xml.XmlExtensionModelLoader;
import org.mule.runtime.module.deployment.api.DeploymentListener;
import org.mule.runtime.module.deployment.impl.internal.MuleArtifactResourcesRegistry;
import org.mule.runtime.module.deployment.impl.internal.builder.ApplicationFileBuilder;
import org.mule.runtime.module.deployment.impl.internal.builder.ArtifactPluginFileBuilder;
import org.mule.runtime.module.deployment.impl.internal.builder.JarFileBuilder;
import org.mule.runtime.module.deployment.impl.internal.domain.DefaultDomainManager;
import org.mule.tck.junit4.rule.SystemProperty;

import java.io.File;
import java.net.URL;
import java.util.List;
import java.util.Optional;
import java.util.Properties;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.ReentrantLock;

import org.apache.commons.io.IOUtils;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;

import io.qameta.allure.Feature;
import io.qameta.allure.Issue;
import io.qameta.allure.Story;

/**
 * Contains test for application deployment on the default domain
 */
public class ApplicationDeploymentTestCase extends AbstractApplicationDeploymentTestCase {

  private static final String OVERWRITTEN_PROPERTY = "configFile";
  private static final String OVERWRITTEN_PROPERTY_SYSTEM_VALUE = "nonExistent.yaml";
  private static final String OVERWRITTEN_PROPERTY_DEPLOYMENT_VALUE = "someProps.yaml";

  protected static ApplicationFileBuilder dummyAppDescriptorWithPropsDependencyFileBuilder;

  @Rule
  public SystemProperty systemProperty = new SystemProperty(OVERWRITTEN_PROPERTY, OVERWRITTEN_PROPERTY_SYSTEM_VALUE);

  @Rule
  public SystemProperty otherSystemProperty = new SystemProperty("oneProperty", "someValue");

  public ApplicationDeploymentTestCase(boolean parallelDeployment) {
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
        .containingClass(echoTestClassFile,
                         "org/foo/EchoTest.class");
    dummyAppDescriptorWithPropsDependencyFileBuilder = appFileBuilder("dummy-app-with-props-dependencies")
        .withMinMuleVersion("4.3.0") // MULE-19038
        .definedBy("dummy-app-with-props-dependencies-config.xml");
    dummyAppDescriptorWithStoppedFlowFileBuilder = appFileBuilder("dummy-app-with-stopped-flow-config")
        .withMinMuleVersion("4.3.0") // MULE-19127
        .definedBy("dummy-app-with-stopped-flow-config.xml")
        .containingClass(echoTestClassFile,
                         "org/foo/EchoTest.class");

    // Application plugin artifact builders
    echoPluginWithLib1 = new ArtifactPluginFileBuilder("echoPlugin1")
        .configuredWith(EXPORTED_CLASS_PACKAGES_PROPERTY, "org.foo")
        .dependingOn(new JarFileBuilder("barUtils1", barUtils1_0JarFile))
        .containingClass(pluginEcho1TestClassFile, "org/foo/Plugin1Echo.class");
  }

  @Test
  public void deploysAppZipOnStartup() throws Exception {
    addPackedAppFromBuilder(dummyAppDescriptorFileBuilder);

    startDeployment();

    assertApplicationDeploymentSuccess(applicationDeploymentListener, dummyAppDescriptorFileBuilder.getId());
    assertAppsDir(NONE, new String[] {dummyAppDescriptorFileBuilder.getId()}, true);
    assertApplicationAnchorFileExists(dummyAppDescriptorFileBuilder.getId());

    // just assert no privileged entries were put in the registry
    final Application app = findApp(dummyAppDescriptorFileBuilder.getId(), 1);

    // Checks that the configuration's ID was properly configured
    assertThat(app.getRegistry().<MuleConfiguration>lookupByName(OBJECT_MULE_CONFIGURATION).get().getId(),
               equalTo(dummyAppDescriptorFileBuilder.getId()));
  }

  @Test
  public void extensionManagerPresent() throws Exception {
    final Application app = deployApplication(emptyAppFileBuilder);
    assertThat(app.getRegistry().<ExtensionManager>lookupByName(MuleProperties.OBJECT_EXTENSION_MANAGER).get(),
               is(notNullValue()));
  }

  @Test
  public void appHomePropertyIsPresent() throws Exception {
    final ApplicationFileBuilder globalPropertyAppFileBuilder =
        appFileBuilder("property-app").definedBy("app-properties-config.xml");

    addExplodedAppFromBuilder(globalPropertyAppFileBuilder);

    startDeployment();
    assertApplicationDeploymentSuccess(applicationDeploymentListener, globalPropertyAppFileBuilder.getId());

    final Application app = findApp(globalPropertyAppFileBuilder.getId(), 1);

    Optional<ConfigurationProperties> configurationProperties =
        app.getRegistry().lookupByType(ConfigurationProperties.class);
    assertThat(configurationProperties.isPresent(), is(true));

    File appHome = new File(configurationProperties.get().resolveStringProperty("appHome")
        .orElseThrow(() -> new RuntimeException("Could not find property appHome")));
    assertThat(appHome.exists(), is(true));
    assertThat(appHome.getName(), is(globalPropertyAppFileBuilder.getId()));
  }

  @Test
  public void deploysExplodedAppAndVerifyAnchorFileIsCreatedAfterDeploymentEnds() throws Exception {
    Action deployExplodedWaitAppAction = () -> addExplodedAppFromBuilder(waitAppFileBuilder);
    deploysAppAndVerifyAnchorFileIsCreatedAfterDeploymentEnds(deployExplodedWaitAppAction);
  }

  @Test
  public void deploysPackagedAppAndVerifyAnchorFileIsCreatedAfterDeploymentEnds() throws Exception {
    Action deployPackagedWaitAppAction = () -> addPackedAppFromBuilder(waitAppFileBuilder);
    deploysAppAndVerifyAnchorFileIsCreatedAfterDeploymentEnds(deployPackagedWaitAppAction);
  }

  @Test
  public void deploysAppZipAfterStartup() throws Exception {
    deployAfterStartUp(dummyAppDescriptorFileBuilder);
  }

  @Test
  public void deploysAppZipWithExtensionUpperCaseAfterStartup() throws Exception {
    final ApplicationFileBuilder dummyAppDescriptorFileBuilderWithUpperCaseInExtension =
        appFileBuilder("dummy-app", true)
            .definedBy("dummy-app-config.xml").configuredWith("myCustomProp", "someValue")
            .containingClass(echoTestClassFile, "org/foo/EchoTest.class");

    deployAfterStartUp(dummyAppDescriptorFileBuilderWithUpperCaseInExtension);
  }

  @Test
  public void deploysAppWithNonExistentConfigResourceOnDeclaration() throws Exception {
    ApplicationFileBuilder appBundleNonExistentConfigResource = appFileBuilder("non-existent-app-config-resource")
        .definedBy("empty-config.xml").deployedWith(PROPERTY_CONFIG_RESOURCES, "mule-non-existent-config.xml");

    addPackedAppFromBuilder(appBundleNonExistentConfigResource);
    startDeployment();

    assertDeploymentFailure(applicationDeploymentListener, appBundleNonExistentConfigResource.getId());
  }


  @Test
  public void deploysBrokenAppZipOnStartup() throws Exception {
    addPackedAppFromBuilder(brokenAppFileBuilder);

    startDeployment();

    assertDeploymentFailure(applicationDeploymentListener, brokenAppFileBuilder.getId());

    assertAppsDir(new String[] {brokenAppFileBuilder.getDeployedPath()}, NONE, true);

    assertApplicationAnchorFileDoesNotExists(brokenAppFileBuilder.getId());

    assertArtifactIsRegisteredAsZombie(brokenAppFileBuilder.getDeployedPath(), deploymentService.getZombieApplications());
  }

  @Test
  public void deployAndRedeployAppWithDeploymentProperties() throws Exception {
    Properties deploymentProperties = new Properties();
    deploymentProperties.put(FLOW_PROPERTY_NAME, FLOW_PROPERTY_NAME_VALUE);
    startDeployment();
    deployAndVerifyPropertyInRegistry(dummyAppDescriptorWithPropsFileBuilder.getArtifactFile().toURI(), deploymentProperties,
                                      (registry) -> registry.lookupByName(FLOW_PROPERTY_NAME).get()
                                          .equals(FLOW_PROPERTY_NAME_VALUE));

    // Redeploys without deployment properties (remains the same, as it takes the deployment properties from the persisted file)
    redeployAndVerifyPropertyInRegistry(dummyAppDescriptorWithPropsFileBuilder
        .getId(), null, (registry) -> registry.lookupByName(FLOW_PROPERTY_NAME).get().equals(FLOW_PROPERTY_NAME_VALUE));

    // Redeploy with new deployment properties
    deploymentProperties.clear();
    deploymentProperties.put(FLOW_PROPERTY_NAME, FLOW_PROPERTY_NAME_VALUE_ON_REDEPLOY);
    redeployAndVerifyPropertyInRegistry(dummyAppDescriptorWithPropsFileBuilder.getId(), deploymentProperties,
                                        (registry) -> registry.lookupByName(FLOW_PROPERTY_NAME).get()
                                            .equals(FLOW_PROPERTY_NAME_VALUE_ON_REDEPLOY));
  }

  @Issue("MULE-16688")
  @Test
  public void deployAppWithDeploymentPropertiesInImportTag() throws Exception {
    Properties deploymentProperties = new Properties();
    deploymentProperties.put("environment", "dev");
    startDeployment();
    ApplicationFileBuilder applicationFileBuilder = appFileBuilder("app-import-file")
        .definedBy("app-import-file.xml").usingResource("config-dev.xml", "config-dev.xml");
    deployAndVerifyPropertyInRegistry(applicationFileBuilder.getArtifactFile().toURI(),
                                      deploymentProperties,
                                      (registry) -> registry.lookupByName("environment").get()
                                          .equals("dev"));
  }

  @Issue("MULE-16688")
  @Test
  public void deployAppWithOverwrittenDeploymentPropertiesInImportTag() throws Exception {
    Properties deploymentProperties = new Properties();
    deploymentProperties.put("oneProperty", "dev");
    startDeployment();
    ApplicationFileBuilder applicationFileBuilder = appFileBuilder("app-import-file-overwritten")
        .definedBy("app-import-file-overwritten.xml").usingResource("config-dev.xml", "config-dev.xml");
    deployAndVerifyPropertyInRegistry(applicationFileBuilder.getArtifactFile().toURI(),
                                      deploymentProperties,
                                      (registry) -> registry.lookupByName("oneProperty").get()
                                          .equals("dev"));
  }

  @Test
  public void deploymentPropertiesUsedInConfigurationProperties() throws Exception {
    Properties deploymentProperties = new Properties();
    deploymentProperties.put(OVERWRITTEN_PROPERTY, OVERWRITTEN_PROPERTY_DEPLOYMENT_VALUE);
    startDeployment();
    deployAndVerifyPropertyInRegistry(dummyAppDescriptorWithPropsDependencyFileBuilder.getArtifactFile().toURI(),
                                      deploymentProperties,
                                      (registry) -> registry.lookupByName(OVERWRITTEN_PROPERTY).get()
                                          .equals(OVERWRITTEN_PROPERTY_DEPLOYMENT_VALUE));
  }

  @Issue("MULE-19040")
  @Test
  public void whenAppIsStoppedStateIsPersistedAsDeploymentProperty() throws Exception {
    final Application app = deployApplication(emptyAppFileBuilder);
    app.stop();

    assertThat(app.getRegistry().lookupByName(ARTIFACT_STOPPED_LISTENER), is(notNullValue()));
    Properties deploymentProperties = resolveDeploymentProperties(emptyAppFileBuilder.getId(), empty());
    assertThat(deploymentProperties.get(START_ARTIFACT_ON_DEPLOYMENT_PROPERTY), is(notNullValue()));
    assertThat(deploymentProperties.get(START_ARTIFACT_ON_DEPLOYMENT_PROPERTY), is("false"));
  }

  @Issue("MULE-19040")
  @Test
  public void whenAppIsStoppedByUndeploymentStateIsNotPersistedAsDeploymentProperty() throws Exception {
    final Application app = deployApplication(emptyAppFileBuilder);

    assertThat(app.getRegistry().lookupByName(ARTIFACT_STOPPED_LISTENER), is(notNullValue()));
    deploymentService.undeploy(app);

    Properties deploymentProperties = resolveDeploymentProperties(emptyAppFileBuilder.getId(), empty());
    assertThat(deploymentProperties.get(START_ARTIFACT_ON_DEPLOYMENT_PROPERTY), is(nullValue()));
  }

  /**
   * This tests deploys a broken app which name has a weird character. It verifies that after failing deploying that app, it
   * doesn't try to do it again, which is a behavior than can be seen in some file systems due to path handling issues
   */
  @Test
  public void doesNotRetriesBrokenAppWithFunkyName() throws Exception {
    addPackedAppFromBuilder(brokenAppWithFunkyNameAppFileBuilder);

    startDeployment();

    assertDeploymentFailure(applicationDeploymentListener, brokenAppWithFunkyNameAppFileBuilder.getId());
    assertAppsDir(new String[] {brokenAppWithFunkyNameAppFileBuilder.getDeployedPath()}, NONE, true);
    assertApplicationAnchorFileDoesNotExists(brokenAppWithFunkyNameAppFileBuilder.getId());

    assertArtifactIsRegisteredAsZombie(brokenAppWithFunkyNameAppFileBuilder.getDeployedPath(),
                                       deploymentService.getZombieApplications());

    reset(applicationDeploymentListener);

    addPackedAppFromBuilder(dummyAppDescriptorFileBuilder);

    assertApplicationDeploymentSuccess(applicationDeploymentListener, dummyAppDescriptorFileBuilder.getId());
    assertDeploymentFailure(applicationDeploymentListener, brokenAppWithFunkyNameAppFileBuilder.getId(), never());

    addPackedAppFromBuilder(emptyAppFileBuilder);
    assertApplicationDeploymentSuccess(applicationDeploymentListener, emptyAppFileBuilder.getId());
    assertDeploymentFailure(applicationDeploymentListener, brokenAppWithFunkyNameAppFileBuilder.getId(), never());
  }

  @Test
  public void deploysBrokenAppZipAfterStartup() throws Exception {
    startDeployment();

    addPackedAppFromBuilder(brokenAppFileBuilder);

    assertDeploymentFailure(applicationDeploymentListener, brokenAppFileBuilder.getId());

    assertAppsDir(new String[] {brokenAppFileBuilder.getDeployedPath()}, NONE, true);

    assertApplicationAnchorFileDoesNotExists(brokenAppFileBuilder.getId());

    assertArtifactIsRegisteredAsZombie(brokenAppFileBuilder.getId(), deploymentService.getZombieApplications());
  }

  @Test
  public void redeploysAppZipDeployedOnStartup() throws Exception {
    startDeployment();

    addPackedAppFromBuilder(emptyAppFileBuilder);

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
  public void removesPreviousAppFolderOnStart() throws Exception {
    addExplodedAppFromBuilder(emptyAppFileBuilder);

    ApplicationFileBuilder emptyAppFileBuilder =
        appFileBuilder("empty-app").usingResource("empty-config.xml", "empty-config.xml")
            .deployedWith(PROPERTY_CONFIG_RESOURCES, "empty-config.xml");

    addPackedAppFromBuilder(emptyAppFileBuilder);

    startDeployment();

    assertApplicationDeploymentSuccess(applicationDeploymentListener, emptyAppFileBuilder.getId());

    assertEquals("Application has not been properly registered with Mule", 1, deploymentService.getApplications().size());
    assertAppsDir(NONE, new String[] {emptyAppFileBuilder.getId()}, true);

    assertApplicationFiles(emptyAppFileBuilder.getId(), new String[] {"empty-config.xml"});
  }

  @Test
  public void deploysExplodedAppOnStartup() throws Exception {
    addExplodedAppFromBuilder(emptyAppFileBuilder);

    startDeployment();

    assertApplicationDeploymentSuccess(applicationDeploymentListener, emptyAppFileBuilder.getId());
    assertAppsDir(NONE, new String[] {emptyAppFileBuilder.getId()}, true);
    assertApplicationAnchorFileExists(emptyAppFileBuilder.getId());
  }

  @Test
  public void deploysPackagedAppOnStartupWhenExplodedAppIsAlsoPresent() throws Exception {
    addExplodedAppFromBuilder(dummyAppDescriptorFileBuilder);
    addPackedAppFromBuilder(dummyAppDescriptorFileBuilder);

    startDeployment();

    assertApplicationDeploymentSuccess(applicationDeploymentListener, dummyAppDescriptorFileBuilder.getId());
    addPackedAppFromBuilder(emptyAppFileBuilder);

    assertApplicationDeploymentSuccess(applicationDeploymentListener, emptyAppFileBuilder.getId());

    // Checks that dummy app was deployed just once
    assertApplicationDeploymentSuccess(applicationDeploymentListener, dummyAppDescriptorFileBuilder.getId());
  }

  @Test
  public void deploysExplodedAppAfterStartup() throws Exception {
    startDeployment();

    addExplodedAppFromBuilder(emptyAppFileBuilder);

    assertApplicationDeploymentSuccess(applicationDeploymentListener, emptyAppFileBuilder.getId());
    assertAppsDir(NONE, new String[] {emptyAppFileBuilder.getId()}, true);
    assertApplicationAnchorFileExists(emptyAppFileBuilder.getId());
  }

  @Test
  public void deploysInvalidExplodedAppOnStartup() throws Exception {
    addExplodedAppFromBuilder(emptyAppFileBuilder, "app with spaces");

    startDeployment();

    assertDeploymentFailure(applicationDeploymentListener, "app with spaces");

    // Maintains app dir created
    assertAppsDir(NONE, new String[] {"app with spaces"}, true);
    assertArtifactIsRegisteredAsZombie("app with spaces", deploymentService.getZombieApplications());
  }

  @Test
  public void deploysInvalidExplodedAppAfterStartup() throws Exception {
    startDeployment();

    addExplodedAppFromBuilder(emptyAppFileBuilder, "app with spaces");

    assertDeploymentFailure(applicationDeploymentListener, "app with spaces");

    // Maintains app dir created
    assertAppsDir(NONE, new String[] {"app with spaces"}, true);
    assertArtifactIsRegisteredAsZombie("app with spaces", deploymentService.getZombieApplications());
  }

  @Test
  public void deploysInvalidExplodedOnlyOnce() throws Exception {
    startDeployment();

    addExplodedAppFromBuilder(emptyAppFileBuilder, "app with spaces");
    assertDeploymentFailure(applicationDeploymentListener, "app with spaces", times(1));

    addExplodedAppFromBuilder(dummyAppDescriptorFileBuilder);
    assertApplicationDeploymentSuccess(applicationDeploymentListener, dummyAppDescriptorFileBuilder.getId());

    addExplodedAppFromBuilder(emptyAppFileBuilder);
    assertApplicationDeploymentSuccess(applicationDeploymentListener, emptyAppFileBuilder.getId());

    // After three update cycles should have only one deployment failure notification for the broken app
    assertDeploymentFailure(applicationDeploymentListener, "app with spaces");
  }

  @Test
  public void deploysBrokenExplodedAppOnStartup() throws Exception {
    addExplodedAppFromBuilder(incompleteAppFileBuilder);

    startDeployment();

    assertDeploymentFailure(applicationDeploymentListener, incompleteAppFileBuilder.getId());

    assertApplicationAnchorFileDoesNotExists(incompleteAppFileBuilder.getId());

    // Maintains app dir created
    assertAppsDir(NONE, new String[] {incompleteAppFileBuilder.getId()}, true);
    String appId = incompleteAppFileBuilder.getId();
    assertArtifactIsRegisteredAsZombie(appId, deploymentService.getZombieApplications());
    assertThat(deploymentService.findApplication(appId).getRegistry(), nullValue());
  }

  @Test
  public void deploysBrokenExplodedAppAfterStartup() throws Exception {
    startDeployment();

    addExplodedAppFromBuilder(incompleteAppFileBuilder);

    assertDeploymentFailure(applicationDeploymentListener, incompleteAppFileBuilder.getId());

    assertApplicationAnchorFileDoesNotExists(incompleteAppFileBuilder.getId());

    // Maintains app dir created
    assertAppsDir(NONE, new String[] {incompleteAppFileBuilder.getId()}, true);
    assertArtifactIsRegisteredAsZombie(incompleteAppFileBuilder.getId(), deploymentService.getZombieApplications());
    assertThat(deploymentService.findApplication(incompleteAppFileBuilder.getId()).getRegistry(), nullValue());
  }

  @Test
  public void removesZombieFilesAfterremovesZombieFilesAfterFailedAppIsDeleted() throws Exception {
    final String appName = "bad-config-app";

    final ApplicationFileBuilder badConfigAppFileBuilder = appFileBuilder(appName).definedBy("bad-app-config.xml");

    addPackedAppFromBuilder(badConfigAppFileBuilder);
    startDeployment();

    assertDeploymentFailure(applicationDeploymentListener, badConfigAppFileBuilder.getId());
    assertAppsDir(new String[] {}, new String[] {badConfigAppFileBuilder.getId()}, true);

    assertArtifactIsRegisteredAsZombie(badConfigAppFileBuilder.getId(), deploymentService.getZombieApplications());

    final Application app = findApp(badConfigAppFileBuilder.getId(), 1);
    assertStatus(app, ApplicationStatus.DEPLOYMENT_FAILED);
    assertApplicationAnchorFileDoesNotExists(app.getArtifactName());

    reset(applicationDeploymentListener);
    deleteDirectory(new File(appsDir, app.getArtifactName()));
    assertAppFolderIsDeleted(appName);
    assertAtLeastOneUndeploymentSuccess(applicationDeploymentListener, badConfigAppFileBuilder.getId());
    assertNoZombiePresent(deploymentService.getZombieApplications());
  }

  @Test
  public void brokenAppArchiveWithoutArgument() throws Exception {
    doBrokenAppArchiveTest();
  }

  @Test
  public void brokenAppArchiveAsArgument() throws Exception {
    testWithSystemProperty(DEPLOYMENT_APPLICATION_PROPERTY, brokenAppFileBuilder.getId(), () -> doBrokenAppArchiveTest());
  }

  @Test
  public void deploysInvalidZipAppOnStartup() throws Exception {
    addPackedAppFromBuilder(emptyAppFileBuilder, "app with spaces.jar");

    startDeployment();
    assertDeploymentFailure(applicationDeploymentListener, "app with spaces");

    // zip stays intact, no app dir created
    assertAppsDir(new String[] {"app with spaces.jar"}, NONE, true);
    assertArtifactIsRegisteredAsZombie("app with spaces.jar", deploymentService.getZombieApplications());
  }

  @Test
  public void deploysInvalidZipAppAfterStartup() throws Exception {
    startDeployment();

    addPackedAppFromBuilder(emptyAppFileBuilder, "app with spaces.jar");

    assertDeploymentFailure(applicationDeploymentListener, "app with spaces");

    // zip stays intact, no app dir created
    assertAppsDir(new String[] {"app with spaces.jar"}, NONE, true);
    assertArtifactIsRegisteredAsZombie("app with spaces.jar", deploymentService.getZombieApplications());
  }

  @Test
  public void deployAppNameWithZipSuffix() throws Exception {
    final ApplicationFileBuilder applicationFileBuilder = appFileBuilder("empty-app.jar", emptyAppFileBuilder);
    addPackedAppFromBuilder(applicationFileBuilder);

    startDeployment();

    assertApplicationDeploymentSuccess(applicationDeploymentListener, applicationFileBuilder.getId());
    reset(applicationDeploymentListener);

    assertAppsDir(NONE, new String[] {applicationFileBuilder.getDeployedPath()}, true);
    assertEquals("Application has not been properly registered with Mule", 1, deploymentService.getApplications().size());

    // Checks that the empty-app.zip folder is not processed as a zip file
    assertNoDeploymentInvoked(applicationDeploymentListener);
  }

  @Test
  public void deploysPackedAppsInOrderWhenAppArgumentIsUsed() throws Exception {
    assumeThat(parallelDeployment, is(false));

    ApplicationFileBuilder app1 = createEmptyApp().withVersion("1.0.0");
    ApplicationFileBuilder app2 = createEmptyApp().withVersion("2.0.0");
    ApplicationFileBuilder app3 = createEmptyApp().withVersion("3.0.0");

    addPackedAppFromBuilder(app1, "1.jar");
    addPackedAppFromBuilder(app2, "2.jar");
    addPackedAppFromBuilder(app3, "3.jar");

    testWithSystemProperty(DEPLOYMENT_APPLICATION_PROPERTY, "3:1:2", () -> {
      startDeployment();

      assertApplicationDeploymentSuccess(applicationDeploymentListener, "1");
      assertApplicationDeploymentSuccess(applicationDeploymentListener, "2");
      assertApplicationDeploymentSuccess(applicationDeploymentListener, "3");
      assertAppsDir(NONE, new String[] {"1", "2", "3"}, true);

      // When apps are passed as -app app1:app2:app3 the startup order matters
      List<Application> applications = deploymentService.getApplications();
      assertNotNull(applications);
      assertEquals(3, applications.size());
      assertEquals("3", applications.get(0).getArtifactName());
      assertEquals("1", applications.get(1).getArtifactName());
      assertEquals("2", applications.get(2).getArtifactName());
    });
  }

  private ApplicationFileBuilder createEmptyApp() {
    return appFileBuilder("empty-app").definedBy("empty-config.xml");
  }

  @Test
  public void deploysExplodedAppsInOrderWhenAppArgumentIsUsed() throws Exception {
    assumeThat(parallelDeployment, is(false));

    ApplicationFileBuilder appFileBuilder1 = appFileBuilder("1").definedBy("empty-config.xml");
    addExplodedAppFromBuilder(appFileBuilder1);
    ApplicationFileBuilder appFileBuilder2 = appFileBuilder("2").definedBy("empty-config.xml");
    addExplodedAppFromBuilder(appFileBuilder2);
    ApplicationFileBuilder appFileBuilder3 = appFileBuilder("3").definedBy("empty-config.xml");
    addExplodedAppFromBuilder(appFileBuilder3);

    String apps = format("%s:%s:%s", appFileBuilder3.getId(), appFileBuilder1.getId(), appFileBuilder2.getId());
    testWithSystemProperty(DEPLOYMENT_APPLICATION_PROPERTY, apps, () -> {
      startDeployment();

      assertApplicationDeploymentSuccess(applicationDeploymentListener, appFileBuilder3.getId());
      assertApplicationDeploymentSuccess(applicationDeploymentListener, appFileBuilder1.getId());
      assertApplicationDeploymentSuccess(applicationDeploymentListener, appFileBuilder2.getId());

      assertAppsDir(NONE, new String[] {appFileBuilder1.getId(), appFileBuilder2.getId(), appFileBuilder3.getId()},
                    true);

      // When apps are passed as -app app1:app2:app3 the startup order matters
      List<Application> applications = deploymentService.getApplications();
      assertNotNull(applications);
      assertEquals(3, applications.size());
      assertEquals(appFileBuilder3.getId(), applications.get(0).getArtifactName());
      assertEquals(appFileBuilder1.getId(), applications.get(1).getArtifactName());
      assertEquals(appFileBuilder2.getId(), applications.get(2).getArtifactName());
    });
  }

  @Test
  public void deploysAppJustOnce() throws Exception {
    addPackedAppFromBuilder(emptyAppFileBuilder);

    String appName = "empty-app-1.0.0-mule-application";
    String apps = format("%s:%s:%s", appName, appName, appName);

    testWithSystemProperty(DEPLOYMENT_APPLICATION_PROPERTY, apps, () -> {
      startDeployment();

      assertApplicationDeploymentSuccess(applicationDeploymentListener, emptyAppFileBuilder.getId());
      assertAppsDir(NONE, new String[] {emptyAppFileBuilder.getId()}, true);

      List<Application> applications = deploymentService.getApplications();
      assertEquals(1, applications.size());
    });
  }

  @Test
  public void tracksAppConfigUpdateTime() throws Exception {
    addExplodedAppFromBuilder(emptyAppFileBuilder);

    // Sets a modification time in the future
    File appFolder = new File(appsDir.getPath(), emptyAppFileBuilder.getId());
    File configFile = new File(appFolder, MULE_CONFIG_XML_FILE);
    configFile.setLastModified(currentTimeMillis() + ONE_HOUR_IN_MILLISECONDS);

    startDeployment();
    assertApplicationDeploymentSuccess(applicationDeploymentListener, emptyAppFileBuilder.getId());
    reset(applicationDeploymentListener);

    assertNoDeploymentInvoked(applicationDeploymentListener);
  }

  @Test
  public void receivesMuleContextDeploymentNotifications() throws Exception {
    // NOTE: need an integration test like this because DefaultMuleApplication
    // class cannot be unit tested.
    addPackedAppFromBuilder(emptyAppFileBuilder);

    startDeployment();

    assertApplicationDeploymentSuccess(applicationDeploymentListener, emptyAppFileBuilder.getId());
    assertMuleContextCreated(applicationDeploymentListener, emptyAppFileBuilder.getId());
    assertMuleContextInitialized(applicationDeploymentListener, emptyAppFileBuilder.getId());
  }

  @Test
  public void undeploysStoppedApp() throws Exception {
    final Application app = deployApplication(emptyAppFileBuilder);
    app.stop();
    assertStatus(app, STOPPED);

    deploymentService.undeploy(app);
  }

  @Issue("MULE-19040")
  @Test
  public void undeploysStoppedAppAndDoesNotStartItOnDeploy() throws Exception {
    final Application app = deployApplication(emptyAppFileBuilder);
    app.stop();
    assertStatus(app, STOPPED);

    restartServer();

    assertAppDeploymentAndStatus(emptyAppFileBuilder, CREATED);
  }

  @Issue("MULE-19040")
  @Test
  public void undeploysStoppedAppDoesNotStartItOnDeployButCanBeStartedManually() throws Exception {
    final Application app = deployApplication(emptyAppFileBuilder);
    app.stop();
    assertStatus(app, STOPPED);

    restartServer();

    assertDeploymentSuccess(applicationDeploymentListener, emptyAppFileBuilder.getId());
    final Application app_2 = findApp(emptyAppFileBuilder.getId(), 1);
    assertStatus(app_2, CREATED);
    app_2.start();
    assertStatus(app_2, STARTED);
  }

  @Issue("MULE-19040")
  @Test
  public void undeploysNotStoppedAppAndStartsItOnDeploy() throws Exception {
    final Application app = deployApplication(emptyAppFileBuilder);
    assertStatus(app, STARTED);

    restartServer();

    assertAppDeploymentAndStatus(emptyAppFileBuilder, STARTED);
  }

  @Test
  @Issue("MULE-19127")
  @Feature(DEPLOYMENT_CONFIGURATION)
  @Story(FLOW_STATE_PERSISTENCE)
  public void undeploysAppWithStoppedFlowAndDoesNotStartItOnDeploy() throws Exception {
    final Application app = deployApplication(dummyAppDescriptorFileBuilder);
    for (Flow flow : app.getRegistry().lookupAllByType(Flow.class)) {
      flow.stop();
    }

    restartServer();

    final Application app_2 = assertAppDeploymentAndStatus(dummyAppDescriptorFileBuilder, STARTED);
    assertIfFlowsHaveStarted(app_2, false);
  }

  @Test
  @Issue("MULE-19127")
  @Feature(DEPLOYMENT_CONFIGURATION)
  @Story(FLOW_STATE_PERSISTENCE)
  public void undeploysAppWithNotStoppedFlowAndStartsItOnDeploy() throws Exception {
    addPackedAppFromBuilder(dummyAppDescriptorFileBuilder);

    startDeployment();

    assertApplicationDeploymentSuccess(applicationDeploymentListener, dummyAppDescriptorFileBuilder.getId());

    restartServer();

    final Application app_2 = assertAppDeploymentAndStatus(dummyAppDescriptorFileBuilder, STARTED);
    assertIfFlowsHaveStarted(app_2, true);
  }

  @Test
  @Issue("MULE-19127")
  @Feature(DEPLOYMENT_CONFIGURATION)
  @Story(FLOW_STATE_PERSISTENCE)
  public void redeploysAppWithStoppedFlowAndDoesNotStartItOnDeploy() throws Exception {
    final Application app = deployApplication(dummyAppDescriptorFileBuilder);
    for (Flow flow : app.getRegistry().lookupAllByType(Flow.class)) {
      flow.stop();
    }

    reset(applicationDeploymentListener);
    deploymentService.redeploy(dummyAppDescriptorFileBuilder.getId());
    assertApplicationRedeploymentSuccess(dummyAppDescriptorFileBuilder.getId());

    final Application app_2 = assertAppDeploymentAndStatus(dummyAppDescriptorFileBuilder, STARTED);
    assertIfFlowsHaveStarted(app_2, false);
  }

  @Test
  @Issue("MULE-19127")
  @Feature(DEPLOYMENT_CONFIGURATION)
  @Story(FLOW_STATE_PERSISTENCE)
  public void redeploysAppWithStoppedFlowAndDoesNotStartItOnDeployButCanBeStartedManually() throws Exception {
    final Application app = deployApplication(dummyAppDescriptorFileBuilder);
    for (Flow flow : app.getRegistry().lookupAllByType(Flow.class)) {
      flow.stop();
    }

    reset(applicationDeploymentListener);
    deploymentService.redeploy(dummyAppDescriptorFileBuilder.getId());

    final Application app_2 = assertAppDeploymentAndStatus(dummyAppDescriptorFileBuilder, STARTED);
    for (Flow flow : app_2.getRegistry().lookupAllByType(Flow.class)) {
      assertThat(flow.getLifecycleState().isStarted(), is(false));
      flow.start();
      assertThat(flow.getLifecycleState().isStarted(), is(true));
    }
  }

  @Test
  @Issue("MULE-19127")
  @Feature(DEPLOYMENT_CONFIGURATION)
  @Story(FLOW_STATE_PERSISTENCE)
  public void stopsAndStartsAppWithStoppedFlowAndDoesNotStartIt() throws Exception {
    final Application app = deployApplication(dummyAppDescriptorFileBuilder);
    for (Flow flow : app.getRegistry().lookupAllByType(Flow.class)) {
      flow.stop();
    }
    app.stop();
    assertStatus(app, STOPPED);
    app.start();
    assertStatus(app, STARTED);

    assertIfFlowsHaveStarted(app, false);
  }

  @Test
  @Issue("MULE-19127")
  @Feature(DEPLOYMENT_CONFIGURATION)
  @Story(FLOW_STATE_PERSISTENCE)
  public void stopsAndStartsAppWithStartedFlowAndDoesNotStopIt() throws Exception {
    final Application app = deployApplication(dummyAppDescriptorFileBuilder);
    app.stop();
    assertStatus(app, STOPPED);
    app.start();
    assertStatus(app, STARTED);

    assertIfFlowsHaveStarted(app, true);
  }

  @Test
  @Issue("MULE-19127")
  public void stopsAndStartsAppWithStoppedFlowWithInitialStateStoppedAndStartsIt() throws Exception {
    final Application app = deployApplication(dummyAppDescriptorWithStoppedFlowFileBuilder);
    for (Flow flow : app.getRegistry().lookupAllByType(Flow.class)) {
      flow.start();
      flow.stop();
    }
    reset(applicationDeploymentListener);
    deploymentService.redeploy(dummyAppDescriptorWithStoppedFlowFileBuilder.getId());

    final Application app_2 = assertAppDeploymentAndStatus(dummyAppDescriptorWithStoppedFlowFileBuilder, STARTED);
    for (Flow flow : app_2.getRegistry().lookupAllByType(Flow.class)) {
      assertThat(flow.getLifecycleState().isStarted(), is(false));
      flow.start();
      assertThat(flow.getLifecycleState().isStarted(), is(true));
    }
  }

  @Test
  public void undeploysApplicationRemovingAnchorFile() throws Exception {
    Application app = deployApplication(emptyAppFileBuilder);

    assertTrue("Unable to remove anchor file", removeAppAnchorFile(emptyAppFileBuilder.getId()));

    assertUndeploymentSuccess(applicationDeploymentListener, emptyAppFileBuilder.getId());
    assertStatus(app, DESTROYED);
  }

  @Test
  public void undeploysAppCompletelyEvenOnStoppingException() throws Exception {
    addPackedAppFromBuilder(emptyAppFileBuilder);

    final DefaultDomainManager domainManager = new DefaultDomainManager();
    domainManager.addDomain(createDefaultDomain());

    TestApplicationFactory appFactory =
        createTestApplicationFactory(new MuleApplicationClassLoaderFactory(new DefaultNativeLibraryFinderFactory()),
                                     domainManager, serviceManager, extensionModelLoaderManager, moduleRepository,
                                     createDescriptorLoaderRepository());
    appFactory.setFailOnStopApplication(true);

    deploymentService.setAppFactory(appFactory);
    startDeployment();

    assertApplicationDeploymentSuccess(applicationDeploymentListener, emptyAppFileBuilder.getId());
    Application app = findApp(emptyAppFileBuilder.getId(), 1);

    assertTrue("Unable to remove anchor file", removeAppAnchorFile(emptyAppFileBuilder.getId()));

    assertUndeploymentSuccess(applicationDeploymentListener, emptyAppFileBuilder.getId());

    assertAppFolderIsDeleted(emptyAppFileBuilder.getId());
    assertStatus(app, DESTROYED);
  }

  @Test
  public void deploymentFailureWhenDomainNotFound() throws Exception {
    final DefaultDomainManager emptyDomainManager = new DefaultDomainManager();
    TestApplicationFactory appFactory =
        createTestApplicationFactory(new MuleApplicationClassLoaderFactory(new DefaultNativeLibraryFinderFactory()),
                                     emptyDomainManager, serviceManager, extensionModelLoaderManager, moduleRepository,
                                     createDescriptorLoaderRepository());
    appFactory.setFailOnStopApplication(true);
    deploymentService.setAppFactory(appFactory);

    addPackedAppFromBuilder(emptyAppFileBuilder);
    startDeployment();
    assertDeploymentFailure(applicationDeploymentListener, emptyAppFileBuilder.getId());

    reset(applicationDeploymentListener);
    emptyDomainManager.addDomain(createDefaultDomain());
    addPackedAppFromBuilder(emptyAppFileBuilder);
    assertDeploymentSuccess(applicationDeploymentListener, emptyAppFileBuilder.getId());
  }

  @Test
  public void deploymentSuccessWhenUsingDefaultDomain() throws Exception {
    final DefaultDomainManager domainManager = new DefaultDomainManager();
    TestApplicationFactory appFactory =
        createTestApplicationFactory(new MuleApplicationClassLoaderFactory(new DefaultNativeLibraryFinderFactory()),
                                     domainManager, serviceManager, extensionModelLoaderManager, moduleRepository,
                                     createDescriptorLoaderRepository());
    appFactory.setFailOnStopApplication(true);
    deploymentService.setAppFactory(appFactory);

    domainManager.addDomain(createDefaultDomain());
    addPackedAppFromBuilder(emptyAppFileBuilder);
    startDeployment();

    assertDeploymentSuccess(applicationDeploymentListener, emptyAppFileBuilder.getId());
  }

  @Test
  public void undeploysAppCompletelyEvenOnDisposingException() throws Exception {
    addPackedAppFromBuilder(emptyAppFileBuilder);

    final DefaultDomainManager domainManager = new DefaultDomainManager();
    domainManager.addDomain(createDefaultDomain());

    TestApplicationFactory appFactory =
        createTestApplicationFactory(new MuleApplicationClassLoaderFactory(new DefaultNativeLibraryFinderFactory()),
                                     domainManager, serviceManager, extensionModelLoaderManager, moduleRepository,
                                     createDescriptorLoaderRepository());
    appFactory.setFailOnDisposeApplication(true);
    deploymentService.setAppFactory(appFactory);
    startDeployment();

    assertApplicationDeploymentSuccess(applicationDeploymentListener, emptyAppFileBuilder.getId());
    Application app = findApp(emptyAppFileBuilder.getId(), 1);

    assertTrue("Unable to remove anchor file", removeAppAnchorFile(emptyAppFileBuilder.getId()));

    assertUndeploymentSuccess(applicationDeploymentListener, emptyAppFileBuilder.getId());
    assertStatus(app, STOPPED);
    assertAppFolderIsDeleted(emptyAppFileBuilder.getId());
  }

  @Test
  public void deploysIncompleteZipAppOnStartup() throws Exception {
    addPackedAppFromBuilder(incompleteAppFileBuilder);

    startDeployment();

    assertDeploymentFailure(applicationDeploymentListener, incompleteAppFileBuilder.getId());

    // Deploys another app to confirm that DeploymentService has execute the updater thread
    addPackedAppFromBuilder(emptyAppFileBuilder);

    assertApplicationDeploymentSuccess(applicationDeploymentListener, emptyAppFileBuilder.getId());

    // Check that the failed application folder is still there
    assertAppFolderIsMaintained(incompleteAppFileBuilder.getId());
    assertArtifactIsRegisteredAsZombie(incompleteAppFileBuilder.getId(), deploymentService.getZombieApplications());
  }

  @Test
  public void deploysIncompleteZipAppAfterStartup() throws Exception {
    startDeployment();

    addPackedAppFromBuilder(incompleteAppFileBuilder);

    assertDeploymentFailure(applicationDeploymentListener, incompleteAppFileBuilder.getId());

    // Deploys another app to confirm that DeploymentService has execute the updater thread
    addPackedAppFromBuilder(emptyAppFileBuilder);

    assertApplicationDeploymentSuccess(applicationDeploymentListener, emptyAppFileBuilder.getId());

    // Check that the failed application folder is still there
    assertAppFolderIsMaintained(incompleteAppFileBuilder.getId());
    assertArtifactIsRegisteredAsZombie(incompleteAppFileBuilder.getId(), deploymentService.getZombieApplications());
  }

  @Test
  public void mantainsAppFolderOnExplodedAppDeploymentError() throws Exception {
    startDeployment();

    addPackedAppFromBuilder(incompleteAppFileBuilder);

    assertDeploymentFailure(applicationDeploymentListener, incompleteAppFileBuilder.getId());

    // Deploys another app to confirm that DeploymentService has execute the updater thread
    addPackedAppFromBuilder(emptyAppFileBuilder);

    assertApplicationDeploymentSuccess(applicationDeploymentListener, emptyAppFileBuilder.getId());

    // Check that the failed application folder is still there
    assertAppFolderIsMaintained(incompleteAppFileBuilder.getId());
    assertArtifactIsRegisteredAsZombie(incompleteAppFileBuilder.getId(), deploymentService.getZombieApplications());
  }

  @Test
  public void deploysAppZipWithPlugin() throws Exception {
    final ApplicationFileBuilder echoPluginAppFileBuilder =
        appFileBuilder("dummyWithEchoPlugin").definedBy("app-with-echo-plugin-config.xml").dependingOn(echoPlugin);

    addPackedAppFromBuilder(echoPluginAppFileBuilder);

    startDeployment();

    assertDeploymentSuccess(applicationDeploymentListener, echoPluginAppFileBuilder.getId());
  }

  @Test
  public void deploysAppZipWithExtensionPlugin() throws Exception {
    ApplicationFileBuilder applicationFileBuilder = createExtensionApplicationWithServices(APP_WITH_EXTENSION_PLUGIN_CONFIG,
                                                                                           helloExtensionV1Plugin);
    addPackedAppFromBuilder(applicationFileBuilder);

    startDeployment();

    assertDeploymentSuccess(applicationDeploymentListener, applicationFileBuilder.getId());
  }

  @Test
  public void failsToDeploysAppZipWithInvalidPrivilegedExtensionPlugin() throws Exception {
    ArtifactPluginFileBuilder invalidPrivilegedPlugin =
        new ArtifactPluginFileBuilder("invalidPrivilegedPlugin")
            .dependingOn(new JarFileBuilder("privilegedExtensionV1", privilegedExtensionV1JarFile))
            .configuredWith(EXPORTED_RESOURCE_PROPERTY, "/");

    ApplicationFileBuilder applicationFileBuilder = appFileBuilder("invalidPrivilegedPluginApp")
        .definedBy(APP_WITH_PRIVILEGED_EXTENSION_PLUGIN_CONFIG).dependingOn(invalidPrivilegedPlugin);
    addPackedAppFromBuilder(applicationFileBuilder);

    startDeployment();

    assertDeploymentFailure(applicationDeploymentListener, applicationFileBuilder.getId());
  }

  @Test
  public void deploysWithExtensionXmlPlugin() throws Exception {
    ApplicationFileBuilder applicationFileBuilder = appFileBuilder("appWithExtensionXmlPlugin")
        .definedBy("app-with-extension-xml-plugin-module-bye.xml").dependingOn(byeXmlExtensionPlugin);
    addPackedAppFromBuilder(applicationFileBuilder);

    final DefaultDomainManager domainManager = new DefaultDomainManager();
    domainManager.addDomain(createDefaultDomain());

    TestApplicationFactory appFactory =
        createTestApplicationFactory(new MuleApplicationClassLoaderFactory(new DefaultNativeLibraryFinderFactory()),
                                     domainManager, serviceManager, extensionModelLoaderManager, moduleRepository,
                                     createDescriptorLoaderRepository());

    deploymentService.setAppFactory(appFactory);
    startDeployment();

    assertDeploymentSuccess(applicationDeploymentListener, applicationFileBuilder.getId());
  }

  @Test
  public void deploysWithExtensionXmlPluginWithXmlDependencies() throws Exception {
    ApplicationFileBuilder applicationFileBuilder = appFileBuilder("appWithExtensionXmlPluginWithXmlDependencies")
        .definedBy("app-with-extension-xml-plugin-module-using-bye.xml")
        .dependingOn(moduleUsingByeXmlExtensionPlugin);
    addPackedAppFromBuilder(applicationFileBuilder);

    final DefaultDomainManager domainManager = new DefaultDomainManager();
    domainManager.addDomain(createDefaultDomain());

    TestApplicationFactory appFactory =
        createTestApplicationFactory(new MuleApplicationClassLoaderFactory(new DefaultNativeLibraryFinderFactory()),
                                     domainManager, serviceManager, extensionModelLoaderManager, moduleRepository,
                                     createDescriptorLoaderRepository());

    deploymentService.setAppFactory(appFactory);
    startDeployment();

    assertDeploymentSuccess(applicationDeploymentListener, applicationFileBuilder.getId());
  }

  @Test
  public void deploysWithExtensionXmlPluginWithDependencies() throws Exception {
    String moduleFileName = "module-using-java.xml";
    String extensionName = "using-java-extension";
    String moduleDestination = "org/mule/module/" + moduleFileName;
    MulePluginModel.MulePluginModelBuilder builder =
        new MulePluginModel.MulePluginModelBuilder().setName(extensionName).setMinMuleVersion(MIN_MULE_VERSION)
            .setRequiredProduct(MULE);
    builder.withExtensionModelDescriber().setId(XmlExtensionModelLoader.DESCRIBER_ID).addProperty(RESOURCE_XML,
                                                                                                  moduleDestination);
    builder.withClassLoaderModelDescriptorLoader(new MuleArtifactLoaderDescriptorBuilder()
        .addProperty(EXPORTED_PACKAGES, asList("org.foo")).setId(MULE_LOADER_ID).build());
    builder.withBundleDescriptorLoader(createBundleDescriptorLoader(extensionName, MULE_EXTENSION_CLASSIFIER, MULE_LOADER_ID));
    builder.setRequiredProduct(MULE).setMinMuleVersion(MIN_MULE_VERSION);

    final ArtifactPluginFileBuilder byeXmlExtensionPlugin = new ArtifactPluginFileBuilder(extensionName)
        .describedBy(builder.build())
        .containingResource("module-using-javaSource.xml", moduleDestination)
        .dependingOn(new JarFileBuilder("echoTestJar", echoTestJarFile))
        .describedBy(builder.build());

    ApplicationFileBuilder applicationFileBuilder = appFileBuilder("appWithExtensionXmlPluginWithDependencies")
        .definedBy("app-with-extension-xml-plugin-module-using-java.xml")
        .dependingOn(byeXmlExtensionPlugin);
    addPackedAppFromBuilder(applicationFileBuilder);

    final DefaultDomainManager domainManager = new DefaultDomainManager();
    domainManager.addDomain(createDefaultDomain());

    TestApplicationFactory appFactory =
        createTestApplicationFactory(new MuleApplicationClassLoaderFactory(new DefaultNativeLibraryFinderFactory()),
                                     domainManager, serviceManager, extensionModelLoaderManager, moduleRepository,
                                     createDescriptorLoaderRepository());

    deploymentService.setAppFactory(appFactory);
    startDeployment();

    assertDeploymentSuccess(applicationDeploymentListener, applicationFileBuilder.getId());
  }

  @Test
  public void deploysWithExtensionXmlPluginWithResourcesOnly() throws Exception {
    final String dwlResourceTestFile = "module-resources-dwlSource.dwl";
    final String prefixModuleName = "module-resources";
    final String extensionName = "resources-extension";
    final String resources = "org/mule/module/";
    final String dwExportedFile = resources + "module-resources-dwl.dwl";
    final String moduleDestination = resources + prefixModuleName + ".xml";
    final MulePluginModel.MulePluginModelBuilder builder =
        new MulePluginModel.MulePluginModelBuilder().setName(extensionName).setMinMuleVersion(MIN_MULE_VERSION);
    builder.withExtensionModelDescriber().setId(XmlExtensionModelLoader.DESCRIBER_ID).addProperty(RESOURCE_XML,
                                                                                                  moduleDestination);
    builder.withClassLoaderModelDescriptorLoader(new MuleArtifactLoaderDescriptorBuilder()
        .setId(MULE_LOADER_ID)
        .addProperty(EXPORTED_RESOURCES, asList(dwExportedFile))
        .build());
    builder.withBundleDescriptorLoader(createBundleDescriptorLoader(extensionName, MULE_EXTENSION_CLASSIFIER, MULE_LOADER_ID));
    builder.setRequiredProduct(MULE).setMinMuleVersion(MIN_MULE_VERSION);

    final ArtifactPluginFileBuilder resourcesXmlPluginFileBuilder = new ArtifactPluginFileBuilder(extensionName)
        .containingResource("module-resourcesSource.xml", moduleDestination)
        .containingResource(dwlResourceTestFile, dwExportedFile)
        .describedBy(builder.build());

    ApplicationFileBuilder applicationFileBuilder = appFileBuilder("appWithExtensionResourcesXmlPlugin")
        .definedBy("app-with-extension-xml-plugin-module-resources.xml").dependingOn(resourcesXmlPluginFileBuilder);
    addPackedAppFromBuilder(applicationFileBuilder);

    final DefaultDomainManager domainManager = new DefaultDomainManager();
    domainManager.addDomain(createDefaultDomain());

    TestApplicationFactory appFactory =
        createTestApplicationFactory(new MuleApplicationClassLoaderFactory(new DefaultNativeLibraryFinderFactory()),
                                     domainManager, serviceManager, extensionModelLoaderManager, moduleRepository,
                                     createDescriptorLoaderRepository());

    deploymentService.setAppFactory(appFactory);
    startDeployment();
    assertDeploymentSuccess(applicationDeploymentListener, applicationFileBuilder.getId());

    // final assertion is to guarantee the DWL file can be accessed in the app classloader (cannot evaluate the expression as the
    // test uses a mocked expression evaluator
    final ClassLoader appClassLoader = deploymentService.getApplications().get(0).getArtifactClassLoader().getClassLoader();
    final URL appDwlResource = appClassLoader.getResource(dwExportedFile);
    assertThat(appDwlResource, not(nullValue()));
    final String expectedResource = IOUtils.toString(currentThread().getContextClassLoader().getResource(dwlResourceTestFile));
    assertThat(IOUtils.toString(appDwlResource), is(expectedResource));
  }


  @Test
  public void failsToDeployWithExtensionThatHasNonExistingIdForExtensionModel() throws Exception {
    String extensionName = "extension-with-extension-model-id-non-existing";
    MulePluginModel.MulePluginModelBuilder builder =
        new MulePluginModel.MulePluginModelBuilder().setName(extensionName).setMinMuleVersion(MIN_MULE_VERSION);
    builder.withExtensionModelDescriber().setId("a-non-existing-ID-describer").addProperty("aProperty", "aValue");
    builder.withBundleDescriptorLoader(createBundleDescriptorLoader(extensionName, MULE_EXTENSION_CLASSIFIER,
                                                                    PROPERTIES_BUNDLE_DESCRIPTOR_LOADER_ID));
    builder.withClassLoaderModelDescriptorLoader(new MuleArtifactLoaderDescriptor(MULE_LOADER_ID, emptyMap()));

    final ArtifactPluginFileBuilder byeXmlExtensionPlugin = new ArtifactPluginFileBuilder(extensionName)
        .describedBy(builder.build());

    ApplicationFileBuilder applicationFileBuilder = appFileBuilder("appWithExtensionXmlPluginFails")
        .definedBy("app-with-extension-xml-plugin-module-bye.xml").dependingOn(byeXmlExtensionPlugin);
    addPackedAppFromBuilder(applicationFileBuilder);

    startDeployment();

    assertDeploymentFailure(applicationDeploymentListener, applicationFileBuilder.getId());
  }

  @Test
  public void deploysAppWithPluginBootstrapProperty() throws Exception {
    final ArtifactPluginFileBuilder pluginFileBuilder = new ArtifactPluginFileBuilder("bootstrapPlugin")
        .containingResource("plugin-bootstrap.properties", BOOTSTRAP_PROPERTIES)
        .containingClass(echoTestClassFile, "org/foo/EchoTest.class")
        .configuredWith(EXPORTED_RESOURCE_PROPERTY, BOOTSTRAP_PROPERTIES);

    ApplicationFileBuilder applicationFileBuilder = appFileBuilder("app-with-plugin-bootstrap")
        .definedBy("app-with-plugin-bootstrap.xml").dependingOn(pluginFileBuilder);
    final Application application = deployApplication(applicationFileBuilder);
    final Optional<Object> lookupObject = application.getRegistry().lookupByName("plugin.echotest");
    assertThat(lookupObject.isPresent(), is(true));
    assertThat(lookupObject.get().getClass().getName(), equalTo("org.foo.EchoTest"));
  }

  @Test
  public void failsToDeployApplicationOnMissingService() throws Exception {
    ArtifactPluginFileBuilder extensionPlugin = new ArtifactPluginFileBuilder("extensionPlugin")
        .dependingOn(new JarFileBuilder("bundleExtensionv1", helloExtensionV1JarFile))
        .configuredWith(EXPORTED_RESOURCE_PROPERTY, "/, META-INF");
    ApplicationFileBuilder applicationFileBuilder = appFileBuilder("appWithExtensionPlugin")
        .definedBy(APP_WITH_EXTENSION_PLUGIN_CONFIG).dependingOn(extensionPlugin);
    addPackedAppFromBuilder(applicationFileBuilder);

    startDeployment();

    assertDeploymentFailure(applicationDeploymentListener, applicationFileBuilder.getId());
  }

  @Test
  public void deploysMultipleAppsZipOnStartup() throws Exception {
    final int totalApps = 20;

    for (int i = 1; i <= totalApps; i++) {
      addExplodedAppFromBuilder(appFileBuilder(Integer.toString(i), emptyAppFileBuilder));
    }

    startDeployment();

    for (int i = 1; i <= totalApps; i++) {
      assertDeploymentSuccess(applicationDeploymentListener,
                              appFileBuilder(Integer.toString(i), emptyAppFileBuilder).getId());
    }
  }

  @Test
  public void synchronizesAppDeployFromClient() throws Exception {
    final Action action = () -> deploymentService.deploy(dummyAppDescriptorFileBuilder.getArtifactFile().toURI());

    final Action assertAction =
        () -> verify(applicationDeploymentListener, never()).onDeploymentStart(dummyAppDescriptorFileBuilder.getId());
    doSynchronizedAppDeploymentActionTest(action, assertAction);
  }

  @Test
  public void synchronizesAppUndeployFromClient() throws Exception {
    final Action action = () -> deploymentService.undeploy(emptyAppFileBuilder.getId());

    final Action assertAction =
        () -> verify(applicationDeploymentListener, never()).onUndeploymentStart(emptyAppFileBuilder.getId());
    doSynchronizedAppDeploymentActionTest(action, assertAction);
  }

  @Test
  public void synchronizesAppRedeployFromClient() throws Exception {
    final Action action = () -> {
      // Clears notification from first deployment
      reset(applicationDeploymentListener);
      deploymentService.redeploy(emptyAppFileBuilder.getId());
    };

    final Action assertAction =
        () -> verify(applicationDeploymentListener, never()).onDeploymentStart(emptyAppFileBuilder.getId());
    doSynchronizedAppDeploymentActionTest(action, assertAction);
  }

  private void doSynchronizedAppDeploymentActionTest(final Action deploymentAction, final Action assertAction) throws Exception {
    addPackedAppFromBuilder(emptyAppFileBuilder);

    doSynchronizedArtifactDeploymentActionTest(deploymentAction, assertAction, applicationDeploymentListener,
                                               emptyAppFileBuilder.getId());
  }

  @Test
  public void synchronizesDeploymentOnStart() throws Exception {
    addPackedAppFromBuilder(emptyAppFileBuilder);

    Thread deploymentServiceThread = new Thread(() -> {
      try {
        startDeployment();
      } catch (MuleException e) {
        throw new RuntimeException(e);
      }
    });

    final boolean[] lockedFromClient = new boolean[1];

    doAnswer(invocation -> {

      Thread deploymentClientThread = new Thread(() -> {
        ReentrantLock deploymentLock = deploymentService.getLock();

        try {
          try {
            lockedFromClient[0] = deploymentLock.tryLock(1000, TimeUnit.MILLISECONDS);
          } catch (InterruptedException e) {
            // Ignore
          }
        } finally {
          if (deploymentLock.isHeldByCurrentThread()) {
            deploymentLock.unlock();
          }
        }
      });

      deploymentClientThread.start();
      deploymentClientThread.join();

      return null;
    }).when(applicationDeploymentListener).onDeploymentStart(emptyAppFileBuilder.getId());

    deploymentServiceThread.start();

    assertApplicationDeploymentSuccess(applicationDeploymentListener, emptyAppFileBuilder.getId());

    assertFalse("Able to lock deployment service during start", lockedFromClient[0]);
  }

  @Test
  public void undeploysAppRemovesTemporaryData() throws Exception {
    addPackedAppFromBuilder(dummyAppDescriptorFileBuilder);

    startDeployment();

    assertDeploymentSuccess(applicationDeploymentListener, dummyAppDescriptorFileBuilder.getId());
    final Application app = findApp(dummyAppDescriptorFileBuilder.getId(), 1);

    File metaFolder = getAppMetaFolder(app);

    // As this app has a plugin, the tmp directory must exist
    assertThat(metaFolder, exists);

    // Remove the anchor file so undeployment starts
    assertTrue("Unable to remove anchor file", removeAppAnchorFile(dummyAppDescriptorFileBuilder.getId()));

    assertUndeploymentSuccess(applicationDeploymentListener, dummyAppDescriptorFileBuilder.getId());
    assertStatus(app, DESTROYED);

    // Check the tmp directory was effectively removed
    assertThat(metaFolder, not(exists));
  }

  @Test
  public void explodedAppRedeploymentDoesNotDeleteTempFile() throws Exception {
    testTempFileOnRedeployment(() -> addExplodedAppFromBuilder(emptyAppFileBuilder),
                               () -> addExplodedAppFromBuilder(emptyAppFileBuilder));
  }

  @Test
  public void packedAppRedeploymentDoesNotDeleteTempFile() throws Exception {
    testTempFileOnRedeployment(() -> addPackedAppFromBuilder(emptyAppFileBuilder),
                               () -> addPackedAppFromBuilder(emptyAppFileBuilder));
  }

  @Test
  public void packedAppRedeploymentWithExplodedDoesNotDeleteTempFile() throws Exception {
    testTempFileOnRedeployment(() -> addPackedAppFromBuilder(emptyAppFileBuilder),
                               () -> addExplodedAppFromBuilder(emptyAppFileBuilder));
  }

  @Test
  public void explodedAppRedeploymentWithPackedDoesNotDeleteTempFile() throws Exception {
    testTempFileOnRedeployment(() -> addExplodedAppFromBuilder(emptyAppFileBuilder),
                               () -> addPackedAppFromBuilder(emptyAppFileBuilder));
  }

  @Test
  public void deployMethodRedeploysIfApplicationIsAlreadyDeployedPacked() throws Exception {
    DeploymentListener mockDeploymentListener = spy(new DeploymentStatusTracker());
    deploymentService.addDeploymentListener(mockDeploymentListener);

    // Deploy an application (packed)
    addPackedAppFromBuilder(dummyAppDescriptorFileBuilder);
    startDeployment();

    // Application was deployed
    assertApplicationDeploymentSuccess(applicationDeploymentListener, dummyAppDescriptorFileBuilder.getId());
    verify(mockDeploymentListener, times(1)).onDeploymentSuccess(
                                                                 dummyAppDescriptorFileBuilder.getId());
    verify(mockDeploymentListener, times(0)).onRedeploymentSuccess(
                                                                   dummyAppDescriptorFileBuilder.getId());

    reset(mockDeploymentListener);

    // Redeploy by using deploy method
    deploymentService.deploy(dummyAppDescriptorFileBuilder.getArtifactFile().toURI());

    // Application was redeployed
    verify(mockDeploymentListener, times(1)).onRedeploymentSuccess(
                                                                   dummyAppDescriptorFileBuilder.getId());
  }

  @Test
  public void deployMethodRedeploysIfApplicationIsAlreadyDeployedExploded() throws Exception {
    DeploymentListener mockDeploymentListener = spy(new DeploymentStatusTracker());
    deploymentService.addDeploymentListener(mockDeploymentListener);

    // Deploy an application (exploded)
    addExplodedAppFromBuilder(dummyAppDescriptorFileBuilder);
    startDeployment();

    // Application was deployed
    assertApplicationDeploymentSuccess(applicationDeploymentListener, dummyAppDescriptorFileBuilder.getId());
    verify(mockDeploymentListener, times(1)).onDeploymentSuccess(dummyAppDescriptorFileBuilder.getId());
    verify(mockDeploymentListener, times(0)).onRedeploymentSuccess(dummyAppDescriptorFileBuilder.getId());

    // Redeploy by using deploy method
    deploymentService.deploy(dummyAppDescriptorFileBuilder.getArtifactFile().toURI());

    // Application was redeployed
    verify(mockDeploymentListener, times(1)).onRedeploymentSuccess(dummyAppDescriptorFileBuilder.getId());
  }

  private void testTempFileOnRedeployment(CheckedRunnable deployApp, CheckedRunnable redeployApp) throws Exception {
    final String TEST_FILE_NAME = "testFile";
    startDeployment();
    deployApp.run();
    assertDeploymentSuccess(applicationDeploymentListener, emptyAppFileBuilder.getId());

    Application app = findApp(emptyAppFileBuilder.getId(), 1);

    final File preRedeploymentMetaFolder = getAppMetaFolder(app);

    // Add test files to check if any of them was deleted
    final File preRedeploymentMetaTestFile = new File(preRedeploymentMetaFolder, TEST_FILE_NAME);
    preRedeploymentMetaTestFile.createNewFile();

    assertThat(preRedeploymentMetaTestFile, exists);

    reset(applicationDeploymentListener);

    // file.lastModified() has seconds precision(it truncates the milliseconds timestamp, adding 3 zeroes at the end).
    // This forces the first none-zero digit (from the right) to be different and trigger redeployment.
    Thread.sleep(1000);


    redeployApp.run();

    assertApplicationRedeploymentSuccess(emptyAppFileBuilder.getId());

    app = findApp(emptyAppFileBuilder.getId(), 1);
    final File postRedeploymentMetaFolder = getAppMetaFolder(app);
    final File postRedeploymentMetaTestFile = new File(postRedeploymentMetaFolder, TEST_FILE_NAME);

    assertThat(postRedeploymentMetaFolder, exists);
    assertThat(postRedeploymentMetaTestFile, exists);
  }

  private File getAppMetaFolder(Application app) {
    return new File((app.getRegistry().<MuleConfiguration>lookupByName(MuleProperties.OBJECT_MULE_CONFIGURATION).get()
        .getWorkingDirectory()));
  }

  private void deployAfterStartUp(ApplicationFileBuilder applicationFileBuilder) throws Exception {
    startDeployment();

    addPackedAppFromBuilder(applicationFileBuilder);

    assertApplicationDeploymentSuccess(applicationDeploymentListener, applicationFileBuilder.getId());
    assertAppsDir(NONE, new String[] {applicationFileBuilder.getId()}, true);
    assertApplicationAnchorFileExists(applicationFileBuilder.getId());
  }

  private void deploysAppAndVerifyAnchorFileIsCreatedAfterDeploymentEnds(Action deployArtifactAction) throws Exception {
    Action verifyAnchorFileDoesNotExistsAction = () -> assertApplicationAnchorFileDoesNotExists(waitAppFileBuilder.getId());
    Action verifyDeploymentSuccessfulAction =
        () -> assertApplicationDeploymentSuccess(applicationDeploymentListener, waitAppFileBuilder.getId());
    Action verifyAnchorFileExistsAction = () -> assertApplicationAnchorFileExists(waitAppFileBuilder.getId());
    deploysArtifactAndVerifyAnchorFileCreatedWhenDeploymentEnds(deployArtifactAction, verifyAnchorFileDoesNotExistsAction,
                                                                verifyDeploymentSuccessfulAction, verifyAnchorFileExistsAction);
  }

  private void doBrokenAppArchiveTest() throws Exception {
    addPackedAppFromBuilder(brokenAppFileBuilder);

    startDeployment();

    assertDeploymentFailure(applicationDeploymentListener, brokenAppFileBuilder.getId());
    reset(applicationDeploymentListener);

    // let the file system's write-behind cache commit the delete operation?
    Thread.sleep(FILE_TIMESTAMP_PRECISION_MILLIS);

    // zip stays intact, no app dir created
    assertAppsDir(new String[] {brokenAppFileBuilder.getDeployedPath()}, NONE, true);
    // don't assert dir contents, we want to check internal deployer state next
    assertAppsDir(NONE, new String[] {brokenAppFileBuilder.getId()}, false);
    assertEquals("No apps should have been registered with Mule.", 0, deploymentService.getApplications().size());
    assertArtifactIsRegisteredAsZombie(brokenAppFileBuilder.getDeployedPath(), deploymentService.getZombieApplications());

    // Checks that the invalid zip was not deployed again
    try {
      assertDeploymentFailure(applicationDeploymentListener, "broken-app.jar");
      fail("Install was invoked again for the broken application file");
    } catch (AssertionError expected) {
    }
  }

  private void restartServer() throws MuleException {
    serviceManager.stop();
    extensionModelLoaderManager.stop();
    deploymentService.stop();

    reset(applicationDeploymentListener);

    MuleArtifactResourcesRegistry muleArtifactResourcesRegistry =
        new MuleArtifactResourcesRegistry.Builder().moduleRepository(moduleRepository).build();

    serviceManager = muleArtifactResourcesRegistry.getServiceManager();
    serviceManager.start();

    extensionModelLoaderManager = muleArtifactResourcesRegistry.getExtensionModelLoaderManager();
    extensionModelLoaderManager.start();

    deploymentService = new TestMuleDeploymentService(muleArtifactResourcesRegistry.getDomainFactory(),
                                                      muleArtifactResourcesRegistry.getApplicationFactory(),
                                                      () -> findSchedulerService(serviceManager));
    configureDeploymentService();
    deploymentService.start();
  }

  private Application deployApplication(ApplicationFileBuilder applicationFileBuilder) throws Exception {
    addPackedAppFromBuilder(applicationFileBuilder);

    startDeployment();

    assertApplicationDeploymentSuccess(applicationDeploymentListener, applicationFileBuilder.getId());
    return findApp(applicationFileBuilder.getId(), 1);
  }

  private Application assertAppDeploymentAndStatus(ApplicationFileBuilder applicationFileBuilder, ApplicationStatus status) {
    assertDeploymentSuccess(applicationDeploymentListener, applicationFileBuilder.getId());
    final Application app = findApp(applicationFileBuilder.getId(), 1);
    assertStatus(app, status);
    return app;
  }

  private void assertIfFlowsHaveStarted(Application app, boolean started) {
    for (Flow flow : app.getRegistry().lookupAllByType(Flow.class)) {
      assertThat(flow.getLifecycleState().isStarted(), is(started));
    }
  }
}
