/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.module.deployment.internal;

import static java.io.File.separator;
import static java.lang.System.currentTimeMillis;
import static java.lang.System.getProperty;
import static java.lang.System.setProperty;
import static java.util.Arrays.asList;
import static java.util.Collections.emptyList;
import static org.apache.commons.collections.CollectionUtils.isEqualCollection;
import static org.apache.commons.io.FileUtils.copyFile;
import static org.apache.commons.io.FileUtils.copyFileToDirectory;
import static org.apache.commons.io.filefilter.DirectoryFileFilter.DIRECTORY;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.instanceOf;
import static org.hamcrest.Matchers.not;
import static org.hamcrest.Matchers.sameInstance;
import static org.hamcrest.core.Is.is;
import static org.hamcrest.core.IsNull.notNullValue;
import static org.hamcrest.core.IsNull.nullValue;
import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNotSame;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;
import static org.junit.Assume.assumeThat;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mule.functional.services.TestServicesUtils.buildSchedulerServiceFile;
import static org.mule.runtime.container.api.MuleFoldersUtil.CONTAINER_APP_PLUGINS;
import static org.mule.runtime.container.api.MuleFoldersUtil.getContainerAppPluginsFolder;
import static org.mule.runtime.container.api.MuleFoldersUtil.getDomainFolder;
import static org.mule.runtime.container.api.MuleFoldersUtil.getServicesFolder;
import static org.mule.runtime.core.MessageExchangePattern.REQUEST_RESPONSE;
import static org.mule.runtime.core.api.config.MuleProperties.MULE_HOME_DIRECTORY_PROPERTY;
import static org.mule.runtime.core.config.bootstrap.ClassLoaderRegistryBootstrapDiscoverer.BOOTSTRAP_PROPERTIES;
import static org.mule.runtime.core.util.ClassUtils.withContextClassLoader;
import static org.mule.runtime.deployment.model.api.application.ApplicationStatus.DESTROYED;
import static org.mule.runtime.deployment.model.api.application.ApplicationStatus.STOPPED;
import static org.mule.runtime.deployment.model.api.domain.Domain.DEFAULT_DOMAIN_NAME;
import static org.mule.runtime.deployment.model.api.domain.Domain.DOMAIN_CONFIG_FILE_LOCATION;
import static org.mule.runtime.module.artifact.classloader.DefaultArtifactClassLoaderFilter.EXPORTED_CLASS_PACKAGES_PROPERTY;
import static org.mule.runtime.module.artifact.classloader.DefaultArtifactClassLoaderFilter.EXPORTED_RESOURCE_PROPERTY;
import static org.mule.runtime.module.deployment.internal.DeploymentDirectoryWatcher.CHANGE_CHECK_INTERVAL_PROPERTY;
import static org.mule.runtime.module.deployment.internal.MuleDeploymentService.PARALLEL_DEPLOYMENT_PROPERTY;
import static org.mule.runtime.module.deployment.impl.internal.application.PropertiesDescriptorParser.PROPERTY_DOMAIN;
import static org.mule.runtime.module.deployment.internal.TestApplicationFactory.createTestApplicationFactory;
import static org.mule.runtime.module.service.ServiceDescriptorFactory.SERVICE_PROVIDER_CLASS_NAME;
import static org.mule.tck.junit4.AbstractMuleContextTestCase.TEST_MESSAGE;

import org.mule.runtime.api.exception.MuleException;
import org.mule.runtime.core.DefaultEventContext;
import org.mule.runtime.core.api.Event;
import org.mule.runtime.core.api.MuleContext;
import org.mule.runtime.api.lifecycle.Initialisable;
import org.mule.runtime.api.lifecycle.InitialisationException;
import org.mule.runtime.core.api.message.InternalMessage;
import org.mule.runtime.core.api.registry.MuleRegistry;
import org.mule.runtime.core.config.StartupContext;
import org.mule.runtime.core.construct.Flow;
import org.mule.runtime.core.exception.MessagingException;
import org.mule.runtime.core.util.FileUtils;
import org.mule.runtime.core.util.IOUtils;
import org.mule.runtime.core.util.StringUtils;
import org.mule.runtime.core.util.concurrent.Latch;
import org.mule.runtime.deployment.model.api.application.Application;
import org.mule.runtime.deployment.model.api.application.ApplicationStatus;
import org.mule.runtime.deployment.model.api.domain.Domain;
import org.mule.runtime.deployment.model.api.domain.DomainDescriptor;
import org.mule.runtime.deployment.model.internal.application.MuleApplicationClassLoaderFactory;
import org.mule.runtime.deployment.model.internal.domain.DomainClassLoaderFactory;
import org.mule.runtime.deployment.model.internal.nativelib.DefaultNativeLibraryFinderFactory;
import org.mule.runtime.module.artifact.builder.TestArtifactDescriptor;
import org.mule.runtime.module.artifact.classloader.ArtifactClassLoader;
import org.mule.runtime.module.deployment.api.DeploymentListener;
import org.mule.runtime.module.deployment.impl.internal.builder.ApplicationFileBuilder;
import org.mule.runtime.module.deployment.impl.internal.builder.ArtifactPluginFileBuilder;
import org.mule.runtime.module.deployment.impl.internal.builder.DomainFileBuilder;
import org.mule.runtime.module.deployment.impl.internal.domain.DefaultDomainManager;
import org.mule.runtime.module.deployment.impl.internal.domain.DefaultMuleDomain;
import org.mule.runtime.module.service.ServiceManager;
import org.mule.runtime.module.service.builder.ServiceFileBuilder;
import org.mule.tck.junit4.AbstractMuleTestCase;
import org.mule.tck.junit4.rule.DynamicPort;
import org.mule.tck.junit4.rule.SystemProperty;
import org.mule.tck.probe.JUnitProbe;
import org.mule.tck.probe.PollingProber;
import org.mule.tck.probe.Probe;
import org.mule.tck.probe.Prober;
import org.mule.tck.probe.file.FileDoesNotExists;
import org.mule.tck.probe.file.FileExists;
import org.mule.tck.util.CompilerUtils;
import org.mule.tck.util.CompilerUtils.SingleClassCompiler;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.net.URISyntaxException;
import java.net.URL;
import java.net.URLDecoder;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.ReentrantLock;

import org.junit.After;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.mockito.verification.VerificationMode;

@RunWith(Parameterized.class)
public class DeploymentServiceTestCase extends AbstractMuleTestCase {

  private static final int FILE_TIMESTAMP_PRECISION_MILLIS = 1000;
  protected static final int DEPLOYMENT_TIMEOUT = 10000;
  protected static final String[] NONE = new String[0];
  protected static final int ONE_HOUR_IN_MILLISECONDS = 3600000;

  // Resources
  private static final String MULE_CONFIG_XML_FILE = "mule-config.xml";
  private static final String MULE_DOMAIN_CONFIG_XML_FILE = "mule-domain-config.xml";
  private static final String EMPTY_APP_CONFIG_XML = "/empty-config.xml";
  private static final String BAD_APP_CONFIG_XML = "/bad-app-config.xml";
  private static final String BROKEN_CONFIG_XML = "/broken-config.xml";
  private static final String EMPTY_DOMAIN_CONFIG_XML = "/empty-domain-config.xml";
  private DefaultClassLoaderManager artifactClassLoaderManager;

  @Parameterized.Parameters(name = "Parallel: {0}")
  public static List<Object[]> parameters() {
    return Arrays.asList(new Object[][] {
        {false},
        {true},
    });
  }

  // Dynamically compiled classes and jars
  private static final File barUtils1_0JarFile =
      new CompilerUtils.JarCompiler().compiling(getResourceFile("/org/bar1/BarUtils.java")).compile("bar-1.0.jar");

  private static final File barUtils2_0JarFile =
      new CompilerUtils.JarCompiler().compiling(getResourceFile("/org/bar2/BarUtils.java")).compile("bar-2.0.jar");

  private static final File echoTestJarFile =
      new CompilerUtils.JarCompiler().compiling(getResourceFile("/org/foo/EchoTest.java")).compile("echo.jar");

  private static final File defaulServiceEchoJarFile = new CompilerUtils.JarCompiler()
      .compiling(getResourceFile("/org/mule/echo/DefaultEchoService.java"),
                 getResourceFile("/org/mule/echo/EchoServiceProvider.java"))
      .compile("mule-module-service-echo-default-4.0-SNAPSHOT.jar");

  private static final File defaultFooServiceJarFile =
      new CompilerUtils.JarCompiler().compiling(getResourceFile("/org/mule/service/foo/DefaultFooService.java"),
                                                getResourceFile("/org/mule/service/foo/FooServiceProvider.java"))
          .dependingOn(defaulServiceEchoJarFile.getAbsoluteFile())
          .compile("mule-module-service-foo-default-4.0-SNAPSHOT.jar");

  private static final File helloExtensionJarFile = new CompilerUtils.ExtensionCompiler()
      .compiling(
                 getResourceFile("/org/foo/hello/HelloExtension.java"),
                 getResourceFile("/org/foo/hello/HelloOperation.java"))
      .compile("mule-module-hello-4.0-SNAPSHOT.jar", "1.0");

  private static final File echoTestClassFile = new SingleClassCompiler()
      .compile(getResourceFile("/org/foo/EchoTest.java"));

  private static final File pluginEcho1TestClassFile = new SingleClassCompiler()
      .dependingOn(barUtils1_0JarFile).compile(getResourceFile("/org/foo/Plugin1Echo.java"));

  private static final File pluginEcho2TestClassFile = new SingleClassCompiler()
      .dependingOn(barUtils2_0JarFile).compile(getResourceFile("/org/foo/echo/Plugin2Echo.java"));

  private static final File pluginEcho3TestClassFile = new SingleClassCompiler()
      .compile(getResourceFile("/org/foo/echo/Plugin3Echo.java"));

  private static final File resourceConsumerClassFile = new SingleClassCompiler()
      .compile(getResourceFile("/org/foo/resource/ResourceConsumer.java"));

  private static File getResourceFile(String resource) {
    return new File(DeploymentServiceTestCase.class.getResource(resource).getFile());
  }

  // Application plugin file builders
  private final ArtifactPluginFileBuilder echoPlugin = new ArtifactPluginFileBuilder("echoPlugin")
      .configuredWith(EXPORTED_CLASS_PACKAGES_PROPERTY, "org.foo").usingLibrary(echoTestJarFile.getAbsolutePath());
  private final ArtifactPluginFileBuilder echoPluginWithLib1 =
      new ArtifactPluginFileBuilder("echoPlugin1").configuredWith(EXPORTED_CLASS_PACKAGES_PROPERTY, "org.foo")
          .usingLibrary(barUtils1_0JarFile.getAbsolutePath())
          .containingClass(pluginEcho1TestClassFile, "org/foo/Plugin1Echo.class");
  private final ArtifactPluginFileBuilder echoPluginWithoutLib1 = new ArtifactPluginFileBuilder("echoPlugin1")
      .configuredWith(EXPORTED_CLASS_PACKAGES_PROPERTY, "org.foo")
      .containingClass(pluginEcho1TestClassFile, "org/foo/Plugin1Echo.class");
  private final ArtifactPluginFileBuilder echoPluginWithLib2 =
      new ArtifactPluginFileBuilder("echoPlugin2").configuredWith(EXPORTED_CLASS_PACKAGES_PROPERTY, "org.foo.echo")
          .usingLibrary(barUtils2_0JarFile.getAbsolutePath())
          .containingClass(pluginEcho2TestClassFile, "org/foo/echo/Plugin2Echo.class");
  private final ArtifactPluginFileBuilder pluginWithResource =
      new ArtifactPluginFileBuilder("resourcePlugin").configuredWith(EXPORTED_RESOURCE_PROPERTY, "/pluginResource.properties")
          .containingResource("pluginResourceSource.properties", "pluginResource.properties");

  private final ArtifactPluginFileBuilder pluginUsingAppResource =
      new ArtifactPluginFileBuilder("appResourcePlugin").configuredWith(EXPORTED_CLASS_PACKAGES_PROPERTY, "org.foo.resource")
          .containingClass(resourceConsumerClassFile, "org/foo/resource/ResourceConsumer.class");

  // Application file builders
  private final ApplicationFileBuilder emptyAppFileBuilder =
      new ApplicationFileBuilder("empty-app").definedBy("empty-config.xml");
  private final ApplicationFileBuilder springPropertyAppFileBuilder =
      new ApplicationFileBuilder("property-app").definedBy("app-properties-config.xml");
  private final ApplicationFileBuilder dummyAppDescriptorFileBuilder = new ApplicationFileBuilder("dummy-app")
      .definedBy("dummy-app-config.xml").configuredWith("myCustomProp", "someValue")
      .containingClass(echoTestClassFile, "org/foo/EchoTest.class");
  private final ApplicationFileBuilder waitAppFileBuilder =
      new ApplicationFileBuilder("wait-app").definedBy("wait-app-config.xml");
  private final ApplicationFileBuilder brokenAppFileBuilder = new ApplicationFileBuilder("broken-app").corrupted();
  private final ApplicationFileBuilder incompleteAppFileBuilder =
      new ApplicationFileBuilder("incomplete-app").definedBy("incomplete-app-config.xml");
  private final ApplicationFileBuilder echoPluginAppFileBuilder =
      new ApplicationFileBuilder("dummyWithEchoPlugin").definedBy("app-with-echo-plugin-config.xml").containingPlugin(echoPlugin);
  private final ApplicationFileBuilder differentLibPluginAppFileBuilder =
      new ApplicationFileBuilder("appWithLibDifferentThanPlugin").definedBy("app-plugin-different-lib-config.xml")
          .containingPlugin(echoPluginWithLib1).usingLibrary(barUtils2_0JarFile.getAbsolutePath())
          .containingClass(pluginEcho2TestClassFile, "org/foo/echo/Plugin2Echo.class");
  private final ApplicationFileBuilder multiLibPluginAppFileBuilder = new ApplicationFileBuilder("multiPluginLibVersion")
      .definedBy("multi-plugin-app-config.xml").containingPlugin(echoPluginWithLib1).containingPlugin(echoPluginWithLib2);
  private final ApplicationFileBuilder resourcePluginAppFileBuilder = new ApplicationFileBuilder("dummyWithPluginResource")
      .definedBy("plugin-resource-app-config.xml").containingPlugin(pluginWithResource);
  private final ApplicationFileBuilder sharedLibPluginAppFileBuilder = new ApplicationFileBuilder("shared-plugin-lib-app")
      .definedBy("app-with-echo1-plugin-config.xml").containingPlugin(echoPluginWithoutLib1)
      .sharingLibrary(barUtils1_0JarFile.getAbsolutePath());
  private final ApplicationFileBuilder brokenAppWithFunkyNameAppFileBuilder =
      new ApplicationFileBuilder("broken-app+", brokenAppFileBuilder);
  private final ApplicationFileBuilder dummyDomainApp1FileBuilder =
      new ApplicationFileBuilder("dummy-domain-app1").definedBy("empty-config.xml").deployedWith(PROPERTY_DOMAIN, "dummy-domain");
  private final ApplicationFileBuilder dummyDomainApp2FileBuilder =
      new ApplicationFileBuilder("dummy-domain-app2").definedBy("empty-config.xml").deployedWith(PROPERTY_DOMAIN, "dummy-domain");
  private final ApplicationFileBuilder dummyDomainApp3FileBuilder = new ApplicationFileBuilder("dummy-domain-app3")
      .definedBy("bad-app-config.xml").deployedWith(PROPERTY_DOMAIN, "dummy-domain");
  private final ApplicationFileBuilder sharedAAppFileBuilder = new ApplicationFileBuilder("shared-app-a")
      .definedBy("shared-a-app-config.xml").deployedWith(PROPERTY_DOMAIN, "shared-domain");
  private final ApplicationFileBuilder sharedBAppFileBuilder = new ApplicationFileBuilder("shared-app-b")
      .definedBy("shared-b-app-config.xml").deployedWith(PROPERTY_DOMAIN, "shared-domain");

  // Domain file builders
  private final DomainFileBuilder brokenDomainFileBuilder = new DomainFileBuilder("brokenDomain").corrupted();
  private final DomainFileBuilder emptyDomainFileBuilder =
      new DomainFileBuilder("empty-domain").definedBy("empty-domain-config.xml");
  private final DomainFileBuilder waitDomainFileBuilder =
      new DomainFileBuilder("wait-domain").definedBy("wait-domain-config.xml");
  private final DomainFileBuilder incompleteDomainFileBuilder =
      new DomainFileBuilder("incompleteDomain").definedBy("incomplete-domain-config.xml");
  private final DomainFileBuilder invalidDomainBundleFileBuilder =
      new DomainFileBuilder("invalid-domain-bundle").definedBy("incomplete-domain-config.xml").containing(emptyAppFileBuilder);
  private final DomainFileBuilder dummyDomainBundleFileBuilder = new DomainFileBuilder("dummy-domain-bundle")
      .containing(new ApplicationFileBuilder(dummyAppDescriptorFileBuilder).deployedWith(PROPERTY_DOMAIN, "dummy-domain-bundle"));
  private final DomainFileBuilder dummyDomainFileBuilder =
      new DomainFileBuilder("dummy-domain").definedBy("empty-domain-config.xml");
  private final DomainFileBuilder dummyUndeployableDomainFileBuilder = new DomainFileBuilder("dummy-undeployable-domain")
      .definedBy("empty-domain-config.xml").deployedWith("redeployment.enabled", "false");
  private final DomainFileBuilder sharedDomainFileBuilder =
      new DomainFileBuilder("shared-domain").definedBy("shared-domain-config.xml");
  private final DomainFileBuilder sharedBundleDomainFileBuilder = new DomainFileBuilder("shared-domain")
      .definedBy("shared-domain-config.xml").containing(sharedAAppFileBuilder).containing(sharedBAppFileBuilder);

