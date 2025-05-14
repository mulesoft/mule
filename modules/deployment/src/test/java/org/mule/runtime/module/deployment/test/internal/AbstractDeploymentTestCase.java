/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.module.deployment.test.internal;

import static org.mule.runtime.api.deployment.meta.Product.MULE;
import static org.mule.runtime.api.i18n.I18nMessageFactory.createStaticMessage;
import static org.mule.runtime.container.api.MuleFoldersUtil.getDomainFolder;
import static org.mule.runtime.container.api.MuleFoldersUtil.getServicesFolder;
import static org.mule.runtime.container.api.discoverer.ModuleDiscoverer.EXPORTED_CLASS_PACKAGES_PROPERTY;
import static org.mule.runtime.core.api.config.MuleProperties.MULE_HOME_DIRECTORY_PROPERTY;
import static org.mule.runtime.core.api.lifecycle.LifecycleUtils.startIfNeeded;
import static org.mule.runtime.core.api.lifecycle.LifecycleUtils.stopIfNeeded;
import static org.mule.runtime.core.api.util.ClassUtils.withContextClassLoader;
import static org.mule.runtime.core.api.util.FileUtils.deleteTree;
import static org.mule.runtime.core.api.util.FileUtils.unzip;
import static org.mule.runtime.core.internal.config.RuntimeLockFactoryUtil.getRuntimeLockFactory;
import static org.mule.runtime.deployment.model.api.application.ApplicationStatus.STARTED;
import static org.mule.runtime.deployment.model.api.builder.DeployableArtifactClassLoaderFactoryProvider.domainClassLoaderFactory;
import static org.mule.runtime.module.artifact.api.descriptor.ArtifactDescriptorConstants.MULE_LOADER_ID;
import static org.mule.runtime.module.artifact.api.descriptor.ArtifactDescriptorConstants.SERIALIZED_ARTIFACT_AST_LOCATION;
import static org.mule.runtime.module.artifact.api.descriptor.ArtifactPluginDescriptor.EXTENSION_BUNDLE_TYPE;
import static org.mule.runtime.module.artifact.api.descriptor.DomainDescriptor.DEFAULT_DOMAIN_NAME;
import static org.mule.runtime.module.deployment.impl.internal.policy.loader.PropertiesBundleDescriptorLoader.ARTIFACT_ID;
import static org.mule.runtime.module.deployment.impl.internal.policy.loader.PropertiesBundleDescriptorLoader.CLASSIFIER;
import static org.mule.runtime.module.deployment.impl.internal.policy.loader.PropertiesBundleDescriptorLoader.GROUP_ID;
import static org.mule.runtime.module.deployment.impl.internal.policy.loader.PropertiesBundleDescriptorLoader.PROPERTIES_BUNDLE_DESCRIPTOR_LOADER_ID;
import static org.mule.runtime.module.deployment.impl.internal.policy.loader.PropertiesBundleDescriptorLoader.TYPE;
import static org.mule.runtime.module.deployment.impl.internal.policy.loader.PropertiesBundleDescriptorLoader.VERSION;
import static org.mule.runtime.module.deployment.internal.DefaultArchiveDeployer.JAR_FILE_SUFFIX;
import static org.mule.runtime.module.deployment.internal.DeploymentDirectoryWatcher.CHANGE_CHECK_INTERVAL_PROPERTY;
import static org.mule.runtime.module.deployment.internal.MuleDeploymentService.JAR_ARTIFACT_FILTER;
import static org.mule.runtime.module.deployment.internal.MuleDeploymentService.PARALLEL_DEPLOYMENT_PROPERTY;
import static org.mule.runtime.module.deployment.internal.MuleDeploymentService.findSchedulerService;
import static org.mule.runtime.module.deployment.internal.processor.SerializedAstArtifactConfigurationProcessor.serializedAstWithFallbackArtifactConfigurationProcessor;
import static org.mule.runtime.module.deployment.test.internal.TestArtifactsCatalog.callbackExtensionPlugin;
import static org.mule.runtime.module.deployment.test.internal.TestArtifactsCatalog.defaultFooServiceJarFile;
import static org.mule.runtime.module.deployment.test.internal.TestArtifactsCatalog.defaultServiceEchoJarFile;
import static org.mule.runtime.module.deployment.test.internal.TestArtifactsCatalog.echoTestClassFile;
import static org.mule.runtime.module.deployment.test.internal.TestArtifactsCatalog.exceptionThrowingPlugin;
import static org.mule.runtime.module.deployment.test.internal.TestArtifactsCatalog.helloExtensionV1Plugin;
import static org.mule.runtime.module.deployment.test.internal.TestArtifactsCatalog.helloExtensionV2Plugin;
import static org.mule.runtime.module.deployment.test.internal.TestArtifactsCatalog.pluginEcho3ClassFile;
import static org.mule.runtime.module.deployment.test.internal.TestPolicyProcessor.correlationIdCount;
import static org.mule.runtime.module.deployment.test.internal.TestPolicyProcessor.invocationCount;
import static org.mule.runtime.module.deployment.test.internal.TestPolicyProcessor.policyParametrization;
import static org.mule.runtime.module.deployment.test.internal.util.DeploymentServiceTestUtils.deploy;
import static org.mule.runtime.module.deployment.test.internal.util.DeploymentServiceTestUtils.redeploy;
import static org.mule.runtime.module.deployment.test.internal.util.DeploymentServiceTestUtils.undeploy;
import static org.mule.tck.junit4.AbstractMuleContextTestCase.TEST_MESSAGE;

import static java.lang.String.format;
import static java.lang.System.currentTimeMillis;
import static java.lang.System.getProperty;
import static java.lang.System.setProperty;
import static java.lang.Thread.currentThread;
import static java.util.Arrays.asList;
import static java.util.Arrays.stream;
import static java.util.Collections.emptyList;
import static java.util.Collections.emptyMap;
import static java.util.Collections.emptySet;
import static java.util.concurrent.Executors.newSingleThreadExecutor;
import static java.util.stream.Collectors.joining;

import static com.github.valfirst.slf4jtest.TestLoggerFactory.getTestLogger;
import static org.apache.commons.io.FileUtils.copyDirectory;
import static org.apache.commons.io.FileUtils.copyFile;
import static org.apache.commons.io.FileUtils.deleteDirectory;
import static org.apache.commons.io.FileUtils.moveDirectory;
import static org.apache.commons.io.FileUtils.toFile;
import static org.apache.commons.io.filefilter.DirectoryFileFilter.DIRECTORY;
import static org.apache.commons.io.filefilter.FileFileFilter.FILE;
import static org.apache.commons.lang3.StringUtils.removeEnd;
import static org.apache.commons.lang3.reflect.FieldUtils.readDeclaredStaticField;
import static org.apache.commons.lang3.reflect.FieldUtils.writeDeclaredStaticField;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.arrayContaining;
import static org.hamcrest.Matchers.arrayWithSize;
import static org.hamcrest.Matchers.not;
import static org.hamcrest.Matchers.nullValue;
import static org.hamcrest.Matchers.sameInstance;
import static org.hamcrest.collection.ArrayMatching.arrayContainingInAnyOrder;
import static org.hamcrest.collection.IsCollectionWithSize.hasSize;
import static org.hamcrest.core.Is.is;
import static org.hamcrest.core.IsNull.notNullValue;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.fail;
import static org.junit.rules.RuleChain.outerRule;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.slf4j.LoggerFactory.getLogger;

