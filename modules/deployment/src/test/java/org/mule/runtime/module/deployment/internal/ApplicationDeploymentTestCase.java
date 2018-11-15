/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.runtime.module.deployment.internal;

import static java.io.File.separator;
import static java.lang.String.format;
import static java.lang.Thread.currentThread;
import static java.util.Arrays.asList;
import static java.util.Collections.emptyMap;
import static org.apache.commons.io.FileUtils.copyFile;
import static org.apache.commons.io.FileUtils.deleteDirectory;
import static org.apache.commons.io.FileUtils.writeStringToFile;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.instanceOf;
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
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mule.runtime.api.deployment.meta.Product.MULE;
import static org.mule.runtime.container.api.MuleFoldersUtil.getMuleBaseFolder;
import static org.mule.runtime.container.internal.ClasspathModuleDiscoverer.EXPORTED_CLASS_PACKAGES_PROPERTY;
import static org.mule.runtime.container.internal.ClasspathModuleDiscoverer.EXPORTED_RESOURCE_PROPERTY;
import static org.mule.runtime.container.internal.ClasspathModuleDiscoverer.PRIVILEGED_ARTIFACTS_PROPERTY;
import static org.mule.runtime.container.internal.ClasspathModuleDiscoverer.PRIVILEGED_EXPORTED_CLASS_PACKAGES_PROPERTY;
import static org.mule.runtime.core.api.config.MuleProperties.OBJECT_MULE_CONFIGURATION;
import static org.mule.runtime.core.internal.config.bootstrap.ClassLoaderRegistryBootstrapDiscoverer.BOOTSTRAP_PROPERTIES;
import static org.mule.runtime.deployment.model.api.DeployableArtifactDescriptor.PROPERTY_CONFIG_RESOURCES;
import static org.mule.runtime.deployment.model.api.application.ApplicationStatus.DESTROYED;
import static org.mule.runtime.deployment.model.api.application.ApplicationStatus.STOPPED;
import static org.mule.runtime.deployment.model.api.artifact.ArtifactDescriptorConstants.EXPORTED_PACKAGES;
import static org.mule.runtime.deployment.model.api.artifact.ArtifactDescriptorConstants.EXPORTED_RESOURCES;
import static org.mule.runtime.deployment.model.api.artifact.ArtifactDescriptorConstants.MULE_LOADER_ID;
import static org.mule.runtime.extension.api.loader.xml.XmlExtensionModelLoader.RESOURCE_XML;
import static org.mule.runtime.module.deployment.impl.internal.policy.PropertiesBundleDescriptorLoader.PROPERTIES_BUNDLE_DESCRIPTOR_LOADER_ID;
import static org.mule.runtime.module.deployment.internal.DeploymentDirectoryWatcher.DEPLOYMENT_APPLICATION_PROPERTY;
import static org.mule.runtime.module.deployment.internal.TestApplicationFactory.createTestApplicationFactory;
import static org.mule.runtime.module.extension.api.loader.java.DefaultJavaExtensionModelLoader.JAVA_LOADER_ID;
import static org.mule.tck.MuleTestUtils.testWithSystemProperty;
import org.mule.runtime.api.component.ConfigurationProperties;
import org.mule.runtime.api.deployment.meta.MuleArtifactLoaderDescriptor;
import org.mule.runtime.api.deployment.meta.MuleArtifactLoaderDescriptorBuilder;
import org.mule.runtime.api.deployment.meta.MulePluginModel;
import org.mule.runtime.api.exception.MuleException;
import org.mule.runtime.api.exception.MuleFatalException;
import org.mule.runtime.core.api.config.MuleConfiguration;
import org.mule.runtime.core.api.config.MuleProperties;
import org.mule.runtime.core.api.extension.ExtensionManager;
import org.mule.runtime.core.api.util.func.CheckedRunnable;
import org.mule.runtime.deployment.model.api.application.Application;
import org.mule.runtime.deployment.model.api.application.ApplicationStatus;
import org.mule.runtime.deployment.model.internal.application.MuleApplicationClassLoaderFactory;
import org.mule.runtime.deployment.model.internal.nativelib.DefaultNativeLibraryFinderFactory;
import org.mule.runtime.extension.api.loader.xml.XmlExtensionModelLoader;
import org.mule.runtime.module.artifact.builder.TestArtifactDescriptor;
import org.mule.runtime.module.deployment.impl.internal.builder.ApplicationFileBuilder;
import org.mule.runtime.module.deployment.impl.internal.builder.ArtifactPluginFileBuilder;
import org.mule.runtime.module.deployment.impl.internal.builder.JarFileBuilder;
import org.mule.runtime.module.deployment.impl.internal.domain.DefaultDomainManager;
import org.mule.tck.util.CompilerUtils;
import org.mule.tck.util.CompilerUtils.SingleClassCompiler;

import java.io.File;
import java.net.URL;
import java.nio.file.Paths;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Properties;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.ReentrantLock;

import io.qameta.allure.Description;
import io.qameta.allure.Issue;
import org.apache.commons.io.IOUtils;
import org.hamcrest.BaseMatcher;
import org.hamcrest.Matcher;
import org.junit.BeforeClass;
import org.junit.Test;

/**
 * Contains test for application deployment on the default domain
 */
public class ApplicationDeploymentTestCase extends AbstractDeploymentTestCase {

  private static final String PRIVILEGED_EXTENSION_ARTIFACT_ID = "privilegedExtensionPlugin";
  private static final String PRIVILEGED_EXTENSION_ARTIFACT_FULL_ID = "org.mule.test:" + PRIVILEGED_EXTENSION_ARTIFACT_ID;
  private static final String APP_WITH_PRIVILEGED_EXTENSION_PLUGIN_CONFIG = "app-with-privileged-extension-plugin-config.xml";
  private static final String BROKEN_CONFIG_XML = "/broken-config.xml";