  private final boolean parallelDeployment;
  protected File muleHome;
  protected File appsDir;
  protected File domainsDir;
  protected File containerAppPluginsDir;
  protected File tmpAppsDir;
  protected ServiceManager serviceManager;
  protected MuleDeploymentService deploymentService;
  protected DeploymentListener applicationDeploymentListener;
  protected DeploymentListener domainDeploymentListener;
  private ArtifactClassLoader containerClassLoader;

  @Rule
  public SystemProperty changeChangeInterval = new SystemProperty(CHANGE_CHECK_INTERVAL_PROPERTY, "10");

  @Rule
  public DynamicPort httpPort = new DynamicPort("httpPort");

  @Rule
  public TemporaryFolder compilerWorkFolder = new TemporaryFolder();

  private File services;

  public DeploymentServiceTestCase(boolean parallelDeployment) {
    this.parallelDeployment = parallelDeployment;
  }

  @Before
  public void setUp() throws Exception {

    if (parallelDeployment) {
      setProperty(PARALLEL_DEPLOYMENT_PROPERTY, "");
    }

    final String tmpDir = getProperty("java.io.tmpdir");
    muleHome = new File(new File(tmpDir, "mule home"), getClass().getSimpleName() + currentTimeMillis());
    appsDir = new File(muleHome, "apps");
    appsDir.mkdirs();
    containerAppPluginsDir = new File(muleHome, CONTAINER_APP_PLUGINS);
    containerAppPluginsDir.mkdirs();
    tmpAppsDir = new File(muleHome, "tmp");
    tmpAppsDir.mkdirs();
    domainsDir = new File(muleHome, "domains");
    domainsDir.mkdirs();
    setProperty(MULE_HOME_DIRECTORY_PROPERTY, muleHome.getCanonicalPath());

    final File domainFolder = getDomainFolder(DEFAULT_DOMAIN_NAME);
    assertThat(domainFolder.mkdirs(), is(true));

    services = getServicesFolder();
    services.mkdirs();
    copyFileToDirectory(buildSchedulerServiceFile(compilerWorkFolder.newFolder("schedulerService")), services);

    applicationDeploymentListener = mock(DeploymentListener.class);
    domainDeploymentListener = mock(DeploymentListener.class);
    MuleArtifactResourcesRegistry muleArtifactResourcesRegistry = new MuleArtifactResourcesRegistry();
    serviceManager = muleArtifactResourcesRegistry.getServiceManager();
    containerClassLoader = muleArtifactResourcesRegistry.getContainerClassLoader();
    artifactClassLoaderManager = muleArtifactResourcesRegistry.getArtifactClassLoaderManager();

    deploymentService = new MuleDeploymentService(muleArtifactResourcesRegistry.getDomainFactory(),
                                                  muleArtifactResourcesRegistry.getApplicationFactory());
    deploymentService.addDeploymentListener(applicationDeploymentListener);
    deploymentService.addDomainDeploymentListener(domainDeploymentListener);
  }

  @After
  public void tearDown() throws Exception {
    if (deploymentService != null) {
      deploymentService.stop();
    }

    if (serviceManager != null) {
      serviceManager.stop();
    }

    FileUtils.deleteTree(muleHome);

    // this is a complex classloader setup and we can't reproduce standalone Mule 100%,
    // so trick the next test method into thinking it's the first run, otherwise
    // app resets CCL ref to null and breaks the next test
    Thread.currentThread().setContextClassLoader(ClassLoader.getSystemClassLoader());

    if (parallelDeployment) {
      System.clearProperty(PARALLEL_DEPLOYMENT_PROPERTY);
    }
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
    final MuleRegistry registry = getMuleRegistry(app);

    // mule-app.properties from the zip archive must have loaded properly
    assertEquals("mule-app.properties should have been loaded.", "someValue", registry.get("myCustomProp"));

    // Checks that the configuration's ID was properly configured
    assertThat(app.getMuleContext().getConfiguration().getId(), equalTo(dummyAppDescriptorFileBuilder.getId()));
  }

  @Test
  public void extensionManagerPresent() throws Exception {
    addPackedAppFromBuilder(emptyAppFileBuilder);

    startDeployment();

    assertApplicationDeploymentSuccess(applicationDeploymentListener, emptyAppFileBuilder.getId());

    final Application app = findApp(emptyAppFileBuilder.getId(), 1);
    assertThat(app.getMuleContext().getExtensionManager(), is(notNullValue()));
  }

  @Test
  public void appHomePropertyIsPresent() throws Exception {
    addExplodedAppFromBuilder(springPropertyAppFileBuilder);

    startDeployment();
    assertApplicationDeploymentSuccess(applicationDeploymentListener, springPropertyAppFileBuilder.getId());

    final Application app = findApp(springPropertyAppFileBuilder.getId(), 1);
    final MuleRegistry registry = getMuleRegistry(app);

    Map<String, Object> appProperties = registry.get("appProperties");
    assertThat(appProperties, is(notNullValue()));

    String appHome = (String) appProperties.get("appHome");
    assertThat(new File(appHome).exists(), is(true));
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
    startDeployment();

    addPackedAppFromBuilder(dummyAppDescriptorFileBuilder);

    assertApplicationDeploymentSuccess(applicationDeploymentListener, dummyAppDescriptorFileBuilder.getId());
    assertAppsDir(NONE, new String[] {dummyAppDescriptorFileBuilder.getId()}, true);
    assertApplicationAnchorFileExists(dummyAppDescriptorFileBuilder.getId());

    // just assert no privileged entries were put in the registry
    final Application app = findApp(dummyAppDescriptorFileBuilder.getId(), 1);
    final MuleRegistry registry = getMuleRegistry(app);

    // mule-app.properties from the zip archive must have loaded properly
    assertEquals("mule-app.properties should have been loaded.", "someValue", registry.get("myCustomProp"));
  }

  @Test
  public void deploysBrokenAppZipOnStartup() throws Exception {
    addPackedAppFromBuilder(brokenAppFileBuilder);

    startDeployment();

    assertDeploymentFailure(applicationDeploymentListener, brokenAppFileBuilder.getId());

    assertAppsDir(new String[] {brokenAppFileBuilder.getDeployedPath()}, NONE, true);

    assertApplicationAnchorFileDoesNotExists(brokenAppFileBuilder.getId());

    final Map<URL, Long> zombieMap = deploymentService.getZombieApplications();
    assertEquals("Wrong number of zombie apps registered.", 1, zombieMap.size());
    final Map.Entry<URL, Long> zombie = zombieMap.entrySet().iterator().next();
    assertEquals("Wrong URL tagged as zombie.", brokenAppFileBuilder.getDeployedPath(),
                 new File(zombie.getKey().getFile()).getName());
    assertTrue("Invalid lastModified value for file URL.", zombie.getValue() != -1);
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

    final Map<URL, Long> zombieMap = deploymentService.getZombieApplications();
    assertEquals("Wrong number of zombie apps registered.", 1, zombieMap.size());
    final Map.Entry<URL, Long> zombie = zombieMap.entrySet().iterator().next();
    assertEquals("Wrong URL tagged as zombie.", brokenAppWithFunkyNameAppFileBuilder.getDeployedPath(),
                 new File(zombie.getKey().getFile()).getName());
    assertTrue("Invalid lastModified value for file URL.", zombie.getValue() != -1);

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

    assertDeploymentFailure(applicationDeploymentListener, "broken-app");

    assertAppsDir(new String[] {"broken-app.zip"}, NONE, true);

    assertApplicationAnchorFileDoesNotExists(brokenAppFileBuilder.getId());

    final Map<URL, Long> zombieMap = deploymentService.getZombieApplications();
    assertEquals("Wrong number of zombie apps registered.", 1, zombieMap.size());
    final Map.Entry<URL, Long> zombie = zombieMap.entrySet().iterator().next();
    assertEquals("Wrong URL tagged as zombie.", "broken-app.zip", new File(zombie.getKey().getFile()).getName());
    assertTrue("Invalid lastModified value for file URL.", zombie.getValue() != -1);
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

    assertUndeploymentSuccess(applicationDeploymentListener, emptyAppFileBuilder.getId());
    assertApplicationDeploymentSuccess(applicationDeploymentListener, emptyAppFileBuilder.getId());

    assertEquals("Application has not been properly registered with Mule", 1, deploymentService.getApplications().size());
    assertAppsDir(NONE, new String[] {emptyAppFileBuilder.getId()}, true);
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

    assertUndeploymentSuccess(applicationDeploymentListener, emptyAppFileBuilder.getId());
    assertApplicationDeploymentSuccess(applicationDeploymentListener, emptyAppFileBuilder.getId());
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
    final Map<URL, Long> zombieMap = deploymentService.getZombieApplications();
    assertEquals("Wrong number of zombie apps registered.", 1, zombieMap.size());
    final Map.Entry<URL, Long> zombie = zombieMap.entrySet().iterator().next();
    // Spaces are converted to %20 is returned by java file api :/
    String appName = URLDecoder.decode(new File(zombie.getKey().getFile()).getName(), "UTF-8");
    assertEquals("Wrong URL tagged as zombie.", "app with spaces", appName);
    assertTrue("Invalid lastModified value for file URL.", zombie.getValue() != -1);
  }

  @Test
  public void deploysInvalidExplodedAppAfterStartup() throws Exception {
    startDeployment();

    addExplodedAppFromBuilder(emptyAppFileBuilder, "app with spaces");

    assertDeploymentFailure(applicationDeploymentListener, "app with spaces");

    // Maintains app dir created
    assertAppsDir(NONE, new String[] {"app with spaces"}, true);
    final Map<URL, Long> zombieMap = deploymentService.getZombieApplications();
    assertEquals("Wrong number of zombie apps registered.", 1, zombieMap.size());
    final Map.Entry<URL, Long> zombie = zombieMap.entrySet().iterator().next();
    // Spaces are converted to %20 is returned by java file api :/
    String appName = URLDecoder.decode(new File(zombie.getKey().getFile()).getName(), "UTF-8");
    assertEquals("Wrong URL tagged as zombie.", "app with spaces", appName);
    assertTrue("Invalid lastModified value for file URL.", zombie.getValue() != -1);
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
    final Map<URL, Long> zombieMap = deploymentService.getZombieApplications();
    assertEquals("Wrong number of zombie apps registered.", 1, zombieMap.size());
    final Map.Entry<URL, Long> zombie = zombieMap.entrySet().iterator().next();
    assertEquals("Wrong URL tagged as zombie.", incompleteAppFileBuilder.getId(),
                 new File(zombie.getKey().getFile()).getParentFile().getName());
    assertTrue("Invalid lastModified value for file URL.", zombie.getValue() != -1);
  }

  @Test
  public void deploysBrokenExplodedAppAfterStartup() throws Exception {
    startDeployment();

    addExplodedAppFromBuilder(incompleteAppFileBuilder);

    assertDeploymentFailure(applicationDeploymentListener, incompleteAppFileBuilder.getId());

    assertApplicationAnchorFileDoesNotExists(incompleteAppFileBuilder.getId());

    // Maintains app dir created
    assertAppsDir(NONE, new String[] {incompleteAppFileBuilder.getId()}, true);
    final Map<URL, Long> zombieMap = deploymentService.getZombieApplications();
    assertEquals("Wrong number of zombie apps registered.", 1, zombieMap.size());
    final Map.Entry<URL, Long> zombie = zombieMap.entrySet().iterator().next();
    assertEquals("Wrong URL tagged as zombie.", incompleteAppFileBuilder.getId(),
                 new File(zombie.getKey().getFile()).getParentFile().getName());
    assertTrue("Invalid lastModified value for file URL.", zombie.getValue() != -1);
  }

  @Test
  public void redeploysExplodedAppOnStartup() throws Exception {
    addExplodedAppFromBuilder(dummyAppDescriptorFileBuilder);

    startDeployment();

    assertApplicationDeploymentSuccess(applicationDeploymentListener, dummyAppDescriptorFileBuilder.getId());
    assertAppsDir(NONE, new String[] {dummyAppDescriptorFileBuilder.getId()}, true);

    reset(applicationDeploymentListener);

    File configFile = new File(appsDir + "/" + dummyAppDescriptorFileBuilder.getDeployedPath(), MULE_CONFIG_XML_FILE);
    configFile.setLastModified(configFile.lastModified() + FILE_TIMESTAMP_PRECISION_MILLIS);

    assertApplicationDeploymentSuccess(applicationDeploymentListener, dummyAppDescriptorFileBuilder.getId());
  }

  @Test
  public void redeploysExplodedAppAfterStartup() throws Exception {
    startDeployment();

    addExplodedAppFromBuilder(emptyAppFileBuilder);

    assertApplicationDeploymentSuccess(applicationDeploymentListener, emptyAppFileBuilder.getId());
    assertAppsDir(NONE, new String[] {emptyAppFileBuilder.getId()}, true);

    reset(applicationDeploymentListener);

    File configFile = new File(appsDir + "/" + emptyAppFileBuilder.getDeployedPath(), MULE_CONFIG_XML_FILE);
    assertThat("Configuration file does not exists", configFile.exists(), is(true));
    assertThat("Could not update last updated time in configuration file",
               configFile.setLastModified(configFile.lastModified() + FILE_TIMESTAMP_PRECISION_MILLIS), is(true));

    assertApplicationDeploymentSuccess(applicationDeploymentListener, emptyAppFileBuilder.getId());
  }

  @Test
  public void redeploysBrokenExplodedAppOnStartup() throws Exception {
    addExplodedAppFromBuilder(incompleteAppFileBuilder);

    startDeployment();

    assertDeploymentFailure(applicationDeploymentListener, incompleteAppFileBuilder.getId());

    // Maintains app dir created
    assertAppsDir(NONE, new String[] {incompleteAppFileBuilder.getId()}, true);
    final Map<URL, Long> zombieMap = deploymentService.getZombieApplications();
    assertEquals("Wrong number of zombie apps registered.", 1, zombieMap.size());
    final Map.Entry<URL, Long> zombie = zombieMap.entrySet().iterator().next();
    assertEquals("Wrong URL tagged as zombie.", incompleteAppFileBuilder.getId(),
                 new File(zombie.getKey().getFile()).getParentFile().getName());
    assertTrue("Invalid lastModified value for file URL.", zombie.getValue() != -1);

    reset(applicationDeploymentListener);

    final ReentrantLock lock = deploymentService.getLock();
    lock.lock();
    try {
      File configFile = new File(appsDir + "/" + incompleteAppFileBuilder.getDeployedPath(), MULE_CONFIG_XML_FILE);
      assertThat(configFile.exists(), is(true));
      configFile.setLastModified(configFile.lastModified() + FILE_TIMESTAMP_PRECISION_MILLIS);
    } finally {
      lock.unlock();
    }

    assertDeploymentFailure(applicationDeploymentListener, incompleteAppFileBuilder.getId());
  }

  @Test
  public void redeploysBrokenExplodedAppAfterStartup() throws Exception {
    startDeployment();

    addExplodedAppFromBuilder(incompleteAppFileBuilder);

    assertDeploymentFailure(applicationDeploymentListener, incompleteAppFileBuilder.getId());

    // Maintains app dir created
    assertAppsDir(NONE, new String[] {incompleteAppFileBuilder.getId()}, true);
    final Map<URL, Long> zombieMap = deploymentService.getZombieApplications();
    assertEquals("Wrong number of zombie apps registered.", 1, zombieMap.size());
    final Map.Entry<URL, Long> zombie = zombieMap.entrySet().iterator().next();
    assertEquals("Wrong URL tagged as zombie.", incompleteAppFileBuilder.getId(),
                 new File(zombie.getKey().getFile()).getParentFile().getName());
    assertTrue("Invalid lastModified value for file URL.", zombie.getValue() != -1);

    reset(applicationDeploymentListener);

    File configFile = new File(appsDir + "/" + incompleteAppFileBuilder.getDeployedPath(), MULE_CONFIG_XML_FILE);
    assertThat(configFile.exists(), is(true));
    configFile.setLastModified(configFile.lastModified() + FILE_TIMESTAMP_PRECISION_MILLIS);

    assertDeploymentFailure(applicationDeploymentListener, incompleteAppFileBuilder.getId());
  }