import org.mule.functional.api.flow.FlowRunner;
import org.mule.functional.config.TestComponentBuildingDefinitionProvider;
import org.mule.runtime.api.artifact.Registry;
import org.mule.runtime.api.config.custom.CustomizationService;
import org.mule.runtime.api.deployment.meta.MuleArtifactLoaderDescriptor;
import org.mule.runtime.api.deployment.meta.MulePolicyModel.MulePolicyModelBuilder;
import org.mule.runtime.api.exception.MuleException;
import org.mule.runtime.api.exception.MuleRuntimeException;
import org.mule.runtime.api.lifecycle.Initialisable;
import org.mule.runtime.api.lifecycle.InitialisationException;
import org.mule.runtime.api.memory.management.MemoryManagementService;
import org.mule.runtime.api.scheduler.SchedulerService;
import org.mule.runtime.api.util.collection.SmallMap;
import org.mule.runtime.api.util.concurrent.Latch;
import org.mule.runtime.config.api.properties.PropertiesResolverUtils;
import org.mule.runtime.container.api.ModuleRepository;
import org.mule.runtime.container.api.MuleFoldersUtil;
import org.mule.runtime.container.internal.MuleClassLoaderLookupPolicy;
import org.mule.runtime.core.api.event.CoreEvent;
import org.mule.runtime.core.internal.processor.LoggerMessageProcessor;
import org.mule.runtime.core.internal.registry.DefaultRegistry;
import org.mule.runtime.deployment.model.api.application.Application;
import org.mule.runtime.deployment.model.api.application.ApplicationStatus;
import org.mule.runtime.deployment.model.api.domain.Domain;
import org.mule.runtime.globalconfig.api.GlobalConfigLoader;
import org.mule.runtime.module.artifact.activation.api.extension.discovery.ExtensionModelLoaderRepository;
import org.mule.runtime.module.artifact.api.classloader.ArtifactClassLoader;
import org.mule.runtime.module.artifact.api.classloader.RegionClassLoader;
import org.mule.runtime.module.artifact.api.descriptor.ApplicationDescriptor;
import org.mule.runtime.module.artifact.api.descriptor.ArtifactDescriptorValidatorBuilder;
import org.mule.runtime.module.artifact.api.descriptor.DomainDescriptor;
import org.mule.runtime.module.artifact.builder.AbstractDependencyFileBuilder;
import org.mule.runtime.module.artifact.builder.TestArtifactDescriptor;
import org.mule.runtime.module.artifact.internal.util.ServiceRegistryDescriptorLoaderRepository;
import org.mule.runtime.module.deployment.api.DeploymentListener;
import org.mule.runtime.module.deployment.api.DeploymentService;
import org.mule.runtime.module.deployment.impl.internal.MuleArtifactResourcesRegistry;
import org.mule.runtime.module.deployment.impl.internal.application.DefaultApplicationFactory;
import org.mule.runtime.module.deployment.impl.internal.artifact.DefaultClassLoaderManager;
import org.mule.runtime.module.deployment.impl.internal.builder.ApplicationFileBuilder;
import org.mule.runtime.module.deployment.impl.internal.builder.ArtifactPluginFileBuilder;
import org.mule.runtime.module.deployment.impl.internal.builder.DomainFileBuilder;
import org.mule.runtime.module.deployment.impl.internal.builder.PolicyFileBuilder;
import org.mule.runtime.module.deployment.impl.internal.domain.DefaultDomainFactory;
import org.mule.runtime.module.deployment.impl.internal.domain.DefaultMuleDomain;
import org.mule.runtime.module.deployment.impl.internal.policy.PolicyTemplateDescriptorFactory;
import org.mule.runtime.module.deployment.internal.ArchiveDeployer;
import org.mule.runtime.module.deployment.internal.ArtifactDeployer;
import org.mule.runtime.module.deployment.internal.CompositeDeploymentListener;
import org.mule.runtime.module.deployment.internal.DefaultArchiveDeployer;
import org.mule.runtime.module.deployment.internal.DeploymentMuleContextListenerFactory;
import org.mule.runtime.module.deployment.internal.DomainArchiveDeployer;
import org.mule.runtime.module.deployment.internal.DomainDeploymentTemplate;
import org.mule.runtime.module.deployment.internal.MuleDeploymentService;
import org.mule.runtime.module.deployment.internal.util.ObservableList;
import org.mule.runtime.module.deployment.test.api.TestDeploymentListener;
import org.mule.runtime.module.deployment.test.internal.util.container.TestPrivilegedApiModuleRepository;
import org.mule.runtime.module.service.api.manager.ServiceManager;
import org.mule.runtime.module.service.builder.ServiceFileBuilder;
import org.mule.tck.junit4.AbstractMuleTestCase;
import org.mule.tck.junit4.rule.DynamicPort;
import org.mule.tck.junit4.rule.SystemProperty;
import org.mule.tck.probe.JUnitProbe;
import org.mule.tck.probe.PollingProber;
import org.mule.tck.probe.Prober;
import org.mule.tck.probe.file.FileDoesNotExists;
import org.mule.tck.probe.file.FileExists;
import org.mule.tck.util.CompilerUtils;

import java.io.File;
import java.io.IOException;
import java.net.JarURLConnection;
import java.net.URI;
import java.net.URL;
import java.nio.file.Paths;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.ReentrantLock;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.function.Supplier;

import com.github.valfirst.slf4jtest.TestLogger;

import org.apache.logging.log4j.LogManager;
import org.junit.AfterClass;
import org.slf4j.Logger;
import org.slf4j.event.Level;

import org.junit.After;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.ClassRule;
import org.junit.Rule;
import org.junit.rules.RuleChain;
import org.junit.rules.TemporaryFolder;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;

import org.mockito.verification.VerificationMode;

/**
 * Base class for deployment tests using a {@link MuleDeploymentService} instance.
 * <p>
 * Provides a set of test artifacts and resources to use on different test classes.
 */
@RunWith(Parameterized.class)
public abstract class AbstractDeploymentTestCase extends AbstractMuleTestCase {

  public static final Logger logger = getLogger(AbstractDeploymentTestCase.class);
  protected static final int FILE_TIMESTAMP_PRECISION_MILLIS = 2000;
  protected static final String FLOW_PROPERTY_NAME = "flowName";
  protected static final String COMPONENT_NAME = "componentValue";
  protected static final String COMPONENT_NAME_IN_APP = "component";
  protected static final String COMPONENT_CLASS =
      "org.mule.runtime.module.deployment.test.internal.AbstractDeploymentTestCase$TestComponent";
  protected static final String COMPONENT_CLASS_ON_REDEPLOY =
      "org.mule.runtime.module.deployment.test.internal.AbstractDeploymentTestCase$TestComponentOnRedeploy";
  protected static final String FLOW_PROPERTY_NAME_VALUE = "flow1";
  protected static final String FLOW_PROPERTY_NAME_VALUE_ON_REDEPLOY = "flow2";
  private static final int DEPLOYMENT_TIMEOUT = 10000;
  protected static final String[] NONE = new String[0];
  protected static final int ONE_HOUR_IN_MILLISECONDS = 3600000;
  protected static final String MULE_POLICY_CLASSIFIER = "mule-policy";
  protected static final String MANUAL_EXECUTION_CORRELATION_ID = "manualExecution";

  // Resources
  protected static final String MULE_CONFIG_XML_FILE = "mule-config.xml";
  private static final String MULE_DOMAIN_CONFIG_XML_FILE = "mule-domain-config.xml";
  protected static final String EMPTY_APP_CONFIG_XML = "/empty-config.xml";
  private static final String BAD_APP_CONFIG_XML = "/bad-app-config.xml";
  protected static final String EMPTY_DOMAIN_CONFIG_XML = "/empty-domain-config.xml";
  protected static final String APP_WITH_EXTENSION_PLUGIN_CONFIG = "app-with-extension-plugin-config.xml";
  protected static final String APP_WITH_SHARED_EXTENSION_PLUGIN_CONFIG = "app-with-shared-extension-plugin-config.xml";

  protected static final String BAR_POLICY_NAME = "barPolicy";
  protected static final String BAZ_POLICY_NAME = "bazPolicy";
  protected static final String APP_PLUGIN_POLICY_NAME = "appPluginPolicy";
  protected static final String EXCEPTION_POLICY_NAME = "exceptionPolicy";
  protected static final String FOO_POLICY_ID = "fooPolicy";

  protected static final String MIN_MULE_VERSION = "4.0.0";

  protected static TemporaryFolder compilerWorkFolder = new TemporaryFolder();
  protected static TestArtifactsCatalog testArtifactsCatalog = new TestArtifactsCatalog(compilerWorkFolder);
  protected static TestServicesSetup testServicesSetup = new TestServicesSetup(compilerWorkFolder);

  @ClassRule
  public static RuleChain ruleChain = outerRule(compilerWorkFolder).around(testArtifactsCatalog).around(testServicesSetup);

  @ClassRule
  public static SystemProperty duplicateProvidersLax =
      new SystemProperty(PropertiesResolverUtils.class.getName() + ".duplicateProvidersLax", "true");

