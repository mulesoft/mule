/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.module.deployment.test.internal;

import static org.mule.runtime.api.deployment.meta.Product.MULE;
import static org.mule.runtime.api.util.MuleSystemProperties.DEPLOYMENT_APPLICATION_PROPERTY;
import static org.mule.runtime.container.api.MuleFoldersUtil.getAppNativeLibrariesTempFolder;
import static org.mule.runtime.container.internal.ClasspathModuleDiscoverer.EXPORTED_RESOURCE_PROPERTY;
import static org.mule.runtime.core.api.config.MuleProperties.OBJECT_EXTENSION_MANAGER;
import static org.mule.runtime.core.api.config.MuleProperties.OBJECT_MULE_CONFIGURATION;
import static org.mule.runtime.core.api.lifecycle.LifecycleUtils.startIfNeeded;
import static org.mule.runtime.core.api.lifecycle.LifecycleUtils.stopIfNeeded;
import static org.mule.runtime.core.internal.config.bootstrap.ClassLoaderRegistryBootstrapDiscoverer.BOOTSTRAP_PROPERTIES;
import static org.mule.runtime.core.internal.context.ArtifactStoppedPersistenceListener.ARTIFACT_STOPPED_LISTENER;
import static org.mule.runtime.deployment.model.api.DeployableArtifactDescriptor.PROPERTY_CONFIG_RESOURCES;
import static org.mule.runtime.deployment.model.api.application.ApplicationStatus.CREATED;
import static org.mule.runtime.deployment.model.api.application.ApplicationStatus.DEPLOYMENT_FAILED;
import static org.mule.runtime.deployment.model.api.application.ApplicationStatus.DESTROYED;
import static org.mule.runtime.deployment.model.api.application.ApplicationStatus.STARTED;
import static org.mule.runtime.deployment.model.api.application.ApplicationStatus.STOPPED;
import static org.mule.runtime.deployment.model.api.artifact.ArtifactDescriptorConstants.EXPORTED_PACKAGES;
import static org.mule.runtime.deployment.model.api.artifact.ArtifactDescriptorConstants.EXPORTED_RESOURCES;
import static org.mule.runtime.deployment.model.api.artifact.ArtifactDescriptorConstants.MULE_LOADER_ID;
import static org.mule.runtime.extension.internal.loader.xml.XmlExtensionModelLoader.RESOURCE_XML;
import static org.mule.runtime.module.artifact.api.descriptor.BundleDescriptor.MULE_PLUGIN_CLASSIFIER;
import static org.mule.runtime.module.deployment.impl.internal.policy.loader.PropertiesBundleDescriptorLoader.PROPERTIES_BUNDLE_DESCRIPTOR_LOADER_ID;
import static org.mule.runtime.module.deployment.impl.internal.util.DeploymentPropertiesUtils.getPersistedArtifactStatusDeploymentProperties;
import static org.mule.runtime.module.deployment.impl.internal.util.DeploymentPropertiesUtils.getPersistedDeploymentProperties;
import static org.mule.runtime.module.deployment.impl.internal.util.DeploymentPropertiesUtils.getPersistedFlowDeploymentProperties;
import static org.mule.runtime.module.deployment.impl.internal.util.DeploymentPropertiesUtils.resolveArtifactStatusDeploymentProperties;
import static org.mule.runtime.module.deployment.impl.internal.util.DeploymentPropertiesUtils.resolveDeploymentProperties;
import static org.mule.runtime.module.deployment.impl.internal.util.DeploymentPropertiesUtils.resolveFlowDeploymentProperties;
import static org.mule.runtime.module.deployment.internal.DefaultArchiveDeployer.START_ARTIFACT_ON_DEPLOYMENT_PROPERTY;
import static org.mule.runtime.module.deployment.internal.FlowStoppedDeploymentPersistenceListener.START_FLOW_ON_DEPLOYMENT_PROPERTY;
import static org.mule.runtime.module.deployment.internal.MuleDeploymentService.findSchedulerService;
import static org.mule.runtime.module.deployment.test.internal.TestApplicationFactory.createTestApplicationFactory;
import static org.mule.runtime.module.deployment.test.internal.TestArtifactsCatalog.byeXmlExtensionPlugin;
import static org.mule.runtime.module.deployment.test.internal.TestArtifactsCatalog.callbackExtensionPlugin;
import static org.mule.runtime.module.deployment.test.internal.TestArtifactsCatalog.echoPlugin;
import static org.mule.runtime.module.deployment.test.internal.TestArtifactsCatalog.echoTestClassFile;
import static org.mule.runtime.module.deployment.test.internal.TestArtifactsCatalog.echoTestJarFile;
import static org.mule.runtime.module.deployment.test.internal.TestArtifactsCatalog.helloExtensionV1JarFile;
import static org.mule.runtime.module.deployment.test.internal.TestArtifactsCatalog.helloExtensionV1Plugin;
import static org.mule.runtime.module.deployment.test.internal.TestArtifactsCatalog.moduleUsingByeXmlExtensionPlugin;
import static org.mule.runtime.module.deployment.test.internal.TestArtifactsCatalog.privilegedExtensionV1JarFile;
import static org.mule.runtime.module.deployment.internal.processor.SerializedAstArtifactConfigurationProcessor.serializedAstWithFallbackArtifactConfigurationProcessor;
import static org.mule.runtime.module.deployment.test.internal.util.DeploymentServiceTestUtils.deploy;
import static org.mule.runtime.module.deployment.test.internal.util.DeploymentServiceTestUtils.redeploy;
import static org.mule.tck.MuleTestUtils.testWithSystemProperty;
import static org.mule.test.allure.AllureConstants.ArtifactAst.ArtifactAstSerialization.AST_JSON_DESERIALIZER;
import static org.mule.test.allure.AllureConstants.ArtifactDeploymentFeature.APP_DEPLOYMENT;
import static org.mule.test.allure.AllureConstants.ArtifactDeploymentFeature.DeploymentFailureStory.DEPLOYMENT_FAILURE;
import static org.mule.test.allure.AllureConstants.ArtifactDeploymentFeature.DeploymentSuccessfulStory.DEPLOYMENT_SUCCESS;
import static org.mule.test.allure.AllureConstants.ArtifactDeploymentFeature.UndeploymentFailureStory.UNDEPLOYMENT;
import static org.mule.test.allure.AllureConstants.DeploymentConfiguration.DEPLOYMENT_CONFIGURATION;
import static org.mule.test.allure.AllureConstants.DeploymentConfiguration.FlowStatePersistenceStory.FLOW_STATE_PERSISTENCE;
import static org.mule.test.allure.AllureConstants.DeploymentTypeFeature.RedeploymentStory.APPLICATION_REDEPLOYMENT;
import static org.mule.test.allure.AllureConstants.XmlSdk.XML_SDK;

import static java.lang.String.format;
import static java.lang.System.currentTimeMillis;
import static java.lang.Thread.currentThread;
import static java.nio.charset.StandardCharsets.UTF_8;
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

import org.mule.runtime.api.component.ConfigurationProperties;
import org.mule.runtime.api.deployment.meta.MuleArtifactLoaderDescriptor;
import org.mule.runtime.api.deployment.meta.MuleArtifactLoaderDescriptorBuilder;
import org.mule.runtime.api.deployment.meta.MulePluginModel;
import org.mule.runtime.api.exception.MuleException;
import org.mule.runtime.api.lifecycle.Startable;
import org.mule.runtime.api.memory.management.MemoryManagementService;
import org.mule.runtime.core.api.config.MuleConfiguration;
import org.mule.runtime.core.api.construct.Flow;
import org.mule.runtime.core.api.event.CoreEvent;
import org.mule.runtime.core.api.extension.ExtensionManager;
import org.mule.runtime.core.api.processor.Processor;
import org.mule.runtime.core.api.util.func.CheckedRunnable;
import org.mule.runtime.deployment.model.api.application.Application;
import org.mule.runtime.deployment.model.api.application.ApplicationStatus;
import org.mule.runtime.deployment.model.api.artifact.ArtifactConfigurationProcessor;
import org.mule.runtime.extension.internal.loader.xml.XmlExtensionModelLoader;
import org.mule.runtime.module.deployment.api.DeploymentListener;
import org.mule.runtime.module.deployment.impl.internal.MuleArtifactResourcesRegistry;
import org.mule.runtime.module.deployment.impl.internal.builder.ApplicationFileBuilder;
import org.mule.runtime.module.deployment.impl.internal.builder.ArtifactPluginFileBuilder;
import org.mule.runtime.module.deployment.impl.internal.builder.JarFileBuilder;
import org.mule.runtime.module.deployment.impl.internal.domain.DefaultDomainManager;
import org.mule.runtime.module.deployment.internal.DeploymentStatusTracker;
import org.mule.runtime.module.deployment.internal.processor.SerializedAstArtifactConfigurationProcessor;
import org.mule.tck.junit4.rule.SystemProperty;