  @Test
  public void redeploysInvalidExplodedAppAfterSuccessfulDeploymentOnStartup() throws Exception {
    addExplodedAppFromBuilder(dummyAppDescriptorFileBuilder);

    startDeployment();

    assertApplicationDeploymentSuccess(applicationDeploymentListener, dummyAppDescriptorFileBuilder.getId());

    assertAppsDir(NONE, new String[] {dummyAppDescriptorFileBuilder.getId()}, true);

    reset(applicationDeploymentListener);

    File originalConfigFile = new File(appsDir + "/" + dummyAppDescriptorFileBuilder.getDeployedPath(), MULE_CONFIG_XML_FILE);
    URL url = getClass().getResource(BROKEN_CONFIG_XML);
    File newConfigFile = new File(url.toURI());
    copyFile(newConfigFile, originalConfigFile);

    assertDeploymentFailure(applicationDeploymentListener, dummyAppDescriptorFileBuilder.getId());
    assertStatus(dummyAppDescriptorFileBuilder.getId(), ApplicationStatus.DEPLOYMENT_FAILED);
  }

  @Test
  public void redeploysInvalidExplodedAppAfterSuccessfulDeploymentAfterStartup() throws Exception {
    startDeployment();

    addExplodedAppFromBuilder(dummyAppDescriptorFileBuilder);

    assertApplicationDeploymentSuccess(applicationDeploymentListener, dummyAppDescriptorFileBuilder.getId());
    assertAppsDir(NONE, new String[] {dummyAppDescriptorFileBuilder.getId()}, true);

    reset(applicationDeploymentListener);

    File originalConfigFile = new File(appsDir + "/" + dummyAppDescriptorFileBuilder.getDeployedPath(), MULE_CONFIG_XML_FILE);
    assertThat(originalConfigFile.exists(), is(true));
    URL url = getClass().getResource(BROKEN_CONFIG_XML);
    File newConfigFile = new File(url.toURI());
    copyFile(newConfigFile, originalConfigFile);

    assertDeploymentFailure(applicationDeploymentListener, dummyAppDescriptorFileBuilder.getId());
    assertStatus(dummyAppDescriptorFileBuilder.getId(), ApplicationStatus.DEPLOYMENT_FAILED);
  }

  @Test
  public void redeploysFixedAppAfterBrokenExplodedAppOnStartup() throws Exception {
    addExplodedAppFromBuilder(incompleteAppFileBuilder);

    startDeployment();

    assertDeploymentFailure(applicationDeploymentListener, incompleteAppFileBuilder.getId());

    reset(applicationDeploymentListener);

    File originalConfigFile = new File(appsDir + "/" + incompleteAppFileBuilder.getDeployedPath(), MULE_CONFIG_XML_FILE);
    assertThat(originalConfigFile.exists(), is(true));
    URL url = getClass().getResource(EMPTY_APP_CONFIG_XML);
    File newConfigFile = new File(url.toURI());
    copyFile(newConfigFile, originalConfigFile);
    assertApplicationDeploymentSuccess(applicationDeploymentListener, incompleteAppFileBuilder.getId());

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
      File originalConfigFile = new File(appsDir + "/" + incompleteAppFileBuilder.getDeployedPath(), MULE_CONFIG_XML_FILE);
      URL url = getClass().getResource(EMPTY_DOMAIN_CONFIG_XML);
      File newConfigFile = new File(url.toURI());
      copyFile(newConfigFile, originalConfigFile);
    } finally {
      deploymentLock.unlock();
    }

    assertApplicationDeploymentSuccess(applicationDeploymentListener, incompleteAppFileBuilder.getId());

    addPackedAppFromBuilder(emptyAppFileBuilder);
    assertApplicationDeploymentSuccess(applicationDeploymentListener, emptyAppFileBuilder.getId());