  private static final ExecutorService executor = newSingleThreadExecutor();

  private DefaultClassLoaderManager artifactClassLoaderManager;
  protected ModuleRepository moduleRepository;

  @Parameterized.Parameters(name = "Parallel: {0}")
  public static List<Boolean> params() {
    return asList(false, true);
  }

  private static boolean jarCacheDefaultValue;

  private static Boolean internalIsRunningTests;

  protected static Latch undeployLatch = new Latch();

  /**
   * Disables the JAR URLConnection cache to imitate the behavior of the Mule runtime.
   * <p>
   * The Mule Container disables JAR URL caching to avoid file locking issues (particularly on Windows systems) when loading and
   * unloading JAR-based artifacts during deployment and undeployment operations. This method ensures tests mimic that behavior.
   */
  @BeforeClass
  public static void disableJarCache() {
    jarCacheDefaultValue = JarURLConnection.getDefaultUseCaches("jar");
    JarURLConnection.setDefaultUseCaches("jar", false);
  }

  /**
   * Restores the default caching behavior for JAR URLConnections after tests complete.
   */
  @AfterClass
  public static void restoreJarCacheDefaultValue() {
    JarURLConnection.setDefaultUseCaches("jar", jarCacheDefaultValue);
  }

  @BeforeClass
  public static void beforeClass() throws IllegalAccessException {
    internalIsRunningTests =
        (Boolean) readDeclaredStaticField(TestComponentBuildingDefinitionProvider.class, "internalIsRunningTests", true);
    writeDeclaredStaticField(TestComponentBuildingDefinitionProvider.class, "internalIsRunningTests", true, true);
  }

  @AfterClass
  public static void afterClass() throws IllegalAccessException {
    writeDeclaredStaticField(TestComponentBuildingDefinitionProvider.class, "internalIsRunningTests", internalIsRunningTests,
                             true);
  }

  // Application file builders
  protected final ApplicationFileBuilder emptyAppFileBuilder =
      new ApplicationFileBuilder("empty-app").definedBy("empty-config.xml");
  protected final ApplicationFileBuilder dummyAppDescriptorFileBuilder = new ApplicationFileBuilder("dummy-app")
      .definedBy("dummy-app-config.xml")
      .configuredWith("myCustomProp", "someValue")
      .containingResource("serialized/dummy-app.ast", SERIALIZED_ARTIFACT_AST_LOCATION)
      .dependingOn(callbackExtensionPlugin)
      .containingClass(echoTestClassFile, "org/foo/EchoTest.class");
  protected final ApplicationFileBuilder dummyAppWithBrokenAstDescriptorFileBuilder = new ApplicationFileBuilder("dummy-app")
      .definedBy("dummy-app-config.xml")
      .configuredWith("myCustomProp", "someValue")
      .containingResource("serialized/broken.ast", SERIALIZED_ARTIFACT_AST_LOCATION)
      .dependingOn(callbackExtensionPlugin)
      .containingClass(echoTestClassFile, "org/foo/EchoTest.class");
  protected final ApplicationFileBuilder dummyFlowErrorAppDescriptorFileBuilder =
      new ApplicationFileBuilder("dummy-error-flow-app")
          .definedBy("dummy-app-several-flows.xml").configuredWith("myCustomProp", "someValue")
          .dependingOn(callbackExtensionPlugin)
          .containingClass(echoTestClassFile, "org/foo/EchoTest.class");
  protected final ApplicationFileBuilder dummyErrorAppOnStartDescriptorFileBuilder =
      new ApplicationFileBuilder("dummy-error-app-start")
          .definedBy("dummy-error-app-start.xml").configuredWith("myCustomProp", "someValue");

  // Domain file builders
  protected DomainFileBuilder dummyDomainFileBuilder =
      new DomainFileBuilder("dummy-domain").definedBy("empty-domain-config.xml");
  protected final DomainFileBuilder exceptionThrowingPluginImportingDomain =
      new DomainFileBuilder("exception-throwing-plugin-importing-domain").definedBy("empty-domain-config.xml")
          .dependingOn(exceptionThrowingPlugin);

  // Policy file builders
  protected final PolicyFileBuilder barPolicyFileBuilder = createPolicyFileBuilder(BAR_POLICY_NAME);
  protected final PolicyFileBuilder policyUsingAppPluginFileBuilder = createPolicyFileBuilder(APP_PLUGIN_POLICY_NAME);
  protected final PolicyFileBuilder policyIncludingPluginFileBuilder =
      createPolicyIncludingPluginFileBuilder();
  protected final PolicyFileBuilder policyIncludingHelloPluginV2FileBuilder =
      createPolicyIncludingHelloPluginV2FileBuilder();
  protected final PolicyFileBuilder exceptionThrowingPluginImportingPolicyFileBuilder =
      createExceptionThrowingPluginImportingPolicyFileBuilder();
  protected final PolicyFileBuilder policyIncludingDependantPluginFileBuilder =
      createPolicyIncludingDependantPluginFileBuilder();

  private File muleHome;
  protected File appsDir;
  protected File domainsDir;
  protected ServiceManager serviceManager;
  protected ExtensionModelLoaderRepository extensionModelLoaderRepository;
  protected MuleDeploymentService deploymentService;
  protected DeploymentListener applicationDeploymentListener;
  protected DeploymentListener domainDeploymentListener;
  protected DeploymentListener domainBundleDeploymentListener;
  protected TestDeploymentListener testDeploymentListener;
  protected ArtifactClassLoader containerClassLoader;
  protected TestPolicyManager policyManager;

  @Rule
  public SystemProperty jvmVersionExtensionEnforcementLoose =
      new SystemProperty("mule.jvm.version.extension.enforcement", "LOOSE");

  @Rule
  public SystemProperty changeChangeInterval = new SystemProperty(CHANGE_CHECK_INTERVAL_PROPERTY, "100000");

  @Rule
  public SystemProperty parallelDeployment;

  @Rule
  public DynamicPort httpPort = new DynamicPort("httpPort");

  private File services;

  public AbstractDeploymentTestCase(boolean parallelDeployment) {
    if (parallelDeployment) {
      this.parallelDeployment = new SystemProperty(PARALLEL_DEPLOYMENT_PROPERTY, "");
    }
  }

  @Before
  public void setUp() throws Exception {
    final String tmpDir = getProperty("java.io.tmpdir");
    muleHome = new File(new File(tmpDir, "mule_home"), getClass().getSimpleName() + currentTimeMillis());
    appsDir = new File(muleHome, "apps");
    appsDir.mkdirs();
    domainsDir = new File(muleHome, "domains");
    domainsDir.mkdirs();
    setProperty(MULE_HOME_DIRECTORY_PROPERTY, muleHome.getCanonicalPath());
    GlobalConfigLoader.reset();

    final File domainFolder = getDomainFolder(DEFAULT_DOMAIN_NAME);
    assertThat(domainFolder.mkdirs(), is(true));

    services = getServicesFolder();
    services.mkdirs();
    testServicesSetup.copyServicesToFolder(services);

    applicationDeploymentListener = mock(DeploymentListener.class);
    testDeploymentListener = new TestDeploymentListener();
    domainDeploymentListener = mock(DeploymentListener.class);
    domainBundleDeploymentListener = mock(DeploymentListener.class);
    moduleRepository = new TestPrivilegedApiModuleRepository(getPrivilegedArtifactIds());
    MuleArtifactResourcesRegistry muleArtifactResourcesRegistry =
        new MuleArtifactResourcesRegistry.Builder()
            .moduleRepository(moduleRepository)
            .artifactConfigurationProcessor(serializedAstWithFallbackArtifactConfigurationProcessor())
            .withBootPackage("org.mule.runtime.module.deployment.test.internal")
            .withBootPackage("org.mule.runtime.module.deployment.test.api")
            .withBootPackage("org.mule.runtime.service.test.api")
            .withBootPackage("org.mule.functional.api.component")
            .withAdditionalResourceDirectory("")
            .build();
    muleArtifactResourcesRegistry.inject(muleArtifactResourcesRegistry.getContainerProfilingService());
    serviceManager = muleArtifactResourcesRegistry.getServiceManager();
    containerClassLoader = muleArtifactResourcesRegistry.getContainerClassLoader();
    extensionModelLoaderRepository = muleArtifactResourcesRegistry.getExtensionModelLoaderRepository();
    artifactClassLoaderManager = muleArtifactResourcesRegistry.getArtifactClassLoaderManager();

    deploymentService = new TestMuleDeploymentService(muleArtifactResourcesRegistry.getDomainFactory(),
                                                      muleArtifactResourcesRegistry.getApplicationFactory(),
                                                      () -> {
                                                        try {
                                                          return findSchedulerService(serviceManager);
                                                        } catch (Exception e) {
                                                          throw new MuleRuntimeException(createStaticMessage("Available services: "
                                                              + serviceManager.getServices()), e);
                                                        }
                                                      });
    configureDeploymentService();

    policyManager = new TestPolicyManager(deploymentService,
                                          new PolicyTemplateDescriptorFactory(
                                                                              muleArtifactResourcesRegistry
                                                                                  .getArtifactPluginDescriptorLoader(),
                                                                              createDescriptorLoaderRepository(),
                                                                              ArtifactDescriptorValidatorBuilder.builder()));
    // Reset test component state
    invocationCount = 0;
    correlationIdCount.clear();
    policyParametrization = "";
  }