  private static final Matcher<File> exists = new BaseMatcher<File>() {

    @Override
    public boolean matches(Object o) {
      return ((File) o).exists();
    }

    @Override
    public void describeTo(org.hamcrest.Description description) {
      description.appendText("File does not exist");
    }
  };

  // Classes and JAR resources
  private static File pluginEcho3TestClassFile;
  private static File pluginEcho2TestClassFile;
  private static File privilegedExtensionV1JarFile;

  // Application artifact builders
  private final ApplicationFileBuilder incompleteAppFileBuilder =
      new ApplicationFileBuilder("incomplete-app").definedBy("incomplete-app-config.xml");
  private final ApplicationFileBuilder brokenAppFileBuilder = new ApplicationFileBuilder("broken-app").corrupted();
  private final ApplicationFileBuilder brokenAppWithFunkyNameAppFileBuilder =
      new ApplicationFileBuilder("broken-app+", brokenAppFileBuilder);
  private final ApplicationFileBuilder waitAppFileBuilder =
      new ApplicationFileBuilder("wait-app").definedBy("wait-app-config.xml");
  private final ApplicationFileBuilder dummyAppDescriptorWithPropsFileBuilder = new ApplicationFileBuilder(
                                                                                                           "dummy-app-with-props")
                                                                                                               .definedBy("dummy-app-with-props-config.xml")
                                                                                                               .containingClass(echoTestClassFile,
                                                                                                                                "org/foo/EchoTest.class");

  // Application plugin artifact builders
  private final ArtifactPluginFileBuilder echoPluginWithLib1 =
      new ArtifactPluginFileBuilder("echoPlugin1").configuredWith(EXPORTED_CLASS_PACKAGES_PROPERTY, "org.foo")
          .dependingOn(new JarFileBuilder("barUtils1", barUtils1_0JarFile))
          .containingClass(pluginEcho1TestClassFile, "org/foo/Plugin1Echo.class");

  @BeforeClass
  public static void compileTestClasses() throws Exception {
    pluginEcho2TestClassFile =
        new CompilerUtils.SingleClassCompiler().dependingOn(barUtils2_0JarFile)
            .compile(getResourceFile("/org/foo/echo/Plugin2Echo.java"));
    pluginEcho3TestClassFile = new CompilerUtils.SingleClassCompiler().compile(getResourceFile("/org/foo/echo/Plugin3Echo.java"));

    privilegedExtensionV1JarFile =
        new CompilerUtils.ExtensionCompiler().compiling(getResourceFile("/org/foo/privileged/PrivilegedExtension.java"),
                                                        getResourceFile("/org/foo/privileged/PrivilegedOperation.java"))
            .compile("mule-module-privileged-1.0.jar", "1.0");
  }

  public ApplicationDeploymentTestCase(boolean parallelDeployment) {
    super(parallelDeployment);
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
    addPackedAppFromBuilder(emptyAppFileBuilder);

    startDeployment();

    assertApplicationDeploymentSuccess(applicationDeploymentListener, emptyAppFileBuilder.getId());

    final Application app = findApp(emptyAppFileBuilder.getId(), 1);
    assertThat(app.getRegistry().<ExtensionManager>lookupByName(MuleProperties.OBJECT_EXTENSION_MANAGER).get(),
               is(notNullValue()));
  }

  @Test
  public void appHomePropertyIsPresent() throws Exception {
    final ApplicationFileBuilder globalPropertyAppFileBuilder =
        new ApplicationFileBuilder("property-app").definedBy("app-properties-config.xml");

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
        new ApplicationFileBuilder("dummy-app", true)
            .definedBy("dummy-app-config.xml").configuredWith("myCustomProp", "someValue")
            .containingClass(echoTestClassFile, "org/foo/EchoTest.class");

    deployAfterStartUp(dummyAppDescriptorFileBuilderWithUpperCaseInExtension);
  }