    // Check that the failed application folder is still there
    assertAppFolderIsMaintained(incompleteAppFileBuilder.getId());
  }

  @Test
  public void redeployModifiedDomainAndRedeployFailedApps() throws Exception {
    addExplodedDomainFromBuilder(sharedBundleDomainFileBuilder);

    // change shared config name to use a wrong name
    File domainConfigFile =
        new File(domainsDir + "/" + sharedBundleDomainFileBuilder.getDeployedPath(), DOMAIN_CONFIG_FILE_LOCATION);
    String correctDomainConfigContent = IOUtils.toString(new FileInputStream(domainConfigFile));
    String wrongDomainFileContext = correctDomainConfigContent.replace("test-shared-config", "test-shared-config-wrong");
    FileUtils.copyInputStreamToFile(new ByteArrayInputStream(wrongDomainFileContext.getBytes()), domainConfigFile);
    long firstFileTimestamp = domainConfigFile.lastModified();

    startDeployment();

    assertDeploymentSuccess(domainDeploymentListener, sharedDomainFileBuilder.getId());
    assertDeploymentFailure(applicationDeploymentListener, sharedAAppFileBuilder.getId());
    assertDeploymentFailure(applicationDeploymentListener, sharedBAppFileBuilder.getId());

    reset(applicationDeploymentListener);
    reset(domainDeploymentListener);

    FileUtils.copyInputStreamToFile(new ByteArrayInputStream(correctDomainConfigContent.getBytes()), domainConfigFile);
    alterTimestampIfNeeded(domainConfigFile, firstFileTimestamp);

    assertDeploymentSuccess(domainDeploymentListener, sharedDomainFileBuilder.getId());
    assertDeploymentSuccess(applicationDeploymentListener, sharedAAppFileBuilder.getId());
    assertDeploymentSuccess(applicationDeploymentListener, sharedBAppFileBuilder.getId());
  }

  @Test
  public void redeploysZipAppOnConfigChanges() throws Exception {
    addPackedAppFromBuilder(dummyAppDescriptorFileBuilder);

    startDeployment();

    assertApplicationDeploymentSuccess(applicationDeploymentListener, dummyAppDescriptorFileBuilder.getId());

    assertAppsDir(NONE, new String[] {dummyAppDescriptorFileBuilder.getId()}, true);
    assertEquals("Application has not been properly registered with Mule", 1, deploymentService.getApplications().size());

    reset(applicationDeploymentListener);

    File configFile = new File(appsDir + "/" + dummyAppDescriptorFileBuilder.getDeployedPath(), MULE_CONFIG_XML_FILE);
    configFile.setLastModified(configFile.lastModified() + FILE_TIMESTAMP_PRECISION_MILLIS);

    assertUndeploymentSuccess(applicationDeploymentListener, dummyAppDescriptorFileBuilder.getId());
    assertApplicationDeploymentSuccess(applicationDeploymentListener, dummyAppDescriptorFileBuilder.getId());
    assertEquals("Application has not been properly registered with Mule", 1, deploymentService.getApplications().size());
    assertAppsDir(NONE, new String[] {dummyAppDescriptorFileBuilder.getId()}, true);
  }

  @Test
  public void brokenAppArchiveWithoutArgument() throws Exception {
    doBrokenAppArchiveTest();
  }

  @Test
  public void brokenAppArchiveAsArgument() throws Exception {
    Map<String, Object> startupOptions = new HashMap<>();
    startupOptions.put("app", brokenAppFileBuilder.getId());
    StartupContext.get().setStartupOptions(startupOptions);

    doBrokenAppArchiveTest();
  }

  @Test
  public void deploysInvalidZipAppOnStartup() throws Exception {
    addPackedAppFromBuilder(emptyAppFileBuilder, "app with spaces.zip");

    startDeployment();
    assertDeploymentFailure(applicationDeploymentListener, "app with spaces");

    // zip stays intact, no app dir created
    assertAppsDir(new String[] {"app with spaces.zip"}, NONE, true);
    final Map<URL, Long> zombieMap = deploymentService.getZombieApplications();
    assertEquals("Wrong number of zombie apps registered.", 1, zombieMap.size());
    final Map.Entry<URL, Long> zombie = zombieMap.entrySet().iterator().next();
    // Spaces are converted to %20 is returned by java file api :/
    String appName = URLDecoder.decode(new File(zombie.getKey().getFile()).getName(), "UTF-8");
    assertEquals("Wrong URL tagged as zombie.", "app with spaces.zip", appName);
    assertTrue("Invalid lastModified value for file URL.", zombie.getValue() != -1);
  }

  @Test
  public void deploysInvalidZipAppAfterStartup() throws Exception {
    startDeployment();

    addPackedAppFromBuilder(emptyAppFileBuilder, "app with spaces.zip");

    assertDeploymentFailure(applicationDeploymentListener, "app with spaces");

    // zip stays intact, no app dir created
    assertAppsDir(new String[] {"app with spaces.zip"}, NONE, true);
    final Map<URL, Long> zombieMap = deploymentService.getZombieApplications();
    assertEquals("Wrong number of zombie apps registered.", 1, zombieMap.size());
    final Map.Entry<URL, Long> zombie = zombieMap.entrySet().iterator().next();
    // Spaces are converted to %20 is returned by java file api :/
    String appName = URLDecoder.decode(new File(zombie.getKey().getFile()).getName(), "UTF-8");
    assertEquals("Wrong URL tagged as zombie.", "app with spaces.zip", appName);
    assertTrue("Invalid lastModified value for file URL.", zombie.getValue() != -1);
  }

  @Test
  public void deployAppNameWithZipSuffix() throws Exception {
    final ApplicationFileBuilder applicationFileBuilder = new ApplicationFileBuilder("empty-app.zip", emptyAppFileBuilder);
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

    addPackedAppFromBuilder(emptyAppFileBuilder, "1.zip");
    addPackedAppFromBuilder(emptyAppFileBuilder, "2.zip");
    addPackedAppFromBuilder(emptyAppFileBuilder, "3.zip");

    Map<String, Object> startupOptions = new HashMap<>();
    startupOptions.put("app", "3:1:2");
    StartupContext.get().setStartupOptions(startupOptions);

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
  }

  @Test
  public void deploysExplodedAppsInOrderWhenAppArgumentIsUsed() throws Exception {
    assumeThat(parallelDeployment, is(false));

    addExplodedAppFromBuilder(emptyAppFileBuilder, "1");
    addExplodedAppFromBuilder(emptyAppFileBuilder, "2");
    addExplodedAppFromBuilder(emptyAppFileBuilder, "3");

    Map<String, Object> startupOptions = new HashMap<>();
    startupOptions.put("app", "3:1:2");
    StartupContext.get().setStartupOptions(startupOptions);

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
  }

  @Test
  public void deploysAppJustOnce() throws Exception {
    addPackedAppFromBuilder(emptyAppFileBuilder);

    Map<String, Object> startupOptions = new HashMap<>();
    startupOptions.put("app", "empty-app:empty-app:empty-app");
    StartupContext.get().setStartupOptions(startupOptions);

    startDeployment();

    assertApplicationDeploymentSuccess(applicationDeploymentListener, emptyAppFileBuilder.getId());
    assertAppsDir(NONE, new String[] {emptyAppFileBuilder.getId()}, true);

    List<Application> applications = deploymentService.getApplications();
    assertEquals(1, applications.size());
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

    File configFile = new File(appFolder, MULE_CONFIG_XML_FILE);
    FileUtils.writeStringToFile(configFile, "you shall not pass");

    startDeployment();
    assertDeploymentFailure(applicationDeploymentListener, emptyAppFileBuilder.getId());
    reset(applicationDeploymentListener);

    URL url = getClass().getResource(EMPTY_DOMAIN_CONFIG_XML);
    copyFile(new File(url.toURI()), configFile);

    assertApplicationDeploymentSuccess(applicationDeploymentListener, emptyAppFileBuilder.getId());
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
    assertMuleContextConfigured(applicationDeploymentListener, emptyAppFileBuilder.getId());
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
                                     domainManager, serviceManager);
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
                                     domainManager, serviceManager);
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
    final Map.Entry<URL, Long> zombie = deploymentService.getZombieApplications().entrySet().iterator().next();
    assertEquals("Wrong URL tagged as zombie.", incompleteAppFileBuilder.getId(),
                 new File(zombie.getKey().getFile()).getParentFile().getName());
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
    final Map.Entry<URL, Long> zombie = deploymentService.getZombieApplications().entrySet().iterator().next();
    assertEquals("Wrong URL tagged as zombie.", incompleteAppFileBuilder.getId(),
                 new File(zombie.getKey().getFile()).getParentFile().getName());
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
    final Map.Entry<URL, Long> zombie = deploymentService.getZombieApplications().entrySet().iterator().next();
    assertEquals("Wrong URL tagged as zombie.", incompleteAppFileBuilder.getId(),
                 new File(zombie.getKey().getFile()).getParentFile().getName());
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
    assertApplicationDeploymentSuccess(applicationDeploymentListener, incompleteAppFileBuilder.getId());

    assertEquals("Failed app still appears as zombie after a successful redeploy", 0,
                 deploymentService.getZombieApplications().size());
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
    assertApplicationDeploymentSuccess(applicationDeploymentListener, incompleteAppFileBuilder.getId());

    assertEquals("Failed app still appears as zombie after a successful redeploy", 0,
                 deploymentService.getZombieApplications().size());
  }

  @Test
  public void redeploysInvalidZipAppAfterSuccessfulDeploymentOnStartup() throws Exception {
    addPackedAppFromBuilder(emptyAppFileBuilder);

    startDeployment();

    assertApplicationDeploymentSuccess(applicationDeploymentListener, emptyAppFileBuilder.getId());

    addPackedAppFromBuilder(incompleteAppFileBuilder, emptyAppFileBuilder.getZipPath());

    assertDeploymentFailure(applicationDeploymentListener, emptyAppFileBuilder.getId());

    final Map.Entry<URL, Long> zombie = deploymentService.getZombieApplications().entrySet().iterator().next();
    assertEquals("Wrong URL tagged as zombie.", emptyAppFileBuilder.getId(),
                 new File(zombie.getKey().getFile()).getParentFile().getName());
  }

  @Test
  public void redeploysInvalidZipAppAfterSuccessfulDeploymentAfterStartup() throws Exception {
    startDeployment();

    addPackedAppFromBuilder(emptyAppFileBuilder);
    assertApplicationDeploymentSuccess(applicationDeploymentListener, emptyAppFileBuilder.getId());

    addPackedAppFromBuilder(incompleteAppFileBuilder, emptyAppFileBuilder.getZipPath());
    assertDeploymentFailure(applicationDeploymentListener, emptyAppFileBuilder.getId());

    final Map.Entry<URL, Long> zombie = deploymentService.getZombieApplications().entrySet().iterator().next();
    assertEquals("Wrong URL tagged as zombie.", emptyAppFileBuilder.getId(),
                 new File(zombie.getKey().getFile()).getParentFile().getName());
  }

  @Test
  public void redeploysInvalidZipAppAfterFailedDeploymentOnStartup() throws Exception {
    addPackedAppFromBuilder(incompleteAppFileBuilder);

    startDeployment();

    assertDeploymentFailure(applicationDeploymentListener, incompleteAppFileBuilder.getId());

    reset(applicationDeploymentListener);

    addPackedAppFromBuilder(incompleteAppFileBuilder);

    assertDeploymentFailure(applicationDeploymentListener, incompleteAppFileBuilder.getId());

    final Map.Entry<URL, Long> zombie = deploymentService.getZombieApplications().entrySet().iterator().next();
    assertEquals("Wrong URL tagged as zombie.", incompleteAppFileBuilder.getId(),
                 new File(zombie.getKey().getFile()).getParentFile().getName());
  }

  @Test
  public void redeploysInvalidZipAppAfterFailedDeploymentAfterStartup() throws Exception {
    startDeployment();

    addPackedAppFromBuilder(incompleteAppFileBuilder);
    assertDeploymentFailure(applicationDeploymentListener, incompleteAppFileBuilder.getId());

    reset(applicationDeploymentListener);

    addPackedAppFromBuilder(incompleteAppFileBuilder);
    assertDeploymentFailure(applicationDeploymentListener, incompleteAppFileBuilder.getId());

    final Map.Entry<URL, Long> zombie = deploymentService.getZombieApplications().entrySet().iterator().next();
    assertEquals("Wrong URL tagged as zombie.", incompleteAppFileBuilder.getId(),
                 new File(zombie.getKey().getFile()).getParentFile().getName());
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

    assertApplicationDeploymentSuccess(applicationDeploymentListener, incompleteAppFileBuilder.getId());
    assertEquals("Failed app still appears as zombie after a successful redeploy", 0,
                 deploymentService.getZombieApplications().size());
  }

  @Test
  public void deploysAppZipWithPlugin() throws Exception {
    addPackedAppFromBuilder(echoPluginAppFileBuilder);

    startDeployment();

    assertDeploymentSuccess(applicationDeploymentListener, echoPluginAppFileBuilder.getId());
  }

  @Test
  public void deploysAppZipWithContainerPluginBroken() throws Exception {
    ArtifactPluginFileBuilder echoPluginBroken = new ArtifactPluginFileBuilder("echoPlugin")
        .configuredWith(EXPORTED_CLASS_PACKAGES_PROPERTY, "org.foo").usingLibrary(echoTestJarFile.getAbsolutePath()).corrupted();

    installContainerPlugin(echoPluginBroken);

    final ApplicationFileBuilder applicationFileBuilder =
        new ApplicationFileBuilder("my-app.zip", emptyAppFileBuilder).containingPlugin(echoPluginWithLib1);
    addPackedAppFromBuilder(applicationFileBuilder);

    startDeployment();

    assertDeploymentFailure(applicationDeploymentListener, applicationFileBuilder.getId());
  }

  @Test
  public void deploysAppZipWithPluginAlreadyInContainerPlugins() throws Exception {
    installContainerPlugin(echoPlugin);
    // Just an non-zip file to validate that it is only looking for zip files
    copyFileToContainerPluginFolder(echoPlugin.getArtifactFile(), "invalidPlugin.tar");

    final ApplicationFileBuilder applicationFileBuilder =
        new ApplicationFileBuilder("my-app.zip", emptyAppFileBuilder).containingPlugin(echoPlugin);
    addPackedAppFromBuilder(applicationFileBuilder);

    startDeployment();

    assertDeploymentFailure(applicationDeploymentListener, applicationFileBuilder.getId());
  }

  @Test
  public void deploysAppZipWithPluginShouldIncludedBundledPluginsFromContainer() throws Exception {
    installContainerPlugin(echoPlugin);

    final ApplicationFileBuilder applicationFileBuilder =
        new ApplicationFileBuilder("dummyWithEchoPlugin").definedBy("app-with-echo-plugin-config.xml");
    addPackedAppFromBuilder(applicationFileBuilder);

    startDeployment();

    assertApplicationDeploymentSuccess(applicationDeploymentListener, applicationFileBuilder.getId());
    assertAppsDir(NONE, new String[] {applicationFileBuilder.getId()}, true);
    assertContainerAppPluginExplodedDir(new String[] {echoPlugin.getDeployedPath()});
  }

  @Test
  public void deploysAppZipWithPluginShouldIncludedBundledPluginsExpandedFromContainer() throws Exception {
    installContainerPluginExpanded(echoPlugin);

    final ApplicationFileBuilder applicationFileBuilder =
        new ApplicationFileBuilder("dummyWithEchoPlugin").definedBy("app-with-echo-plugin-config.xml");
    addPackedAppFromBuilder(applicationFileBuilder);

    startDeployment();

    assertApplicationDeploymentSuccess(applicationDeploymentListener, applicationFileBuilder.getId());
    assertAppsDir(NONE, new String[] {applicationFileBuilder.getId()}, true);
    assertContainerAppPluginExplodedDir(new String[] {echoPlugin.getDeployedPath()});
  }

  @Test
  public void deploysAppWithPluginSharedLibrary() throws Exception {
    addPackedAppFromBuilder(sharedLibPluginAppFileBuilder);

    startDeployment();

    assertApplicationDeploymentSuccess(applicationDeploymentListener, sharedLibPluginAppFileBuilder.getId());
    assertAppsDir(NONE, new String[] {sharedLibPluginAppFileBuilder.getId()}, true);
    assertApplicationAnchorFileExists(sharedLibPluginAppFileBuilder.getId());

    executeApplicationFlow("main");
  }

  @Test
  public void deploysAppZipWithExtensionPlugin() throws Exception {
    final ServiceFileBuilder echoService =
        new ServiceFileBuilder("echoService").configuredWith(SERVICE_PROVIDER_CLASS_NAME, "org.mule.echo.EchoServiceProvider")
            .usingLibrary(defaulServiceEchoJarFile.getAbsolutePath());
    File installedService = new File(services, echoService.getArtifactFile().getName());
    copyFile(echoService.getArtifactFile(), installedService);

    final ServiceFileBuilder fooService = new ServiceFileBuilder("fooService")
        .configuredWith(SERVICE_PROVIDER_CLASS_NAME, "org.mule.service.foo.FooServiceProvider")
        .usingLibrary(defaultFooServiceJarFile.getAbsolutePath());
    installedService = new File(services, fooService.getArtifactFile().getName());
    copyFile(fooService.getArtifactFile(), installedService);

    ArtifactPluginFileBuilder extensionPlugin =
        new ArtifactPluginFileBuilder("extensionPlugin").usingLibrary(helloExtensionJarFile.getAbsolutePath())
            .configuredWith(EXPORTED_RESOURCE_PROPERTY,
                            "/, META-INF/mule-hello.xsd, META-INF/spring.handlers, META-INF/spring.schemas");
    ApplicationFileBuilder applicationFileBuilder = new ApplicationFileBuilder("appWithExtensionPlugin")
        .definedBy("app-with-extension-plugin-config.xml").containingPlugin(extensionPlugin);
    addPackedAppFromBuilder(applicationFileBuilder);

    startDeployment();

    assertDeploymentSuccess(applicationDeploymentListener, applicationFileBuilder.getId());
  }

  @Test
  public void deploysAppWithPluginBootstrapProperty() throws Exception {
    final ArtifactPluginFileBuilder pluginFileBuilder = new ArtifactPluginFileBuilder("bootstrapPlugin")
        .containingResource("plugin-bootstrap.properties", BOOTSTRAP_PROPERTIES)
        .containingClass(echoTestClassFile, "org/foo/EchoTest.class");

    ApplicationFileBuilder applicationFileBuilder = new ApplicationFileBuilder("app-with-plugin-bootstrap")
        .definedBy("app-with-plugin-bootstrap.xml").containingPlugin(pluginFileBuilder);
    addPackedAppFromBuilder(applicationFileBuilder);

    startDeployment();

    assertApplicationDeploymentSuccess(applicationDeploymentListener, applicationFileBuilder.getId());

    final Application application = findApp(applicationFileBuilder.getId(), 1);
    final Object lookupObject = application.getMuleContext().getRegistry().lookupObject("plugin.echotest");
    assertThat(lookupObject, is(not(nullValue())));
    assertThat(lookupObject.getClass().getName(), equalTo("org.foo.EchoTest"));
  }

  @Test
  public void failsToDeployApplicationOnMissingService() throws Exception {
    ArtifactPluginFileBuilder extensionPlugin = new ArtifactPluginFileBuilder("extensionPlugin")
        .usingLibrary(helloExtensionJarFile.getAbsolutePath()).configuredWith(EXPORTED_RESOURCE_PROPERTY, "/, META-INF");
    ApplicationFileBuilder applicationFileBuilder = new ApplicationFileBuilder("appWithExtensionPlugin")
        .definedBy("app-with-extension-plugin-config.xml").containingPlugin(extensionPlugin);
    addPackedAppFromBuilder(applicationFileBuilder);

    startDeployment();

    assertDeploymentFailure(applicationDeploymentListener, applicationFileBuilder.getId());
  }

  @Test
  public void synchronizesAppDeployFromClient() throws Exception {
    final Action action = () -> deploymentService.deploy(dummyAppDescriptorFileBuilder.getArtifactFile().toURI().toURL());

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
  public void deploysDomainWithSharedLibPrecedenceOverApplicationSharedLib() throws Exception {
    final String domainId = "shared-lib";
    final ApplicationFileBuilder applicationFileBuilder = new ApplicationFileBuilder("shared-lib-precedence-app")
        .definedBy("app-shared-lib-precedence-config.xml").sharingLibrary(barUtils2_0JarFile.getAbsolutePath())
        .containingClass(pluginEcho1TestClassFile, "org/foo/Plugin1Echo.class")
        .deployedWith(PROPERTY_DOMAIN, domainId);
    final DomainFileBuilder domainFileBuilder =
        new DomainFileBuilder(domainId).usingLibrary(barUtils1_0JarFile.getAbsolutePath()).containing(applicationFileBuilder);

    addPackedDomainFromBuilder(domainFileBuilder);
    startDeployment();

    assertDeploymentSuccess(domainDeploymentListener, domainFileBuilder.getId());
    assertApplicationDeploymentSuccess(applicationDeploymentListener, applicationFileBuilder.getId());

    executeApplicationFlow("main");
  }

  @Test
  public void deploysDomainWithSharedLibPrecedenceOverApplicationLib() throws Exception {
    final String domainId = "shared-lib";
    final ApplicationFileBuilder applicationFileBuilder =
        new ApplicationFileBuilder("shared-lib-precedence-app").definedBy("app-shared-lib-precedence-config.xml")
            .usingLibrary(barUtils2_0JarFile.getAbsolutePath())
            .containingClass(pluginEcho1TestClassFile, "org/foo/Plugin1Echo.class")
            .deployedWith(PROPERTY_DOMAIN, domainId);
    final DomainFileBuilder domainFileBuilder =
        new DomainFileBuilder(domainId).usingLibrary(barUtils1_0JarFile.getAbsolutePath()).containing(applicationFileBuilder);

    addPackedDomainFromBuilder(domainFileBuilder);
    startDeployment();

    assertDeploymentSuccess(domainDeploymentListener, domainFileBuilder.getId());
    assertApplicationDeploymentSuccess(applicationDeploymentListener, applicationFileBuilder.getId());

    executeApplicationFlow("main");
  }

  @Test
  public void deploysDomainWithSharedLibPrecedenceOverApplicationPluginLib() throws Exception {
    final String domainId = "shared-lib";
    final ArtifactPluginFileBuilder pluginFileBuilder =
        new ArtifactPluginFileBuilder("echoPlugin1").configuredWith(EXPORTED_CLASS_PACKAGES_PROPERTY, "org.foo")
            .containingClass(pluginEcho1TestClassFile, "org/foo/Plugin1Echo.class")
            .usingLibrary(barUtils2_0JarFile.getAbsolutePath());
    final ApplicationFileBuilder applicationFileBuilder =
        new ApplicationFileBuilder("shared-lib-precedence-app").definedBy("app-shared-lib-precedence-config.xml")
            .containingPlugin(pluginFileBuilder).deployedWith(PROPERTY_DOMAIN, domainId);
    final DomainFileBuilder domainFileBuilder =
        new DomainFileBuilder(domainId).usingLibrary(barUtils1_0JarFile.getAbsolutePath()).containing(applicationFileBuilder);

    addPackedDomainFromBuilder(domainFileBuilder);
    startDeployment();

    assertDeploymentSuccess(domainDeploymentListener, domainFileBuilder.getId());
    assertApplicationDeploymentSuccess(applicationDeploymentListener, applicationFileBuilder.getId());

    executeApplicationFlow("main");
  }

  @Test
  public void deploysMultiPluginVersionLib() throws Exception {
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
            .dependingOn(echoPlugin.getId());

    final TestArtifactDescriptor artifactFileBuilder = new ApplicationFileBuilder("plugin-depending-on-plugin-app")
        .definedBy("plugin-depending-on-plugin-app-config.xml").containingPlugin(echoPlugin).containingPlugin(dependantPlugin);
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
        .definedBy("plugin-depending-on-plugin-app-config.xml").containingPlugin(echoPlugin).containingPlugin(dependantPlugin);
    addPackedAppFromBuilder(artifactFileBuilder);

    startDeployment();

    assertDeploymentSuccess(applicationDeploymentListener, artifactFileBuilder.getId());

    try {
      executeApplicationFlow("main");
      fail("Expected to fail as there should be a missing class");
    } catch (MessagingException e) {
      assertThat(e.getCause().getCause(), instanceOf(NoClassDefFoundError.class));
      assertThat(e.getCause().getCause().getMessage(), containsString("org/foo/EchoTest"));
    }
  }

  @Test
  public void deploysAppWithLibDifferentThanPlugin() throws Exception {
    addPackedAppFromBuilder(differentLibPluginAppFileBuilder);

    startDeployment();

    assertDeploymentSuccess(applicationDeploymentListener, differentLibPluginAppFileBuilder.getId());

    executeApplicationFlow("main");
  }

  @Test
  public void deploysAppUsingPluginResource() throws Exception {
    addPackedAppFromBuilder(resourcePluginAppFileBuilder);

    startDeployment();

    assertDeploymentSuccess(applicationDeploymentListener, resourcePluginAppFileBuilder.getId());
  }

  @Test
  public void deploysAppProvidingResourceForPlugin() throws Exception {
    final TestArtifactDescriptor artifactFileBuilder =
        new ApplicationFileBuilder("appProvidingResourceForPlugin").definedBy("app-providing-resource-for-plugin.xml")
            .containingPlugin(pluginUsingAppResource).usingResource("test-resource.txt", "META-INF/app-resource.txt");
    addPackedAppFromBuilder(artifactFileBuilder);

    startDeployment();

    assertDeploymentSuccess(applicationDeploymentListener, artifactFileBuilder.getId());

    executeApplicationFlow("main");
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
  public void deploysMultipleAppsZipOnStartup() throws Exception {
    final int totalApps = 20;

    for (int i = 1; i <= totalApps; i++) {
      addExplodedAppFromBuilder(emptyAppFileBuilder, Integer.toString(i));
    }

    startDeployment();

    for (int i = 1; i <= totalApps; i++) {
      assertDeploymentSuccess(applicationDeploymentListener, Integer.toString(i));
    }
  }

  @Test
  public void deploysDomainZipOnStartup() throws Exception {
    addPackedDomainFromBuilder(emptyDomainFileBuilder);

    startDeployment();

    assertDeploymentSuccess(domainDeploymentListener, emptyDomainFileBuilder.getId());

    assertDomainDir(NONE, new String[] {DEFAULT_DOMAIN_NAME, emptyDomainFileBuilder.getId()}, true);

    final Domain domain = findADomain(emptyDomainFileBuilder.getId());
    assertNotNull(domain);
    assertNotNull(domain.getMuleContext());
    assertDomainAnchorFileExists(emptyDomainFileBuilder.getId());
  }

  @Test
  public void deploysPackagedDomainAndVerifyAnchorFileIsCreatedAfterDeploymentEnds() throws Exception {
    Action deployPackagedWaitDomainAction = () -> addPackedDomainFromBuilder(waitDomainFileBuilder);
    deploysDomainAndVerifyAnchorFileIsCreatedAfterDeploymentEnds(deployPackagedWaitDomainAction);
  }

  @Test
  public void deploysExplodedDomainAndVerifyAnchorFileIsCreatedAfterDeploymentEnds() throws Exception {
    Action deployExplodedWaitDomainAction = () -> addExplodedDomainFromBuilder(waitDomainFileBuilder);
    deploysDomainAndVerifyAnchorFileIsCreatedAfterDeploymentEnds(deployExplodedWaitDomainAction);
  }

  @Test
  public void deploysExplodedDomainBundleOnStartup() throws Exception {
    addExplodedDomainFromBuilder(dummyDomainBundleFileBuilder);

    startDeployment();

    deploysDomainBundle();
  }

  @Test
  public void deploysExplodedDomainBundleAfterStartup() throws Exception {
    startDeployment();

    addExplodedDomainFromBuilder(dummyDomainBundleFileBuilder);

    deploysDomainBundle();
  }

  @Test
  public void deploysDomainBundleZipOnStartup() throws Exception {
    addPackedDomainFromBuilder(dummyDomainBundleFileBuilder);

    startDeployment();

    deploysDomainBundle();
  }

  @Test
  public void deploysDomainBundleZipAfterStartup() throws Exception {
    startDeployment();

    addPackedDomainFromBuilder(dummyDomainBundleFileBuilder);

    deploysDomainBundle();
  }

  private void deploysDomainBundle() {
    assertDeploymentSuccess(domainDeploymentListener, dummyDomainBundleFileBuilder.getId());

    assertDomainDir(NONE, new String[] {DEFAULT_DOMAIN_NAME, dummyDomainBundleFileBuilder.getId()}, true);

    final Domain domain = findADomain(dummyDomainBundleFileBuilder.getId());
    assertNotNull(domain);
    assertNull(domain.getMuleContext());

    assertApplicationDeploymentSuccess(applicationDeploymentListener, dummyAppDescriptorFileBuilder.getId());
    assertAppsDir(NONE, new String[] {dummyAppDescriptorFileBuilder.getId()}, true);

    final Application app = findApp(dummyAppDescriptorFileBuilder.getId(), 1);
    assertNotNull(app);
  }

  @Test
  public void deploysInvalidExplodedDomainBundleOnStartup() throws Exception {
    addExplodedDomainFromBuilder(invalidDomainBundleFileBuilder);

    startDeployment();

    deploysInvalidDomainBundleZip();
  }

  @Test
  public void deploysInvalidExplodedDomainBundleAfterStartup() throws Exception {
    startDeployment();

    addExplodedDomainFromBuilder(invalidDomainBundleFileBuilder);

    deploysInvalidDomainBundleZip();
  }

  @Test
  public void deploysInvalidDomainBundleZipOnStartup() throws Exception {
    addPackedDomainFromBuilder(invalidDomainBundleFileBuilder);

    startDeployment();

    deploysInvalidDomainBundleZip();
  }

  @Test
  public void deploysInvalidDomainBundleZipAfterStartup() throws Exception {
    addPackedDomainFromBuilder(invalidDomainBundleFileBuilder);

    startDeployment();

    deploysInvalidDomainBundleZip();
  }

  private void deploysInvalidDomainBundleZip() {
    assertDeploymentFailure(domainDeploymentListener, invalidDomainBundleFileBuilder.getId());

    assertDomainDir(NONE, new String[] {DEFAULT_DOMAIN_NAME, invalidDomainBundleFileBuilder.getId()}, true);

    assertAppsDir(NONE, new String[] {}, true);
  }

  @Test
  public void deploysDomainZipAfterStartup() throws Exception {
    startDeployment();

    addPackedDomainFromBuilder(emptyDomainFileBuilder);

    assertDeploymentSuccess(domainDeploymentListener, emptyDomainFileBuilder.getId());

    assertDomainDir(NONE, new String[] {DEFAULT_DOMAIN_NAME, emptyDomainFileBuilder.getId()}, true);

    final Domain domain = findADomain(emptyDomainFileBuilder.getId());
    assertNotNull(domain);
    assertNotNull(domain.getMuleContext());
    assertDomainAnchorFileExists(emptyDomainFileBuilder.getId());
  }

  @Test
  public void deploysBrokenDomainZipOnStartup() throws Exception {
    addPackedDomainFromBuilder(brokenDomainFileBuilder);

    startDeployment();

    assertDeploymentFailure(domainDeploymentListener, brokenDomainFileBuilder.getId());

    assertDomainDir(new String[] {brokenDomainFileBuilder.getDeployedPath()}, new String[] {DEFAULT_DOMAIN_NAME}, true);

    assertDomainAnchorFileDoesNotExists(brokenDomainFileBuilder.getId());

    final Map<URL, Long> zombieMap = deploymentService.getZombieDomains();
    assertEquals("Wrong number of zombie domains registered.", 1, zombieMap.size());
    final Map.Entry<URL, Long> zombie = zombieMap.entrySet().iterator().next();
    assertEquals("Wrong URL tagged as domain", brokenDomainFileBuilder.getDeployedPath(),
                 new File(zombie.getKey().getFile()).getName());
    assertTrue("Invalid lastModified value for file URL.", zombie.getValue() != -1);
  }

  @Test
  public void deploysBrokenDomainZipAfterStartup() throws Exception {
    startDeployment();

    addPackedDomainFromBuilder(brokenDomainFileBuilder);

    assertDeploymentFailure(domainDeploymentListener, brokenDomainFileBuilder.getId());

    assertDomainDir(new String[] {brokenDomainFileBuilder.getDeployedPath()}, new String[] {DEFAULT_DOMAIN_NAME}, true);

    assertDomainAnchorFileDoesNotExists(brokenDomainFileBuilder.getId());

    final Map<URL, Long> zombieMap = deploymentService.getZombieDomains();
    assertEquals("Wrong number of zombie domains registered.", 1, zombieMap.size());
    final Map.Entry<URL, Long> zombie = zombieMap.entrySet().iterator().next();
    assertEquals("Wrong URL tagged as domain.", brokenDomainFileBuilder.getDeployedPath(),
                 new File(zombie.getKey().getFile()).getName());
    assertTrue("Invalid lastModified value for file URL.", zombie.getValue() != -1);
  }

  @Test
  public void redeploysDomainZipDeployedOnStartup() throws Exception {
    startDeployment();

    addPackedDomainFromBuilder(emptyAppFileBuilder);
    File dummyDomainFile = new File(domainsDir, emptyAppFileBuilder.getZipPath());
    long firstFileTimestamp = dummyDomainFile.lastModified();

    assertDeploymentSuccess(domainDeploymentListener, emptyAppFileBuilder.getId());

    assertDomainDir(NONE, new String[] {DEFAULT_DOMAIN_NAME, emptyAppFileBuilder.getId()}, true);
    assertEquals("Domain has not been properly registered with Mule", 2, deploymentService.getDomains().size());

    reset(domainDeploymentListener);

    addPackedDomainFromBuilder(emptyAppFileBuilder);
    alterTimestampIfNeeded(dummyDomainFile, firstFileTimestamp);

    assertUndeploymentSuccess(domainDeploymentListener, emptyAppFileBuilder.getId());
    assertDeploymentSuccess(domainDeploymentListener, emptyAppFileBuilder.getId());
    assertEquals("Domain has not been properly registered with Mule", 2, deploymentService.getDomains().size());
    assertDomainDir(NONE, new String[] {DEFAULT_DOMAIN_NAME, emptyAppFileBuilder.getId()}, true);
  }

  @Test
  public void redeployedDomainsAreDifferent() throws Exception {
    startDeployment();

    addPackedDomainFromBuilder(emptyAppFileBuilder);
    File dummyDomainFile = new File(domainsDir, emptyAppFileBuilder.getZipPath());
    long firstFileTimestamp = dummyDomainFile.lastModified();

    assertDeploymentSuccess(domainDeploymentListener, emptyAppFileBuilder.getId());

    assertEquals("Domain has not been properly registered with Mule", 2, deploymentService.getDomains().size());
    Domain firstDomain = findADomain(emptyAppFileBuilder.getId());

    reset(domainDeploymentListener);

    addPackedDomainFromBuilder(emptyAppFileBuilder);
    alterTimestampIfNeeded(dummyDomainFile, firstFileTimestamp);

    assertUndeploymentSuccess(domainDeploymentListener, emptyAppFileBuilder.getId());
    assertDeploymentSuccess(domainDeploymentListener, emptyAppFileBuilder.getId());
    assertEquals("Domain has not been properly registered with Mule", 2, deploymentService.getDomains().size());
    Domain secondDomain = findADomain(emptyAppFileBuilder.getId());

    assertNotSame(firstDomain, secondDomain);
  }

  @Test
  public void redeploysDomainZipRefreshesApps() throws Exception {
    addPackedDomainFromBuilder(dummyDomainFileBuilder);
    File dummyDomainFile = new File(domainsDir, dummyDomainFileBuilder.getZipPath());
    long firstFileTimestamp = dummyDomainFile.lastModified();

    addPackedAppFromBuilder(dummyDomainApp1FileBuilder);

    startDeployment();

    assertDeploymentSuccess(domainDeploymentListener, dummyDomainFileBuilder.getId());
    assertApplicationDeploymentSuccess(applicationDeploymentListener, dummyDomainApp1FileBuilder.getId());

    reset(domainDeploymentListener);
    reset(applicationDeploymentListener);

    addPackedDomainFromBuilder(dummyDomainFileBuilder);
    alterTimestampIfNeeded(dummyDomainFile, firstFileTimestamp);

    assertUndeploymentSuccess(applicationDeploymentListener, dummyDomainApp1FileBuilder.getId());
    assertUndeploymentSuccess(domainDeploymentListener, dummyDomainFileBuilder.getId());
    assertDeploymentSuccess(domainDeploymentListener, dummyDomainFileBuilder.getId());
    assertApplicationDeploymentSuccess(applicationDeploymentListener, dummyDomainApp1FileBuilder.getId());
  }

  @Test
  public void redeploysDomainZipDeployedAfterStartup() throws Exception {
    addPackedDomainFromBuilder(dummyDomainFileBuilder);
    File dummyDomainFile = new File(domainsDir, dummyDomainFileBuilder.getZipPath());
    long firstFileTimestamp = dummyDomainFile.lastModified();

    startDeployment();

    assertDeploymentSuccess(domainDeploymentListener, dummyDomainFileBuilder.getId());

    assertDomainDir(NONE, new String[] {DEFAULT_DOMAIN_NAME, dummyDomainFileBuilder.getId()}, true);
    assertEquals("Domain has not been properly registered with Mule", 2, deploymentService.getDomains().size());

    reset(domainDeploymentListener);

    addPackedDomainFromBuilder(dummyDomainFileBuilder);
    alterTimestampIfNeeded(dummyDomainFile, firstFileTimestamp);

    assertUndeploymentSuccess(domainDeploymentListener, dummyDomainFileBuilder.getId());
    assertDeploymentSuccess(domainDeploymentListener, dummyDomainFileBuilder.getId());
    assertEquals("Domain has not been properly registered with Mule", 2, deploymentService.getDomains().size());
    assertDomainDir(NONE, new String[] {DEFAULT_DOMAIN_NAME, dummyDomainFileBuilder.getId()}, true);
  }

  protected void alterTimestampIfNeeded(File file, long firstTimestamp) {
    if (!file.exists()) {
      throw new IllegalArgumentException("File does not exists: " + file.getAbsolutePath());
    }
    if (firstTimestamp == file.lastModified()) {
      // File systems only have second precision. If both file writes happen during the same second, the last
      // change will be ignored by the directory scanner.
      assertThat(file.setLastModified(file.lastModified() + FILE_TIMESTAMP_PRECISION_MILLIS), is(true));
    }
  }

  @Test
  public void deploysExplodedDomainOnStartup() throws Exception {
    addExplodedDomainFromBuilder(emptyDomainFileBuilder);

    startDeployment();

    assertDeploymentSuccess(domainDeploymentListener, emptyDomainFileBuilder.getId());
    assertDomainDir(NONE, new String[] {DEFAULT_DOMAIN_NAME, emptyDomainFileBuilder.getId()}, true);
    assertDomainAnchorFileExists(emptyDomainFileBuilder.getId());
  }

  @Test
  public void deploysPackagedDomainOnStartupWhenExplodedDomainIsAlsoPresent() throws Exception {
    addExplodedDomainFromBuilder(emptyDomainFileBuilder);
    addPackedDomainFromBuilder(emptyDomainFileBuilder);

    startDeployment();

    assertDeploymentSuccess(domainDeploymentListener, emptyDomainFileBuilder.getId());

    addExplodedDomainFromBuilder(emptyDomainFileBuilder);

    assertDeploymentSuccess(domainDeploymentListener, emptyDomainFileBuilder.getId());

    // Checks that dummy app was deployed just once
    assertDeploymentSuccess(domainDeploymentListener, emptyDomainFileBuilder.getId());
  }

  @Test
  public void deploysExplodedDomainAfterStartup() throws Exception {
    startDeployment();

    addExplodedDomainFromBuilder(emptyDomainFileBuilder);

    assertDeploymentSuccess(domainDeploymentListener, emptyDomainFileBuilder.getId());
    assertDomainDir(NONE, new String[] {DEFAULT_DOMAIN_NAME, emptyDomainFileBuilder.getId()}, true);
    assertDomainAnchorFileExists(emptyDomainFileBuilder.getId());
  }

  @Test
  public void deploysInvalidExplodedDomainOnStartup() throws Exception {
    addExplodedDomainFromBuilder(emptyDomainFileBuilder, "domain with spaces");

    startDeployment();

    assertDeploymentFailure(domainDeploymentListener, "domain with spaces");

    // Maintains app dir created
    assertDomainDir(NONE, new String[] {DEFAULT_DOMAIN_NAME, "domain with spaces"}, true);
    final Map<URL, Long> zombieMap = deploymentService.getZombieDomains();
    assertEquals("Wrong number of zombie domains registered.", 1, zombieMap.size());
    final Map.Entry<URL, Long> zombie = zombieMap.entrySet().iterator().next();
    // Spaces are converted to %20 is returned by java file api :/
    String domainName = URLDecoder.decode(new File(zombie.getKey().getFile()).getName(), "UTF-8");
    assertEquals("Wrong URL tagged as zombie.", "domain with spaces", domainName);
    assertTrue("Invalid lastModified value for file URL.", zombie.getValue() != -1);
  }

  @Test
  public void deploysInvalidExplodedDomainAfterStartup() throws Exception {
    startDeployment();

    addExplodedDomainFromBuilder(emptyDomainFileBuilder, "domain with spaces");

    assertDeploymentFailure(domainDeploymentListener, "domain with spaces");

    // Maintains app dir created
    assertDomainDir(NONE, new String[] {DEFAULT_DOMAIN_NAME, "domain with spaces"}, true);
    final Map<URL, Long> zombieMap = deploymentService.getZombieDomains();
    assertEquals("Wrong number of zombie domains registered.", 1, zombieMap.size());
    final Map.Entry<URL, Long> zombie = zombieMap.entrySet().iterator().next();
    // Spaces are converted to %20 is returned by java file api :/
    String appName = URLDecoder.decode(new File(zombie.getKey().getFile()).getName(), "UTF-8");
    assertEquals("Wrong URL tagged as zombie.", "domain with spaces", appName);
    assertTrue("Invalid lastModified value for file URL.", zombie.getValue() != -1);
  }

  @Test
  public void deploysInvalidExplodedDomainOnlyOnce() throws Exception {
    startDeployment();

    addExplodedDomainFromBuilder(emptyDomainFileBuilder, "domain with spaces");
    assertDeploymentFailure(domainDeploymentListener, "domain with spaces", times(1));

    addExplodedDomainFromBuilder(emptyDomainFileBuilder);
    assertDeploymentSuccess(domainDeploymentListener, emptyDomainFileBuilder.getId());

    addExplodedDomainFromBuilder(emptyAppFileBuilder, "empty2-domain");
    assertDeploymentSuccess(domainDeploymentListener, "empty2-domain");

    // After three update cycles should have only one deployment failure notification for the broken app
    assertDeploymentFailure(domainDeploymentListener, "domain with spaces");
  }

  @Test
  public void deploysBrokenExplodedDomainOnStartup() throws Exception {
    addExplodedDomainFromBuilder(incompleteDomainFileBuilder);

    startDeployment();

    assertDeploymentFailure(domainDeploymentListener, incompleteDomainFileBuilder.getId());

    // Maintains app dir created
    assertDomainDir(NONE, new String[] {DEFAULT_DOMAIN_NAME, incompleteDomainFileBuilder.getId()}, true);
    final Map<URL, Long> zombieMap = deploymentService.getZombieDomains();
    assertEquals("Wrong number of zombie domains registered.", 1, zombieMap.size());
    final Map.Entry<URL, Long> zombie = zombieMap.entrySet().iterator().next();
    assertEquals("Wrong URL tagged as zombie.", incompleteDomainFileBuilder.getId(),
                 new File(zombie.getKey().getFile()).getParentFile().getName());
    assertTrue("Invalid lastModified value for file URL.", zombie.getValue() != -1);
  }

  @Test
  public void deploysBrokenExplodedDomainAfterStartup() throws Exception {
    startDeployment();

    addExplodedDomainFromBuilder(incompleteDomainFileBuilder);

    assertDeploymentFailure(domainDeploymentListener, incompleteDomainFileBuilder.getId());

    // Maintains app dir created
    assertDomainDir(NONE, new String[] {DEFAULT_DOMAIN_NAME, incompleteDomainFileBuilder.getId()}, true);
    final Map<URL, Long> zombieMap = deploymentService.getZombieDomains();
    assertEquals("Wrong number of zombie apps registered.", 1, zombieMap.size());
    final Map.Entry<URL, Long> zombie = zombieMap.entrySet().iterator().next();
    assertEquals("Wrong URL tagged as zombie.", incompleteDomainFileBuilder.getId(),
                 new File(zombie.getKey().getFile()).getParentFile().getName());
    assertTrue("Invalid lastModified value for file URL.", zombie.getValue() != -1);
  }

  @Test
  public void receivesDomainMuleContextDeploymentNotifications() throws Exception {
    // NOTE: need an integration test like this because DefaultMuleApplication
    // class cannot be unit tested.
    addPackedDomainFromBuilder(sharedDomainFileBuilder);

    startDeployment();

    assertDeploymentSuccess(domainDeploymentListener, sharedDomainFileBuilder.getId());
    assertMuleContextCreated(domainDeploymentListener, sharedDomainFileBuilder.getId());
    assertMuleContextInitialized(domainDeploymentListener, sharedDomainFileBuilder.getId());
    assertMuleContextConfigured(domainDeploymentListener, sharedDomainFileBuilder.getId());
  }

  @Test
  public void undeploysStoppedDomain() throws Exception {
    addPackedDomainFromBuilder(emptyAppFileBuilder);

    startDeployment();

    assertDeploymentSuccess(domainDeploymentListener, emptyAppFileBuilder.getId());
    final Domain domain = findADomain(emptyAppFileBuilder.getId());
    domain.stop();

    deploymentService.undeploy(domain);
  }

  @Test
  public void undeploysDomainRemovingAnchorFile() throws Exception {
    addPackedDomainFromBuilder(emptyAppFileBuilder);

    startDeployment();

    assertDeploymentSuccess(domainDeploymentListener, emptyAppFileBuilder.getId());

    assertTrue("Unable to remove anchor file", removeDomainAnchorFile(emptyAppFileBuilder.getId()));

    assertUndeploymentSuccess(domainDeploymentListener, emptyAppFileBuilder.getId());
  }

  @Test
  public void undeploysDomainAndDomainsApps() throws Exception {
    doDomainUndeployAndVerifyAppsAreUndeployed(() -> {
      Domain domain = findADomain(dummyDomainFileBuilder.getId());
      deploymentService.undeploy(domain);
    });
  }

  @Test
  public void undeploysDomainAndDomainsAppsRemovingAnchorFile() throws Exception {
    doDomainUndeployAndVerifyAppsAreUndeployed(createUndeployDummyDomainAction());
  }

  @Test
  public void undeployDomainDoesNotDeployAllApplications() throws Exception {
    addPackedAppFromBuilder(emptyAppFileBuilder);

    doDomainUndeployAndVerifyAppsAreUndeployed(createUndeployDummyDomainAction());

    assertThat(findApp(emptyAppFileBuilder.getId(), 1), notNullValue());
  }

  @Test(expected = IllegalArgumentException.class)
  public void findDomainApplicationsWillNullDomainFails() {
    deploymentService.findDomainApplications(null);
  }

  @Test
  public void findDomainApplicationsWillNonExistentDomainReturnsEmptyCollection() {
    Collection<Application> domainApplications = deploymentService.findDomainApplications("");
    assertThat(domainApplications, notNullValue());
    assertThat(domainApplications.isEmpty(), is(true));
  }

  @Test
  public void undeploysDomainCompletelyEvenOnStoppingException() throws Exception {
    addPackedDomainFromBuilder(emptyDomainFileBuilder);

    TestDomainFactory testDomainFactory =
        new TestDomainFactory(new DomainClassLoaderFactory(getClass().getClassLoader()),
                              containerClassLoader, artifactClassLoaderManager, serviceManager);
    testDomainFactory.setMuleContextListenerFactory(new DeploymentMuleContextListenerFactory(domainDeploymentListener));
    testDomainFactory.setFailOnStopApplication();

    deploymentService.setDomainFactory(testDomainFactory);
    startDeployment();

    assertDeploymentSuccess(domainDeploymentListener, emptyDomainFileBuilder.getId());

    assertTrue("Unable to remove anchor file", removeDomainAnchorFile(emptyDomainFileBuilder.getId()));

    assertUndeploymentSuccess(domainDeploymentListener, emptyDomainFileBuilder.getId());

    assertAppFolderIsDeleted(emptyDomainFileBuilder.getId());
  }

  @Test
  public void undeploysDomainCompletelyEvenOnDisposingException() throws Exception {
    addPackedDomainFromBuilder(emptyDomainFileBuilder);

    TestDomainFactory testDomainFactory =
        new TestDomainFactory(new DomainClassLoaderFactory(getClass().getClassLoader()),
                              containerClassLoader, artifactClassLoaderManager, serviceManager);
    testDomainFactory.setMuleContextListenerFactory(new DeploymentMuleContextListenerFactory(domainDeploymentListener));
    testDomainFactory.setFailOnDisposeApplication();
    deploymentService.setDomainFactory(testDomainFactory);
    startDeployment();

    assertDeploymentSuccess(domainDeploymentListener, emptyDomainFileBuilder.getId());

    assertTrue("Unable to remove anchor file", removeDomainAnchorFile(emptyDomainFileBuilder.getId()));

    assertUndeploymentSuccess(domainDeploymentListener, emptyDomainFileBuilder.getId());

    assertAppFolderIsDeleted(emptyDomainFileBuilder.getId());
  }

  @Test
  public void deploysIncompleteZipDomainOnStartup() throws Exception {
    addPackedDomainFromBuilder(incompleteDomainFileBuilder);

    startDeployment();

    assertDeploymentFailure(domainDeploymentListener, incompleteDomainFileBuilder.getId());

    // Deploys another app to confirm that DeploymentService has execute the updater thread
    addPackedDomainFromBuilder(emptyDomainFileBuilder);

    assertDeploymentSuccess(domainDeploymentListener, emptyDomainFileBuilder.getId());

    // Check that the failed application folder is still there
    assertDomainFolderIsMaintained(incompleteDomainFileBuilder.getId());
    final Map.Entry<URL, Long> zombie = deploymentService.getZombieDomains().entrySet().iterator().next();
    assertEquals("Wrong URL tagged as zombie.", incompleteDomainFileBuilder.getId(),
                 new File(zombie.getKey().getFile()).getParentFile().getName());
  }

  @Test
  public void deploysIncompleteZipDomainAfterStartup() throws Exception {
    startDeployment();

    addPackedDomainFromBuilder(incompleteDomainFileBuilder);

    assertDeploymentFailure(domainDeploymentListener, incompleteDomainFileBuilder.getId());

    // Deploys another app to confirm that DeploymentService has execute the updater thread
    addPackedDomainFromBuilder(emptyDomainFileBuilder);

    assertDeploymentSuccess(domainDeploymentListener, emptyDomainFileBuilder.getId());

    // Check that the failed application folder is still there
    assertDomainFolderIsMaintained(incompleteDomainFileBuilder.getId());
    final Map.Entry<URL, Long> zombie = deploymentService.getZombieDomains().entrySet().iterator().next();
    assertEquals("Wrong URL tagged as zombie.", incompleteDomainFileBuilder.getId(),
                 new File(zombie.getKey().getFile()).getParentFile().getName());
  }

  @Test
  public void mantainsDomainFolderOnExplodedAppDeploymentError() throws Exception {
    startDeployment();

    addPackedDomainFromBuilder(incompleteDomainFileBuilder);

    assertDeploymentFailure(domainDeploymentListener, incompleteDomainFileBuilder.getId());

    // Deploys another app to confirm that DeploymentService has execute the updater thread
    addPackedDomainFromBuilder(emptyAppFileBuilder);

    assertDeploymentSuccess(domainDeploymentListener, emptyAppFileBuilder.getId());

    // Check that the failed application folder is still there
    assertDomainFolderIsMaintained(incompleteDomainFileBuilder.getId());
    final Map.Entry<URL, Long> zombie = deploymentService.getZombieDomains().entrySet().iterator().next();
    assertEquals("Wrong URL tagged as zombie.", incompleteDomainFileBuilder.getId(),
                 new File(zombie.getKey().getFile()).getParentFile().getName());
  }

  @Test
  public void redeploysZipDomainAfterDeploymentErrorOnStartup() throws Exception {
    addPackedDomainFromBuilder(incompleteDomainFileBuilder);

    startDeployment();

    assertDeploymentFailure(domainDeploymentListener, incompleteDomainFileBuilder.getId());

    // Deploys another app to confirm that DeploymentService has execute the updater thread
    addPackedDomainFromBuilder(emptyAppFileBuilder);

    assertDeploymentSuccess(domainDeploymentListener, emptyAppFileBuilder.getId());

    // Deploys another app to confirm that DeploymentService has execute the updater thread
    addPackedDomainFromBuilder(emptyDomainFileBuilder, incompleteDomainFileBuilder.getZipPath());
    assertDeploymentSuccess(domainDeploymentListener, incompleteDomainFileBuilder.getId());

    assertEquals("Failed domain still appears as zombie after a successful redeploy", 0,
                 deploymentService.getZombieDomains().size());
  }

  @Test
  public void redeploysZipDomainAfterDeploymentErrorAfterStartup() throws Exception {
    startDeployment();

    addPackedDomainFromBuilder(incompleteDomainFileBuilder);

    assertDeploymentFailure(domainDeploymentListener, incompleteDomainFileBuilder.getId());

    // Deploys another app to confirm that DeploymentService has execute the updater thread
    addPackedDomainFromBuilder(dummyDomainFileBuilder);

    assertDeploymentSuccess(domainDeploymentListener, dummyDomainFileBuilder.getId());

    // Deploys another app to confirm that DeploymentService has execute the updater thread
    addPackedDomainFromBuilder(emptyDomainFileBuilder, incompleteDomainFileBuilder.getZipPath());
    assertDeploymentSuccess(domainDeploymentListener, incompleteDomainFileBuilder.getId());

    assertEquals("Failed domain still appears as zombie after a successful redeploy", 0,
                 deploymentService.getZombieDomains().size());
  }

  @Ignore("MULE-6926: flaky test")
  @Test
  public void refreshDomainClassloaderAfterRedeployment() throws Exception {
    startDeployment();

    // Deploy domain and apps and wait until success
    addPackedDomainFromBuilder(sharedDomainFileBuilder);
    addPackedAppFromBuilder(sharedAAppFileBuilder);
    addPackedAppFromBuilder(sharedBAppFileBuilder);
    assertDeploymentSuccess(domainDeploymentListener, sharedDomainFileBuilder.getId());
    assertDeploymentSuccess(applicationDeploymentListener, sharedAAppFileBuilder.getId());
    assertDeploymentSuccess(applicationDeploymentListener, sharedBAppFileBuilder.getId());

    // Ensure resources are registered at domain's registry
    Domain domain = findADomain(sharedDomainFileBuilder.getId());
    assertThat(domain.getMuleContext().getRegistry().get("http-listener-config"), not(is(nullValue())));

    ArtifactClassLoader initialArtifactClassLoader = domain.getArtifactClassLoader();

    reset(domainDeploymentListener);
    reset(applicationDeploymentListener);

    // Force redeployment by touching the domain's config file
    File domainFolder = new File(domainsDir.getPath(), sharedDomainFileBuilder.getId());
    File configFile = new File(domainFolder, sharedDomainFileBuilder.getConfigFile());
    long firstFileTimestamp = configFile.lastModified();
    FileUtils.touch(configFile);
    alterTimestampIfNeeded(configFile, firstFileTimestamp);

    assertUndeploymentSuccess(applicationDeploymentListener, sharedAAppFileBuilder.getId());
    assertUndeploymentSuccess(applicationDeploymentListener, sharedBAppFileBuilder.getId());
    assertUndeploymentSuccess(domainDeploymentListener, sharedDomainFileBuilder.getId());

    assertDeploymentSuccess(domainDeploymentListener, sharedDomainFileBuilder.getId());
    assertDeploymentSuccess(applicationDeploymentListener, sharedAAppFileBuilder.getId());
    assertDeploymentSuccess(applicationDeploymentListener, sharedBAppFileBuilder.getId());

    domain = findADomain(sharedDomainFileBuilder.getId());
    ArtifactClassLoader artifactClassLoaderAfterRedeployment = domain.getArtifactClassLoader();

    // Ensure that after redeployment the domain's class loader has changed
    assertThat(artifactClassLoaderAfterRedeployment, not(sameInstance(initialArtifactClassLoader)));

    // Undeploy domain and apps
    removeAppAnchorFile(sharedAAppFileBuilder.getId());
    removeAppAnchorFile(sharedBAppFileBuilder.getId());
    removeDomainAnchorFile(sharedDomainFileBuilder.getId());
    assertUndeploymentSuccess(applicationDeploymentListener, sharedAAppFileBuilder.getId());
    assertUndeploymentSuccess(applicationDeploymentListener, sharedBAppFileBuilder.getId());
    assertUndeploymentSuccess(domainDeploymentListener, sharedDomainFileBuilder.getId());
  }

  @Test
  public void redeploysInvalidZipDomainAfterSuccessfulDeploymentOnStartup() throws Exception {
    addPackedDomainFromBuilder(emptyDomainFileBuilder);

    startDeployment();

    assertDeploymentSuccess(domainDeploymentListener, emptyDomainFileBuilder.getId());

    addPackedDomainFromBuilder(incompleteDomainFileBuilder, emptyDomainFileBuilder.getZipPath());

    assertDeploymentFailure(domainDeploymentListener, emptyDomainFileBuilder.getId());

    final Map.Entry<URL, Long> zombie = deploymentService.getZombieDomains().entrySet().iterator().next();
    assertEquals("Wrong URL tagged as zombie.", emptyDomainFileBuilder.getId(),
                 new File(zombie.getKey().getFile()).getParentFile().getName());
  }

  @Test
  public void redeploysInvalidZipDomainAfterSuccessfulDeploymentAfterStartup() throws Exception {
    startDeployment();

    addPackedDomainFromBuilder(emptyDomainFileBuilder);
    assertDeploymentSuccess(domainDeploymentListener, emptyDomainFileBuilder.getId());

    addPackedDomainFromBuilder(incompleteDomainFileBuilder, emptyDomainFileBuilder.getZipPath());
    assertDeploymentFailure(domainDeploymentListener, emptyDomainFileBuilder.getId());

    final Map.Entry<URL, Long> zombie = deploymentService.getZombieDomains().entrySet().iterator().next();
    assertEquals("Wrong URL tagged as zombie.", emptyDomainFileBuilder.getId(),
                 new File(zombie.getKey().getFile()).getParentFile().getName());
  }

  @Test
  public void redeploysInvalidZipDomainAfterFailedDeploymentOnStartup() throws Exception {
    addPackedDomainFromBuilder(incompleteDomainFileBuilder);

    startDeployment();

    assertDeploymentFailure(domainDeploymentListener, incompleteDomainFileBuilder.getId());

    reset(domainDeploymentListener);

    addPackedDomainFromBuilder(incompleteDomainFileBuilder);

    assertDeploymentFailure(domainDeploymentListener, incompleteDomainFileBuilder.getId());

    final Map.Entry<URL, Long> zombie = deploymentService.getZombieDomains().entrySet().iterator().next();
    assertEquals("Wrong URL tagged as zombie.", incompleteDomainFileBuilder.getId(),
                 new File(zombie.getKey().getFile()).getParentFile().getName());
  }

  @Test
  public void redeploysInvalidZipDomainAfterFailedDeploymentAfterStartup() throws Exception {
    startDeployment();

    addPackedDomainFromBuilder(incompleteDomainFileBuilder);
    assertDeploymentFailure(domainDeploymentListener, incompleteDomainFileBuilder.getId());

    reset(domainDeploymentListener);

    addPackedDomainFromBuilder(incompleteDomainFileBuilder);
    assertDeploymentFailure(domainDeploymentListener, incompleteDomainFileBuilder.getId());

    final Map.Entry<URL, Long> zombie = deploymentService.getZombieDomains().entrySet().iterator().next();
    assertEquals("Wrong URL tagged as zombie.", incompleteDomainFileBuilder.getId(),
                 new File(zombie.getKey().getFile()).getParentFile().getName());
  }

  @Test
  public void redeploysExplodedDomainAfterDeploymentError() throws Exception {
    startDeployment();

    addPackedDomainFromBuilder(incompleteDomainFileBuilder);

    assertDeploymentFailure(domainDeploymentListener, incompleteDomainFileBuilder.getId());

    // Deploys another app to confirm that DeploymentService has execute the updater thread
    addPackedDomainFromBuilder(emptyAppFileBuilder);

    assertDeploymentSuccess(domainDeploymentListener, emptyAppFileBuilder.getId());

    // Redeploys a fixed version for incompleteDomain
    addExplodedDomainFromBuilder(emptyDomainFileBuilder, incompleteDomainFileBuilder.getId());

    assertDeploymentSuccess(domainDeploymentListener, incompleteDomainFileBuilder.getId());
    assertEquals("Failed domain still appears as zombie after a successful redeploy", 0,
                 deploymentService.getZombieDomains().size());
  }

  /**
   * Copies the artifact file (zip) of the applicationPluginFileBuilder to the container application plugins directory so the
   * given plugin would be treated as container application plugin.
   *
   * @param artifactPluginFileBuilder
   * @throws Exception
   */
  private void installContainerPlugin(ArtifactPluginFileBuilder artifactPluginFileBuilder) throws Exception {
    copyFileToContainerPluginFolder(artifactPluginFileBuilder.getArtifactFile(), artifactPluginFileBuilder.getId() + ".zip");
  }

  private void installContainerPluginExpanded(ArtifactPluginFileBuilder artifactPluginFileBuilder) throws Exception {
    final File pluginFolderExpanded = new File(getContainerAppPluginsFolder(), separator + artifactPluginFileBuilder.getId());
    FileUtils.unzip(artifactPluginFileBuilder.getArtifactFile(), pluginFolderExpanded);
  }

  private void copyFileToContainerPluginFolder(File sourceFile, String targetFileName) throws IOException {
    copyFile(sourceFile, new File(containerAppPluginsDir, targetFileName));
  }

  private Action createUndeployDummyDomainAction() {
    return () -> removeDomainAnchorFile(dummyDomainFileBuilder.getId());
  }

  private void doDomainUndeployAndVerifyAppsAreUndeployed(Action undeployAction) throws Exception {
    startDeployment();

    addPackedDomainFromBuilder(dummyDomainFileBuilder);

    assertDeploymentSuccess(domainDeploymentListener, dummyDomainFileBuilder.getId());

    addPackedAppFromBuilder(dummyDomainApp1FileBuilder);
    assertApplicationDeploymentSuccess(applicationDeploymentListener, dummyDomainApp1FileBuilder.getId());

    addPackedAppFromBuilder(dummyDomainApp2FileBuilder);
    assertApplicationDeploymentSuccess(applicationDeploymentListener, dummyDomainApp2FileBuilder.getId());

    deploymentService.getLock().lock();
    try {
      undeployAction.perform();
    } finally {
      deploymentService.getLock().unlock();
    }

    assertUndeploymentSuccess(applicationDeploymentListener, dummyDomainApp1FileBuilder.getId());
    assertUndeploymentSuccess(applicationDeploymentListener, dummyDomainApp2FileBuilder.getId());
    assertUndeploymentSuccess(domainDeploymentListener, dummyDomainFileBuilder.getId());
  }

  @Test
  public void redeploysFixedDomainAfterBrokenExplodedDomainOnStartup() throws Exception {
    addExplodedDomainFromBuilder(incompleteDomainFileBuilder);

    startDeployment();

    doRedeployFixedDomainAfterBrokenDomain();
  }

  @Test
  public void redeploysFixedDomainAfterBrokenExplodedDomainAfterStartup() throws Exception {
    startDeployment();

    addExplodedDomainFromBuilder(incompleteDomainFileBuilder);

    doRedeployFixedDomainAfterBrokenDomain();
  }

  @Test
  public void redeploysDomainAndItsApplications() throws Exception {
    addExplodedDomainFromBuilder(dummyDomainFileBuilder, dummyDomainFileBuilder.getId());

    addExplodedAppFromBuilder(dummyDomainApp1FileBuilder, dummyDomainApp1FileBuilder.getId());
    addExplodedAppFromBuilder(dummyDomainApp2FileBuilder, dummyDomainApp2FileBuilder.getId());

    startDeployment();

    assertDeploymentSuccess(domainDeploymentListener, dummyDomainFileBuilder.getId());

    assertApplicationDeploymentSuccess(applicationDeploymentListener, dummyDomainApp1FileBuilder.getId());
    assertApplicationDeploymentSuccess(applicationDeploymentListener, dummyDomainApp2FileBuilder.getId());

    reset(domainDeploymentListener);
    reset(applicationDeploymentListener);

    doRedeployDummyDomainByChangingConfigFileWithGoodOne();

    assertDeploymentSuccess(domainDeploymentListener, dummyDomainFileBuilder.getId());

    assertApplicationDeploymentSuccess(applicationDeploymentListener, dummyDomainApp1FileBuilder.getId());
    assertApplicationDeploymentSuccess(applicationDeploymentListener, dummyDomainApp2FileBuilder.getId());
  }

  @Test
  public void doesNotRedeployDomainWithRedeploymentDisabled() throws Exception {
    addExplodedDomainFromBuilder(dummyUndeployableDomainFileBuilder, dummyUndeployableDomainFileBuilder.getId());
    addPackedAppFromBuilder(emptyAppFileBuilder, "empty-app.zip");

    startDeployment();

    assertDeploymentSuccess(domainDeploymentListener, dummyUndeployableDomainFileBuilder.getId());
    assertApplicationDeploymentSuccess(applicationDeploymentListener, emptyAppFileBuilder.getId());

    reset(domainDeploymentListener);
    reset(applicationDeploymentListener);

    // change domain and app since once the app redeploys we can check the domain did not
    doRedeployDomainByChangingConfigFileWithGoodOne(dummyUndeployableDomainFileBuilder);
    doRedeployAppByChangingConfigFileWithGoodOne(emptyAppFileBuilder.getDeployedPath());

    assertDeploymentSuccess(applicationDeploymentListener, emptyAppFileBuilder.getId());
    verify(domainDeploymentListener, never()).onDeploymentSuccess(dummyUndeployableDomainFileBuilder.getId());
  }

  @Test
  public void redeploysDomainAndFails() throws Exception {
    addExplodedDomainFromBuilder(dummyDomainFileBuilder, dummyDomainFileBuilder.getId());

    addExplodedAppFromBuilder(dummyDomainApp1FileBuilder, dummyDomainApp1FileBuilder.getId());
    addExplodedAppFromBuilder(dummyDomainApp2FileBuilder, dummyDomainApp2FileBuilder.getId());

    startDeployment();

    assertDeploymentSuccess(domainDeploymentListener, dummyDomainFileBuilder.getId());

    assertApplicationDeploymentSuccess(applicationDeploymentListener, dummyDomainApp1FileBuilder.getId());
    assertApplicationDeploymentSuccess(applicationDeploymentListener, dummyDomainApp2FileBuilder.getId());

    reset(domainDeploymentListener);
    reset(applicationDeploymentListener);

    doRedeployDummyDomainByChangingConfigFileWithBadOne();

    assertDeploymentFailure(domainDeploymentListener, dummyDomainFileBuilder.getId());

    assertNoDeploymentInvoked(applicationDeploymentListener);
  }

  @Test
  public void redeploysDomainWithOneApplicationFailedOnFirstDeployment() throws Exception {
    startDeployment();

    addExplodedDomainFromBuilder(dummyDomainFileBuilder, dummyDomainFileBuilder.getId());

    addExplodedAppFromBuilder(dummyDomainApp1FileBuilder, dummyDomainApp1FileBuilder.getId());
    addExplodedAppFromBuilder(dummyDomainApp2FileBuilder, dummyDomainApp2FileBuilder.getId());
    addExplodedAppFromBuilder(dummyDomainApp3FileBuilder, dummyDomainApp3FileBuilder.getId());

    assertDeploymentSuccess(domainDeploymentListener, dummyDomainFileBuilder.getId());

    assertApplicationDeploymentSuccess(applicationDeploymentListener, dummyDomainApp1FileBuilder.getId());
    assertApplicationDeploymentSuccess(applicationDeploymentListener, dummyDomainApp2FileBuilder.getId());
    assertDeploymentFailure(applicationDeploymentListener, dummyDomainApp3FileBuilder.getId());

    reset(domainDeploymentListener);
    reset(applicationDeploymentListener);

    deploymentService.getLock().lock();
    try {
      doRedeployDummyDomainByChangingConfigFileWithGoodOne();
      doRedeployAppByChangingConfigFileWithGoodOne(dummyDomainApp3FileBuilder.getDeployedPath());
    } finally {
      deploymentService.getLock().unlock();
    }

    assertDeploymentSuccess(domainDeploymentListener, dummyDomainFileBuilder.getId());

    assertApplicationDeploymentSuccess(applicationDeploymentListener, dummyDomainApp1FileBuilder.getId());
    assertApplicationDeploymentSuccess(applicationDeploymentListener, dummyDomainApp2FileBuilder.getId());
    assertDeploymentSuccess(applicationDeploymentListener, dummyDomainApp3FileBuilder.getId());
  }

  @Test
  public void redeploysDomainWithOneApplicationFailedAfterRedeployment() throws Exception {
    startDeployment();

    addExplodedDomainFromBuilder(dummyDomainFileBuilder, dummyDomainFileBuilder.getId());

    addExplodedAppFromBuilder(dummyDomainApp1FileBuilder, dummyDomainApp1FileBuilder.getId());
    addExplodedAppFromBuilder(dummyDomainApp2FileBuilder, dummyDomainApp2FileBuilder.getId());

    assertDeploymentSuccess(domainDeploymentListener, dummyDomainFileBuilder.getId());

    assertApplicationDeploymentSuccess(applicationDeploymentListener, dummyDomainApp1FileBuilder.getId());
    assertApplicationDeploymentSuccess(applicationDeploymentListener, dummyDomainApp2FileBuilder.getId());

    reset(domainDeploymentListener);
    reset(applicationDeploymentListener);

    deploymentService.getLock().lock();
    try {
      doRedeployDummyDomainByChangingConfigFileWithGoodOne();
      doRedeployAppByChangingConfigFileWithBadOne(dummyDomainApp2FileBuilder.getDeployedPath());
    } finally {
      deploymentService.getLock().unlock();
    }

    assertDeploymentSuccess(domainDeploymentListener, dummyDomainFileBuilder.getId());

    assertApplicationDeploymentSuccess(applicationDeploymentListener, dummyDomainApp1FileBuilder.getId());
    assertDeploymentFailure(applicationDeploymentListener, dummyDomainApp2FileBuilder.getId());
  }

  @Test
  public void deployFailsWhenMissingFile() throws Exception {
    startDeployment();

    addExplodedAppFromBuilder(emptyAppFileBuilder);

    assertApplicationDeploymentSuccess(applicationDeploymentListener, emptyAppFileBuilder.getId());
    reset(applicationDeploymentListener);

    File originalConfigFile = new File(appsDir + "/" + emptyAppFileBuilder.getDeployedPath(), MULE_CONFIG_XML_FILE);
    FileUtils.forceDelete(originalConfigFile);

    assertDeploymentFailure(applicationDeploymentListener, emptyAppFileBuilder.getId());
    assertStatus(emptyAppFileBuilder.getId(), ApplicationStatus.DEPLOYMENT_FAILED);
  }

  @Test
  public void synchronizesDomainDeployFromClient() throws Exception {
    final Action action = () -> deploymentService.deployDomain(dummyDomainFileBuilder.getArtifactFile().toURI().toURL());

    final Action assertAction = () -> verify(domainDeploymentListener, never()).onDeploymentStart(dummyDomainFileBuilder.getId());
    doSynchronizedDomainDeploymentActionTest(action, assertAction);
  }

  @Test
  public void synchronizesDomainUndeployFromClient() throws Exception {
    final Action action = () -> deploymentService.undeployDomain(emptyDomainFileBuilder.getId());

    final Action assertAction =
        () -> verify(domainDeploymentListener, never()).onUndeploymentStart(emptyDomainFileBuilder.getId());
    doSynchronizedDomainDeploymentActionTest(action, assertAction);
  }

  @Test
  public void synchronizesDomainRedeployFromClient() throws Exception {
    final Action action = () -> {
      // Clears notification from first deployment
      reset(domainDeploymentListener);
      deploymentService.redeployDomain(emptyDomainFileBuilder.getId());
    };

    final Action assertAction = () -> verify(domainDeploymentListener, never()).onDeploymentStart(emptyDomainFileBuilder.getId());
    doSynchronizedDomainDeploymentActionTest(action, assertAction);
  }

  private void doSynchronizedDomainDeploymentActionTest(final Action deploymentAction, final Action assertAction)
      throws Exception {
    addPackedDomainFromBuilder(emptyDomainFileBuilder);
    final DeploymentListener domainDeploymentListener = this.domainDeploymentListener;
    final String artifactId = emptyDomainFileBuilder.getId();

    doSynchronizedArtifactDeploymentActionTest(deploymentAction, assertAction, domainDeploymentListener, artifactId);
  }

  private void doSynchronizedArtifactDeploymentActionTest(final Action deploymentAction, final Action assertAction,
                                                          DeploymentListener domainDeploymentListener, String artifactId) {
    Thread deploymentServiceThread = new Thread(() -> {
      try {
        startDeployment();
      } catch (MuleException e) {
        throw new RuntimeException("Unable to start deployment service");
      }
    });

    final boolean[] deployedFromClient = new boolean[1];

    doAnswer(invocation -> {

      Thread deploymentClientThread = new Thread(() -> {
        try {
          deploymentAction.perform();
        } catch (Exception e) {
          // Ignore
        }
      });

      deploymentClientThread.start();
      deploymentClientThread.join();
      try {
        assertAction.perform();
      } catch (AssertionError e) {
        deployedFromClient[0] = true;
      }

      return null;
    }).when(domainDeploymentListener).onDeploymentStart(artifactId);

    deploymentServiceThread.start();

    assertDeploymentSuccess(domainDeploymentListener, artifactId);

    assertFalse("Able to perform a deployment action while another deployment operation was in progress", deployedFromClient[0]);
  }

  private void doRedeployAppByChangingConfigFileWithGoodOne(String applicationPath) throws Exception {
    changeConfigFile(applicationPath, EMPTY_APP_CONFIG_XML);
  }

  private void doRedeployAppByChangingConfigFileWithBadOne(String applicationPath) throws Exception {
    changeConfigFile(applicationPath, BAD_APP_CONFIG_XML);
  }

  private void changeConfigFile(String applicationPath, String configFile) throws Exception {
    File originalConfigFile = new File(new File(appsDir, applicationPath), MULE_CONFIG_XML_FILE);
    assertThat("Original config file doe snot exists: " + originalConfigFile, originalConfigFile.exists(), is(true));
    URL url = getClass().getResource(configFile);
    File newConfigFile = new File(url.toURI());
    copyFile(newConfigFile, originalConfigFile);
  }

  private void doRedeployDummyDomainByChangingConfigFileWithGoodOne() throws URISyntaxException, IOException {
    doRedeployDomainByChangingConfigFile("/empty-domain-config.xml", dummyDomainFileBuilder);
  }

  private void doRedeployDomainByChangingConfigFileWithGoodOne(DomainFileBuilder domain) throws URISyntaxException, IOException {
    doRedeployDomainByChangingConfigFile("/empty-domain-config.xml", domain);
  }

  private void doRedeployDummyDomainByChangingConfigFileWithBadOne() throws URISyntaxException, IOException {
    doRedeployDomainByChangingConfigFile("/bad-domain-config.xml", dummyDomainFileBuilder);
  }

  private void doRedeployDomainByChangingConfigFile(String configFile, DomainFileBuilder domain)
      throws URISyntaxException, IOException {
    File originalConfigFile = new File(new File(domainsDir, domain.getDeployedPath()), domain.getConfigFile());
    assertThat("Cannot find domain config file: " + originalConfigFile, originalConfigFile.exists(), is(true));
    URL url = getClass().getResource(configFile);
    File newConfigFile = new File(url.toURI());
    copyFile(newConfigFile, originalConfigFile);
  }

  private void doRedeployFixedDomainAfterBrokenDomain() throws Exception {
    assertDeploymentFailure(domainDeploymentListener, "incompleteDomain");

    reset(domainDeploymentListener);

    File originalConfigFile = new File(domainsDir + "/incompleteDomain", "mule-domain-config.xml");
    URL url = getClass().getResource("/empty-domain-config.xml");
    File newConfigFile = new File(url.toURI());
    copyFile(newConfigFile, originalConfigFile);
    assertDeploymentSuccess(domainDeploymentListener, "incompleteDomain");

    addPackedDomainFromBuilder(emptyAppFileBuilder);
    assertDeploymentSuccess(domainDeploymentListener, emptyAppFileBuilder.getId());

    // Check that the failed application folder is still there
    assertDomainFolderIsMaintained("incompleteDomain");
  }

  public void doBrokenAppArchiveTest() throws Exception {
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
    final Map<URL, Long> zombieMap = deploymentService.getZombieApplications();
    assertEquals("Wrong number of zombie apps registered.", 1, zombieMap.size());
    final Map.Entry<URL, Long> zombie = zombieMap.entrySet().iterator().next();
    assertEquals("Wrong URL tagged as zombie.", brokenAppFileBuilder.getDeployedPath(),
                 new File(zombie.getKey().getFile()).getName());
    assertTrue("Invalid lastModified value for file URL.", zombie.getValue() != -1);

    // Checks that the invalid zip was not deployed again
    try {
      assertDeploymentFailure(applicationDeploymentListener, "broken-app.zip");
      fail("Install was invoked again for the broken application file");
    } catch (AssertionError expected) {
    }
  }

  private void deploysAppAndVerifyAnchorFileIsCreatedAfterDeploymentEnds(Action deployArtifactAction) throws Exception {
    Action verifyAnchorFileDoesNotExistsAction = () -> assertApplicationAnchorFileDoesNotExists(waitAppFileBuilder.getId());
    Action verifyDeploymentSuccessfulAction =
        () -> assertApplicationDeploymentSuccess(applicationDeploymentListener, waitAppFileBuilder.getId());
    Action verifyAnchorFileExistsAction = () -> assertApplicationAnchorFileExists(waitAppFileBuilder.getId());
    deploysArtifactAndVerifyAnchorFileCreatedWhenDeploymentEnds(deployArtifactAction, verifyAnchorFileDoesNotExistsAction,
                                                                verifyDeploymentSuccessfulAction, verifyAnchorFileExistsAction);
  }

  private void deploysDomainAndVerifyAnchorFileIsCreatedAfterDeploymentEnds(Action deployArtifactAction) throws Exception {
    Action verifyAnchorFileDoesNotExists = () -> assertDomainAnchorFileDoesNotExists(waitDomainFileBuilder.getId());
    Action verifyDeploymentSuccessful = () -> assertDeploymentSuccess(domainDeploymentListener, waitDomainFileBuilder.getId());
    Action verifyAnchorFileExists = () -> assertDomainAnchorFileExists(waitDomainFileBuilder.getId());
    deploysArtifactAndVerifyAnchorFileCreatedWhenDeploymentEnds(deployArtifactAction, verifyAnchorFileDoesNotExists,
                                                                verifyDeploymentSuccessful, verifyAnchorFileExists);
  }

  private void deploysArtifactAndVerifyAnchorFileCreatedWhenDeploymentEnds(Action deployArtifactAction,
                                                                           Action verifyAnchorFileDoesNotExistsAction,
                                                                           Action verifyDeploymentSuccessfulAction,
                                                                           Action verifyAnchorFileExistsAction)
      throws Exception {
    WaitComponent.reset();
    startDeployment();
    deployArtifactAction.perform();
    try {
      if (!WaitComponent.componentInitializedLatch.await(DEPLOYMENT_TIMEOUT, TimeUnit.MILLISECONDS)) {
        fail("WaitComponent should be initilaized already. Probably app deployment failed");
      }
      verifyAnchorFileDoesNotExistsAction.perform();
    } finally {
      WaitComponent.waitLatch.release();
    }
    verifyDeploymentSuccessfulAction.perform();
    verifyAnchorFileExistsAction.perform();
  }

  private void startDeployment() throws MuleException {
    serviceManager.start();
    deploymentService.start();
  }

  private void assertApplicationDeploymentSuccess(DeploymentListener listener, String artifactName) {
    assertDeploymentSuccess(listener, artifactName);
    assertStatus(artifactName, ApplicationStatus.STARTED);
  }

  private void assertDeploymentSuccess(final DeploymentListener listener, final String artifactName) {
    Prober prober = new PollingProber(DEPLOYMENT_TIMEOUT, 100);
    prober.check(new JUnitProbe() {

      @Override
      protected boolean test() throws Exception {
        verify(listener, times(1)).onDeploymentSuccess(artifactName);
        return true;
      }

      @Override
      public String describeFailure() {
        return "Failed to deploy application: " + artifactName + System.lineSeparator() + super.describeFailure();
      }
    });
  }

  private void assertMuleContextCreated(final DeploymentListener listener, final String appName) {
    Prober prober = new PollingProber(DEPLOYMENT_TIMEOUT, 100);
    prober.check(new JUnitProbe() {

      @Override
      public boolean test() {
        verify(listener, times(1)).onMuleContextCreated(eq(appName), any(MuleContext.class));
        return true;
      }

      @Override
      public String describeFailure() {
        return String.format("Did not received notification '%s' for app '%s'", "onMuleContextCreated", appName)
            + System.lineSeparator() + super.describeFailure();
      }
    });
  }

  private void assertMuleContextInitialized(final DeploymentListener listener, final String appName) {
    Prober prober = new PollingProber(DEPLOYMENT_TIMEOUT, 100);
    prober.check(new JUnitProbe() {

      @Override
      public boolean test() {
        verify(listener, times(1)).onMuleContextInitialised(eq(appName), any(MuleContext.class));
        return true;
      }

      @Override
      public String describeFailure() {
        return String.format("Did not received notification '%s' for app '%s'", "onMuleContextInitialised", appName)
            + System.lineSeparator() + super.describeFailure();
      }
    });
  }

  private void assertMuleContextConfigured(final DeploymentListener listener, final String appName) {
    Prober prober = new PollingProber(DEPLOYMENT_TIMEOUT, 100);
    prober.check(new JUnitProbe() {

      @Override
      public boolean test() {
        verify(listener, times(1)).onMuleContextConfigured(eq(appName), any(MuleContext.class));
        return true;
      }

      @Override
      public String describeFailure() {
        return String.format("Did not received notification '%s' for app '%s'", "onMuleContextConfigured", appName)
            + System.lineSeparator() + super.describeFailure();
      }
    });
  }

  private void assertUndeploymentSuccess(final DeploymentListener listener, final String appName) {
    Prober prober = new PollingProber(DEPLOYMENT_TIMEOUT, 100);
    prober.check(new JUnitProbe() {

      @Override
      public boolean test() {
        verify(listener, times(1)).onUndeploymentSuccess(appName);
        return true;
      }

      @Override
      public String describeFailure() {
        return "Failed to undeploy artifact: " + appName + System.lineSeparator() + super.describeFailure();
      }
    });
  }

  private MuleRegistry getMuleRegistry(Application app) {
    return withContextClassLoader(app.getArtifactClassLoader().getClassLoader(), () -> app.getMuleContext().getRegistry());
  }

  private void assertDeploymentFailure(final DeploymentListener listener, final String artifactName) {
    assertDeploymentFailure(listener, artifactName, times(1));
  }

  private void assertStatus(String appName, ApplicationStatus status) {
    assertStatus(appName, status, -1);
  }

  private void assertStatus(String appName, ApplicationStatus status, int expectedApps) {
    Application app = findApp(appName, expectedApps);
    assertThat(app, notNullValue());
    assertStatus(app, status);
  }

  private void assertStatus(final Application application, final ApplicationStatus status) {
    Prober prober = new PollingProber(DEPLOYMENT_TIMEOUT, 100);
    prober.check(new JUnitProbe() {

      @Override
      protected boolean test() throws Exception {
        assertThat(application.getStatus(), is(status));
        return true;
      }

      @Override
      public String describeFailure() {
        return String.format("Application %s was expected to be in status %s but was %s instead", application.getArtifactName(),
                             status.name(), application.getStatus().name());
      }
    });

  }

  private void assertDeploymentFailure(final DeploymentListener listener, final String artifactName,
                                       final VerificationMode mode) {
    Prober prober = new PollingProber(DEPLOYMENT_TIMEOUT, 100);
    prober.check(new JUnitProbe() {

      @Override
      public boolean test() {
        verify(listener, mode).onDeploymentFailure(eq(artifactName), any(Throwable.class));
        return true;
      }

      @Override
      public String describeFailure() {
        return "Application deployment was supposed to fail for: " + artifactName + super.describeFailure();
      }
    });
  }

  private void assertNoDeploymentInvoked(final DeploymentListener deploymentListener) {
    //  TODO(pablo.kraan): look for a better way to test this
    boolean invoked;
    Prober prober = new PollingProber(DeploymentDirectoryWatcher.DEFAULT_CHANGES_CHECK_INTERVAL_MS * 2, 100);
    try {
      prober.check(new Probe() {

        @Override
        public boolean isSatisfied() {
          try {
            verify(deploymentListener, times(1)).onDeploymentStart(any(String.class));
            return true;
          } catch (AssertionError e) {
            return false;
          }
        }

        @Override
        public String describeFailure() {
          return "No deployment has started";
        }
      });

      invoked = true;
    } catch (AssertionError e) {
      invoked = false;
    }

    assertFalse("A deployment was started", invoked);
  }

  /**
   * Find a deployed app, performing some basic assertions.
   */
  private Application findApp(final String appName, int totalAppsExpected) {
    // list all apps to validate total count
    final List<Application> apps = deploymentService.getApplications();
    assertNotNull(apps);

    if (totalAppsExpected >= 0) {
      assertEquals(totalAppsExpected, apps.size());
    }

    final Application app = deploymentService.findApplication(appName);
    assertNotNull(app);
    return app;
  }

  private DefaultMuleDomain createDefaultDomain() {
    return new DefaultMuleDomain(new DomainDescriptor(DEFAULT_DOMAIN_NAME),
                                 new DomainClassLoaderFactory(getClass().getClassLoader())
                                     .create("domain/" + DEFAULT_DOMAIN_NAME, containerClassLoader,
                                             new DomainDescriptor(DEFAULT_DOMAIN_NAME), emptyList()),
                                 artifactClassLoaderManager, serviceManager);
  }

  /**
   * Finds a deployed domain
   */
  private Domain findADomain(final String domainName) {
    final Domain domain = deploymentService.findDomain(domainName);
    assertNotNull(domain);
    return domain;
  }

  private void assertAppsDir(String[] expectedZips, String[] expectedApps, boolean performValidation) {
    assertArtifactDir(appsDir, expectedZips, expectedApps, performValidation);
  }

  /**
   * Asserts that the core application plugins are expanded and match the number of expected plugins to be expanded
   * 
   * @param expectedPlugins
   */
  private void assertContainerAppPluginExplodedDir(String[] expectedPlugins) {
    final String[] actualArtifactPlugins = containerAppPluginsDir.list(DIRECTORY);
    assertTrue("Invalid Mule core application plugins exploded",
               isEqualCollection(asList(expectedPlugins), asList(actualArtifactPlugins)));
  }

  private void assertDomainDir(String[] expectedZips, String[] expectedDomains, boolean performValidation) {
    assertArtifactDir(domainsDir, expectedZips, expectedDomains, performValidation);
  }

  private void assertArtifactDir(File artifactDir, String[] expectedZips, String[] expectedArtifacts, boolean performValidation) {
    final String[] actualZips = artifactDir.list(MuleDeploymentService.ZIP_ARTIFACT_FILTER);
    if (performValidation) {
      assertArrayEquals("Invalid Mule artifact archives set", expectedZips, actualZips);
    }
    final String[] actualArtifacts = artifactDir.list(DIRECTORY);
    if (performValidation) {
      assertTrue("Invalid Mule exploded artifact set",
                 isEqualCollection(asList(expectedArtifacts), asList(actualArtifacts)));
    }
  }

  private void addPackedAppFromBuilder(TestArtifactDescriptor artifactFileBuilder) throws Exception {
    addPackedAppFromBuilder(artifactFileBuilder, null);
  }

  private void addPackedAppFromBuilder(TestArtifactDescriptor artifactFileBuilder, String targetName) throws Exception {
    addPackedAppArchive(artifactFileBuilder, targetName);
  }

  private void addPackedDomainFromBuilder(TestArtifactDescriptor artifactFileBuilder) throws Exception {
    addPackedDomainFromBuilder(artifactFileBuilder, null);
  }

  private void addPackedDomainFromBuilder(TestArtifactDescriptor artifactFileBuilder, String targetName) throws Exception {
    addArchive(domainsDir, artifactFileBuilder.getArtifactFile().toURI().toURL(), targetName);
  }

  /**
   * Copies a given app archive with a given target name to the apps folder for deployment
   */
  private void addPackedAppArchive(TestArtifactDescriptor artifactFileBuilder, String targetFile) throws Exception {
    addArchive(appsDir, artifactFileBuilder.getArtifactFile().toURI().toURL(), targetFile);
  }

  private void addArchive(File outputDir, URL url, String targetFile) throws Exception {
    ReentrantLock lock = deploymentService.getLock();

    lock.lock();
    try {
      // copy is not atomic, copy to a temp file and rename instead (rename is atomic)
      final String tempFileName = new File((targetFile == null ? url.getFile() : targetFile) + ".part").getName();
      final File tempFile = new File(outputDir, tempFileName);
      FileUtils.copyURLToFile(url, tempFile);
      final File destFile = new File(StringUtils.removeEnd(tempFile.getAbsolutePath(), ".part"));
      tempFile.renameTo(destFile);
      assertThat("File does not exists: " + destFile.getAbsolutePath(), destFile.exists(), is(true));
    } finally {
      lock.unlock();
    }
  }

  private void addExplodedAppFromBuilder(TestArtifactDescriptor artifactFileBuilder) throws Exception {
    addExplodedAppFromBuilder(artifactFileBuilder, null);
  }

  private void addExplodedAppFromBuilder(TestArtifactDescriptor artifactFileBuilder, String appName) throws Exception {
    addExplodedArtifactFromBuilder(artifactFileBuilder, appName, MULE_CONFIG_XML_FILE, appsDir);
  }

  private void addExplodedDomainFromBuilder(TestArtifactDescriptor artifactFileBuilder) throws Exception {
    addExplodedDomainFromBuilder(artifactFileBuilder, null);
  }

  private void addExplodedDomainFromBuilder(TestArtifactDescriptor artifactFileBuilder, String appName) throws Exception {
    addExplodedArtifactFromBuilder(artifactFileBuilder, appName, MULE_DOMAIN_CONFIG_XML_FILE, domainsDir);
  }

  private void addExplodedArtifactFromBuilder(TestArtifactDescriptor artifactFileBuilder, String artifactName,
                                              String configFileName, File destinationDir)
      throws Exception {
    addExplodedArtifactFromUrl(artifactFileBuilder.getArtifactFile().toURI().toURL(), artifactName, configFileName,
                               destinationDir);
  }

  private void addExplodedArtifactFromUrl(URL resource, String artifactName, String configFileName, File destinationDir)
      throws Exception, URISyntaxException {
    assertNotNull("Resource URL cannot be null", resource);

    String artifactFolder = artifactName;
    if (artifactFolder == null) {
      File file = new File(resource.getFile());
      int index = file.getName().lastIndexOf(".");

      if (index > 0) {
        artifactFolder = file.getName().substring(0, index);
      } else {
        artifactFolder = file.getName();
      }
    }

    addExplodedArtifact(resource, artifactFolder, configFileName, destinationDir);
  }

  /**
   * Copies a given app archive with a given target name to the apps folder for deployment
   */
  private void addExplodedArtifact(URL url, String artifactName, String configFileName, File destinationDir)
      throws Exception, URISyntaxException {
    ReentrantLock lock = deploymentService.getLock();

    lock.lock();
    try {
      File tempFolder = new File(muleHome, artifactName);
      FileUtils.unzip(new File(url.toURI()), tempFolder);

      // Under some platforms, file.lastModified is managed at second level, not milliseconds.
      // Need to update the config file lastModified ere to ensure that is different from previous value
      File configFile = new File(tempFolder, configFileName);
      if (configFile.exists()) {
        configFile.setLastModified(System.currentTimeMillis() + FILE_TIMESTAMP_PRECISION_MILLIS);
      }

      File appFolder = new File(destinationDir, artifactName);

      if (appFolder.exists()) {
        FileUtils.deleteTree(appFolder);
      }

      FileUtils.moveDirectory(tempFolder, appFolder);
    } finally {
      lock.unlock();
    }
  }

  /**
   * Removes a given application anchor file in order to start application undeployment
   *
   * @param appName name of application to undeployArtifact
   * @return true if anchor file was deleted, false otherwise
   */
  private boolean removeAppAnchorFile(String appName) {
    File anchorFile = getArtifactAnchorFile(appName, appsDir);
    return anchorFile.delete();
  }

  /**
   * Removes a given domain anchor file in order to start application undeployment
   *
   * @param domainName name of application to undeployArtifact
   * @return true if anchor file was deleted, false otherwise
   */
  private boolean removeDomainAnchorFile(String domainName) {
    File anchorFile = getArtifactAnchorFile(domainName, domainsDir);
    return anchorFile.delete();
  }

  private void assertApplicationAnchorFileExists(String applicationName) {
    assertThat(getArtifactAnchorFile(applicationName, appsDir).exists(), is(true));
  }

  private void assertApplicationAnchorFileDoesNotExists(String applicationName) {
    assertThat(getArtifactAnchorFile(applicationName, appsDir).exists(), is(false));
  }

  private void assertDomainAnchorFileDoesNotExists(String domainName) {
    assertThat(getArtifactAnchorFile(domainName, domainsDir).exists(), is(false));
  }

  private void assertDomainAnchorFileExists(String domainName) {
    assertThat(getArtifactAnchorFile(domainName, domainsDir).exists(), is(true));
  }

  private File getArtifactAnchorFile(String artifactName, File artifactDir) {
    String anchorFileName = artifactName + MuleDeploymentService.ARTIFACT_ANCHOR_SUFFIX;
    return new File(artifactDir, anchorFileName);
  }

  private void assertAppFolderIsDeleted(String appName) {
    Prober prober = new PollingProber(DEPLOYMENT_TIMEOUT, 100);
    File appFolder = new File(appsDir, appName);
    prober.check(new FileDoesNotExists(appFolder));
  }

  private void assertAppFolderIsMaintained(String appName) {
    assetArtifactFolderIsMaintained(appName, appsDir);
  }

  private void assertDomainFolderIsMaintained(String domainName) {
    assetArtifactFolderIsMaintained(domainName, domainsDir);
  }

  private void assetArtifactFolderIsMaintained(String artifactName, File artifactDir) {
    Prober prober = new PollingProber(DEPLOYMENT_TIMEOUT, 100);
    File appFolder = new File(artifactDir, artifactName);
    prober.check(new FileExists(appFolder));
  }

  private void executeApplicationFlow(String flowName) throws MuleException {
    Flow mainFlow =
        (Flow) deploymentService.getApplications().get(0).getMuleContext().getRegistry().lookupFlowConstruct(flowName);
    InternalMessage muleMessage = InternalMessage.builder().payload(TEST_MESSAGE).build();

    mainFlow.process(Event.builder(DefaultEventContext.create(mainFlow, TEST_CONNECTOR))
        .message(muleMessage)
        .exchangePattern(REQUEST_RESPONSE).flow(mainFlow).build());
  }


  /**
   * Allows to execute custom actions before or after executing logic or checking preconditions / verifications.
   */
  private interface Action {

    void perform() throws Exception;
  }

  public static class WaitComponent implements Initialisable {

    public static Latch componentInitializedLatch = new Latch();
    public static Latch waitLatch = new Latch();

    @Override
    public void initialise() throws InitialisationException {
      try {
        componentInitializedLatch.release();
        waitLatch.await();
      } catch (InterruptedException e) {
        throw new InitialisationException(e, this);
      }
    }

    public static void reset() {
      componentInitializedLatch = new Latch();
      waitLatch = new Latch();
    }
  }
}