import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.util.List;
import java.util.Optional;
import java.util.Properties;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.ReentrantLock;

import javax.inject.Inject;

import io.qameta.allure.Feature;
import io.qameta.allure.Issue;
import io.qameta.allure.Story;
import org.apache.commons.io.IOUtils;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;

/**
 * Contains test for application deployment on the default domain
 */
@Feature(APP_DEPLOYMENT)
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
  }

  @Test
  @Story(DEPLOYMENT_SUCCESS)
  public void deploysAppZipOnStartup() throws Exception {
    addPackedAppFromBuilder(dummyAppDescriptorFileBuilder);

    startDeployment();

    assertApplicationDeploymentSuccess(applicationDeploymentListener, dummyAppDescriptorFileBuilder.getId());
    assertAppsDir(NONE, new String[] {dummyAppDescriptorFileBuilder.getId()}, true);
    assertApplicationAnchorFileExists(dummyAppDescriptorFileBuilder.getId());

    // just assert no privileged entries were put in the registry
    final Application app = findApp(dummyAppDescriptorFileBuilder.getId(), 1);

    // Checks that the configuration's ID was properly configured
    assertThat(app.getArtifactContext().getRegistry().<MuleConfiguration>lookupByName(OBJECT_MULE_CONFIGURATION).get().getId(),
               equalTo(dummyAppDescriptorFileBuilder.getId()));
  }

  @Test
  @Story(AST_JSON_DESERIALIZER)
  public void deploysAppZipOnStartupUsingSerializedAst() throws Exception {
    restartServer(new SerializedAstArtifactConfigurationProcessor());

    addPackedAppFromBuilder(dummyAppDescriptorFileBuilder);

    assertApplicationDeploymentSuccess(applicationDeploymentListener, dummyAppDescriptorFileBuilder.getId());
    assertAppsDir(NONE, new String[] {dummyAppDescriptorFileBuilder.getId()}, true);
    assertApplicationAnchorFileExists(dummyAppDescriptorFileBuilder.getId());
  }

  @Test
  public void deploysAppZipOnStartupUsingSerializedAstFallback() throws Exception {
    addPackedAppFromBuilder(dummyAppWithBrokenAstDescriptorFileBuilder);

    startDeployment();

    assertApplicationDeploymentSuccess(applicationDeploymentListener, dummyAppWithBrokenAstDescriptorFileBuilder.getId());
    assertAppsDir(NONE, new String[] {dummyAppWithBrokenAstDescriptorFileBuilder.getId()}, true);
    assertApplicationAnchorFileExists(dummyAppWithBrokenAstDescriptorFileBuilder.getId());

    // just assert no privileged entries were put in the registry
    final Application app = findApp(dummyAppWithBrokenAstDescriptorFileBuilder.getId(), 1);

    // Checks that the configuration's ID was properly configured
    assertThat(app.getArtifactContext().getRegistry().<MuleConfiguration>lookupByName(OBJECT_MULE_CONFIGURATION).get().getId(),
               equalTo(dummyAppWithBrokenAstDescriptorFileBuilder.getId()));
  }

  @Test
  public void memoryManagementCanBeInjectedInApplication() throws Exception {
    addPackedAppFromBuilder(dummyAppDescriptorFileBuilder);

    startDeployment();

    assertApplicationDeploymentSuccess(applicationDeploymentListener, dummyAppDescriptorFileBuilder.getId());
    assertAppsDir(NONE, new String[] {dummyAppDescriptorFileBuilder.getId()}, true);
    assertApplicationAnchorFileExists(dummyAppDescriptorFileBuilder.getId());

    // just assert no privileged entries were put in the registry
    final Application app = findApp(dummyAppDescriptorFileBuilder.getId(), 1);

    InjectedMemoryManagement injectedMemoryManagementService = new InjectedMemoryManagement();
    app.getArtifactContext().getMuleContext().getInjector().inject(injectedMemoryManagementService);

    assertThat(injectedMemoryManagementService.getMemoryManagementService(), is(notNullValue()));
  }

  @Test
  @Story(DEPLOYMENT_SUCCESS)
  public void extensionManagerPresent() throws Exception {
    final Application app = deployApplication(emptyAppFileBuilder);
    assertThat(app.getArtifactContext().getRegistry().<ExtensionManager>lookupByName(OBJECT_EXTENSION_MANAGER).get(),
               is(notNullValue()));
  }

  @Test
  @Story(DEPLOYMENT_SUCCESS)
  public void appHomePropertyIsPresent() throws Exception {
    final ApplicationFileBuilder globalPropertyAppFileBuilder =
        appFileBuilder("property-app").definedBy("app-properties-config.xml");

    addExplodedAppFromBuilder(globalPropertyAppFileBuilder);

    startDeployment();
    assertApplicationDeploymentSuccess(applicationDeploymentListener, globalPropertyAppFileBuilder.getId());

    final Application app = findApp(globalPropertyAppFileBuilder.getId(), 1);

    Optional<ConfigurationProperties> configurationProperties =
        app.getArtifactContext().getRegistry().lookupByType(ConfigurationProperties.class);
    assertThat(configurationProperties.isPresent(), is(true));

    File appHome = new File(configurationProperties.get().resolveStringProperty("appHome")
        .orElseThrow(() -> new RuntimeException("Could not find property appHome")));
    assertThat(appHome.exists(), is(true));
    assertThat(appHome.getName(), is(globalPropertyAppFileBuilder.getId()));
  }

  @Test
  @Story(DEPLOYMENT_SUCCESS)
  public void deploysExplodedAppAndVerifyAnchorFileIsCreatedAfterDeploymentEnds() throws Exception {
    Action deployExplodedWaitAppAction = () -> addExplodedAppFromBuilder(waitAppFileBuilder);
    deploysAppAndVerifyAnchorFileIsCreatedAfterDeploymentEnds(deployExplodedWaitAppAction);
  }

  @Test
  @Story(DEPLOYMENT_SUCCESS)
  public void deploysPackagedAppAndVerifyAnchorFileIsCreatedAfterDeploymentEnds() throws Exception {
    Action deployPackagedWaitAppAction = () -> addPackedAppFromBuilder(waitAppFileBuilder);
    deploysAppAndVerifyAnchorFileIsCreatedAfterDeploymentEnds(deployPackagedWaitAppAction);
  }

  @Test
  @Story(DEPLOYMENT_SUCCESS)
  public void deploysAppZipAfterStartup() throws Exception {
    deployAfterStartUp(dummyAppDescriptorFileBuilder);
  }

  @Test
  @Story(DEPLOYMENT_SUCCESS)
  public void deploysAppZipWithExtensionUpperCaseAfterStartup() throws Exception {
    final ApplicationFileBuilder dummyAppDescriptorFileBuilderWithUpperCaseInExtension =
        appFileBuilder("dummy-app", true)
            .definedBy("dummy-app-config.xml").configuredWith("myCustomProp", "someValue")
            .dependingOn(callbackExtensionPlugin)
            .containingClass(echoTestClassFile, "org/foo/EchoTest.class");

    deployAfterStartUp(dummyAppDescriptorFileBuilderWithUpperCaseInExtension);
  }

  @Test
  @Story(DEPLOYMENT_FAILURE)
  public void deploysAppWithNonExistentConfigResourceOnDeclaration() throws Exception {
    ApplicationFileBuilder appBundleNonExistentConfigResource = appFileBuilder("non-existent-app-config-resource")
        .definedBy("empty-config.xml").deployedWith(PROPERTY_CONFIG_RESOURCES, "mule-non-existent-config.xml");

    addPackedAppFromBuilder(appBundleNonExistentConfigResource);
    startDeployment();

    assertDeploymentFailure(applicationDeploymentListener, appBundleNonExistentConfigResource.getId());
  }


  @Test
  @Story(DEPLOYMENT_FAILURE)
  public void deploysBrokenAppZipOnStartup() throws Exception {
    addPackedAppFromBuilder(brokenAppFileBuilder);

    startDeployment();

    assertDeploymentFailure(applicationDeploymentListener, brokenAppFileBuilder.getId());

    assertAppsDir(new String[] {brokenAppFileBuilder.getDeployedPath()}, NONE, true);

    assertApplicationAnchorFileDoesNotExists(brokenAppFileBuilder.getId());

    assertArtifactIsRegisteredAsZombie(brokenAppFileBuilder.getDeployedPath(), deploymentService.getZombieApplications());
  }

  @Test
  @Story(DEPLOYMENT_SUCCESS)
  public void deployAndRedeployAppWithDeploymentProperties() throws Exception {
    Properties deploymentProperties = new Properties();
    deploymentProperties.put(FLOW_PROPERTY_NAME, FLOW_PROPERTY_NAME_VALUE);
    startDeployment();

    // The first run of the directory watcher will deploy the domain 'default', needed for the app.
    triggerDirectoryWatcher();
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

  @Test
  @Issue("W-10984029")
  public void deployRedeployAndStopAppWithDeploymentProperties() throws Exception {
    Properties deploymentProperties = new Properties();
    deploymentProperties.put(FLOW_PROPERTY_NAME, FLOW_PROPERTY_NAME_VALUE);
    startDeployment();
    // The first run of the directory watcher will deploy the domain 'default', needed for the app.
    triggerDirectoryWatcher();
    deployAndVerifyPropertyInRegistry(dummyAppDescriptorWithPropsFileBuilder.getArtifactFile().toURI(), deploymentProperties,
                                      (registry) -> registry.lookupByName(FLOW_PROPERTY_NAME).get()
                                          .equals(FLOW_PROPERTY_NAME_VALUE));

    stopAppAndVerifyDeploymentAndAppStatusProperties(dummyAppDescriptorWithPropsFileBuilder.getId(), FLOW_PROPERTY_NAME_VALUE);

    // Redeploys without deployment properties (remains the same, as it takes the deployment properties from the persisted file)
    redeployAndVerifyPropertyInRegistry(dummyAppDescriptorWithPropsFileBuilder
        .getId(), null, (registry) -> registry.lookupByName(FLOW_PROPERTY_NAME).get().equals(FLOW_PROPERTY_NAME_VALUE));

    stopAppAndVerifyDeploymentAndAppStatusProperties(dummyAppDescriptorWithPropsFileBuilder.getId(), FLOW_PROPERTY_NAME_VALUE);

    // Redeploy with new deployment properties
    deploymentProperties.clear();
    deploymentProperties.put(FLOW_PROPERTY_NAME, FLOW_PROPERTY_NAME_VALUE_ON_REDEPLOY);
    redeployAndVerifyPropertyInRegistry(dummyAppDescriptorWithPropsFileBuilder.getId(), deploymentProperties,
                                        (registry) -> registry.lookupByName(FLOW_PROPERTY_NAME).get()
                                            .equals(FLOW_PROPERTY_NAME_VALUE_ON_REDEPLOY));

    stopAppAndVerifyDeploymentAndAppStatusProperties(dummyAppDescriptorWithPropsFileBuilder.getId(),
                                                     FLOW_PROPERTY_NAME_VALUE_ON_REDEPLOY);
  }

  @Test
  @Story(DEPLOYMENT_SUCCESS)
  public void deployAndRedeployAppRemovingDeploymentProperties() throws Exception {
    Properties deploymentProperties = new Properties();
    deploymentProperties.put(FLOW_PROPERTY_NAME, FLOW_PROPERTY_NAME_VALUE);
    startDeployment();

    // The first run of the directory watcher will deploy the domain 'default', needed for the app.
    triggerDirectoryWatcher();
    deployAndVerifyPropertyInRegistry(dummyAppDescriptorWithPropsFileBuilder.getArtifactFile().toURI(), deploymentProperties,
                                      (registry) -> registry.lookupByName(FLOW_PROPERTY_NAME).get()
                                          .equals(FLOW_PROPERTY_NAME_VALUE));

    // Redeploys with empty deployment properties (removes the property)
    deploymentProperties.clear();
    redeployAndVerifyPropertyInRegistry(dummyAppDescriptorWithPropsFileBuilder
        .getId(), deploymentProperties,
                                        (registry) -> !registry.lookupByName(FLOW_PROPERTY_NAME).isPresent());
  }

  private void stopAppAndVerifyDeploymentAndAppStatusProperties(String artifactName, String deploymentPropertyValue)
      throws IOException {
    Application app = findApp(dummyAppDescriptorWithPropsFileBuilder.getId(), 1);
    app.stop();

    Optional<Properties> artifactStatusDeploymentProperties =
        getPersistedArtifactStatusDeploymentProperties(dummyAppDescriptorWithPropsFileBuilder.getId());
    assertThat(artifactStatusDeploymentProperties.isPresent(), is(true));
    assertThat(artifactStatusDeploymentProperties.get().get(START_ARTIFACT_ON_DEPLOYMENT_PROPERTY), is("false"));

    Optional<Properties> updatedDeploymentProperties =
        getPersistedDeploymentProperties(dummyAppDescriptorWithPropsFileBuilder.getId());
    assertThat(updatedDeploymentProperties.isPresent(), is(true));
    assertThat(updatedDeploymentProperties.get().get(FLOW_PROPERTY_NAME), is(deploymentPropertyValue));
    assertThat(updatedDeploymentProperties.get().get(START_ARTIFACT_ON_DEPLOYMENT_PROPERTY), is(nullValue()));
  }

  @Issue("MULE-16688")
  @Test
  @Story(DEPLOYMENT_SUCCESS)
  public void deployAppWithDeploymentPropertiesInImportTag() throws Exception {
    Properties deploymentProperties = new Properties();
    deploymentProperties.put("environment", "dev");
    startDeployment();
    // The first run of the directory watcher will deploy the domain 'default', needed for the app.
    triggerDirectoryWatcher();
    ApplicationFileBuilder applicationFileBuilder = appFileBuilder("app-import-file")
        .definedBy("app-import-file.xml").usingResource("config-dev.xml", "config-dev.xml");
    deployAndVerifyPropertyInRegistry(applicationFileBuilder.getArtifactFile().toURI(),
                                      deploymentProperties,
                                      (registry) -> registry.lookupByName("environment").get()
                                          .equals("dev"));
  }

  @Issue("MULE-16688")
  @Test
  @Story(DEPLOYMENT_SUCCESS)
  public void deployAppWithOverwrittenDeploymentPropertiesInImportTag() throws Exception {
    Properties deploymentProperties = new Properties();
    deploymentProperties.put("oneProperty", "dev");
    startDeployment();
    // The first run of the directory watcher will deploy the domain 'default', needed for the app.
    triggerDirectoryWatcher();
    ApplicationFileBuilder applicationFileBuilder = appFileBuilder("app-import-file-overwritten")
        .definedBy("app-import-file-overwritten.xml").usingResource("config-dev.xml", "config-dev.xml");
    deployAndVerifyPropertyInRegistry(applicationFileBuilder.getArtifactFile().toURI(),
                                      deploymentProperties,
                                      (registry) -> registry.lookupByName("oneProperty").get()
                                          .equals("dev"));
  }

  @Test
  @Story(DEPLOYMENT_SUCCESS)
  public void deploymentPropertiesUsedInConfigurationProperties() throws Exception {
    Properties deploymentProperties = new Properties();
    deploymentProperties.put(OVERWRITTEN_PROPERTY, OVERWRITTEN_PROPERTY_DEPLOYMENT_VALUE);
    startDeployment();
    // The first run of the directory watcher will deploy the domain 'default', needed for the app.
    triggerDirectoryWatcher();
    deployAndVerifyPropertyInRegistry(dummyAppDescriptorWithPropsDependencyFileBuilder.getArtifactFile().toURI(),
                                      deploymentProperties,
                                      (registry) -> registry.lookupByName(OVERWRITTEN_PROPERTY).get()
                                          .equals(OVERWRITTEN_PROPERTY_DEPLOYMENT_VALUE));
  }

  @Issue("MULE-19040")
  @Test
  @Story(DEPLOYMENT_SUCCESS)
  public void whenAppIsStoppedStateIsPersistedAsDeploymentProperty() throws Exception {
    final Application app = deployApplication(emptyAppFileBuilder);
    app.stop();

    assertThat(app.getArtifactContext().getRegistry().lookupByName(ARTIFACT_STOPPED_LISTENER), is(notNullValue()));
    Optional<Properties> deploymentProperties = getPersistedArtifactStatusDeploymentProperties(emptyAppFileBuilder.getId());
    assertThat(deploymentProperties.isPresent(), is(true));
    assertThat(deploymentProperties.get().get(START_ARTIFACT_ON_DEPLOYMENT_PROPERTY), is(notNullValue()));
    assertThat(deploymentProperties.get().get(START_ARTIFACT_ON_DEPLOYMENT_PROPERTY), is("false"));
  }

  @Issue("MULE-19040")
  @Test
  @Story(DEPLOYMENT_SUCCESS)
  public void whenAppIsStoppedByUndeploymentStateIsNotPersistedAsDeploymentProperty() throws Exception {
    final Application app = deployApplication(emptyAppFileBuilder);

    assertThat(app.getArtifactContext().getRegistry().lookupByName(ARTIFACT_STOPPED_LISTENER), is(notNullValue()));
    deploymentService.undeploy(app);

    Optional<Properties> deploymentProperties = getPersistedArtifactStatusDeploymentProperties(emptyAppFileBuilder.getId());
    assertThat(deploymentProperties.isPresent(), is(true));
    assertThat(deploymentProperties.get().get(START_ARTIFACT_ON_DEPLOYMENT_PROPERTY), is(nullValue()));
  }

  /**
   * This tests deploys a broken app which name has a weird character. It verifies that after failing deploying that app, it
   * doesn't try to do it again, which is a behavior than can be seen in some file systems due to path handling issues
   */
  @Test
  @Story(DEPLOYMENT_FAILURE)
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
  @Story(DEPLOYMENT_FAILURE)
  public void deploysBrokenAppZipAfterStartup() throws Exception {
    startDeployment();

    addPackedAppFromBuilder(brokenAppFileBuilder);

    assertDeploymentFailure(applicationDeploymentListener, brokenAppFileBuilder.getId());

    assertAppsDir(new String[] {brokenAppFileBuilder.getDeployedPath()}, NONE, true);

    assertApplicationAnchorFileDoesNotExists(brokenAppFileBuilder.getId());

    assertArtifactIsRegisteredAsZombie(brokenAppFileBuilder.getId(), deploymentService.getZombieApplications());
  }

  @Test
  @Story(DEPLOYMENT_SUCCESS)
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
  @Story(DEPLOYMENT_SUCCESS)
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
  @Story(DEPLOYMENT_SUCCESS)
  public void deploysExplodedAppOnStartup() throws Exception {
    addExplodedAppFromBuilder(emptyAppFileBuilder);

    startDeployment();

    assertApplicationDeploymentSuccess(applicationDeploymentListener, emptyAppFileBuilder.getId());
    assertAppsDir(NONE, new String[] {emptyAppFileBuilder.getId()}, true);
    assertApplicationAnchorFileExists(emptyAppFileBuilder.getId());
  }

  @Test
  @Story(DEPLOYMENT_SUCCESS)
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
  @Story(DEPLOYMENT_SUCCESS)
  public void deploysExplodedAppAfterStartup() throws Exception {
    startDeployment();

    addExplodedAppFromBuilder(emptyAppFileBuilder);

    assertApplicationDeploymentSuccess(applicationDeploymentListener, emptyAppFileBuilder.getId());
    assertAppsDir(NONE, new String[] {emptyAppFileBuilder.getId()}, true);
    assertApplicationAnchorFileExists(emptyAppFileBuilder.getId());
  }

  @Test
  @Story(DEPLOYMENT_FAILURE)
  public void deploysInvalidExplodedAppOnStartup() throws Exception {
    addExplodedAppFromBuilder(emptyAppFileBuilder, "app with spaces");

    startDeployment();

    assertDeploymentFailure(applicationDeploymentListener, "app with spaces");

    // Maintains app dir created
    assertAppsDir(NONE, new String[] {"app with spaces"}, true);
    assertArtifactIsRegisteredAsZombie("app with spaces", deploymentService.getZombieApplications());
  }

  @Test
  @Story(DEPLOYMENT_FAILURE)
  public void deploysInvalidExplodedAppAfterStartup() throws Exception {
    startDeployment();

    addExplodedAppFromBuilder(emptyAppFileBuilder, "app with spaces");

    assertDeploymentFailure(applicationDeploymentListener, "app with spaces");

    // Maintains app dir created
    assertAppsDir(NONE, new String[] {"app with spaces"}, true);
    assertArtifactIsRegisteredAsZombie("app with spaces", deploymentService.getZombieApplications());
  }

  @Test
  @Story(DEPLOYMENT_FAILURE)
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
  @Story(DEPLOYMENT_FAILURE)
  public void deploysBrokenExplodedAppOnStartup() throws Exception {
    addExplodedAppFromBuilder(incompleteAppFileBuilder);

    startDeployment();

    assertDeploymentFailure(applicationDeploymentListener, incompleteAppFileBuilder.getId());

    assertApplicationAnchorFileDoesNotExists(incompleteAppFileBuilder.getId());

    // Maintains app dir created
    assertAppsDir(NONE, new String[] {incompleteAppFileBuilder.getId()}, true);
    String appId = incompleteAppFileBuilder.getId();
    assertArtifactIsRegisteredAsZombie(appId, deploymentService.getZombieApplications());
    assertThat(deploymentService.findApplication(appId).getArtifactContext(), nullValue());
  }

  @Test
  @Story(DEPLOYMENT_FAILURE)
  public void deploysBrokenExplodedAppAfterStartup() throws Exception {
    startDeployment();

    addExplodedAppFromBuilder(incompleteAppFileBuilder);

    assertDeploymentFailure(applicationDeploymentListener, incompleteAppFileBuilder.getId());

    assertApplicationAnchorFileDoesNotExists(incompleteAppFileBuilder.getId());

    // Maintains app dir created
    assertAppsDir(NONE, new String[] {incompleteAppFileBuilder.getId()}, true);
    assertArtifactIsRegisteredAsZombie(incompleteAppFileBuilder.getId(), deploymentService.getZombieApplications());
    assertThat(deploymentService.findApplication(incompleteAppFileBuilder.getId()).getArtifactContext(), nullValue());
  }

  @Test
  @Story(DEPLOYMENT_FAILURE)
  public void removesZombieFilesAfterRemovesZombieFilesAfterFailedAppIsDeleted() throws Exception {
    final String appName = "bad-config-app";

    final ApplicationFileBuilder badConfigAppFileBuilder = appFileBuilder(appName).definedBy("bad-app-config.xml");

    addPackedAppFromBuilder(badConfigAppFileBuilder);
    startDeployment();

    assertDeploymentFailure(applicationDeploymentListener, badConfigAppFileBuilder.getId());
    assertAppsDir(new String[] {}, new String[] {badConfigAppFileBuilder.getId()}, true);

    assertArtifactIsRegisteredAsZombie(badConfigAppFileBuilder.getId(), deploymentService.getZombieApplications());

    final Application app = findApp(badConfigAppFileBuilder.getId(), 1);
    assertStatus(app, DEPLOYMENT_FAILED);
    assertApplicationAnchorFileDoesNotExists(app.getArtifactName());

    reset(applicationDeploymentListener);
    deleteDirectory(new File(appsDir, app.getArtifactName()));
    assertAppFolderIsDeleted(appName);
    assertAtLeastOneUndeploymentSuccess(applicationDeploymentListener, badConfigAppFileBuilder.getId());
    assertNoZombiePresent(deploymentService.getZombieApplications());
  }

  @Test
  @Story(DEPLOYMENT_FAILURE)
  public void brokenAppArchiveWithoutArgument() throws Exception {
    doBrokenAppArchiveTest();
  }

  @Test
  @Story(DEPLOYMENT_FAILURE)
  public void brokenAppArchiveAsArgument() throws Exception {
    testWithSystemProperty(DEPLOYMENT_APPLICATION_PROPERTY, brokenAppFileBuilder.getId(), () -> doBrokenAppArchiveTest());
  }

  @Test
  @Story(DEPLOYMENT_FAILURE)
  public void deploysInvalidZipAppOnStartup() throws Exception {
    addPackedAppFromBuilder(emptyAppFileBuilder, "app with spaces.jar");

    startDeployment();
    assertDeploymentFailure(applicationDeploymentListener, "app with spaces");

    // zip stays intact, no app dir created
    assertAppsDir(new String[] {"app with spaces.jar"}, NONE, true);
    assertArtifactIsRegisteredAsZombie("app with spaces.jar", deploymentService.getZombieApplications());
  }

  @Test
  @Story(DEPLOYMENT_FAILURE)
  public void deploysInvalidZipAppAfterStartup() throws Exception {
    startDeployment();

    addPackedAppFromBuilder(emptyAppFileBuilder, "app with spaces.jar");

    assertDeploymentFailure(applicationDeploymentListener, "app with spaces");

    // zip stays intact, no app dir created
    assertAppsDir(new String[] {"app with spaces.jar"}, NONE, true);
    assertArtifactIsRegisteredAsZombie("app with spaces.jar", deploymentService.getZombieApplications());
  }

  @Test
  @Story(DEPLOYMENT_FAILURE)
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
  @Story(DEPLOYMENT_SUCCESS)
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
  @Story(DEPLOYMENT_SUCCESS)
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
  @Story(DEPLOYMENT_SUCCESS)
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
  @Story(DEPLOYMENT_SUCCESS)
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
  @Story(DEPLOYMENT_SUCCESS)
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
  @Story(UNDEPLOYMENT)
  public void undeploysStoppedApp() throws Exception {
    final Application app = deployApplication(emptyAppFileBuilder);
    app.stop();
    assertStatus(app, STOPPED);

    deploymentService.undeploy(app);
  }

  @Test
  public void whenAppIsUndeployedStoppedPersistenceIsDeleted() throws Exception {
    final Application app = deployApplication(emptyAppFileBuilder);
    app.stop();
    assertStatus(app, STOPPED);

    Optional<Properties> deploymentProperties = getPersistedArtifactStatusDeploymentProperties(emptyAppFileBuilder.getId());
    assertThat(deploymentProperties.isPresent(), is(true));
    assertThat(deploymentProperties.get().get(START_ARTIFACT_ON_DEPLOYMENT_PROPERTY), is(notNullValue()));
    assertThat(deploymentProperties.get().get(START_ARTIFACT_ON_DEPLOYMENT_PROPERTY), is("false"));

    deploymentService.undeploy(app);

    deploymentProperties = getPersistedArtifactStatusDeploymentProperties(emptyAppFileBuilder.getId());
    assertThat(deploymentProperties.isPresent(), is(true));
    assertThat(deploymentProperties.get().get(START_ARTIFACT_ON_DEPLOYMENT_PROPERTY), is(nullValue()));
  }

  @Issue("MULE-19040")
  @Test
  @Story(UNDEPLOYMENT)
  public void runtimeWithStoppedAppRestartsAndDoesNotStartAppOnDeployBecauseOfStatusDeploymentProperties() throws Exception {
    final Application app = deployApplication(emptyAppFileBuilder);
    app.stop();
    assertStatus(app, STOPPED);

    restartServer();

    assertAppDeploymentAndStatus(emptyAppFileBuilder, CREATED);
    Optional<Properties> deploymentProperties = getPersistedArtifactStatusDeploymentProperties(emptyAppFileBuilder.getId());
    assertThat(deploymentProperties.isPresent(), is(true));
    assertThat(deploymentProperties.get().get(START_ARTIFACT_ON_DEPLOYMENT_PROPERTY), is(notNullValue()));
    assertThat(deploymentProperties.get().get(START_ARTIFACT_ON_DEPLOYMENT_PROPERTY), is("false"));
  }

  @Issue("MULE-19040")
  @Test
  @Story(UNDEPLOYMENT)
  public void runtimeWithStoppedAppRestartsAndDoesNotStartAppOnDeployButItCanBeStartedManually() throws Exception {
    final Application app = deployApplication(emptyAppFileBuilder);
    app.stop();
    assertStatus(app, STOPPED);

    restartServer();

    assertDeploymentSuccess(applicationDeploymentListener, emptyAppFileBuilder.getId());
    final Application app_2 = findApp(emptyAppFileBuilder.getId(), 1);
    assertStatus(app_2, CREATED);

    Optional<Properties> deploymentProperties = getPersistedArtifactStatusDeploymentProperties(emptyAppFileBuilder.getId());
    assertThat(deploymentProperties.isPresent(), is(true));
    assertThat(deploymentProperties.get().get(START_ARTIFACT_ON_DEPLOYMENT_PROPERTY), is(notNullValue()));
    assertThat(deploymentProperties.get().get(START_ARTIFACT_ON_DEPLOYMENT_PROPERTY), is("false"));

    app_2.start();
    assertStatus(app_2, STARTED);
  }

  @Issue("MULE-19040")
  @Test
  @Story(UNDEPLOYMENT)
  public void runtimeWithStartedAppRestartsAndStartsAppOnDeploy() throws Exception {
    final Application app = deployApplication(emptyAppFileBuilder);
    assertStatus(app, STARTED);

    restartServer();

    assertAppDeploymentAndStatus(emptyAppFileBuilder, STARTED);
    Optional<Properties> deploymentProperties = getPersistedArtifactStatusDeploymentProperties(emptyAppFileBuilder.getId());
    assertThat(deploymentProperties.isPresent(), is(true));
    assertThat(deploymentProperties.get().get(START_ARTIFACT_ON_DEPLOYMENT_PROPERTY), is(nullValue()));
  }

  @Test
  public void redeploysStoppedAppAndStartsItOnDeployBecauseStatusPersistenceGetsDeleted() throws Exception {
    final Application app = deployApplication(emptyAppFileBuilder);
    app.stop();
    assertStatus(app, STOPPED);
    Optional<Properties> deploymentProperties = getPersistedArtifactStatusDeploymentProperties(emptyAppFileBuilder.getId());
    assertThat(deploymentProperties.isPresent(), is(true));
    assertThat(deploymentProperties.get().get(START_ARTIFACT_ON_DEPLOYMENT_PROPERTY), is(notNullValue()));
    assertThat(deploymentProperties.get().get(START_ARTIFACT_ON_DEPLOYMENT_PROPERTY), is("false"));

    reset(applicationDeploymentListener);

    redeploy(deploymentService, emptyAppFileBuilder.getId());

    assertAppDeploymentAndStatus(emptyAppFileBuilder, STARTED);
    deploymentProperties = getPersistedArtifactStatusDeploymentProperties(emptyAppFileBuilder.getId());
    assertThat(deploymentProperties.isPresent(), is(true));
    assertThat(deploymentProperties.get().get(START_ARTIFACT_ON_DEPLOYMENT_PROPERTY), is(nullValue()));
  }

  @Test
  @Issue("MULE-19127")
  @Feature(DEPLOYMENT_CONFIGURATION)
  @Story(FLOW_STATE_PERSISTENCE)
  public void undeploysAppWithStoppedFlowAndDoesNotStartItOnDeploy() throws Exception {
    final Application app = deployApplication(dummyAppDescriptorFileBuilder);
    for (Flow flow : app.getArtifactContext().getRegistry().lookupAllByType(Flow.class)) {
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
    for (Flow flow : app.getArtifactContext().getRegistry().lookupAllByType(Flow.class)) {
      flow.stop();
    }

    reset(applicationDeploymentListener);
    redeploy(deploymentService, dummyAppDescriptorFileBuilder.getId());
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
    for (Flow flow : app.getArtifactContext().getRegistry().lookupAllByType(Flow.class)) {
      flow.stop();
    }

    reset(applicationDeploymentListener);
    redeploy(deploymentService, dummyAppDescriptorFileBuilder.getId());

    final Application app_2 = assertAppDeploymentAndStatus(dummyAppDescriptorFileBuilder, STARTED);
    for (Flow flow : app_2.getArtifactContext().getRegistry().lookupAllByType(Flow.class)) {
      assertThat(flow.getLifecycleState().isStarted(), is(false));
      flow.start();
      assertThat(flow.getLifecycleState().isStarted(), is(true));
    }
  }

  @Test
  @Issue("MULE-19127")
  @Feature(DEPLOYMENT_CONFIGURATION)
  @Story(FLOW_STATE_PERSISTENCE)
  public void redeploysAppWithNotStoppedFlowAndStartsItOnDeploy() throws Exception {
    deployApplication(dummyAppDescriptorFileBuilder);

    reset(applicationDeploymentListener);
    redeploy(deploymentService, dummyAppDescriptorFileBuilder.getId());
    assertApplicationRedeploymentSuccess(dummyAppDescriptorFileBuilder.getId());

    final Application app_2 = assertAppDeploymentAndStatus(dummyAppDescriptorFileBuilder, STARTED);
    assertIfFlowsHaveStarted(app_2, true);
  }

  @Test
  @Issue("MULE-19127")
  @Feature(DEPLOYMENT_CONFIGURATION)
  @Story(FLOW_STATE_PERSISTENCE)
  public void stopsAndStartsAppWithStoppedFlowAndDoesNotStartIt() throws Exception {
    final Application app = deployApplication(dummyAppDescriptorFileBuilder);
    for (Flow flow : app.getArtifactContext().getRegistry().lookupAllByType(Flow.class)) {
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
    for (Flow flow : app.getArtifactContext().getRegistry().lookupAllByType(Flow.class)) {
      flow.start();
      flow.stop();
    }
    reset(applicationDeploymentListener);
    redeploy(deploymentService, dummyAppDescriptorWithStoppedFlowFileBuilder.getId());

    final Application app_2 = assertAppDeploymentAndStatus(dummyAppDescriptorWithStoppedFlowFileBuilder, STARTED);
    for (Flow flow : app_2.getArtifactContext().getRegistry().lookupAllByType(Flow.class)) {
      assertThat(flow.getLifecycleState().isStarted(), is(false));
      flow.start();
      assertThat(flow.getLifecycleState().isStarted(), is(true));
    }
  }

  @Test
  @Issue("W-15750334")
  public void restartAppWithStartedFlowWithInitialStateStoppedBefore48() throws Exception {
    final Application app = deployApplication(dummyAppDescriptorWithStoppedFlowFileBuilder);
    for (Flow flow : app.getArtifactContext().getRegistry().lookupAllByType(Flow.class)) {
      flow.start();
    }
    reset(applicationDeploymentListener);
    redeploy(deploymentService, dummyAppDescriptorWithStoppedFlowFileBuilder.getId());

    final Application app2 = assertAppDeploymentAndStatus(dummyAppDescriptorWithStoppedFlowFileBuilder, STARTED);
    for (Flow flow : app2.getArtifactContext().getRegistry().lookupAllByType(Flow.class)) {
      assertThat(flow.getLifecycleState().isStarted(), is(false));
    }
  }

  @Test
  @Story(UNDEPLOYMENT)
  public void undeploysApplicationRemovingAnchorFile() throws Exception {
    Application app = deployApplication(emptyAppFileBuilder);

    assertTrue("Unable to remove anchor file", removeAppAnchorFile(emptyAppFileBuilder.getId()));

    assertUndeploymentSuccess(applicationDeploymentListener, emptyAppFileBuilder.getId());
    assertStatus(app, DESTROYED);
  }

  @Test
  @Story(UNDEPLOYMENT)
  public void undeploysAppCompletelyEvenOnStoppingException() throws Exception {
    addPackedAppFromBuilder(emptyAppFileBuilder);

    final DefaultDomainManager domainManager = new DefaultDomainManager();
    domainManager.addDomain(createDefaultDomain());

    TestApplicationFactory appFactory =
        createTestApplicationFactory(domainManager, serviceManager, extensionModelLoaderRepository, moduleRepository);
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
  @Story(DEPLOYMENT_FAILURE)
  public void deploymentFailureWhenDomainNotFound() throws Exception {
    final DefaultDomainManager emptyDomainManager = new DefaultDomainManager();
    TestApplicationFactory appFactory =
        createTestApplicationFactory(emptyDomainManager, serviceManager, extensionModelLoaderRepository, moduleRepository);
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
  @Story(DEPLOYMENT_SUCCESS)
  public void deploymentSuccessWhenUsingDefaultDomain() throws Exception {
    final DefaultDomainManager domainManager = new DefaultDomainManager();
    TestApplicationFactory appFactory =
        createTestApplicationFactory(domainManager, serviceManager, extensionModelLoaderRepository, moduleRepository);
    appFactory.setFailOnStopApplication(true);
    deploymentService.setAppFactory(appFactory);

    domainManager.addDomain(createDefaultDomain());
    addPackedAppFromBuilder(emptyAppFileBuilder);
    startDeployment();

    assertDeploymentSuccess(applicationDeploymentListener, emptyAppFileBuilder.getId());
  }

  @Test
  @Story(UNDEPLOYMENT)
  public void undeploysAppCompletelyEvenOnDisposingException() throws Exception {
    addPackedAppFromBuilder(emptyAppFileBuilder);

    final DefaultDomainManager domainManager = new DefaultDomainManager();
    domainManager.addDomain(createDefaultDomain());

    TestApplicationFactory appFactory =
        createTestApplicationFactory(domainManager, serviceManager, extensionModelLoaderRepository, moduleRepository);
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
  @Story(DEPLOYMENT_FAILURE)
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
  @Story(DEPLOYMENT_FAILURE)
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
  @Story(DEPLOYMENT_FAILURE)
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
  @Story(DEPLOYMENT_SUCCESS)
  public void deploysAppZipWithPlugin() throws Exception {
    final ApplicationFileBuilder echoPluginAppFileBuilder =
        appFileBuilder("dummyWithEchoPlugin").definedBy("app-with-echo-plugin-config.xml")
            .dependingOn(callbackExtensionPlugin)
            .dependingOn(echoPlugin);

    addPackedAppFromBuilder(echoPluginAppFileBuilder);

    startDeployment();

    assertDeploymentSuccess(applicationDeploymentListener, echoPluginAppFileBuilder.getId());
  }

  @Test
  @Story(DEPLOYMENT_SUCCESS)
  public void deploysAppZipWithExtensionPlugin() throws Exception {
    ApplicationFileBuilder applicationFileBuilder = createExtensionApplicationWithServices(APP_WITH_EXTENSION_PLUGIN_CONFIG,
                                                                                           helloExtensionV1Plugin);
    addPackedAppFromBuilder(applicationFileBuilder);

    startDeployment();

    assertDeploymentSuccess(applicationDeploymentListener, applicationFileBuilder.getId());
  }

  @Test
  @Story(DEPLOYMENT_FAILURE)
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
  @Feature(XML_SDK)
  @Story(DEPLOYMENT_SUCCESS)
  public void deploysWithExtensionXmlPlugin() throws Exception {
    ApplicationFileBuilder applicationFileBuilder = appFileBuilder("appWithExtensionXmlPlugin")
        .definedBy("app-with-extension-xml-plugin-module-bye.xml").dependingOn(byeXmlExtensionPlugin);
    addPackedAppFromBuilder(applicationFileBuilder);

    final DefaultDomainManager domainManager = new DefaultDomainManager();
    domainManager.addDomain(createDefaultDomain());

    TestApplicationFactory appFactory =
        createTestApplicationFactory(domainManager, serviceManager, extensionModelLoaderRepository, moduleRepository);

    deploymentService.setAppFactory(appFactory);
    startDeployment();

    assertDeploymentSuccess(applicationDeploymentListener, applicationFileBuilder.getId());
  }

  @Test
  @Feature(XML_SDK)
  @Story(DEPLOYMENT_SUCCESS)
  public void deploysWithExtensionXmlPluginWithXmlDependencies() throws Exception {
    ApplicationFileBuilder applicationFileBuilder = appFileBuilder("appWithExtensionXmlPluginWithXmlDependencies")
        .definedBy("app-with-extension-xml-plugin-module-using-bye.xml")
        .dependingOn(moduleUsingByeXmlExtensionPlugin);
    addPackedAppFromBuilder(applicationFileBuilder);

    final DefaultDomainManager domainManager = new DefaultDomainManager();
    domainManager.addDomain(createDefaultDomain());

    TestApplicationFactory appFactory =
        createTestApplicationFactory(domainManager, serviceManager, extensionModelLoaderRepository, moduleRepository);

    deploymentService.setAppFactory(appFactory);
    startDeployment();

    assertDeploymentSuccess(applicationDeploymentListener, applicationFileBuilder.getId());
  }

  @Test
  @Feature(XML_SDK)
  @Story(DEPLOYMENT_SUCCESS)
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
    builder.withBundleDescriptorLoader(createBundleDescriptorLoader(extensionName, MULE_PLUGIN_CLASSIFIER, MULE_LOADER_ID));
    builder.setRequiredProduct(MULE).setMinMuleVersion(MIN_MULE_VERSION);

    final ArtifactPluginFileBuilder byeXmlExtensionPlugin = new ArtifactPluginFileBuilder(extensionName)
        .describedBy(builder.build())
        .containingResource("module-using-javaSource.xml", moduleDestination)
        .dependingOn(callbackExtensionPlugin)
        .dependingOn(new JarFileBuilder("echoTestJar", echoTestJarFile))
        .describedBy(builder.build());

    ApplicationFileBuilder applicationFileBuilder = appFileBuilder("appWithExtensionXmlPluginWithDependencies")
        .definedBy("app-with-extension-xml-plugin-module-using-java.xml")
        .dependingOn(byeXmlExtensionPlugin);
    addPackedAppFromBuilder(applicationFileBuilder);

    final DefaultDomainManager domainManager = new DefaultDomainManager();
    domainManager.addDomain(createDefaultDomain());

    TestApplicationFactory appFactory =
        createTestApplicationFactory(domainManager, serviceManager, extensionModelLoaderRepository, moduleRepository);

    deploymentService.setAppFactory(appFactory);
    startDeployment();

    assertDeploymentSuccess(applicationDeploymentListener, applicationFileBuilder.getId());
  }

  @Test
  @Feature(XML_SDK)
  @Story(DEPLOYMENT_SUCCESS)
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
    builder.withBundleDescriptorLoader(createBundleDescriptorLoader(extensionName, MULE_PLUGIN_CLASSIFIER, MULE_LOADER_ID));
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
        createTestApplicationFactory(domainManager, serviceManager, extensionModelLoaderRepository, moduleRepository);

    deploymentService.setAppFactory(appFactory);
    startDeployment();
    assertDeploymentSuccess(applicationDeploymentListener, applicationFileBuilder.getId());

    // final assertion is to guarantee the DWL file can be accessed in the app classloader (cannot evaluate the expression as the
    // test uses a mocked expression evaluator
    final ClassLoader appClassLoader = deploymentService.getApplications().get(0).getArtifactClassLoader().getClassLoader();
    final URL appDwlResource = appClassLoader.getResource(dwExportedFile);
    assertThat(appDwlResource, not(nullValue()));
    final String expectedResource =
        IOUtils.toString(currentThread().getContextClassLoader().getResource(dwlResourceTestFile), UTF_8);
    assertThat(IOUtils.toString(appDwlResource, UTF_8), is(expectedResource));
  }

  @Test
  @Story(DEPLOYMENT_FAILURE)
  public void failsToDeployWithExtensionThatHasNonExistingIdForExtensionModel() throws Exception {
    String extensionName = "extension-with-extension-model-id-non-existing";
    MulePluginModel.MulePluginModelBuilder builder =
        new MulePluginModel.MulePluginModelBuilder().setName(extensionName).setMinMuleVersion(MIN_MULE_VERSION);
    builder.withExtensionModelDescriber().setId("a-non-existing-ID-describer").addProperty("aProperty", "aValue");
    builder.withBundleDescriptorLoader(createBundleDescriptorLoader(extensionName, MULE_PLUGIN_CLASSIFIER,
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
  @Story(DEPLOYMENT_SUCCESS)
  public void deploysAppWithPluginBootstrapProperty() throws Exception {
    final ArtifactPluginFileBuilder pluginFileBuilder = new ArtifactPluginFileBuilder("bootstrapPlugin")
        .containingResource("plugin-bootstrap.properties", BOOTSTRAP_PROPERTIES)
        .containingClass(echoTestClassFile, "org/foo/EchoTest.class")
        .configuredWith(EXPORTED_RESOURCE_PROPERTY, BOOTSTRAP_PROPERTIES);

    ApplicationFileBuilder applicationFileBuilder = appFileBuilder("app-with-plugin-bootstrap")
        .definedBy("app-with-plugin-bootstrap.xml").dependingOn(pluginFileBuilder);
    final Application application = deployApplication(applicationFileBuilder);
    final Optional<Object> lookupObject = application.getArtifactContext().getRegistry().lookupByName("plugin.echotest");
    assertThat(lookupObject.isPresent(), is(true));
    assertThat(lookupObject.get().getClass().getName(), equalTo("org.foo.EchoTest"));
  }

  @Test
  @Story(DEPLOYMENT_FAILURE)
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
  @Story(DEPLOYMENT_SUCCESS)
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
  @Story(DEPLOYMENT_SUCCESS)
  public void synchronizesAppDeployFromClient() throws Exception {
    final Action action = () -> deploymentService.deploy(dummyAppDescriptorFileBuilder.getArtifactFile().toURI());

    final Action assertAction =
        () -> verify(applicationDeploymentListener, never()).onDeploymentStart(dummyAppDescriptorFileBuilder.getId());
    doSynchronizedAppDeploymentActionTest(action, assertAction);
  }

  @Test
  @Story(UNDEPLOYMENT)
  public void synchronizesAppUndeployFromClient() throws Exception {
    final Action action = () -> deploymentService.undeploy(emptyAppFileBuilder.getId());

    final Action assertAction =
        () -> verify(applicationDeploymentListener, never()).onUndeploymentStart(emptyAppFileBuilder.getId());
    doSynchronizedAppDeploymentActionTest(action, assertAction);
  }

  @Test
  @Story(APPLICATION_REDEPLOYMENT)
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
    startDeployment();

    // The first run of the directory watcher will deploy the domain 'default', needed for the app.
    triggerDirectoryWatcher();

    addPackedAppFromBuilder(emptyAppFileBuilder);

    doSynchronizedArtifactDeploymentActionTest(deploymentAction, assertAction, applicationDeploymentListener,
                                               emptyAppFileBuilder.getId());
  }

  @Test
  @Story(DEPLOYMENT_SUCCESS)
  public void synchronizesDeploymentOnStart() throws Exception {
    addPackedAppFromBuilder(emptyAppFileBuilder);

    Thread deploymentServiceThread = new Thread(() -> {
      try {
        startDeployment(false);
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

    serviceManager.start();
    deploymentServiceThread.start();

    assertApplicationDeploymentSuccess(applicationDeploymentListener, emptyAppFileBuilder.getId());

    deploymentServiceThread.join();

    assertFalse("Able to lock deployment service during start", lockedFromClient[0]);
  }

  @Test
  @Story(UNDEPLOYMENT)
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
  @Story(UNDEPLOYMENT)
  public void undeploysAppRemovesTemporaryNativeLibrariesData() throws Exception {
    addPackedAppFromBuilder(dummyAppDescriptorFileBuilder);

    startDeployment();

    assertDeploymentSuccess(applicationDeploymentListener, dummyAppDescriptorFileBuilder.getId());
    final Application app = findApp(dummyAppDescriptorFileBuilder.getId(), 1);

    File nativeLibrariesTempFolder = getAppNativeLibrariesTempFolder(app.getArtifactName());

    // As this app has a plugin, the tmp directory must exist
    assertThat(nativeLibrariesTempFolder.listFiles().length, is(1));

    // Remove the anchor file so undeployment starts
    assertTrue("Unable to remove anchor file", removeAppAnchorFile(dummyAppDescriptorFileBuilder.getId()));

    assertUndeploymentSuccess(applicationDeploymentListener, dummyAppDescriptorFileBuilder.getId());
    assertStatus(app, DESTROYED);

    // Check the tmp directory was effectively removed
    assertThat(nativeLibrariesTempFolder.listFiles().length, is(0));
  }

  @Test
  @Story(APPLICATION_REDEPLOYMENT)
  public void redeployAppRemovesTemporaryNativeLibrariesData() throws Exception {
    addPackedAppFromBuilder(dummyAppDescriptorFileBuilder);

    startDeployment();

    assertDeploymentSuccess(applicationDeploymentListener, dummyAppDescriptorFileBuilder.getId());
    final Application app = findApp(dummyAppDescriptorFileBuilder.getId(), 1);

    File nativeLibrariesTempFolder = getAppNativeLibrariesTempFolder(app.getArtifactName());
    File nativeLibrariesTempFolderFirstDeployment =
        getAppNativeLibrariesTempFolder(app.getArtifactName(), app.getDescriptor().getLoadedNativeLibrariesFolderName());

    // As this app has a plugin, the tmp directory must exist
    assertThat(nativeLibrariesTempFolder.listFiles().length, is(1));
    assertTrue(nativeLibrariesTempFolderFirstDeployment.exists());

    // Run redeploy
    reset(applicationDeploymentListener);
    redeploy(deploymentService, dummyAppDescriptorFileBuilder.getId());
    assertApplicationRedeploymentSuccess(dummyAppDescriptorFileBuilder.getId());

    // Check the first deployment tmp directory was effectively removed and the one of the second one exists
    assertThat(nativeLibrariesTempFolder.listFiles().length, is(1));
    assertFalse(nativeLibrariesTempFolderFirstDeployment.exists());
  }

  @Test
  @Story(APPLICATION_REDEPLOYMENT)
  public void explodedAppRedeploymentDoesNotDeleteTempFile() throws Exception {
    testTempFileOnRedeployment(() -> addExplodedAppFromBuilder(emptyAppFileBuilder),
                               () -> addExplodedAppFromBuilder(emptyAppFileBuilder));
  }

  @Test
  @Story(APPLICATION_REDEPLOYMENT)
  public void packedAppRedeploymentDoesNotDeleteTempFile() throws Exception {
    testTempFileOnRedeployment(() -> addPackedAppFromBuilder(emptyAppFileBuilder),
                               () -> addPackedAppFromBuilder(emptyAppFileBuilder));
  }

  @Test
  @Story(APPLICATION_REDEPLOYMENT)
  public void packedAppRedeploymentWithExplodedDoesNotDeleteTempFile() throws Exception {
    testTempFileOnRedeployment(() -> addPackedAppFromBuilder(emptyAppFileBuilder),
                               () -> addExplodedAppFromBuilder(emptyAppFileBuilder));
  }

  @Test
  @Story(APPLICATION_REDEPLOYMENT)
  public void explodedAppRedeploymentWithPackedDoesNotDeleteTempFile() throws Exception {
    testTempFileOnRedeployment(() -> addExplodedAppFromBuilder(emptyAppFileBuilder),
                               () -> addPackedAppFromBuilder(emptyAppFileBuilder));
  }

  @Test
  @Story(APPLICATION_REDEPLOYMENT)
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
    deploy(deploymentService, dummyAppDescriptorFileBuilder.getArtifactFile().toURI());

    // Application was redeployed
    verify(mockDeploymentListener, times(1)).onRedeploymentSuccess(
                                                                   dummyAppDescriptorFileBuilder.getId());
  }

  @Test
  @Story(APPLICATION_REDEPLOYMENT)
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
    deploy(deploymentService, dummyAppDescriptorFileBuilder.getArtifactFile().toURI());

    // Application was redeployed
    verify(mockDeploymentListener, times(1)).onRedeploymentSuccess(dummyAppDescriptorFileBuilder.getId());
  }

  @Test
  @Issue("MULE-19127")
  @Feature(DEPLOYMENT_CONFIGURATION)
  @Story(FLOW_STATE_PERSISTENCE)
  public void whenDeploymentFailsDoNotPersistFlows() throws Exception {
    addPackedAppFromBuilder(dummyFlowErrorAppDescriptorFileBuilder);

    startDeployment();

    assertDeploymentFailure(applicationDeploymentListener, dummyFlowErrorAppDescriptorFileBuilder.getId());

    Optional<Properties> flowDeploymentProperties =
        getPersistedFlowDeploymentProperties(dummyFlowErrorAppDescriptorFileBuilder.getId());
    assertThat(flowDeploymentProperties.isPresent(), is(true));

    for (int i = 1; i < 4; i++) {
      Object mustStartFlow = flowDeploymentProperties.get().get("test" + i + "_" + START_FLOW_ON_DEPLOYMENT_PROPERTY);
      if (mustStartFlow != null) {
        assertThat(mustStartFlow, is("true"));
      }
    }
  }

  public static class FailingProcessorTest implements Processor, Startable {

    @Override
    public void start() throws MuleException {
      throw new RuntimeException("Failing processor error");
    }

    @Override
    public CoreEvent process(CoreEvent event) throws MuleException {
      return event;
    }
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
    return new File((app.getArtifactContext().getRegistry().<MuleConfiguration>lookupByName(OBJECT_MULE_CONFIGURATION).get()
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
    restartServer(serializedAstWithFallbackArtifactConfigurationProcessor());
  }

  private void restartServer(ArtifactConfigurationProcessor artifactConfigurationProcessor) throws MuleException {
    serviceManager.stop();
    stopIfNeeded(extensionModelLoaderRepository);
    deploymentService.stop();

    reset(applicationDeploymentListener);

    MuleArtifactResourcesRegistry muleArtifactResourcesRegistry =
        new MuleArtifactResourcesRegistry.Builder()
            .moduleRepository(moduleRepository)
            .artifactConfigurationProcessor(artifactConfigurationProcessor)
            .build();

    serviceManager = muleArtifactResourcesRegistry.getServiceManager();
    serviceManager.start();

    extensionModelLoaderRepository = muleArtifactResourcesRegistry.getExtensionModelLoaderRepository();
    startIfNeeded(extensionModelLoaderRepository);

    deploymentService = new TestMuleDeploymentService(muleArtifactResourcesRegistry.getDomainFactory(),
                                                      muleArtifactResourcesRegistry.getApplicationFactory(),
                                                      () -> findSchedulerService(serviceManager));
    configureDeploymentService();
    deploymentService.start(false);
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
    for (Flow flow : app.getArtifactContext().getRegistry().lookupAllByType(Flow.class)) {
      assertThat(flow.getLifecycleState().isStarted(), is(started));
    }
  }

  /**
   * Class to test injection of memory management.
   */
  private static class InjectedMemoryManagement {

    @Inject
    private MemoryManagementService memoryManagementService;

    public MemoryManagementService getMemoryManagementService() {
      return memoryManagementService;
    }
  }
}