  @Test
  public void deploysAppWithNonExistentConfigResourceOnDeclaration() throws Exception {
    ApplicationFileBuilder appBundleNonExistentConfigResource = new ApplicationFileBuilder("non-existent-app-config-resource")
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
  public void removesPreviousAppFolderOnRedeploy() throws Exception {
    startDeployment();

    addPackedAppFromBuilder(emptyAppFileBuilder);

    assertApplicationDeploymentSuccess(applicationDeploymentListener, emptyAppFileBuilder.getId());

    assertAppsDir(NONE, new String[] {emptyAppFileBuilder.getId()}, true);
    assertEquals("Application has not been properly registered with Mule", 1, deploymentService.getApplications().size());

    reset(applicationDeploymentListener);

    ApplicationFileBuilder emptyAppFileBuilder =
        new ApplicationFileBuilder("empty-app").usingResource("empty-config.xml", "empty-config.xml")
            .deployedWith(PROPERTY_CONFIG_RESOURCES, "empty-config.xml");

    addPackedAppFromBuilder(emptyAppFileBuilder);

    assertApplicationRedeploymentSuccess(emptyAppFileBuilder.getId());

    assertApplicationFiles(emptyAppFileBuilder.getId(), new String[] {"empty-config.xml"});
  }

  @Test
  public void removesPreviousAppFolderOnStart() throws Exception {
    addExplodedAppFromBuilder(emptyAppFileBuilder);

    ApplicationFileBuilder emptyAppFileBuilder =
        new ApplicationFileBuilder("empty-app").usingResource("empty-config.xml", "empty-config.xml")
            .deployedWith(PROPERTY_CONFIG_RESOURCES, "empty-config.xml");

    addPackedAppFromBuilder(emptyAppFileBuilder);

    startDeployment();

    assertApplicationDeploymentSuccess(applicationDeploymentListener, emptyAppFileBuilder.getId());

    assertEquals("Application has not been properly registered with Mule", 1, deploymentService.getApplications().size());
    assertAppsDir(NONE, new String[] {emptyAppFileBuilder.getId()}, true);

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
  public void removesZombieFilesAfterremovesZombieFilesAfterFailedAppIsDeleted() throws Exception {
    final String appName = "bad-config-app";

    final ApplicationFileBuilder badConfigAppFileBuilder = new ApplicationFileBuilder(appName).definedBy("bad-app-config.xml");

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
    final ApplicationFileBuilder applicationFileBuilder = new ApplicationFileBuilder("empty-app.jar", emptyAppFileBuilder);
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
    return new ApplicationFileBuilder("empty-app").definedBy("empty-config.xml");
  }

  @Test
  public void deploysExplodedAppsInOrderWhenAppArgumentIsUsed() throws Exception {
    assumeThat(parallelDeployment, is(false));

    ApplicationFileBuilder appFileBuilder1 = new ApplicationFileBuilder("1").definedBy("empty-config.xml");
    addExplodedAppFromBuilder(appFileBuilder1);
    ApplicationFileBuilder appFileBuilder2 = new ApplicationFileBuilder("2").definedBy("empty-config.xml");
    addExplodedAppFromBuilder(appFileBuilder2);
    ApplicationFileBuilder appFileBuilder3 = new ApplicationFileBuilder("3").definedBy("empty-config.xml");
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
    configFile.setLastModified(System.currentTimeMillis() + ONE_HOUR_IN_MILLISECONDS);

    startDeployment();
    assertApplicationDeploymentSuccess(applicationDeploymentListener, emptyAppFileBuilder.getId());
    reset(applicationDeploymentListener);

    assertNoDeploymentInvoked(applicationDeploymentListener);
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
    addPackedAppFromBuilder(emptyAppFileBuilder);

    startDeployment();

    assertApplicationDeploymentSuccess(applicationDeploymentListener, emptyAppFileBuilder.getId());
    final Application app = findApp(emptyAppFileBuilder.getId(), 1);
    app.stop();
    assertStatus(app, STOPPED);

    deploymentService.undeploy(app);
  }

  @Test
  public void undeploysApplicationRemovingAnchorFile() throws Exception {
    addPackedAppFromBuilder(emptyAppFileBuilder);

    startDeployment();

    assertApplicationDeploymentSuccess(applicationDeploymentListener, emptyAppFileBuilder.getId());
    Application app = findApp(emptyAppFileBuilder.getId(), 1);

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
  public void deploysAppZipWithPlugin() throws Exception {
    final ApplicationFileBuilder echoPluginAppFileBuilder =
        new ApplicationFileBuilder("dummyWithEchoPlugin").definedBy("app-with-echo-plugin-config.xml").dependingOn(echoPlugin);

    addPackedAppFromBuilder(echoPluginAppFileBuilder);

    startDeployment();

    assertDeploymentSuccess(applicationDeploymentListener, echoPluginAppFileBuilder.getId());
  }

  @Test
  public void deploysAppWithPluginSharedLibrary() throws Exception {
    final ArtifactPluginFileBuilder echoPluginWithoutLib1 = new ArtifactPluginFileBuilder("echoPlugin1")
        .configuredWith(EXPORTED_CLASS_PACKAGES_PROPERTY, "org.foo")
        .containingClass(pluginEcho1TestClassFile, "org/foo/Plugin1Echo.class");

    final ApplicationFileBuilder sharedLibPluginAppFileBuilder = new ApplicationFileBuilder("shared-plugin-lib-app")
        .definedBy("app-with-echo1-plugin-config.xml").dependingOn(echoPluginWithoutLib1)
        .dependingOnSharedLibrary(new JarFileBuilder("barUtils", barUtils1_0JarFile));

    addPackedAppFromBuilder(sharedLibPluginAppFileBuilder);

    startDeployment();

    assertApplicationDeploymentSuccess(applicationDeploymentListener, sharedLibPluginAppFileBuilder.getId());
    assertAppsDir(NONE, new String[] {sharedLibPluginAppFileBuilder.getId()}, true);
    assertApplicationAnchorFileExists(sharedLibPluginAppFileBuilder.getId());

    executeApplicationFlow("main");
  }

  @Test
  public void deploysAppWithPluginExportingAlreadyProvidedAppPackage() throws Exception {
    // Defines a plugin that exports org.bar which is also exported on the application
    ArtifactPluginFileBuilder echoPluginWithoutLib1 = new ArtifactPluginFileBuilder("echoPlugin1")
        .configuredWith(EXPORTED_CLASS_PACKAGES_PROPERTY, "org.foo,org.bar")
        .containingClass(pluginEcho1TestClassFile, "org/foo/Plugin1Echo.class");

    ApplicationFileBuilder sharedLibPluginAppFileBuilder = new ApplicationFileBuilder("shared-plugin-lib-app")
        .definedBy("app-with-echo1-plugin-config.xml").dependingOn(echoPluginWithoutLib1)
        .dependingOnSharedLibrary(new JarFileBuilder("barUtils", barUtils1_0JarFile));

    addPackedAppFromBuilder(sharedLibPluginAppFileBuilder);

    startDeployment();

    assertApplicationDeploymentSuccess(applicationDeploymentListener, sharedLibPluginAppFileBuilder.getId());
    assertAppsDir(NONE, new String[] {sharedLibPluginAppFileBuilder.getId()}, true);
    assertApplicationAnchorFileExists(sharedLibPluginAppFileBuilder.getId());

    executeApplicationFlow("main");
  }

  @Test
  public void deploysAppWithExportedPackagePrecedenceOverPlugin() throws Exception {
    // Defines a plugin that contains org.bar package, which is also exported on the application
    ArtifactPluginFileBuilder echoPluginWithoutLib1 = new ArtifactPluginFileBuilder("echoPlugin1")
        .configuredWith(EXPORTED_CLASS_PACKAGES_PROPERTY, "org.foo")
        .containingClass(pluginEcho1TestClassFile, "org/foo/Plugin1Echo.class")
        .dependingOn(new JarFileBuilder("barUtils2_0", barUtils2_0JarFile));

    ApplicationFileBuilder sharedLibPluginAppFileBuilder = new ApplicationFileBuilder("shared-plugin-lib-app")
        .definedBy("app-with-echo1-plugin-config.xml").dependingOn(echoPluginWithoutLib1)
        .dependingOnSharedLibrary(new JarFileBuilder("barUtils", barUtils1_0JarFile));

    addPackedAppFromBuilder(sharedLibPluginAppFileBuilder);

    startDeployment();

    assertApplicationDeploymentSuccess(applicationDeploymentListener, sharedLibPluginAppFileBuilder.getId());
    assertAppsDir(NONE, new String[] {sharedLibPluginAppFileBuilder.getId()}, true);
    assertApplicationAnchorFileExists(sharedLibPluginAppFileBuilder.getId());

    executeApplicationFlow("main");
  }

  @Test
  @Issue("MULE-13756")
  @Description("Tests that code called form plugin's Processor cannot access internal resources/packages of the application")
  public void deploysAppWithNotExportedPackageAndPlugin() throws Exception {
    ArtifactPluginFileBuilder loadsAppResourcePlugin = new ArtifactPluginFileBuilder("loadsAppResourcePlugin")
        .configuredWith(EXPORTED_CLASS_PACKAGES_PROPERTY, "org.foo")
        .containingClass(loadsAppResourceCallbackClassFile, "org/foo/LoadsAppResourceCallback.class");

    ApplicationFileBuilder nonExposingAppFileBuilder = new ApplicationFileBuilder("non-exposing-app")
        .configuredWith(EXPORTED_PACKAGES, "org.bar1")
        .configuredWith(EXPORTED_RESOURCES, "test-resource.txt")
        .definedBy("app-with-loads-app-resource-plugin-config.xml")
        .containingClass(barUtils1ClassFile, "org/bar1/BarUtils.class")
        .containingClass(barUtils2ClassFile, "org/bar2/BarUtils.class")
        .containingResource("test-resource.txt", "test-resource.txt")
        .containingResource("test-resource.txt", "test-resource-not-exported.txt")
        .dependingOn(loadsAppResourcePlugin);

    addPackedAppFromBuilder(nonExposingAppFileBuilder);

    startDeployment();

    assertApplicationDeploymentSuccess(applicationDeploymentListener, nonExposingAppFileBuilder.getId());

    executeApplicationFlow("main");
  }

  @Test
  @Issue("MULE-13756")
  @Description("Tests that code called form application's Processor can access internal resources/packages of the application")
  public void deploysAppWithNotExportedPackage() throws Exception {
    ApplicationFileBuilder nonExposingAppFileBuilder = new ApplicationFileBuilder("non-exposing-app")
        .configuredWith(EXPORTED_PACKAGES, "org.bar1")
        .configuredWith(EXPORTED_RESOURCES, "test-resource.txt")
        .definedBy("app-with-loads-app-resource-plugin-config.xml")
        .containingClass(loadsAppResourceCallbackClassFile, "org/foo/LoadsAppResourceCallback.class")
        .containingClass(barUtils1ClassFile, "org/bar1/BarUtils.class")
        .containingClass(barUtils2ClassFile, "org/bar2/BarUtils.class")
        .containingResource("test-resource.txt", "test-resource.txt")
        .containingResource("test-resource.txt", "test-resource-not-exported.txt");

    addPackedAppFromBuilder(nonExposingAppFileBuilder);

    startDeployment();

    assertApplicationDeploymentSuccess(applicationDeploymentListener, nonExposingAppFileBuilder.getId());

    executeApplicationFlow("main");
  }

  @Test
  @Issue("MULE-13756")
  @Description("Tests that code called form plugin's ProcessorInterceptor cannot access internal resources/packages of the application")
  public void deploysAppWithNotExportedPackageAndPluginWithInterceptors() throws Exception {
    File loadsAppResourceInterceptorFactoryClassFile =
        new SingleClassCompiler().compile(getResourceFile("/org/foo/LoadsAppResourceInterceptorFactory.java"));
    File loadsAppResourceInterceptorClassFile =
        new SingleClassCompiler().compile(getResourceFile("/org/foo/LoadsAppResourceInterceptor.java"));

    ArtifactPluginFileBuilder loadsAppResourceInterceptorPlugin =
        new ArtifactPluginFileBuilder("loadsAppResourceInterceptorPlugin")
            .configuredWith(EXPORTED_CLASS_PACKAGES_PROPERTY, "org.lalala")
            .containingClass(loadsAppResourceInterceptorFactoryClassFile, "org/foo/LoadsAppResourceInterceptorFactory.class")
            .containingClass(loadsAppResourceInterceptorClassFile, "org/foo/LoadsAppResourceInterceptor.class")
            .containingResource("registry-bootstrap-loads-app-resource-pif.properties",
                                "META-INF/org/mule/runtime/core/config/registry-bootstrap.properties");

    ApplicationFileBuilder nonExposingAppFileBuilder = new ApplicationFileBuilder("non-exposing-app")
        .configuredWith(EXPORTED_PACKAGES, "org.bar1")
        .configuredWith(EXPORTED_RESOURCES, "test-resource.txt")
        .definedBy("app-with-plugin-bootstrap.xml")
        .containingClass(barUtils1ClassFile, "org/bar1/BarUtils.class")
        .containingClass(barUtils2ClassFile, "org/bar2/BarUtils.class")
        .containingResource("test-resource.txt", "test-resource.txt")
        .containingResource("test-resource.txt", "test-resource-not-exported.txt")
        .dependingOn(loadsAppResourceInterceptorPlugin);

    addPackedAppFromBuilder(nonExposingAppFileBuilder);

    startDeployment();

    assertApplicationDeploymentSuccess(applicationDeploymentListener, nonExposingAppFileBuilder.getId());

    executeApplicationFlow("main");
  }

  @Test
  @Issue("MULE-13756")
  @Description("Tests that code called form application's ProcessorInterceptor can access internal resources/packages of the application")
  public void deploysAppWithInterceptorsAndNotExportedPackage() throws Exception {
    File loadsOwnResourceInterceptorFactoryClassFile =
        new SingleClassCompiler().compile(getResourceFile("/org/foo/LoadsOwnResourceInterceptorFactory.java"));
    File loadsOwnResourceInterceptorClassFile =
        new SingleClassCompiler().compile(getResourceFile("/org/foo/LoadsOwnResourceInterceptor.java"));

    ApplicationFileBuilder nonExposingAppFileBuilder = new ApplicationFileBuilder("non-exposing-app")
        .configuredWith(EXPORTED_PACKAGES, "org.bar1")
        .configuredWith(EXPORTED_RESOURCES, "test-resource.txt")
        .definedBy("app-with-interceptor.xml")
        .containingClass(loadsOwnResourceInterceptorFactoryClassFile, "org/foo/LoadsOwnResourceInterceptorFactory.class")
        .containingClass(loadsOwnResourceInterceptorClassFile, "org/foo/LoadsOwnResourceInterceptor.class")
        .containingClass(barUtils1ClassFile, "org/bar1/BarUtils.class")
        .containingClass(barUtils2ClassFile, "org/bar2/BarUtils.class")
        .containingResource("test-resource.txt", "test-resource.txt")
        .containingResource("test-resource.txt", "test-resource-not-exported.txt");

    addPackedAppFromBuilder(nonExposingAppFileBuilder);

    startDeployment();

    assertApplicationDeploymentSuccess(applicationDeploymentListener, nonExposingAppFileBuilder.getId());

    executeApplicationFlow("main");
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
  public void deploysAppZipWithPrivilegedExtensionPlugin() throws Exception {
    ArtifactPluginFileBuilder privilegedExtensionPlugin = createPrivilegedExtensionPlugin();

    ApplicationFileBuilder applicationFileBuilder = new ApplicationFileBuilder("privilegedPluginApp")
        .definedBy(APP_WITH_PRIVILEGED_EXTENSION_PLUGIN_CONFIG).dependingOn(privilegedExtensionPlugin);
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

    ApplicationFileBuilder applicationFileBuilder = new ApplicationFileBuilder("invalidPrivilegedPluginApp")
        .definedBy(APP_WITH_PRIVILEGED_EXTENSION_PLUGIN_CONFIG).dependingOn(invalidPrivilegedPlugin);
    addPackedAppFromBuilder(applicationFileBuilder);

    startDeployment();

    assertDeploymentFailure(applicationDeploymentListener, applicationFileBuilder.getId());
  }

  @Test
  public void deploysWithExtensionXmlPlugin() throws Exception {
    ApplicationFileBuilder applicationFileBuilder = new ApplicationFileBuilder("appWithExtensionXmlPlugin")
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
    ApplicationFileBuilder applicationFileBuilder = new ApplicationFileBuilder("appWithExtensionXmlPluginWithXmlDependencies")
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

    ApplicationFileBuilder applicationFileBuilder = new ApplicationFileBuilder("appWithExtensionXmlPluginWithDependencies")
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

    ApplicationFileBuilder applicationFileBuilder = new ApplicationFileBuilder("appWithExtensionResourcesXmlPlugin")
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

    //final assertion is to guarantee the DWL file can be accessed in the app classloader (cannot evaluate the expression as the
    //test uses a mocked expression evaluator
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

    ApplicationFileBuilder applicationFileBuilder = new ApplicationFileBuilder("appWithExtensionXmlPluginFails")
        .definedBy("app-with-extension-xml-plugin-module-bye.xml").dependingOn(byeXmlExtensionPlugin);
    addPackedAppFromBuilder(applicationFileBuilder);

    startDeployment();

    assertDeploymentFailure(applicationDeploymentListener, applicationFileBuilder.getId());
  }

  @Test
  public void failsToDeployWithExtensionThatHasNonExistingIdForClassLoaderModel() throws Exception {
    String extensionName = "extension-with-classloader-model-id-non-existing";

    MulePluginModel.MulePluginModelBuilder builder =
        new MulePluginModel.MulePluginModelBuilder().setName(extensionName).setMinMuleVersion(MIN_MULE_VERSION);
    builder.withClassLoaderModelDescriptorLoader(new MuleArtifactLoaderDescriptorBuilder().setId("a-non-existing-ID-describer")
        .addProperty("aProperty", "aValue").build());
    builder.withBundleDescriptorLoader(createBundleDescriptorLoader(extensionName, MULE_EXTENSION_CLASSIFIER,
                                                                    PROPERTIES_BUNDLE_DESCRIPTOR_LOADER_ID));

    final ArtifactPluginFileBuilder byeXmlExtensionPlugin = new ArtifactPluginFileBuilder(extensionName)
        .describedBy(builder.build());

    ApplicationFileBuilder applicationFileBuilder = new ApplicationFileBuilder("appWithExtensionXmlPluginFails")
        .definedBy("app-with-extension-xml-plugin-module-bye.xml").dependingOn(byeXmlExtensionPlugin);
    addPackedAppFromBuilder(applicationFileBuilder);

    startDeployment();

    assertDeploymentFailure(applicationDeploymentListener, applicationFileBuilder.getId(), times(1));
  }

  @Test
  public void deploysAppWithPluginBootstrapProperty() throws Exception {
    final ArtifactPluginFileBuilder pluginFileBuilder = new ArtifactPluginFileBuilder("bootstrapPlugin")
        .containingResource("plugin-bootstrap.properties", BOOTSTRAP_PROPERTIES)
        .containingClass(echoTestClassFile, "org/foo/EchoTest.class")
        .configuredWith(EXPORTED_RESOURCE_PROPERTY, BOOTSTRAP_PROPERTIES);

    ApplicationFileBuilder applicationFileBuilder = new ApplicationFileBuilder("app-with-plugin-bootstrap")
        .definedBy("app-with-plugin-bootstrap.xml").dependingOn(pluginFileBuilder);
    addPackedAppFromBuilder(applicationFileBuilder);

    startDeployment();

    assertApplicationDeploymentSuccess(applicationDeploymentListener, applicationFileBuilder.getId());

    final Application application = findApp(applicationFileBuilder.getId(), 1);
    final Optional<Object> lookupObject = application.getRegistry().lookupByName("plugin.echotest");
    assertThat(lookupObject.isPresent(), is(true));
    assertThat(lookupObject.get().getClass().getName(), equalTo("org.foo.EchoTest"));
  }

  @Test
  public void failsToDeployApplicationOnMissingService() throws Exception {
    ArtifactPluginFileBuilder extensionPlugin = new ArtifactPluginFileBuilder("extensionPlugin")
        .dependingOn(new JarFileBuilder("bundleExtensionv1", helloExtensionV1JarFile))
        .configuredWith(EXPORTED_RESOURCE_PROPERTY, "/, META-INF");
    ApplicationFileBuilder applicationFileBuilder = new ApplicationFileBuilder("appWithExtensionPlugin")
        .definedBy(APP_WITH_EXTENSION_PLUGIN_CONFIG).dependingOn(extensionPlugin);
    addPackedAppFromBuilder(applicationFileBuilder);

    startDeployment();

    assertDeploymentFailure(applicationDeploymentListener, applicationFileBuilder.getId());
  }

  @Test
  public void deploysMultiPluginVersionLib() throws Exception {
    final ArtifactPluginFileBuilder echoPluginWithLib2 =
        new ArtifactPluginFileBuilder("echoPlugin2").configuredWith(EXPORTED_CLASS_PACKAGES_PROPERTY, "org.foo.echo")
            .dependingOn(new JarFileBuilder("barUtils2", barUtils2_0JarFile))
            .containingClass(pluginEcho2TestClassFile, "org/foo/echo/Plugin2Echo.class");

    final ApplicationFileBuilder multiLibPluginAppFileBuilder = new ApplicationFileBuilder("multiPluginLibVersion")
        .definedBy("multi-plugin-app-config.xml").dependingOn(echoPluginWithLib1).dependingOn(echoPluginWithLib2);

    addPackedAppFromBuilder(multiLibPluginAppFileBuilder);

    startDeployment();

    assertDeploymentSuccess(applicationDeploymentListener, multiLibPluginAppFileBuilder.getId());

    executeApplicationFlow("main");
  }

  @Test
  public void deploysApplicationWithPluginDependingOnPlugin() throws Exception {

    ArtifactPluginFileBuilder dependantPlugin =
        new ArtifactPluginFileBuilder("dependantPlugin").configuredWith(EXPORTED_CLASS_PACKAGES_PROPERTY, "org.foo.echo")
            .containingClass(pluginEcho3TestClassFile, "org/foo/echo/Plugin3Echo.class")
            .dependingOn(echoPlugin);

    final TestArtifactDescriptor artifactFileBuilder = new ApplicationFileBuilder("plugin-depending-on-plugin-app")
        .definedBy("plugin-depending-on-plugin-app-config.xml").dependingOn(dependantPlugin);
    addPackedAppFromBuilder(artifactFileBuilder);

    startDeployment();

    assertDeploymentSuccess(applicationDeploymentListener, artifactFileBuilder.getId());

    executeApplicationFlow("main");
  }

  @Test
  public void deploysLightApplicationWithPluginDependingOnPlugin() throws Exception {

    ArtifactPluginFileBuilder dependantPlugin =
        new ArtifactPluginFileBuilder("dependantPlugin").configuredWith(EXPORTED_CLASS_PACKAGES_PROPERTY, "org.foo.echo")
            .containingClass(pluginEcho3TestClassFile, "org/foo/echo/Plugin3Echo.class")
            .dependingOn(echoPlugin);

    File mavenRepoFolder = Paths.get(getMuleBaseFolder().getAbsolutePath(), "repository").toFile();
    File testGroupIdRepoFolder = Paths.get(mavenRepoFolder.getAbsolutePath(), "org", "mule", "test").toFile();

    JarFileBuilder testJarFileDependency = new JarFileBuilder("echoTestJar", echoTestJarFile);
    copyFile(testJarFileDependency.getArtifactPomFile(),
             Paths.get(testGroupIdRepoFolder.getAbsolutePath(), "echoTestJar", "1.0.0", "echoTestJar-1.0.0.pom").toFile());
    copyFile(testJarFileDependency.getArtifactFile(),
             Paths.get(testGroupIdRepoFolder.getAbsolutePath(), "echoTestJar", "1.0.0", "echoTestJar-1.0.0.jar").toFile());

    copyFile(echoPlugin.getArtifactPomFile(),
             Paths.get(testGroupIdRepoFolder.getAbsolutePath(), "echoPlugin", "1.0.0", "echoPlugin-1.0.0.pom").toFile());
    copyFile(echoPlugin.getArtifactFile(),
             Paths.get(testGroupIdRepoFolder.getAbsolutePath(), "echoPlugin", "1.0.0", "echoPlugin-1.0.0-mule-plugin.jar")
                 .toFile());

    copyFile(dependantPlugin.getArtifactPomFile(),
             Paths.get(testGroupIdRepoFolder.getAbsolutePath(), "dependantPlugin", "1.0.0", "dependantPlugin-1.0.0.pom")
                 .toFile());
    copyFile(dependantPlugin.getArtifactFile(),
             Paths.get(testGroupIdRepoFolder.getAbsolutePath(), "dependantPlugin", "1.0.0",
                       "dependantPlugin-1.0.0-mule-plugin.jar")
                 .toFile());

    final TestArtifactDescriptor artifactFileBuilder = new ApplicationFileBuilder("plugin-depending-on-plugin-app")
        .definedBy("plugin-depending-on-plugin-app-config.xml").dependingOn(dependantPlugin).usingLightWeightPackage();
    addPackedAppFromBuilder(artifactFileBuilder);

    startDeployment();

    assertDeploymentSuccess(applicationDeploymentListener, artifactFileBuilder.getId());

    executeApplicationFlow("main");
  }

  @Test
  public void deploysApplicationWithPrivilegedPluginDependingOnPlugin() throws Exception {
    ArtifactPluginFileBuilder echoPlugin = new ArtifactPluginFileBuilder("echoPlugin")
        .configuredWith(PRIVILEGED_EXPORTED_CLASS_PACKAGES_PROPERTY, "org.foo")
        .configuredWith(PRIVILEGED_ARTIFACTS_PROPERTY, "org.mule.test:dependantPlugin")
        .dependingOn(new JarFileBuilder("echoTestJar", echoTestJarFile));

    ArtifactPluginFileBuilder dependantPlugin =
        new ArtifactPluginFileBuilder("dependantPlugin").configuredWith(EXPORTED_CLASS_PACKAGES_PROPERTY, "org.foo.echo")
            .containingClass(pluginEcho3TestClassFile, "org/foo/echo/Plugin3Echo.class")
            .dependingOn(echoPlugin);

    final TestArtifactDescriptor artifactFileBuilder = new ApplicationFileBuilder("plugin-depending-on-plugin-app")
        .definedBy("plugin-depending-on-plugin-app-config.xml").dependingOn(dependantPlugin);
    addPackedAppFromBuilder(artifactFileBuilder);

    startDeployment();

    assertDeploymentSuccess(applicationDeploymentListener, artifactFileBuilder.getId());

    executeApplicationFlow("main");
  }

  @Test
  public void failsToDeployApplicationWithMissingPluginDependencyOnPlugin() throws Exception {

    ArtifactPluginFileBuilder dependantPlugin =
        new ArtifactPluginFileBuilder("dependantPlugin").configuredWith(EXPORTED_CLASS_PACKAGES_PROPERTY, "org.foo.echo")
            .containingClass(pluginEcho3TestClassFile, "org/foo/echo/Plugin3Echo.class");

    final TestArtifactDescriptor artifactFileBuilder = new ApplicationFileBuilder("plugin-depending-on-plugin-app")
        .definedBy("plugin-depending-on-plugin-app-config.xml").dependingOn(dependantPlugin);
    addPackedAppFromBuilder(artifactFileBuilder);

    startDeployment();

    assertDeploymentSuccess(applicationDeploymentListener, artifactFileBuilder.getId());

    try {
      executeApplicationFlow("main");
      fail("Expected to fail as there should be a missing class");
    } catch (Exception e) {
      assertThat(e.getCause().getCause(), instanceOf(MuleFatalException.class));
      assertThat(e.getCause().getCause().getCause(), instanceOf(NoClassDefFoundError.class));
      assertThat(e.getCause().getCause().getCause().getMessage(), containsString("org/foo/EchoTest"));
    }
  }

  @Test
  public void failsToDeployApplicationWithPluginDependantOnPluginNotShipped() throws Exception {
    ArtifactPluginFileBuilder dependantPlugin =
        new ArtifactPluginFileBuilder("dependantPlugin")
            .dependingOn(echoPlugin);

    final TestArtifactDescriptor artifactFileBuilder = new ApplicationFileBuilder("plugin-depending-on-plugin-app")
        .definedBy("plugin-depending-on-plugin-app-config.xml").dependingOn(dependantPlugin);
    addPackedAppFromBuilder(artifactFileBuilder);

    startDeployment();

    assertDeploymentFailure(applicationDeploymentListener, artifactFileBuilder.getId(), times(1));
  }

  @Test
  public void deploysAppWithLibDifferentThanPlugin() throws Exception {
    final ApplicationFileBuilder differentLibPluginAppFileBuilder =
        new ApplicationFileBuilder("appWithLibDifferentThanPlugin").definedBy("app-plugin-different-lib-config.xml")
            .dependingOn(echoPluginWithLib1).dependingOn(new JarFileBuilder("barUtils2_0", barUtils2_0JarFile))
            .containingClass(pluginEcho2TestClassFile, "org/foo/echo/Plugin2Echo.class");

    addPackedAppFromBuilder(differentLibPluginAppFileBuilder);

    startDeployment();

    assertDeploymentSuccess(applicationDeploymentListener, differentLibPluginAppFileBuilder.getId());

    executeApplicationFlow("main");
  }

  @Test
  public void deploysAppUsingPluginResource() throws Exception {
    final ArtifactPluginFileBuilder pluginWithResource =
        new ArtifactPluginFileBuilder("resourcePlugin").configuredWith(EXPORTED_RESOURCE_PROPERTY, "/pluginResource.properties")
            .containingResource("pluginResourceSource.properties", "pluginResource.properties");

    final ApplicationFileBuilder resourcePluginAppFileBuilder = new ApplicationFileBuilder("dummyWithPluginResource")
        .definedBy("plugin-resource-app-config.xml").dependingOn(pluginWithResource);

    addPackedAppFromBuilder(resourcePluginAppFileBuilder);

    startDeployment();

    assertDeploymentSuccess(applicationDeploymentListener, resourcePluginAppFileBuilder.getId());
  }

  @Test
  public void deploysAppProvidingResourceForPlugin() throws Exception {
    File resourceConsumerClassFile =
        new CompilerUtils.SingleClassCompiler().compile(getResourceFile("/org/foo/resource/ResourceConsumer.java"));

    final ArtifactPluginFileBuilder pluginUsingAppResource =
        new ArtifactPluginFileBuilder("appResourcePlugin").configuredWith(EXPORTED_CLASS_PACKAGES_PROPERTY, "org.foo.resource")
            .containingClass(resourceConsumerClassFile, "org/foo/resource/ResourceConsumer.class");

    final TestArtifactDescriptor artifactFileBuilder =
        new ApplicationFileBuilder("appProvidingResourceForPlugin")
            .definedBy("app-providing-resource-for-plugin.xml")
            .dependingOn(pluginUsingAppResource)
            .configuredWith(EXPORTED_RESOURCES, "META-INF/app-resource.txt")
            .usingResource(getResourceFile("/test-resource.txt").toString(), "META-INF/app-resource.txt");
    addPackedAppFromBuilder(artifactFileBuilder);

    startDeployment();

    assertDeploymentSuccess(applicationDeploymentListener, artifactFileBuilder.getId());

    executeApplicationFlow("main");
  }


  @Test
  public void deploysMultipleAppsZipOnStartup() throws Exception {
    final int totalApps = 20;

    for (int i = 1; i <= totalApps; i++) {
      addExplodedAppFromBuilder(new ApplicationFileBuilder(Integer.toString(i), emptyAppFileBuilder));
    }

    startDeployment();

    for (int i = 1; i <= totalApps; i++) {
      assertDeploymentSuccess(applicationDeploymentListener,
                              new ApplicationFileBuilder(Integer.toString(i), emptyAppFileBuilder).getId());
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
  public void explodedAppRedeploymentDeletesTempFile() throws Exception {
    testTempFileOnRedeployment(() -> addExplodedAppFromBuilder(emptyAppFileBuilder),
                               () -> addExplodedAppFromBuilder(emptyAppFileBuilder));
  }

  @Test
  public void packedAppRedeploymentDeletesTempFile() throws Exception {
    testTempFileOnRedeployment(() -> addPackedAppFromBuilder(emptyAppFileBuilder),
                               () -> addPackedAppFromBuilder(emptyAppFileBuilder));
  }

  @Test
  public void packedAppRedeploymentWithExplodedDeletesTempFile() throws Exception {
    testTempFileOnRedeployment(() -> addPackedAppFromBuilder(emptyAppFileBuilder),
                               () -> addExplodedAppFromBuilder(emptyAppFileBuilder));
  }

  @Test
  public void explodedAppRedeploymentWithPackedDeletesTempFile() throws Exception {
    testTempFileOnRedeployment(() -> addExplodedAppFromBuilder(emptyAppFileBuilder),
                               () -> addPackedAppFromBuilder(emptyAppFileBuilder));
  }

  private void testTempFileOnRedeployment(CheckedRunnable deployApp, CheckedRunnable redeployApp) throws Exception {
    final String TEST_FILE_NAME = "testFile";
    startDeployment();
    deployApp.run();
    assertDeploymentSuccess(applicationDeploymentListener, emptyAppFileBuilder.getId());

    Application app = findApp(emptyAppFileBuilder.getId(), 1);

    final File preRedeploymentMetaFolder = getAppMetaFolder(app);

    //Add test files to check if any of them was deleted
    final File preRedeploymentMetaTestFile = new File(preRedeploymentMetaFolder, TEST_FILE_NAME);
    preRedeploymentMetaTestFile.createNewFile();

    assertThat(preRedeploymentMetaTestFile, exists);

    reset(applicationDeploymentListener);

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

  private File getAppTempFolder(Application app) {
    return new File(getAppMetaFolder(app), "/temp");
  }

  private ArtifactPluginFileBuilder createPrivilegedExtensionPlugin() {
    MulePluginModel.MulePluginModelBuilder mulePluginModelBuilder = new MulePluginModel.MulePluginModelBuilder()
        .setMinMuleVersion(MIN_MULE_VERSION).setName(PRIVILEGED_EXTENSION_ARTIFACT_ID).setRequiredProduct(MULE)
        .withBundleDescriptorLoader(createBundleDescriptorLoader(PRIVILEGED_EXTENSION_ARTIFACT_ID, MULE_EXTENSION_CLASSIFIER,
                                                                 PROPERTIES_BUNDLE_DESCRIPTOR_LOADER_ID, "1.0.0"));
    mulePluginModelBuilder.withClassLoaderModelDescriptorLoader(new MuleArtifactLoaderDescriptorBuilder()
        .setId(MULE_LOADER_ID)
        .build());
    mulePluginModelBuilder.withExtensionModelDescriber().setId(JAVA_LOADER_ID)
        .addProperty("type", "org.foo.hello.PrivilegedExtension")
        .addProperty("version", "1.0");
    return new ArtifactPluginFileBuilder(PRIVILEGED_EXTENSION_ARTIFACT_ID)
        .dependingOn(new JarFileBuilder("privilegedExtensionV1", privilegedExtensionV1JarFile))
        .describedBy(mulePluginModelBuilder.build());
  }

  @Override
  protected Set<String> getPrivilegedArtifactIds() {
    Set<String> privilegedArtifactIds = new HashSet<>();
    privilegedArtifactIds.add(PRIVILEGED_EXTENSION_ARTIFACT_FULL_ID);
    return privilegedArtifactIds;
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
}