  @Before
  public void configulreLogging() {
    // Reduces unnecessary logging
    TestLogger compilerUtilsTestLogger = getTestLogger(CompilerUtils.class);
    compilerUtilsTestLogger.setEnabledLevelsForAllThreads(Level.ERROR);
    TestLogger pollingProberTestLogger = getTestLogger(PollingProber.class);
    pollingProberTestLogger.setEnabledLevelsForAllThreads(Level.ERROR);
    TestLogger testLogger = getTestLogger(LoggerMessageProcessor.class);
    testLogger.setEnabledLevelsForAllThreads(Level.ERROR);
    // Initialises logging plugins with correct classloader
    LogManager.getContext(false);
  }

  @After
  public void undeployApps() {
    if (deploymentService != null) {
      deploymentService.getApplications().forEach(app -> undeploy(deploymentService, app.getArtifactName()));
    }
    TestApplicationFactory.after();
  }

  protected void configureDeploymentService() {
    deploymentService.addDeploymentListener(applicationDeploymentListener);
    deploymentService.addDomainDeploymentListener(domainDeploymentListener);
    deploymentService.addDeploymentListener(testDeploymentListener);
    deploymentService.addDomainDeploymentListener(testDeploymentListener);
    deploymentService.addDomainBundleDeploymentListener(domainBundleDeploymentListener);
  }

  /**
   * Allows sub classes to define privileged artifacts for testing purposes
   *
   * @return privileged artifact IDs
   */
  protected Set<String> getPrivilegedArtifactIds() {
    return emptySet();
  }

  @After
  public void tearDown() throws Exception {
    if (deploymentService != null) {
      deploymentService.stop();
    }

    if (serviceManager != null) {
      serviceManager.stop();
    }

    if (extensionModelLoaderRepository != null) {
      stopIfNeeded(extensionModelLoaderRepository);
    }

    //deleteTree(muleHome);

    // this is a complex classloader setup and we can't reproduce standalone Mule 100%,
    // so trick the next test method into thinking it's the first run, otherwise
    // app resets CCL ref to null and breaks the next test
    currentThread().setContextClassLoader(ClassLoader.getSystemClassLoader());
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

  protected ApplicationFileBuilder createExtensionApplicationWithServices(String appConfigFile,
                                                                          ArtifactPluginFileBuilder... plugins)
      throws Exception {
    installEchoService();
    installFooService();

    ApplicationFileBuilder applicationFileBuilder = new ApplicationFileBuilder("appWithExtensionPlugin")
        .definedBy(appConfigFile);

    for (ArtifactPluginFileBuilder plugin : plugins) {
      applicationFileBuilder.dependingOn(plugin);
    }

    return applicationFileBuilder;
  }

  protected void installFooService() throws IOException {
    installService("fooService", "org.mule.runtime.service.test.api.FooService", "org.mule.service.foo.FooServiceProvider",
                   defaultFooServiceJarFile, defaultServiceEchoJarFile);
  }

  protected void installEchoService() throws IOException {
    installService("echoService", "org.mule.runtime.service.test.api.EchoService", "org.mule.echo.EchoServiceProvider",
                   defaultServiceEchoJarFile);
  }

  private void installService(String serviceName, String satisfiedServiceClassName, String serviceProviderClassName,
                              File... serviceJarFiles)
      throws IOException {
    final File echoService =
        new ServiceFileBuilder(serviceName)
            .forContract(satisfiedServiceClassName)
            .withServiceProviderClass(serviceProviderClassName)
            .usingLibraries(serviceJarFiles)
            .unpack(true)
            .getArtifactFile();

    File installedService = new File(services, echoService.getName());
    copyDirectory(echoService, installedService);
  }

  protected void doSynchronizedArtifactDeploymentActionTest(final Action deploymentAction, final Action assertAction,
                                                            DeploymentListener domainDeploymentListener, String artifactId)
      throws InterruptedException {
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

    deploymentServiceThread.join();
  }

  protected void doRedeployAppByChangingConfigFileWithGoodOne(String applicationPath) throws Exception {
    changeConfigFile(applicationPath, EMPTY_APP_CONFIG_XML);
  }

  protected void doRedeployAppByChangingConfigFileWithBadOne(String applicationPath) throws Exception {
    changeConfigFile(applicationPath, BAD_APP_CONFIG_XML);
  }

  private void changeConfigFile(String applicationPath, String configFile) throws Exception {
    File originalConfigFile = new File(new File(appsDir, applicationPath), getConfigFilePathWithinArtifact(MULE_CONFIG_XML_FILE));
    assertThat("Original config file does not exists: " + originalConfigFile, originalConfigFile.exists(), is(true));
    URL url = getClass().getResource(configFile);
    File newConfigFile = new File(url.toURI());
    copyFile(newConfigFile, originalConfigFile);
  }

  protected void deploysArtifactAndVerifyAnchorFileCreatedWhenDeploymentEnds(Action deployArtifactAction,
                                                                             Action verifyAnchorFileDoesNotExistsAction,
                                                                             Action verifyDeploymentSuccessfulAction,
                                                                             Action verifyAnchorFileExistsAction)
      throws Exception {
    WaitComponent.reset();
    startDeployment();
    deployArtifactAction.perform();
    Future future = triggerDirectoryWatcherAsync();
    try {
      if (!WaitComponent.componentInitializedLatch.await(DEPLOYMENT_TIMEOUT, TimeUnit.MILLISECONDS)) {
        fail("WaitComponent should be initilaized already. Probably app deployment failed");
      }
      verifyAnchorFileDoesNotExistsAction.perform();
    } finally {
      WaitComponent.waitLatch.release();
      future.get();
    }
    verifyDeploymentSuccessfulAction.perform();
    verifyAnchorFileExistsAction.perform();
  }

  protected void startDeployment(boolean startServiceManager) throws MuleException {
    if (startServiceManager) {
      serviceManager.start();
    }
    waitForExpressionLanguageServiceRegistered();
    startIfNeeded(extensionModelLoaderRepository);
    deploymentService.start(false);
  }

  protected void waitForExpressionLanguageServiceRegistered() {
    int retries = 5; // 5 segundos
    Exception last = null;
    while (retries-- > 0) {
        try {
            serviceManager.getServices().forEach(service -> {
                System.out.println("[DIAG] Servicio: " + service.getClass().getName());
                for (Class<?> iface : service.getClass().getInterfaces()) {
                    System.out.println("    [DIAG] Implementa: " + iface.getName());
                }
            });

            Object elService = serviceManager.getServices().stream()
                .filter(service -> {
                    for (Class<?> iface : service.getClass().getInterfaces()) {
                        if (iface.getName().equals("org.mule.runtime.api.el.DefaultExpressionLanguageFactoryService")) {
                            return true;
                        }
                    }
                    return false;
                })
                .findFirst()
                .orElse(null);

            if (elService != null) {
                // Verifica que implemente la interfaz esperada
                boolean implementsInterface = false;
                for (Class<?> iface : elService.getClass().getInterfaces()) {
                    if (iface.getName().equals("org.mule.runtime.api.el.DefaultExpressionLanguageFactoryService")) {
                        implementsInterface = true;
                        break;
                    }
                }
                if (!implementsInterface) {
                    System.out.println("[DIAG] El servicio encontrado no implementa la interfaz esperada.");
                    last = new IllegalStateException("El servicio no implementa la interfaz esperada");
                    Thread.sleep(1000);
                    continue;
                }

                // Intenta invocar getName() y create()
                try {
                    java.lang.reflect.Method getNameMethod = elService.getClass().getMethod("getName");
                    String name = (String) getNameMethod.invoke(elService);
                    System.out.println("[DIAG] ExpressionLanguageService.getName() = " + name);

                    java.lang.reflect.Method createMethod = elService.getClass().getMethod("create");
                    Object expressionLanguage = createMethod.invoke(elService);
                    System.out.println("[DIAG] ExpressionLanguage creado: " + expressionLanguage);

                    // Si llegamos hasta aquí, el servicio está funcional
                    return;
                } catch (Exception e) {
                    System.out.println("[DIAG] Error al invocar métodos del servicio: " + e.getMessage());
                    last = e;
                }
            } else {
                System.out.println("[DIAG] No se encontró el servicio ExpressionLanguage. Reintentando...");
            }
            Thread.sleep(1000);
        } catch (Exception e) {
            last = e;
            try {
                Thread.sleep(1000);
            } catch (InterruptedException ignored) {}
        }
    }
    throw new IllegalStateException("expressionLanguageService no está registrado, su clase no es accesible o no es funcional", last);
  }


  protected void startDeployment() throws MuleException {
    startDeployment(true);
  }

  protected void triggerDirectoryWatcher() {
    deploymentService.triggerDirectoryWatcher();
  }

  protected Future<?> triggerDirectoryWatcherAsync() {
    return executor.submit(this::triggerDirectoryWatcher);
  }

  protected void assertApplicationDeploymentSuccess(DeploymentListener listener, String artifactName) {
    assertDeploymentSuccess(listener, artifactName);
    assertStatus(artifactName, STARTED);
  }

  protected void assertRedeploymentSuccess(DeploymentListener listener, String artifactName,
                                           Supplier<Map<String, Map<URI, Long>>> zombieSupplier) {
    assertRedeploymentStart(listener, artifactName);
    assertRedeploymentSuccess(listener, artifactName);
    verify(listener, times(1)).onUndeploymentStart(artifactName);
    verify(listener, times(1)).onUndeploymentSuccess(artifactName);

    assertArtifactIsNotRegisteredAsZombie(artifactName, zombieSupplier.get());
  }

  private void assertRedeploymentSuccess(DeploymentListener listener, String artifactName) {
    Prober prober = new PollingProber(DEPLOYMENT_TIMEOUT, 100);
    prober.check(new JUnitProbe() {

      @Override
      protected boolean test() throws Exception {
        verify(listener, times(1)).onRedeploymentSuccess(artifactName);
        return true;
      }

      @Override
      public String describeFailure() {
        return "Failed to redeploy artifact: " + artifactName + System.lineSeparator() + super.describeFailure();
      }
    });
  }

  protected void assertRedeploymentFailure(DeploymentListener listener, String artifactName) {
    assertRedeploymentStart(listener, artifactName);
    assertArtifactRedeploymentFailure(listener, artifactName);

    verify(listener, times(1)).onUndeploymentStart(artifactName);
    verify(listener, times(1)).onUndeploymentSuccess(artifactName);
  }

  private void assertArtifactRedeploymentFailure(DeploymentListener listener, String artifactName) {
    triggerDirectoryWatcher();
    Prober prober = new PollingProber(DEPLOYMENT_TIMEOUT, 100);
    prober.check(new JUnitProbe() {

      @Override
      protected boolean test() throws Exception {
        verify(listener).onRedeploymentFailure(eq(artifactName), any(Throwable.class));
        return true;
      }

      @Override
      public String describeFailure() {
        return "Expected failure to redeploy application: " + artifactName + System.lineSeparator() + super.describeFailure();
      }
    });
  }

  private void assertFailedArtifactRedeploymentSuccess(DeploymentListener listener, String artifactName,
                                                       Supplier<Map<String, Map<URI, Long>>> zombieSupplier) {
    assertRedeploymentStart(listener, artifactName);
    assertDeploymentSuccess(listener, artifactName);
    assertRedeploymentSuccess(listener, artifactName);
    verify(listener, never()).onUndeploymentStart(artifactName);
    verify(listener, never()).onUndeploymentSuccess(artifactName);

    assertArtifactIsNotRegisteredAsZombie(artifactName, zombieSupplier.get());
  }

  private void assertFailedArtifactRedeploymentFailure(DeploymentListener listener, String artifactName,
                                                       Supplier<Map<String, Map<URI, Long>>> zombieSupplier) {
    assertRedeploymentStart(listener, artifactName);
    assertDeploymentFailure(listener, artifactName);
    assertArtifactRedeploymentFailure(listener, artifactName);
    verify(listener, times(0)).onUndeploymentStart(artifactName);
    verify(listener, times(0)).onUndeploymentSuccess(artifactName);

    assertArtifactIsRegisteredAsZombie(artifactName, zombieSupplier.get());
  }

  protected void assertFailedApplicationRedeploymentSuccess(String artifactName) {
    assertFailedArtifactRedeploymentSuccess(applicationDeploymentListener, artifactName,
                                            deploymentService::getZombieApplications);

    assertStatus(artifactName, STARTED);
  }

  protected void assertApplicationRedeploymentSuccess(String artifactName) {
    assertRedeploymentSuccess(applicationDeploymentListener, artifactName, deploymentService::getZombieApplications);

    assertStatus(artifactName, STARTED);
  }

  protected void assertApplicationRedeploymentFailure(String artifactName) {
    assertRedeploymentFailure(applicationDeploymentListener, artifactName);

    assertDeploymentFailure(applicationDeploymentListener, artifactName);
    assertArtifactIsRegisteredAsZombie(artifactName, deploymentService.getZombieApplications());
    assertStatus(artifactName, ApplicationStatus.DEPLOYMENT_FAILED);
  }

  protected void assertApplicationMissingOnBundleRedeployment(String artifactName) {
    assertRedeploymentFailure(applicationDeploymentListener, artifactName);

    assertDeploymentFailure(applicationDeploymentListener, artifactName);
    assertArtifactIsNotRegisteredAsZombie(artifactName, deploymentService.getZombieApplications());
  }

  protected void assertFailedApplicationRedeploymentFailure(DeploymentListener listener, String artifactName) {
    assertFailedArtifactRedeploymentFailure(listener, artifactName, deploymentService::getZombieApplications);
    assertStatus(artifactName, ApplicationStatus.DEPLOYMENT_FAILED);
  }

  protected void assertFailedDomainRedeploymentSuccess(String artifactName) {
    assertFailedArtifactRedeploymentSuccess(domainDeploymentListener, artifactName, deploymentService::getZombieDomains);
  }

  protected void assertDomainRedeploymentSuccess(String artifactName) {
    assertRedeploymentSuccess(domainDeploymentListener, artifactName, deploymentService::getZombieDomains);
  }

  protected void assertDomainRedeploymentFailure(String artifactName) {
    assertRedeploymentFailure(domainDeploymentListener, artifactName);

    assertDeploymentFailure(domainDeploymentListener, artifactName);
    assertArtifactIsRegisteredAsZombie(artifactName, deploymentService.getZombieDomains());
  }

  protected void assertFailedDomainRedeploymentFailure(String artifactName) {
    assertFailedArtifactRedeploymentFailure(domainDeploymentListener, artifactName, deploymentService::getZombieDomains);
  }

  protected void assertDeploymentSuccess(final DeploymentListener listener, final String artifactName) {
    triggerDirectoryWatcher();
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

  protected void assertMuleContextCreated(final DeploymentListener listener, final String appName) {
    Prober prober = new PollingProber(DEPLOYMENT_TIMEOUT, 100);
    prober.check(new JUnitProbe() {

      @Override
      public boolean test() {
        verify(listener, times(1)).onArtifactCreated(eq(appName), any(CustomizationService.class));
        return true;
      }

      @Override
      public String describeFailure() {
        return String.format("Did not received notification '%s' for app '%s'", "onArtifactCreated", appName)
            + System.lineSeparator() + super.describeFailure();
      }
    });
  }

  protected void assertConditionOnRegistry(TestDeploymentListener listener, Function<DefaultRegistry, Boolean> verifier) {
    triggerDirectoryWatcher();
    DefaultRegistry registry = (DefaultRegistry) listener.getRegistry();
    if (registry == null) {
      fail("Couldn't obtain registry");
    }
    assertThat(verifier.apply(registry), is(true));
  }

  protected void assertMuleContextInitialized(final DeploymentListener listener, final String appName) {
    Prober prober = new PollingProber(DEPLOYMENT_TIMEOUT, 100);
    prober.check(new JUnitProbe() {

      @Override
      public boolean test() {
        verify(listener, times(1)).onArtifactInitialised(eq(appName), any(Registry.class));
        return true;
      }

      @Override
      public String describeFailure() {
        return String.format("Did not received notification '%s' for app '%s'", "onArtifactInitialised", appName)
            + System.lineSeparator() + super.describeFailure();
      }
    });
  }

  protected void assertUndeploymentSuccess(final DeploymentListener listener, final String appName) {
    triggerDirectoryWatcher();
    verify(listener, times(1)).onUndeploymentSuccess(appName);
  }

  protected void assertAtLeastOneUndeploymentSuccess(final DeploymentListener listener, final String appName) {
    triggerDirectoryWatcher();
    verify(listener, atLeastOnce()).onUndeploymentSuccess(appName);
  }

  protected void assertDeploymentFailure(final DeploymentListener listener, final String artifactName) {
    triggerDirectoryWatcher();
    assertDeploymentFailure(listener, artifactName, times(1));
  }

  protected void assertStatus(String appName, ApplicationStatus status) {
    assertStatus(appName, status, -1);
  }

  private void assertStatus(String appName, ApplicationStatus status, int expectedApps) {
    Application app = findApp(appName, expectedApps);
    assertThat(app, notNullValue());
    assertStatus(app, status);
  }

  protected void assertStatus(final Application application, final ApplicationStatus status) {
    Prober prober = new PollingProber(DEPLOYMENT_TIMEOUT, 100);
    prober.check(new JUnitProbe() {

      @Override
      protected boolean test() throws Exception {
        assertThat(application.getStatus(), is(status));
        return true;
      }

      @Override
      public String describeFailure() {
        return format("Application %s was expected to be in status %s but was %s instead", application.getArtifactName(),
                      status.name(), application.getStatus().name());
      }
    });

  }

  protected void assertDeploymentFailure(final DeploymentListener listener, final String artifactName,
                                         final VerificationMode mode) {
    triggerDirectoryWatcher();
    verify(listener, mode).onDeploymentFailure(eq(artifactName), any(Throwable.class));
  }

  protected void assertRedeploymentStart(final DeploymentListener listener, final String artifactName) {
    triggerDirectoryWatcher();
    verify(listener).onRedeploymentStart(eq(artifactName));
  }

  protected void assertNoDeploymentInvoked(final DeploymentListener deploymentListener) {
    triggerDirectoryWatcher();
    verify(deploymentListener, never()).onDeploymentStart(any(String.class));
  }

  /**
   * Find a deployed app, performing some basic assertions.
   */
  protected Application findApp(final String appName, int totalAppsExpected) {
    // list all apps to validate total count
    final List<Application> apps = deploymentService.getApplications();
    assertThat(apps, not(nullValue()));

    if (totalAppsExpected >= 0) {
      assertThat(apps.stream().map(Application::getArtifactName).collect(joining()),
                 apps, hasSize(totalAppsExpected));
    }

    final Application app = deploymentService.findApplication(appName);
    assertThat(appName + " not in " + apps.stream().map(Application::getArtifactName).collect(joining()),
               app, not(nullValue()));
    return app;
  }

  protected DefaultMuleDomain createDefaultDomain() {
    DomainDescriptor descriptor = new DomainDescriptor(DEFAULT_DOMAIN_NAME);
    return new DefaultMuleDomain(descriptor,
                                 domainClassLoaderFactory(MuleFoldersUtil::getAppDataFolder)
                                     .create("domain/" + DEFAULT_DOMAIN_NAME,
                                             new RegionClassLoader("domainRegion", descriptor,
                                                                   containerClassLoader
                                                                       .getClassLoader(),
                                                                   new MuleClassLoaderLookupPolicy(emptyMap(),
                                                                                                   emptySet())),
                                             new DomainDescriptor(DEFAULT_DOMAIN_NAME), emptyList()),
                                 artifactClassLoaderManager, serviceManager, emptyList(), extensionModelLoaderRepository,
                                 getRuntimeLockFactory(),
                                 mock(MemoryManagementService.class),
                                 serializedAstWithFallbackArtifactConfigurationProcessor());
  }

  /**
   * Finds a deployed domain
   */
  protected Domain findADomain(final String domainName) {
    final Domain domain = deploymentService.findDomain(domainName);
    assertNotNull(domain);
    return domain;
  }

  protected void assertAppsDir(String[] expectedZips, String[] expectedApps, boolean performValidation) {
    assertArtifactDir(appsDir, expectedZips, expectedApps, performValidation);
  }

  protected void assertDomainDir(String[] expectedZips, String[] expectedDomains, boolean performValidation) {
    assertArtifactDir(domainsDir, expectedZips, expectedDomains, performValidation);
  }

  private void assertArtifactDir(File artifactDir, String[] expectedZips, String[] expectedArtifacts, boolean performValidation) {
    final String[] actualZips = artifactDir.list(JAR_ARTIFACT_FILTER);
    if (performValidation) {
      assertThat("Invalid Mule artifact archives set", actualZips,
                 expectedZips.length == 0 ? arrayWithSize(0) : arrayContaining(expectedZips));
    }
    final String[] actualArtifacts = artifactDir.list(DIRECTORY);
    if (performValidation) {
      assertThat("Invalid Mule exploded artifact set", actualArtifacts,
                 expectedArtifacts.length == 0 ? arrayWithSize(0) : arrayContainingInAnyOrder(expectedArtifacts));
    }
  }

  protected void assertApplicationFiles(String appName, String[] expectedFiles) {
    assertArtifactConfigs(new File(appsDir, appName), expectedFiles);
  }

  private void assertArtifactConfigs(File artifactDir, String[] expectedFiles) {
    final String[] artifactFiles = artifactDir.list(FILE);

    assertThat(expectedFiles, arrayContainingInAnyOrder(artifactFiles));
  }

  protected void addPackedAppFromBuilder(TestArtifactDescriptor artifactFileBuilder) throws Exception {
    addPackedAppFromBuilder(artifactFileBuilder, null);
  }

  protected void addPackedAppFromBuilder(TestArtifactDescriptor artifactFileBuilder, String targetName) throws Exception {
    addPackedAppArchive(artifactFileBuilder, targetName);
  }

  protected void addPackedDomainFromBuilder(TestArtifactDescriptor artifactFileBuilder) throws Exception {
    addPackedDomainFromBuilder(artifactFileBuilder, null);
  }

  protected void addPackedDomainFromBuilder(TestArtifactDescriptor artifactFileBuilder, String targetName) throws Exception {
    addArchive(domainsDir, artifactFileBuilder.getArtifactFile().toURI(), targetName);
  }

  /**
   * Copies a given app archive with a given target name to the apps folder for deployment
   */
  private void addPackedAppArchive(TestArtifactDescriptor artifactFileBuilder, String targetFile) throws Exception {
    addArchive(appsDir, artifactFileBuilder.getArtifactFile().toURI(), targetFile);
  }

  protected void addArchive(File outputDir, URI uri, String targetFile) throws Exception {
    ReentrantLock lock = deploymentService.getLock();

    lock.lock();
    try {
      // copy is not atomic, copy to a temp file and rename instead (rename is atomic)
      final String tempFileName = (targetFile == null ? new File(uri) : new File(targetFile)).getName() + ".part";
      final File tempFile = new File(outputDir, tempFileName);
      copyFile(new File(uri), tempFile);
      final File destFile = new File(removeEnd(tempFile.getAbsolutePath(), ".part"));
      File deployFolder = new File(destFile.getAbsolutePath().replace(JAR_FILE_SUFFIX, ""));
      if (deployFolder.exists()) {
        // Delete META-INF folder so maven file do not get duplicated during redeployment testing.
        deleteDirectory(new File(deployFolder, Paths.get("META-INF", "maven").toString()));
      }
      tempFile.renameTo(destFile);
      assertThat("File does not exists: " + destFile.getAbsolutePath(), destFile.exists(), is(true));
    } finally {
      lock.unlock();
    }
  }

  protected void addExplodedAppFromBuilder(TestArtifactDescriptor artifactFileBuilder) throws Exception {
    addExplodedAppFromBuilder(artifactFileBuilder, null);
  }

  protected void addExplodedAppFromBuilder(TestArtifactDescriptor artifactFileBuilder, String appName) throws Exception {
    addExplodedArtifactFromBuilder(artifactFileBuilder, appName, MULE_CONFIG_XML_FILE, appsDir);
  }

  protected void addExplodedDomainFromBuilder(TestArtifactDescriptor artifactFileBuilder) throws Exception {
    addExplodedDomainFromBuilder(artifactFileBuilder, null);
  }

  protected void addExplodedDomainFromBuilder(TestArtifactDescriptor artifactFileBuilder, String domainName) throws Exception {
    addExplodedArtifactFromBuilder(artifactFileBuilder, domainName, MULE_DOMAIN_CONFIG_XML_FILE, domainsDir);
  }

  private void addExplodedArtifactFromBuilder(TestArtifactDescriptor artifactFileBuilder, String artifactName,
                                              String configFileName, File destinationDir)
      throws Exception {
    addExplodedArtifactFromUrl(artifactFileBuilder.getArtifactFile().toURI().toURL(), artifactName, configFileName,
                               destinationDir);
  }

  private void addExplodedArtifactFromUrl(URL resource, String artifactName, String configFileName, File destinationDir)
      throws Exception {
    assertNotNull("Resource URL cannot be null", resource);

    String artifactFolder = artifactName;
    if (artifactFolder == null) {
      File file = toFile(resource);
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
  private void addExplodedArtifact(URL url, String artifactName, String configFileName, File destinationDir) throws Exception {
    ReentrantLock lock = deploymentService.getLock();

    lock.lock();
    try {
      File tempFolder = new File(muleHome, artifactName);
      unzip(new File(url.toURI()), tempFolder);

      // Under some platforms, file.lastModified is managed at second level, not milliseconds.
      // Need to update the config file lastModified ere to ensure that is different from previous value
      File configFile = new File(tempFolder, getConfigFilePathWithinArtifact(configFileName));
      if (configFile.exists()) {
        configFile.setLastModified(currentTimeMillis() + FILE_TIMESTAMP_PRECISION_MILLIS);
      }

      File appFolder = new File(destinationDir, artifactName);

      if (appFolder.exists()) {
        deleteTree(appFolder);
      }

      moveDirectory(tempFolder, appFolder);
    } finally {
      lock.unlock();
    }
  }

  protected String getConfigFilePathWithinArtifact(String configFileName) {
    return Paths.get(configFileName).toString();
  }

  /**
   * Removes a given application anchor file in order to start application undeployment
   *
   * @param appName name of application to undeployArtifact
   * @return true if anchor file was deleted, false otherwise
   */
  protected boolean removeAppAnchorFile(String appName) {
    File anchorFile = getArtifactAnchorFile(appName, appsDir);
    return anchorFile.delete();
  }

  /**
   * Removes a given domain anchor file in order to start application undeployment
   *
   * @param domainName name of application to undeployArtifact
   * @return true if anchor file was deleted, false otherwise
   */
  protected boolean removeDomainAnchorFile(String domainName) {
    File anchorFile = getArtifactAnchorFile(domainName, domainsDir);
    return anchorFile.delete();
  }

  protected void assertApplicationAnchorFileExists(String applicationName) {
    assertThat(getArtifactAnchorFile(applicationName, appsDir).exists(), is(true));
  }

  protected void assertApplicationAnchorFileDoesNotExists(String applicationName) {
    assertThat(getArtifactAnchorFile(applicationName, appsDir).exists(), is(false));
  }

  protected void assertDomainAnchorFileDoesNotExists(String domainName) {
    assertThat(getArtifactAnchorFile(domainName, domainsDir).exists(), is(false));
  }

  protected void assertDomainAnchorFileExists(String domainName) {
    assertThat(getArtifactAnchorFile(domainName, domainsDir).exists(), is(true));
  }

  private File getArtifactAnchorFile(String artifactName, File artifactDir) {
    String anchorFileName = artifactName + MuleDeploymentService.ARTIFACT_ANCHOR_SUFFIX;
    return new File(artifactDir, anchorFileName);
  }

  protected void assertAppFolderIsDeleted(String appName) {
    Prober prober = new PollingProber(DEPLOYMENT_TIMEOUT, 100);
    File appFolder = new File(appsDir, appName);
    prober.check(new FileDoesNotExists(appFolder));
  }

  protected void assertAppFolderIsMaintained(String appName) {
    assetArtifactFolderIsMaintained(appName, appsDir);
  }

  protected void assertDomainFolderIsMaintained(String domainName) {
    assetArtifactFolderIsMaintained(domainName, domainsDir);
  }

  private void assetArtifactFolderIsMaintained(String artifactName, File artifactDir) {
    Prober prober = new PollingProber(DEPLOYMENT_TIMEOUT, 100);
    File appFolder = new File(artifactDir, artifactName);
    prober.check(new FileExists(appFolder));
  }

  protected void executeApplicationFlow(String flowName) throws Exception {
    executeApplicationFlow(flowName, null);
  }

  protected CoreEvent executeApplicationFlow(String flowName, String correlationId) throws Exception {
    return executeApplicationFlow(flowName, correlationId, 0);
  }

  protected CoreEvent executeApplicationFlow(String flowName, String correlationId, int appNumber) throws Exception {
    ClassLoader appClassLoader = deploymentService.getApplications().get(appNumber).getArtifactClassLoader().getClassLoader();
    return withContextClassLoader(appClassLoader, () -> {
      final FlowRunner flowRunner =
          new FlowRunner(deploymentService.getApplications().get(appNumber).getArtifactContext().getRegistry(), flowName)
              .withPayload(TEST_MESSAGE);

      if (correlationId != null) {
        flowRunner.withSourceCorrelationId(correlationId);
      }

      CoreEvent result;
      try {
        result = flowRunner.run();
      } finally {
        flowRunner.dispose();
      }

      assertThat(currentThread().getContextClassLoader(), sameInstance(appClassLoader));

      return result;
    });
  }

  protected void assertNoZombiePresent(Map<String, Map<URI, Long>> zombieMap) {
    assertEquals("Wrong number of zombie artifacts registered.", 0, zombieMap.size());
  }

  protected void assertArtifactIsRegisteredAsZombie(String artifactName, Map<String, Map<URI, Long>> zombieMap) {
    assertEquals("Wrong number of zombie artifacts registered.", 1, zombieMap.size());
    if (!zombieMap.containsKey(artifactName)) {
      Map.Entry<URI, Long> zombieEntry =
          getZombieFromMap(entry -> new File((URI) entry.getKey()).getName().equals(artifactName), zombieMap);
      assertThat("Wrong URL tagged as zombie.", zombieEntry, is(notNullValue()));
    }
  }

  protected void assertArtifactIsNotRegisteredAsZombie(String artifactName, Map<String, Map<URI, Long>> zombieMap) {
    if (zombieMap.containsKey(artifactName)) {
      Map.Entry<URI, Long> zombieEntry =
          getZombieFromMap(entry -> new File((URI) entry.getKey()).getName().equals(artifactName), zombieMap);
      assertThat("Artifact tagged as zombie.", zombieEntry, is(notNullValue()));
    }
  }

  private Map.Entry<URI, Long> getZombieFromMap(Predicate<Map.Entry> filter, Map<String, Map<URI, Long>> zombieMap) {
    Map.Entry<URI, Long> zombieEntry = null;
    for (Map<URI, Long> zombieResource : zombieMap.values()) {
      zombieEntry = zombieResource.entrySet().stream().filter(filter).findFirst().orElse(null);
      if (zombieEntry != null) {
        break;
      }
    }
    return zombieEntry;
  }

  /**
   * Allows to execute custom actions before or after executing logic or checking preconditions / verifications.
   */
  protected interface Action {

    void perform() throws Exception;
  }

  protected static MuleArtifactLoaderDescriptor createBundleDescriptorLoader(String artifactId, String classifier,
                                                                             String bundleDescriptorLoaderId) {
    return createBundleDescriptorLoader(artifactId, classifier, bundleDescriptorLoaderId, "1.0.0");
  }

  protected static MuleArtifactLoaderDescriptor createBundleDescriptorLoader(String artifactId, String classifier,
                                                                             String bundleDescriptorLoaderId, String version) {
    Map<String, Object> attributes = SmallMap.of(VERSION, version,
                                                 GROUP_ID, "org.mule.test",
                                                 ARTIFACT_ID, artifactId,
                                                 CLASSIFIER, classifier,
                                                 TYPE, EXTENSION_BUNDLE_TYPE);

    return new MuleArtifactLoaderDescriptor(bundleDescriptorLoaderId, attributes);
  }

  protected PolicyFileBuilder createPolicyFileBuilder(String policyName) {
    return createPolicyFileBuilder(policyName, emptyMap());
  }

  protected PolicyFileBuilder createPolicyFileBuilder(String policyName,
                                                      Map<String, Object> loaderDescriptorAttributes,
                                                      AbstractDependencyFileBuilder<?>... dependencies) {
    MulePolicyModelBuilder policyModelBuilder = new MulePolicyModelBuilder()
        .setMinMuleVersion(MIN_MULE_VERSION)
        .setName(policyName)
        .setRequiredProduct(MULE)
        .withBundleDescriptorLoader(createBundleDescriptorLoader(policyName,
                                                                 MULE_POLICY_CLASSIFIER,
                                                                 PROPERTIES_BUNDLE_DESCRIPTOR_LOADER_ID))
        .withClassLoaderModelDescriptorLoader(new MuleArtifactLoaderDescriptor(MULE_LOADER_ID, loaderDescriptorAttributes));
    PolicyFileBuilder policyFileBuilder = new PolicyFileBuilder(policyName)
        .describedBy(policyModelBuilder.build());

    stream(dependencies).forEach(policyFileBuilder::dependingOn);
    return policyFileBuilder;
  }

  private PolicyFileBuilder createPolicyIncludingHelloPluginV2FileBuilder() {
    return createPolicyFileBuilder(BAZ_POLICY_NAME, emptyMap(), helloExtensionV2Plugin);
  }

  private PolicyFileBuilder createExceptionThrowingPluginImportingPolicyFileBuilder() {
    return createPolicyFileBuilder(EXCEPTION_POLICY_NAME, emptyMap(), callbackExtensionPlugin);
  }

  private PolicyFileBuilder createPolicyIncludingPluginFileBuilder() {
    return createPolicyFileBuilder(BAZ_POLICY_NAME, emptyMap(), helloExtensionV1Plugin);
  }

  private PolicyFileBuilder createPolicyIncludingDependantPluginFileBuilder() {
    ArtifactPluginFileBuilder dependantPlugin = new ArtifactPluginFileBuilder("dependantPlugin")
        .configuredWith(EXPORTED_CLASS_PACKAGES_PROPERTY, "org.foo.echo")
        .containingClass(pluginEcho3ClassFile, "org/foo/echo/Plugin3Echo.class")
        .dependingOn(helloExtensionV1Plugin);

    return createPolicyFileBuilder(BAZ_POLICY_NAME, emptyMap(), dependantPlugin);
  }

  protected ServiceRegistryDescriptorLoaderRepository createDescriptorLoaderRepository() {
    return new ServiceRegistryDescriptorLoaderRepository();
  }

  protected static class TestComponent implements Initialisable {

    static boolean initialised = false;

    @Override
    public void initialise() throws InitialisationException {
      initialised = true;
    }
  }


  protected static class TestComponentOnRedeploy implements Initialisable {

    static boolean initialised = false;

    @Override
    public void initialise() throws InitialisationException {
      initialised = true;
    }
  }

  protected void redeployAndVerifyPropertyInRegistry(String id, Properties deploymentProperties,
                                                     Function<DefaultRegistry, Boolean> verifier)
      throws IOException {
    redeployId(id, deploymentProperties);
    assertConditionOnRegistry(testDeploymentListener, verifier);
  }

  protected void deployAndVerifyPropertyInRegistry(URI uri, Properties deploymentProperties,
                                                   Function<DefaultRegistry, Boolean> verifier)
      throws IOException {
    deployURI(uri, deploymentProperties);
    assertConditionOnRegistry(testDeploymentListener, verifier);
  }

  protected void deployURI(URI uri, Properties deploymentProperties) throws IOException {
    deploy(deploymentService, uri, deploymentProperties);
  }

  protected void redeployId(String id, Properties deploymentProperties) throws IOException {
    if (deploymentProperties == null) {
      redeploy(deploymentService, id);
    } else {
      redeploy(deploymentService, id, deploymentProperties);
    }
  }

  /**
   * Updates a file's last modified time to be greater than the original timestamp. It locks the deployment lock in order to
   * prevent the directory watcher to redeploy the artifact multiple times.
   *
   * @param timestamp time value in milliseconds of the original file's last modified time
   * @param file      file to update
   */
  protected void updateFileModifiedTime(long timestamp, File file) {
    ReentrantLock deploymentLock = deploymentService.getLock();
    deploymentLock.lock();
    try {
      do {
        file.setLastModified(currentTimeMillis());
      } while (file.lastModified() <= timestamp);
    } finally {
      deploymentLock.unlock();
    }
  }

  protected void resetUndeployLatch() {
    undeployLatch = new Latch();
  }

  protected void assertManualExecutionsCount(int expectedInvokations) throws Exception {
    executeApplicationFlow("main", MANUAL_EXECUTION_CORRELATION_ID);
    if (expectedInvokations > 0) {
      assertThat(correlationIdCount.containsKey(MANUAL_EXECUTION_CORRELATION_ID), is(true));
      assertThat(correlationIdCount.get(MANUAL_EXECUTION_CORRELATION_ID).get(), is(expectedInvokations));
    } else {
      assertThat(correlationIdCount.containsKey(MANUAL_EXECUTION_CORRELATION_ID), is(false));
    }
  }

  protected static class TestMuleDeploymentService extends MuleDeploymentService {

    private final Supplier<SchedulerService> schedulerServiceSupplier;

    public TestMuleDeploymentService(DefaultDomainFactory domainFactory, DefaultApplicationFactory applicationFactory,
                                     Supplier<SchedulerService> schedulerServiceSupplier) {
      super(domainFactory, applicationFactory, schedulerServiceSupplier);
      this.schedulerServiceSupplier = schedulerServiceSupplier;
    }

    @Override
    protected DomainArchiveDeployer createDomainArchiveDeployer(DefaultDomainFactory domainFactory,
                                                                ArtifactDeployer<Domain> domainMuleDeployer,
                                                                ObservableList<Domain> domains,
                                                                DefaultArchiveDeployer<ApplicationDescriptor, Application> applicationDeployer,
                                                                CompositeDeploymentListener applicationDeploymentListener,
                                                                DeploymentListener domainDeploymentListener) {
      return new TestDomainArchiveDeployer(new DefaultArchiveDeployer<>(domainMuleDeployer, domainFactory, domains,
                                                                        new DomainDeploymentTemplate(applicationDeployer,
                                                                                                     this,
                                                                                                     applicationDeploymentListener),
                                                                        new DeploymentMuleContextListenerFactory(domainDeploymentListener),
                                                                        schedulerServiceSupplier),
                                           applicationDeployer, this);

    }
  }


  private static class TestDomainArchiveDeployer extends DomainArchiveDeployer {

    public TestDomainArchiveDeployer(ArchiveDeployer<DomainDescriptor, Domain> domainDeployer,
                                     ArchiveDeployer<ApplicationDescriptor, Application> applicationDeployer,
                                     DeploymentService deploymentService) {
      super(domainDeployer, applicationDeployer, deploymentService);
    }

    @Override
    public void undeployArtifact(String artifactId) {
      super.undeployArtifact(artifactId);
      undeployLatch.countDown();
    }
  }

}
