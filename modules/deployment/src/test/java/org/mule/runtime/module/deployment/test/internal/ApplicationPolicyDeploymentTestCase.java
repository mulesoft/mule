/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.module.deployment.test.internal;

import static org.mule.functional.junit4.matchers.ThrowableMessageMatcher.hasMessage;
import static org.mule.runtime.api.config.MuleRuntimeFeature.ENABLE_POLICY_ISOLATION;
import static org.mule.runtime.api.config.MuleRuntimeFeature.SEPARATE_CLASSLOADER_FOR_POLICY_ISOLATION;
import static org.mule.runtime.api.deployment.meta.Product.MULE;
import static org.mule.runtime.api.notification.PolicyNotification.AFTER_NEXT;
import static org.mule.runtime.api.notification.PolicyNotification.BEFORE_NEXT;
import static org.mule.runtime.api.notification.PolicyNotification.PROCESS_END;
import static org.mule.runtime.api.notification.PolicyNotification.PROCESS_START;
import static org.mule.runtime.container.internal.ClasspathModuleDiscoverer.EXPORTED_RESOURCE_PROPERTY;
import static org.mule.runtime.core.internal.config.bootstrap.ClassLoaderRegistryBootstrapDiscoverer.BOOTSTRAP_PROPERTIES;
import static org.mule.runtime.deployment.model.api.application.ApplicationStatus.STARTED;
import static org.mule.runtime.deployment.model.api.artifact.ArtifactDescriptorConstants.EXPORTED_RESOURCES;
import static org.mule.runtime.deployment.model.api.artifact.ArtifactDescriptorConstants.MULE_LOADER_ID;
import static org.mule.runtime.module.artifact.api.classloader.ChildFirstLookupStrategy.CHILD_FIRST;
import static org.mule.runtime.module.artifact.api.classloader.ParentFirstLookupStrategy.PARENT_FIRST;
import static org.mule.runtime.module.artifact.api.descriptor.BundleDescriptor.MULE_PLUGIN_CLASSIFIER;
import static org.mule.runtime.module.deployment.impl.internal.application.MuleApplicationPolicyProvider.IS_POLICY_REORDER;
import static org.mule.runtime.module.deployment.impl.internal.policy.DefaultApplicationPolicyInstance.IS_SILENT_DEPLOY_PARAMETER_NAME;
import static org.mule.runtime.module.deployment.impl.internal.policy.loader.PropertiesBundleDescriptorLoader.PROPERTIES_BUNDLE_DESCRIPTOR_LOADER_ID;
import static org.mule.runtime.module.deployment.impl.internal.util.DeploymentPropertiesUtils.getPersistedDeploymentProperties;
import static org.mule.runtime.module.deployment.impl.internal.util.DeploymentPropertiesUtils.getPersistedFlowDeploymentProperties;
import static org.mule.runtime.module.deployment.internal.DefaultArchiveDeployer.START_ARTIFACT_ON_DEPLOYMENT_PROPERTY;
import static org.mule.runtime.module.deployment.internal.FlowStoppedDeploymentPersistenceListener.START_FLOW_ON_DEPLOYMENT_PROPERTY;
import static org.mule.runtime.module.deployment.test.internal.TestArtifactsCatalog.byeXmlExtensionPlugin;
import static org.mule.runtime.module.deployment.test.internal.TestArtifactsCatalog.echoTestClassFile;
import static org.mule.runtime.module.deployment.test.internal.TestArtifactsCatalog.exceptionThrowingPlugin;
import static org.mule.runtime.module.deployment.test.internal.TestArtifactsCatalog.helloExtensionV1Plugin;
import static org.mule.runtime.module.deployment.test.internal.TestArtifactsCatalog.httpPlugin;
import static org.mule.runtime.module.deployment.test.internal.TestArtifactsCatalog.moduleUsingByeXmlExtensionPlugin;
import static org.mule.runtime.module.deployment.test.internal.TestArtifactsCatalog.policyConfigurationExtensionJarFile;
import static org.mule.runtime.module.deployment.test.internal.TestArtifactsCatalog.policyDependencyInjectionExtensionJarFile;
import static org.mule.runtime.module.deployment.test.internal.TestArtifactsCatalog.usingObjectStorePlugin;
import static org.mule.runtime.module.deployment.test.internal.TestPolicyProcessor.invocationCount;
import static org.mule.runtime.module.deployment.test.internal.TestPolicyProcessor.policyParametrization;
import static org.mule.runtime.module.deployment.test.internal.util.Utils.getResourceFile;
import static org.mule.runtime.module.extension.internal.loader.java.DefaultJavaExtensionModelLoader.JAVA_LOADER_ID;
import static org.mule.test.allure.AllureConstants.ArtifactDeploymentFeature.POLICY_DEPLOYMENT;
import static org.mule.test.allure.AllureConstants.ArtifactDeploymentFeature.POLICY_REORDER;
import static org.mule.test.allure.AllureConstants.ArtifactDeploymentFeature.POLICY_SILENT_DEPLOY;
import static org.mule.test.allure.AllureConstants.ClassloadingIsolationFeature.CLASSLOADING_ISOLATION;
import static org.mule.test.allure.AllureConstants.DeploymentConfiguration.ApplicationConfiguration.APPLICATION_CONFIGURATION;

import static java.lang.Boolean.parseBoolean;
import static java.lang.String.format;
import static java.util.Arrays.asList;
import static java.util.Collections.emptyList;
import static java.util.Collections.emptyMap;
import static java.util.Collections.singletonList;
import static java.util.Collections.singletonMap;

import static com.github.valfirst.slf4jtest.TestLoggerFactory.getTestLogger;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.allOf;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasItems;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.instanceOf;
import static org.hamcrest.Matchers.startsWith;
import static org.hamcrest.core.Is.is;
import static org.hamcrest.core.IsNull.nullValue;
import static org.junit.Assert.fail;
import static org.junit.rules.ExpectedException.none;
import static org.slf4j.event.Level.DEBUG;
import static org.slf4j.event.Level.INFO;

import org.mule.runtime.api.deployment.meta.MuleArtifactLoaderDescriptor;
import org.mule.runtime.api.deployment.meta.MuleArtifactLoaderDescriptorBuilder;
import org.mule.runtime.api.deployment.meta.MulePluginModel;
import org.mule.runtime.api.deployment.meta.MulePolicyModel;
import org.mule.runtime.api.deployment.meta.Product;
import org.mule.runtime.api.exception.MuleRuntimeException;
import org.mule.runtime.api.lifecycle.InitialisationException;
import org.mule.runtime.api.notification.PolicyNotification;
import org.mule.runtime.api.notification.PolicyNotificationListener;
import org.mule.runtime.api.security.Authentication;
import org.mule.runtime.api.security.SecurityException;
import org.mule.runtime.container.internal.IsolatedPolicyClassLoader;
import org.mule.runtime.core.api.construct.Flow;
import org.mule.runtime.core.api.policy.Policy;
import org.mule.runtime.core.api.policy.PolicyParametrization;
import org.mule.runtime.core.api.security.AbstractSecurityProvider;
import org.mule.runtime.core.internal.context.MuleContextWithRegistry;
import org.mule.runtime.core.internal.policy.NoOpPolicyManager;
import org.mule.runtime.core.internal.policy.PolicyManager;
import org.mule.runtime.deployment.model.api.application.Application;
import org.mule.runtime.deployment.model.api.policy.PolicyRegistrationException;
import org.mule.runtime.deployment.model.api.policy.PolicyTemplateDescriptor;
import org.mule.runtime.module.artifact.api.classloader.ArtifactClassLoader;
import org.mule.runtime.module.artifact.api.classloader.MuleDeployableArtifactClassLoader;
import org.mule.runtime.module.artifact.api.classloader.RegionClassLoader;
import org.mule.runtime.module.deployment.impl.internal.builder.ApplicationFileBuilder;
import org.mule.runtime.module.deployment.impl.internal.builder.ArtifactPluginFileBuilder;
import org.mule.runtime.module.deployment.impl.internal.builder.DomainFileBuilder;
import org.mule.runtime.module.deployment.impl.internal.builder.JarFileBuilder;
import org.mule.runtime.module.deployment.impl.internal.builder.PolicyFileBuilder;
import org.mule.runtime.policy.api.PolicyPointcut;
import org.mule.runtime.policy.api.PolicyPointcutParameters;
import org.mule.tck.junit4.rule.SystemProperty;
import org.mule.tck.probe.JUnitProbe;
import org.mule.tck.probe.PollingProber;
import org.mule.tck.util.CompilerUtils;

import java.io.File;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Properties;
import java.util.stream.Collectors;

import com.github.valfirst.slf4jtest.LoggingEvent;
import com.github.valfirst.slf4jtest.TestLogger;
import io.qameta.allure.Description;
import io.qameta.allure.Feature;
import io.qameta.allure.Issue;
import io.qameta.allure.Story;
import org.junit.BeforeClass;
import org.junit.Ignore;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.junit.runners.Parameterized;

/**
 * Contains test for application deployment with policies on the default domain
 */
@Feature(POLICY_DEPLOYMENT)
public class ApplicationPolicyDeploymentTestCase extends AbstractDeploymentTestCase {

  private static final String APP_WITH_SIMPLE_EXTENSION_CONFIG = "app-with-simple-extension-config.xml";
  private static final String APP_WITH_MODULE_BYE_CONFIG = "app-with-module-bye-config.xml";

  private static final int POLICY_NOTIFICATION_TIMEOUT = 5000;

  private static final String BAR_POLICY_ID = "barPolicy";
  private static final String POLICY_PROPERTY_VALUE = "policyPropertyValue";
  private static final String POLICY_PROPERTY_KEY = "policyPropertyKey";
  private static final String FOO_POLICY_NAME = "fooPolicy";
  private static final String ISOLATED_POLICY_NAME = "isolatedPolicy";

  private static File simpleExtensionJarFile;
  private static File withErrorDeclarationExtensionJarFile;

  @Rule
  public ExpectedException expectedEx = none();

  @Rule
  public SystemProperty enablePolicyIsolationSystemProperty;

  @Rule
  public SystemProperty separateCLforPolicyIsolationSystemProperty;

  @Parameterized.Parameters(name = "Parallel: {0} - Enable policy isolation {1}, Separate ClassLoader for policy isolation {2}")
  public static List<Object[]> parameters() {
    // Only run without parallel deployment since this configuration does not affect policy deployment at all
    return asList(
                  new Object[] {false, true, true},
                  new Object[] {false, false, true});
  }

  // Policy artifact file builders
  private final PolicyFileBuilder fooPolicyFileBuilder =
      new PolicyFileBuilder(FOO_POLICY_NAME).describedBy(new MulePolicyModel.MulePolicyModelBuilder()
          .setMinMuleVersion(MIN_MULE_VERSION)
          .setName(FOO_POLICY_NAME)
          .setRequiredProduct(MULE)
          .withBundleDescriptorLoader(createBundleDescriptorLoader(FOO_POLICY_NAME,
                                                                   MULE_POLICY_CLASSIFIER,
                                                                   PROPERTIES_BUNDLE_DESCRIPTOR_LOADER_ID))
          .withClassLoaderModelDescriptorLoader(
                                                new MuleArtifactLoaderDescriptor(MULE_LOADER_ID, emptyMap()))
          .build());

  private final PolicyFileBuilder policyWithPolicyIsolationFileBuilder =
      new PolicyFileBuilder(ISOLATED_POLICY_NAME).describedBy(new MulePolicyModel.MulePolicyModelBuilder()
          .setMinMuleVersion(MIN_MULE_VERSION)
          .setName(ISOLATED_POLICY_NAME)
          .setRequiredProduct(MULE)
          .withBundleDescriptorLoader(createBundleDescriptorLoader(ISOLATED_POLICY_NAME,
                                                                   MULE_POLICY_CLASSIFIER,
                                                                   PROPERTIES_BUNDLE_DESCRIPTOR_LOADER_ID))
          .withClassLoaderModelDescriptorLoader(
                                                new MuleArtifactLoaderDescriptor(MULE_LOADER_ID, emptyMap()))
          .build());

  public ApplicationPolicyDeploymentTestCase(boolean parallelDeployment, boolean enablePolicyIsolation,
                                             boolean separateCLforPolicyIsolation) {
    super(parallelDeployment);
    this.enablePolicyIsolationSystemProperty =
        new SystemProperty((ENABLE_POLICY_ISOLATION.getOverridingSystemPropertyName().get()),
                           Boolean.toString(enablePolicyIsolation));
    this.separateCLforPolicyIsolationSystemProperty =
        new SystemProperty((SEPARATE_CLASSLOADER_FOR_POLICY_ISOLATION.getOverridingSystemPropertyName().get()),
                           Boolean.toString(separateCLforPolicyIsolation));
  }

  @BeforeClass
  public static void compileTestClasses() throws Exception {
    simpleExtensionJarFile = new CompilerUtils.ExtensionCompiler()
        .compiling(getResourceFile("/org/foo/simple/SimpleExtension.java"),
                   getResourceFile("/org/foo/simple/SimpleOperation.java"))
        .compile("mule-module-simple-4.0-SNAPSHOT.jar", "1.0.0");

    withErrorDeclarationExtensionJarFile =
        new CompilerUtils.ExtensionCompiler()
            .compiling(getResourceFile("/org/foo/withErrorDeclaration/WithErrorDeclarationExtension.java"),
                       getResourceFile("/org/foo/withErrorDeclaration/WithErrorDeclarationOperation.java"))
            .compile("mule-module-with-error-declaration-4.0-SNAPSHOT.jar", "1.0.0");
  }

  @Test
  public void appliesApplicationPolicy() throws Exception {
    doApplicationPolicyExecutionTest(parameters -> true, 1, POLICY_PROPERTY_VALUE);
  }


  @Test
  @Issue("MULE-18433")
  public void policyWithOperationAfterPolicy() throws Exception {
    policyManager.registerPolicyTemplate(fooPolicyFileBuilder.getArtifactFile());
    policyManager.registerPolicyTemplate(policyIncludingPluginFileBuilder.getArtifactFile());

    ApplicationFileBuilder applicationFileBuilder = createExtensionApplicationWithServices(APP_WITH_EXTENSION_PLUGIN_CONFIG,
                                                                                           helloExtensionV1Plugin);
    addPackedAppFromBuilder(applicationFileBuilder);

    startDeployment();
    assertApplicationDeploymentSuccess(applicationDeploymentListener, applicationFileBuilder.getId());

    policyManager.addPolicy(applicationFileBuilder.getId(), fooPolicyFileBuilder.getArtifactId(),
                            new PolicyParametrization(FOO_POLICY_ID, pointparameters -> true, 1,
                                                      singletonMap(POLICY_PROPERTY_KEY, POLICY_PROPERTY_VALUE),
                                                      getResourceFile("/fooPolicy.xml"), emptyList()));
    policyManager.addPolicy(applicationFileBuilder.getId(), policyIncludingPluginFileBuilder.getArtifactId(),
                            new PolicyParametrization(BAR_POLICY_ID, poinparameters -> true, 2, emptyMap(),
                                                      getResourceFile("/policyWithOperation.xml"), emptyList()));

    executeApplicationFlow("main");
    assertThat(invocationCount, equalTo(2));
  }

  @Test
  @Issue("MULE-18442")
  public void applicationPolicyHasNoOpPolicyManagerInjected() throws Exception {
    policyManager.registerPolicyTemplate(fooPolicyFileBuilder.getArtifactFile());

    ApplicationFileBuilder applicationFileBuilder = createExtensionApplicationWithServices(APP_WITH_EXTENSION_PLUGIN_CONFIG,
                                                                                           helloExtensionV1Plugin);
    addPackedAppFromBuilder(applicationFileBuilder);

    startDeployment();
    assertApplicationDeploymentSuccess(applicationDeploymentListener, applicationFileBuilder.getId());

    policyManager.addPolicy(applicationFileBuilder.getId(), fooPolicyFileBuilder.getArtifactId(),
                            new PolicyParametrization(FOO_POLICY_ID, parameters -> true, 1,
                                                      singletonMap(POLICY_PROPERTY_KEY, POLICY_PROPERTY_VALUE),
                                                      getResourceFile("/fooPolicy.xml"), emptyList()));

    List<Policy> policy = policyManager.findOperationPolicies(applicationFileBuilder.getId(), new PolicyPointcutParameters(null));

    assertThat(policy, hasSize(1));
    PolicyManager policyPolicyManager = ((MuleContextWithRegistry) policy.get(0).getPolicyChain().getMuleContext()).getRegistry()
        .lookupObject(PolicyManager.class);
    assertThat(policyPolicyManager, instanceOf(NoOpPolicyManager.class));
  }

  @Test
  public void appliesMultipleApplicationPolicies() throws Exception {
    policyManager.registerPolicyTemplate(fooPolicyFileBuilder.getArtifactFile());
    policyManager.registerPolicyTemplate(barPolicyFileBuilder.getArtifactFile());

    ApplicationFileBuilder applicationFileBuilder = createExtensionApplicationWithServices(APP_WITH_EXTENSION_PLUGIN_CONFIG,
                                                                                           helloExtensionV1Plugin);
    addPackedAppFromBuilder(applicationFileBuilder);

    startDeployment();
    assertApplicationDeploymentSuccess(applicationDeploymentListener, applicationFileBuilder.getId());

    policyManager.addPolicy(applicationFileBuilder.getId(), fooPolicyFileBuilder.getArtifactId(),
                            new PolicyParametrization(FOO_POLICY_ID, pointparameters -> true, 1,
                                                      singletonMap(POLICY_PROPERTY_KEY, POLICY_PROPERTY_VALUE),
                                                      getResourceFile("/fooPolicy.xml"), emptyList()));
    policyManager.addPolicy(applicationFileBuilder.getId(), barPolicyFileBuilder.getArtifactId(),
                            new PolicyParametrization(BAR_POLICY_ID, poinparameters -> true, 2, emptyMap(),
                                                      getResourceFile("/barPolicy.xml"), emptyList()));

    assertManualExecutionsCount(2);
  }

  @Test
  @Story(POLICY_REORDER)
  public void duplicatedApplicationPolicy() throws Exception {
    policyManager.registerPolicyTemplate(fooPolicyFileBuilder.getArtifactFile());

    ApplicationFileBuilder applicationFileBuilder = createExtensionApplicationWithServices(APP_WITH_EXTENSION_PLUGIN_CONFIG,
                                                                                           helloExtensionV1Plugin);
    addPackedAppFromBuilder(applicationFileBuilder);

    startDeployment();
    assertApplicationDeploymentSuccess(applicationDeploymentListener, applicationFileBuilder.getId());

    policyManager.addPolicy(applicationFileBuilder.getId(), fooPolicyFileBuilder.getArtifactId(),
                            new PolicyParametrization(FOO_POLICY_ID, pointparameters -> true, 1,
                                                      singletonMap(POLICY_PROPERTY_KEY, POLICY_PROPERTY_VALUE),
                                                      getResourceFile("/fooPolicy.xml"), emptyList()));

    expectedEx.expect(PolicyRegistrationException.class);
    expectedEx.expectMessage(format("Error occured registering policy '%s'", FOO_POLICY_ID));

    expectedEx.expectCause(allOf(
                                 is(instanceOf(IllegalArgumentException.class)),
                                 hasMessage(format("Policy already registered: '%s'", FOO_POLICY_ID))));

    policyManager.addPolicy(applicationFileBuilder.getId(), fooPolicyFileBuilder.getArtifactId(),
                            new PolicyParametrization(FOO_POLICY_ID, pointparameters -> true, 2,
                                                      singletonMap(POLICY_PROPERTY_KEY, POLICY_PROPERTY_VALUE),
                                                      getResourceFile("/fooPolicy.xml"), emptyList()));
  }

  @Test
  @Story(POLICY_REORDER)
  public void reorderApplicationPolicyDoesNotChangeParameters() throws Exception {
    policyManager.registerPolicyTemplate(fooPolicyFileBuilder.getArtifactFile());

    ApplicationFileBuilder applicationFileBuilder = createExtensionApplicationWithServices(APP_WITH_EXTENSION_PLUGIN_CONFIG,
                                                                                           helloExtensionV1Plugin);
    addPackedAppFromBuilder(applicationFileBuilder);

    startDeployment();
    assertApplicationDeploymentSuccess(applicationDeploymentListener, applicationFileBuilder.getId());

    policyManager.addPolicy(applicationFileBuilder.getId(), fooPolicyFileBuilder.getArtifactId(),
                            new PolicyParametrization(FOO_POLICY_ID, pointparameters -> true, 1,
                                                      singletonMap(POLICY_PROPERTY_KEY, POLICY_PROPERTY_VALUE),
                                                      getResourceFile("/fooPolicy.xml"), emptyList()));

    Map<String, String> parameters = new HashMap<>();
    parameters.put(POLICY_PROPERTY_KEY, FOO_POLICY_NAME);
    parameters.put(IS_POLICY_REORDER, "true");

    policyManager.addPolicy(applicationFileBuilder.getId(), fooPolicyFileBuilder.getArtifactId(),
                            new PolicyParametrization(FOO_POLICY_ID, pointparameters -> true, 2,
                                                      parameters,
                                                      getResourceFile("/fooPolicy.xml"), emptyList()));

    assertManualExecutionsCount(1);
    assertThat(policyParametrization, startsWith(POLICY_PROPERTY_VALUE));
  }

  @Test
  @Story(POLICY_REORDER)
  public void reorderApplicationPolicy() throws Exception {
    policyManager.registerPolicyTemplate(fooPolicyFileBuilder.getArtifactFile());
    policyManager.registerPolicyTemplate(barPolicyFileBuilder.getArtifactFile());

    ApplicationFileBuilder applicationFileBuilder = createExtensionApplicationWithServices(APP_WITH_EXTENSION_PLUGIN_CONFIG,
                                                                                           helloExtensionV1Plugin);
    addPackedAppFromBuilder(applicationFileBuilder);

    startDeployment();
    assertApplicationDeploymentSuccess(applicationDeploymentListener, applicationFileBuilder.getId());

    policyManager.addPolicy(applicationFileBuilder.getId(), fooPolicyFileBuilder.getArtifactId(),
                            new PolicyParametrization(FOO_POLICY_ID, pointparameters -> true, 1,
                                                      singletonMap(POLICY_PROPERTY_KEY, FOO_POLICY_NAME),
                                                      getResourceFile("/fooPolicy.xml"), emptyList()));

    policyManager.addPolicy(applicationFileBuilder.getId(), barPolicyFileBuilder.getArtifactId(),
                            new PolicyParametrization(BAR_POLICY_ID, poinparameters -> true, 2,
                                                      singletonMap(POLICY_PROPERTY_KEY, BAR_POLICY_NAME),
                                                      getResourceFile("/fooPolicy.xml"), emptyList()));

    assertManualExecutionsCount(2);
    assertThat(policyParametrization, startsWith(FOO_POLICY_NAME + BAR_POLICY_NAME));

    Map<String, String> parameters = new HashMap<>();
    parameters.put(POLICY_PROPERTY_KEY, FOO_POLICY_NAME);
    parameters.put(IS_POLICY_REORDER, "true");

    policyManager.addPolicy(applicationFileBuilder.getId(), fooPolicyFileBuilder.getArtifactId(),
                            new PolicyParametrization(FOO_POLICY_ID, pointparameters -> true, 3,
                                                      parameters,
                                                      getResourceFile("/fooPolicy.xml"), emptyList()));

    policyParametrization = "";
    assertManualExecutionsCount(4);
    assertThat(policyParametrization, startsWith(BAR_POLICY_NAME + FOO_POLICY_NAME));
  }

  @Test
  @Story(POLICY_SILENT_DEPLOY)
  public void silentApplicationPolicyLogsWithDebugLevel() throws Exception {
    policyManager.registerPolicyTemplate(fooPolicyFileBuilder.getArtifactFile());
    policyManager.registerPolicyTemplate(barPolicyFileBuilder.getArtifactFile());

    ApplicationFileBuilder applicationFileBuilder = createExtensionApplicationWithServices(APP_WITH_EXTENSION_PLUGIN_CONFIG,
                                                                                           helloExtensionV1Plugin);
    addPackedAppFromBuilder(applicationFileBuilder);

    startDeployment();
    assertApplicationDeploymentSuccess(applicationDeploymentListener, applicationFileBuilder.getId());

    TestLogger testLogger = getTestLogger("org.mule.runtime.core.internal.logging");
    testLogger.clearAll();
    testLogger.setEnabledLevels(DEBUG, INFO);

    Map<String, String> parameters = new HashMap<>();
    parameters.put(POLICY_PROPERTY_KEY, FOO_POLICY_NAME);
    parameters.put(IS_SILENT_DEPLOY_PARAMETER_NAME, "true");

    policyManager.addPolicy(applicationFileBuilder.getId(), fooPolicyFileBuilder.getArtifactId(),
                            new PolicyParametrization(FOO_POLICY_ID, pointparameters -> true, 1,
                                                      parameters,
                                                      getResourceFile("/fooPolicy.xml"), emptyList()));

    policyManager.addPolicy(applicationFileBuilder.getId(), barPolicyFileBuilder.getArtifactId(),
                            new PolicyParametrization(BAR_POLICY_ID, poinparameters -> true, 2,
                                                      singletonMap(POLICY_PROPERTY_KEY, BAR_POLICY_NAME),
                                                      getResourceFile("/fooPolicy.xml"), emptyList()));

    policyManager.removePolicy(applicationFileBuilder.getId(), FOO_POLICY_ID);
    policyManager.removePolicy(applicationFileBuilder.getId(), BAR_POLICY_ID);

    List<LoggingEvent> fooEvents = testLogger.getLoggingEvents().stream()
        .filter(event -> event.getMessage().contains(FOO_POLICY_ID)).collect(Collectors.toList());
    List<LoggingEvent> barEvents = testLogger.getLoggingEvents().stream()
        .filter(event -> event.getMessage().contains(BAR_POLICY_ID)).collect(Collectors.toList());

    assertThat(fooEvents, hasSize(2));
    fooEvents.forEach(event -> assertThat(event.getLevel(), is(DEBUG)));
    assertThat(barEvents, hasSize(2));
    barEvents.forEach(event -> assertThat(event.getLevel(), is(INFO)));
  }

  @Test
  public void appliesApplicationPolicyWithNotificationListener() throws Exception {
    policyManager.registerPolicyTemplate(fooPolicyFileBuilder.getArtifactFile());

    ApplicationFileBuilder applicationFileBuilder = createExtensionApplicationWithServices(APP_WITH_EXTENSION_PLUGIN_CONFIG,
                                                                                           helloExtensionV1Plugin);
    addPackedAppFromBuilder(applicationFileBuilder);

    startDeployment();
    assertApplicationDeploymentSuccess(applicationDeploymentListener, applicationFileBuilder.getId());

    List<Integer> notificationListenerActionIds = new ArrayList<>();
    PolicyNotificationListener<PolicyNotification> notificationListener = notification -> {
      if (MANUAL_EXECUTION_CORRELATION_ID.equals(notification.getInfo().getEvent().getCorrelationId())) {
        notificationListenerActionIds.add(notification.getAction().getActionId());
      }
    };

    policyManager.addPolicy(applicationFileBuilder.getId(), fooPolicyFileBuilder.getArtifactId(),
                            new PolicyParametrization(FOO_POLICY_ID, pointparameters -> true, 1,
                                                      singletonMap(POLICY_PROPERTY_KEY, POLICY_PROPERTY_VALUE),
                                                      getResourceFile("/fooPolicy.xml"),
                                                      singletonList(notificationListener)));

    assertManualExecutionsCount(1);
    new PollingProber(POLICY_NOTIFICATION_TIMEOUT, 100).check(new JUnitProbe() {

      @Override
      protected boolean test() throws Exception {
        assertThat(notificationListenerActionIds, hasSize(4));
        assertThat(notificationListenerActionIds, hasItems(PROCESS_START, BEFORE_NEXT, AFTER_NEXT, PROCESS_END));
        return true;
      }

    });
  }

  @Test
  public void failsToApplyBrokenApplicationPolicy() throws Exception {
    PolicyFileBuilder brokenPolicyFileBuilder =
        new PolicyFileBuilder(BAR_POLICY_NAME).describedBy(new MulePolicyModel.MulePolicyModelBuilder()
            .setMinMuleVersion(MIN_MULE_VERSION).setName(BAR_POLICY_NAME)
            .setRequiredProduct(MULE)
            .withBundleDescriptorLoader(
                                        createBundleDescriptorLoader(BAR_POLICY_NAME,
                                                                     MULE_POLICY_CLASSIFIER,
                                                                     PROPERTIES_BUNDLE_DESCRIPTOR_LOADER_ID))
            .withClassLoaderModelDescriptorLoader(
                                                  new MuleArtifactLoaderDescriptor(MULE_LOADER_ID, emptyMap()))
            .build());

    policyManager.registerPolicyTemplate(brokenPolicyFileBuilder.getArtifactFile());

    ApplicationFileBuilder applicationFileBuilder = createExtensionApplicationWithServices(APP_WITH_EXTENSION_PLUGIN_CONFIG,
                                                                                           helloExtensionV1Plugin);
    addPackedAppFromBuilder(applicationFileBuilder);

    startDeployment();
    assertApplicationDeploymentSuccess(applicationDeploymentListener, applicationFileBuilder.getId());

    try {
      policyManager.addPolicy(applicationFileBuilder.getId(), brokenPolicyFileBuilder.getArtifactId(),
                              new PolicyParametrization(FOO_POLICY_ID, parameters -> true, 1, emptyMap(),
                                                        getResourceFile("/brokenPolicy.xml"), emptyList()));
      fail("Policy application should have failed");
    } catch (PolicyRegistrationException expected) {
    }
  }

  @Test
  public void skipsApplicationPolicy() throws Exception {
    doApplicationPolicyExecutionTest(parameters -> false, 0, "");
  }

  @Test
  public void appliesApplicationPolicyUsingAsyncScope() throws Exception {
    doApplicationPolicyExecutionTest(parameters -> true, 1, POLICY_PROPERTY_VALUE, "/policy-using-async-scope.xml");
  }

  @Test
  public void removesApplicationPolicy() throws Exception {
    policyManager.registerPolicyTemplate(fooPolicyFileBuilder.getArtifactFile());

    ApplicationFileBuilder applicationFileBuilder = createExtensionApplicationWithServices(APP_WITH_EXTENSION_PLUGIN_CONFIG,
                                                                                           helloExtensionV1Plugin);
    addPackedAppFromBuilder(applicationFileBuilder);

    startDeployment();
    assertApplicationDeploymentSuccess(applicationDeploymentListener, applicationFileBuilder.getId());

    policyManager.addPolicy(applicationFileBuilder.getId(), fooPolicyFileBuilder.getArtifactId(),
                            new PolicyParametrization(FOO_POLICY_ID, parameters -> true, 1,
                                                      singletonMap(POLICY_PROPERTY_KEY, POLICY_PROPERTY_VALUE),
                                                      getResourceFile("/fooPolicy.xml"), emptyList()));

    assertManualExecutionsCount(1);

    policyManager.removePolicy(applicationFileBuilder.getId(), FOO_POLICY_ID);

    assertManualExecutionsCount(1);
  }

  @Test
  @Issue("MULE-19191")
  public void removesApplicationPolicyAndDoesNotPersistStoppedApplicationOrFlowsDeploymentProperties() throws Exception {
    policyManager.registerPolicyTemplate(fooPolicyFileBuilder.getArtifactFile());

    ApplicationFileBuilder applicationFileBuilder = createExtensionApplicationWithServices(APP_WITH_EXTENSION_PLUGIN_CONFIG,
                                                                                           helloExtensionV1Plugin);
    addPackedAppFromBuilder(applicationFileBuilder);

    startDeployment();
    assertApplicationDeploymentSuccess(applicationDeploymentListener, applicationFileBuilder.getId());

    policyManager.addPolicy(applicationFileBuilder.getId(), fooPolicyFileBuilder.getArtifactId(),
                            new PolicyParametrization(FOO_POLICY_ID, parameters -> true, 1,
                                                      singletonMap(POLICY_PROPERTY_KEY, POLICY_PROPERTY_VALUE),
                                                      getResourceFile("/fooPolicy.xml"), emptyList()));

    assertManualExecutionsCount(1);

    policyManager.removePolicy(applicationFileBuilder.getId(), FOO_POLICY_ID);

    Optional<Properties> deploymentProperties = getPersistedDeploymentProperties(applicationFileBuilder.getId());
    assertThat(deploymentProperties.isPresent(), is(true));
    assertThat(deploymentProperties.get().get(START_ARTIFACT_ON_DEPLOYMENT_PROPERTY), is(nullValue()));

    Optional<Properties> flowDeploymentProperties = getPersistedFlowDeploymentProperties(applicationFileBuilder.getId());
    assertThat(flowDeploymentProperties.isPresent(), is(true));

    final Application app = findApp(applicationFileBuilder.getId(), 1);
    for (Flow flow : app.getArtifactContext().getRegistry().lookupAllByType(Flow.class)) {
      assertThat(flowDeploymentProperties.get().get(flow.getName() + "_" + START_FLOW_ON_DEPLOYMENT_PROPERTY), is("true"));
    }
    assertStatus(app, STARTED);
  }


  @Test
  @Feature(CLASSLOADING_ISOLATION)
  public void appliesApplicationPolicyUsingAppPlugin() throws Exception {
    PolicyFileBuilder policyFileBuilder = policyUsingAppPluginFileBuilder;
    if (isIsolatedPolicy()) {
      // Since the feature flag ENABLE_POLICY_ISOLATION is active, the policy must declare as dependencies all the extensions
      policyFileBuilder = policyFileBuilder.dependingOn(helloExtensionV1Plugin);
    }
    policyManager.registerPolicyTemplate(policyFileBuilder.getArtifactFile());

    ApplicationFileBuilder applicationFileBuilder = createExtensionApplicationWithServices(APP_WITH_EXTENSION_PLUGIN_CONFIG,
                                                                                           helloExtensionV1Plugin);
    addPackedAppFromBuilder(applicationFileBuilder);

    startDeployment();
    assertApplicationDeploymentSuccess(applicationDeploymentListener, applicationFileBuilder.getId());

    policyManager.addPolicy(applicationFileBuilder.getId(), policyUsingAppPluginFileBuilder.getArtifactId(),
                            new PolicyParametrization(BAR_POLICY_ID, s -> true, 1, emptyMap(),
                                                      getResourceFile("/appPluginPolicy.xml"), emptyList()));

    assertManualExecutionsCount(1);
  }

  @Test
  @Feature(CLASSLOADING_ISOLATION)
  public void appliesApplicationPolicyUsingPluginOnlyInPolicy() throws Exception {
    policyManager.registerPolicyTemplate(policyIncludingPluginFileBuilder.getArtifactFile());

    ApplicationFileBuilder applicationFileBuilder =
        createExtensionApplicationWithServices(APP_WITH_SIMPLE_EXTENSION_CONFIG, createSingleExtensionPlugin());
    addPackedAppFromBuilder(applicationFileBuilder);

    startDeployment();
    assertApplicationDeploymentSuccess(applicationDeploymentListener, applicationFileBuilder.getId());

    policyManager.addPolicy(applicationFileBuilder.getId(), policyIncludingPluginFileBuilder.getArtifactId(),
                            new PolicyParametrization(BAR_POLICY_ID, s -> true, 1, emptyMap(),
                                                      getResourceFile("/appPluginPolicy.xml"), emptyList()));

    executeApplicationFlow("main");
    assertThat(invocationCount, equalTo(1));
  }

  @Test
  @Feature(CLASSLOADING_ISOLATION)
  public void appliesApplicationPolicyIncludingPlugin() throws Exception {
    ArtifactPluginFileBuilder simpleExtensionPlugin = createSingleExtensionPlugin();

    policyManager.registerPolicyTemplate(policyIncludingPluginFileBuilder.getArtifactFile());

    ApplicationFileBuilder applicationFileBuilder = createExtensionApplicationWithServices(APP_WITH_SIMPLE_EXTENSION_CONFIG,
                                                                                           simpleExtensionPlugin);
    addPackedAppFromBuilder(applicationFileBuilder);

    startDeployment();
    assertApplicationDeploymentSuccess(applicationDeploymentListener, applicationFileBuilder.getId());

    policyManager.addPolicy(applicationFileBuilder.getId(), policyIncludingPluginFileBuilder.getArtifactId(),
                            new PolicyParametrization(FOO_POLICY_ID, s -> true, 1, emptyMap(),
                                                      getResourceFile("/appPluginPolicy.xml"), emptyList()));

    executeApplicationFlow("main");
    assertThat(invocationCount, equalTo(1));
  }

  @Test
  @Feature(CLASSLOADING_ISOLATION)
  public void appliesApplicationPolicyDuplicatingExtensionPlugin() throws Exception {
    policyManager.registerPolicyTemplate(policyIncludingPluginFileBuilder.getArtifactFile());

    ApplicationFileBuilder applicationFileBuilder = createExtensionApplicationWithServices(APP_WITH_EXTENSION_PLUGIN_CONFIG,
                                                                                           helloExtensionV1Plugin);
    addPackedAppFromBuilder(applicationFileBuilder);

    startDeployment();
    assertApplicationDeploymentSuccess(applicationDeploymentListener, applicationFileBuilder.getId());

    policyManager.addPolicy(applicationFileBuilder.getId(), policyIncludingPluginFileBuilder.getArtifactId(),
                            new PolicyParametrization(FOO_POLICY_ID, s -> true, 1, emptyMap(),
                                                      getResourceFile("/appPluginPolicy.xml"), emptyList()));

    executeApplicationFlow("main");
  }

  @Test
  @Feature(CLASSLOADING_ISOLATION)
  public void appliesApplicationPolicyUsingModuleThatUsesPlugin() throws Exception {
    PolicyFileBuilder policyIncludingByePlugin = createPolicyIncludingByePlugin();
    policyManager.registerPolicyTemplate(policyIncludingByePlugin.getArtifactFile());

    ApplicationFileBuilder applicationFileBuilder =
        createExtensionApplicationWithServices(APP_WITH_SIMPLE_EXTENSION_CONFIG, createSingleExtensionPlugin());
    addPackedAppFromBuilder(applicationFileBuilder);

    startDeployment();
    assertApplicationDeploymentSuccess(applicationDeploymentListener, applicationFileBuilder.getId());

    policyManager.addPolicy(applicationFileBuilder.getId(), policyIncludingByePlugin.getArtifactId(),
                            new PolicyParametrization(BAR_POLICY_ID, s -> true, 1, emptyMap(),
                                                      getResourceFile("/module-using-bye-policy.xml"), emptyList()));

    executeApplicationFlow("main");
    assertThat(invocationCount, equalTo(1));
  }

  @Test
  @Feature(CLASSLOADING_ISOLATION)
  public void appliesApplicationPolicyUsingModuleThatUsesPluginDuplicatedInTheApplication() throws Exception {
    PolicyFileBuilder policyIncludingByePlugin = createPolicyIncludingByePlugin();
    policyManager.registerPolicyTemplate(policyIncludingByePlugin.getArtifactFile());

    ApplicationFileBuilder applicationFileBuilder =
        createExtensionApplicationWithServices(APP_WITH_MODULE_BYE_CONFIG, createSingleExtensionPlugin(), byeXmlExtensionPlugin);
    addPackedAppFromBuilder(applicationFileBuilder);

    startDeployment();
    assertApplicationDeploymentSuccess(applicationDeploymentListener, applicationFileBuilder.getId());

    policyManager.addPolicy(applicationFileBuilder.getId(), policyIncludingByePlugin.getArtifactId(),
                            new PolicyParametrization(BAR_POLICY_ID, s -> true, 1, emptyMap(),
                                                      getResourceFile("/module-using-bye-policy.xml"), emptyList()));

    executeApplicationFlow("main");
    assertThat(invocationCount, equalTo(1));
  }

  @Test
  @Feature(CLASSLOADING_ISOLATION)
  public void appliesApplicationPolicyDuplicatingPlugin() throws Exception {

    policyManager.registerPolicyTemplate(exceptionThrowingPluginImportingPolicyFileBuilder.getArtifactFile());


    ApplicationFileBuilder applicationFileBuilder = createExtensionApplicationWithServices(APP_WITH_EXTENSION_PLUGIN_CONFIG,
                                                                                           exceptionThrowingPlugin,
                                                                                           helloExtensionV1Plugin);
    addPackedAppFromBuilder(applicationFileBuilder);

    startDeployment();
    assertApplicationDeploymentSuccess(applicationDeploymentListener, applicationFileBuilder.getId());

    policyManager.addPolicy(applicationFileBuilder.getId(), exceptionThrowingPluginImportingPolicyFileBuilder.getArtifactId(),
                            new PolicyParametrization(EXCEPTION_POLICY_NAME, s -> true, 1, emptyMap(),
                                                      getResourceFile("/exceptionThrowingPolicy.xml"), emptyList()));
    try {
      executeApplicationFlow("main");
      fail("Flow execution was expected to throw an exception");
    } catch (MuleRuntimeException expected) {
      assertThat(expected.getCause().getCause().getClass().getName(), is(equalTo("org.exception.CustomException")));
    }
  }

  @Test
  @Feature(CLASSLOADING_ISOLATION)
  public void appliesApplicationPolicyDuplicatingPluginOnDomain() throws Exception {

    addPackedDomainFromBuilder(exceptionThrowingPluginImportingDomain);

    policyManager.registerPolicyTemplate(exceptionThrowingPluginImportingPolicyFileBuilder.getArtifactFile());

    ApplicationFileBuilder applicationFileBuilder = createExtensionApplicationWithServices(APP_WITH_EXTENSION_PLUGIN_CONFIG,
                                                                                           helloExtensionV1Plugin)
                                                                                               .dependingOn(exceptionThrowingPluginImportingDomain);
    addPackedAppFromBuilder(applicationFileBuilder);

    startDeployment();
    assertApplicationDeploymentSuccess(applicationDeploymentListener, applicationFileBuilder.getId());

    policyManager.addPolicy(applicationFileBuilder.getId(), exceptionThrowingPluginImportingPolicyFileBuilder.getArtifactId(),
                            new PolicyParametrization(EXCEPTION_POLICY_NAME, s -> true, 1, emptyMap(),
                                                      getResourceFile("/exceptionThrowingPolicy.xml"), emptyList()));
    try {
      executeApplicationFlow("main");
      fail("Flow execution was expected to throw an exception");
    } catch (MuleRuntimeException expected) {
      assertThat(expected.getCause().getCause().getClass().getName(), is(equalTo("org.exception.CustomException")));
    }
  }

  @Test
  @Ignore("MULE-15842: fix once we support declaring share objects plugins in policies")
  public void failsToApplyApplicationPolicyWithPluginVersionMismatch() throws Exception {
    policyManager.registerPolicyTemplate(policyIncludingHelloPluginV2FileBuilder.getArtifactFile());

    ApplicationFileBuilder applicationFileBuilder = createExtensionApplicationWithServices(APP_WITH_EXTENSION_PLUGIN_CONFIG,
                                                                                           helloExtensionV1Plugin);
    addPackedAppFromBuilder(applicationFileBuilder);

    startDeployment();
    assertApplicationDeploymentSuccess(applicationDeploymentListener, applicationFileBuilder.getId());

    try {
      policyManager.addPolicy(applicationFileBuilder.getId(), policyIncludingHelloPluginV2FileBuilder.getArtifactId(),
                              new PolicyParametrization(FOO_POLICY_ID, s -> true, 1, emptyMap(),
                                                        getResourceFile("/appPluginPolicy.xml"), emptyList()));
      fail("Policy application should have failed");
    } catch (PolicyRegistrationException expected) {
    }
  }

  @Test
  public void injectsObjectsFromApplicationIntoPolicies() throws Exception {
    final ArtifactPluginFileBuilder bootstrapPluginFileBuilder = new ArtifactPluginFileBuilder("bootstrapPlugin")
        .containingResource("plugin-bootstrap.properties", BOOTSTRAP_PROPERTIES)
        .containingClass(echoTestClassFile, "org/foo/EchoTest.class")
        .configuredWith(EXPORTED_RESOURCE_PROPERTY, BOOTSTRAP_PROPERTIES);

    PolicyFileBuilder fooPolicyFileBuilder = createInjectedPolicy();
    policyManager.registerPolicyTemplate(fooPolicyFileBuilder.getArtifactFile());

    ApplicationFileBuilder applicationFileBuilder = createExtensionApplicationWithServices(APP_WITH_SIMPLE_EXTENSION_CONFIG,
                                                                                           createSingleExtensionPlugin(),
                                                                                           bootstrapPluginFileBuilder);
    addPackedAppFromBuilder(applicationFileBuilder);

    startDeployment();
    assertApplicationDeploymentSuccess(applicationDeploymentListener, applicationFileBuilder.getId());

    policyManager.addPolicy(applicationFileBuilder.getId(), fooPolicyFileBuilder.getArtifactId(),
                            new PolicyParametrization(FOO_POLICY_ID, s -> true, 1, emptyMap(),
                                                      getResourceFile("/appPluginPolicy.xml"), emptyList()));

    executeApplicationFlow("main");
    assertThat(invocationCount, equalTo(1));
  }

  @Test
  @Feature(CLASSLOADING_ISOLATION)
  public void appliesPolicyThatUsesPolicyClassOnExpression() throws Exception {
    ArtifactPluginFileBuilder simpleExtensionPlugin = createSingleExtensionPlugin();

    policyManager.registerPolicyTemplate(policyWithPluginAndResource().getArtifactFile());

    ApplicationFileBuilder applicationFileBuilder = createExtensionApplicationWithServices(APP_WITH_SIMPLE_EXTENSION_CONFIG,
                                                                                           simpleExtensionPlugin);

    addPackedAppFromBuilder(applicationFileBuilder);

    startDeployment();
    assertApplicationDeploymentSuccess(applicationDeploymentListener, applicationFileBuilder.getId());

    policyManager.addPolicy(applicationFileBuilder.getId(), policyIncludingPluginFileBuilder.getArtifactId(),
                            new PolicyParametrization(BAR_POLICY_ID, s -> true, 1, emptyMap(),
                                                      getResourceFile("/policy-using-policy-class-in-expression.xml"),
                                                      emptyList()));

    executeApplicationFlow("main");
    assertThat(invocationCount, equalTo(1));
  }

  @Test
  @Issue("MULE-18196")
  @Description("The application doesn't declare an ErrorType that is needed by the policy, but the policy already has it in its own ErrorType repository")
  public void appliesPolicyUsingErrorTypeAndHavingDependencies() throws Exception {
    configureSimpleAppAndPolicyWithErrorDeclarationExtensionAndErrorMapping();

    executeApplicationFlow("main");
    assertThat(invocationCount, equalTo(1));
  }

  @Test
  @Issue("MULE-18196")
  @Description("The application declares an ErrorType that is needed by the policy and but the policy doesn't have it in its own ErrorType repository")
  public void appliesPolicyUsingErrorTypeDeclaredOnAppDependency() throws Exception {
    if (parseBoolean(enablePolicyIsolationSystemProperty.getValue())) {
      expectPolicyRegistrationException();
    }

    configureAppWithErrorDeclarationAndPolicyWithErrorMapping();
    executeApplicationFlow("main");
  }

  @Test
  @Issue("MULE-18196")
  public void appliesPolicyAndAppWithCollidingErrorNamespace() throws Exception {
    ArtifactPluginFileBuilder simpleExtensionPlugin = createSingleExtensionPlugin();

    policyManager.registerPolicyTemplate(policyWithPluginAndResource().getArtifactFile());

    ApplicationFileBuilder applicationFileBuilder = createExtensionApplicationWithServices("app-with-colliding-error.xml",
                                                                                           simpleExtensionPlugin);

    addPackedAppFromBuilder(applicationFileBuilder);

    startDeployment();
    assertApplicationDeploymentSuccess(applicationDeploymentListener, applicationFileBuilder.getId());

    policyManager.addPolicy(applicationFileBuilder.getId(), policyIncludingPluginFileBuilder.getArtifactId(),
                            new PolicyParametrization(BAR_POLICY_ID, s -> true, 1, emptyMap(),
                                                      getResourceFile("/policy-with-colliding-error.xml"),
                                                      emptyList()));

    executeApplicationFlow("main");
    assertThat(invocationCount, equalTo(1));
  }

  private void expectPolicyRegistrationException() {
    expectedEx.expect(PolicyRegistrationException.class);
    expectedEx.expectMessage("Error occured registering policy 'barPolicy'");
    expectedEx.expectCause(instanceOf(InitialisationException.class));
  }

  @Test
  @Issue("MULE-18284")
  @Description("An app and policy depending on a same extension, the policy can handle errors from the extension")
  public void appliesPolicyAndAppWithSameExtensionDeclaringError() throws Exception {
    ArtifactPluginFileBuilder withErrorDeclarationExtensionPlugin = createWithErrorDeclarationExtensionPlugin();

    policyManager.registerPolicyTemplate(policyIncludingPluginFileBuilder
        .dependingOn(withErrorDeclarationExtensionPlugin).getArtifactFile());

    ApplicationFileBuilder applicationFileBuilder =
        createExtensionApplicationWithServices("app-with-error-declaration-extension.xml", withErrorDeclarationExtensionPlugin);

    addPackedAppFromBuilder(applicationFileBuilder);

    startDeployment();
    assertApplicationDeploymentSuccess(applicationDeploymentListener, applicationFileBuilder.getId());

    policyManager.addPolicy(applicationFileBuilder.getId(), policyIncludingPluginFileBuilder
        .dependingOn(withErrorDeclarationExtensionPlugin).getArtifactId(),
                            new PolicyParametrization(BAR_POLICY_ID, s -> true, 1, emptyMap(),
                                                      getResourceFile("/policy-with-error-declaration-extension.xml"),
                                                      emptyList()));

    executeApplicationFlow("main");
    assertThat(invocationCount, equalTo(1));
  }

  @Test
  @Issue("MULE-19226")
  @Feature(CLASSLOADING_ISOLATION)
  public void policyDependencyInjectionIsolation() throws Exception {
    try {
      deployAppAndApplyPolicyWithDependencyInjectionExtension("app-with-extension-declaring-internal-dependency.xml",
                                                              "policy-with-extension-declaring-internal-dependency.xml");
    } catch (PolicyRegistrationException e) {
      if (!isIsolatedPolicy()) {
        // Expected error since the feature flag ENABLE_POLICY_ISOLATION (which fixes this error) is set to false.
        return;
      } else {
        // Unexpected error.
        throw e;
      }
    }
    assertThat("Only an isolated policy should be able to inject all dependencies", isIsolatedPolicy(), is(true));
    executeApplicationFlow("main");
    assertThat(invocationCount, equalTo(1));
  }

  @Test
  @Feature(CLASSLOADING_ISOLATION)
  @Feature(APPLICATION_CONFIGURATION)
  public void policyImplicitConfigurationIsolation() throws Exception {
    deployAppAndApplyPolicyWithPolicyConfigurationExtension("app-with-policy-isolation-extension-and-implicit-configuration.xml",
                                                            "policy-with-policy-isolation-extension-and-implicit-configuration.xml");
    executeApplicationFlow("main");
    assertThat(invocationCount, equalTo(1));
  }

  @Test
  @Feature(CLASSLOADING_ISOLATION)
  @Feature(APPLICATION_CONFIGURATION)
  public void policyExplicitConfigurationIsolation() throws Exception {
    deployAppAndApplyPolicyWithPolicyConfigurationExtension("app-with-policy-isolation-extension-and-explicit-configuration.xml",
                                                            "policy-with-policy-isolation-extension-and-explicit-configuration.xml");
    executeApplicationFlow("main");
    assertThat(invocationCount, equalTo(1));
  }

  @Test
  @Feature(CLASSLOADING_ISOLATION)
  @Feature(APPLICATION_CONFIGURATION)
  public void policyExplicitConfigurationInheritance() throws Exception {
    try {
      deployAppAndApplyPolicyWithPolicyConfigurationExtension("app-with-policy-isolation-extension-and-explicit-configuration.xml",
                                                              "policy-with-policy-isolation-extension-and-inherited-explicit-configuration.xml");
    } catch (PolicyRegistrationException e) {
      if (isIsolatedPolicy()) {
        // Expected error since the feature flag ENABLE_POLICY_ISOLATION (which prevents the policy from using the application
        // configuration) is set to true.
        return;
      } else {
        // Unexpected error.
        throw e;
      }
    }
    assertThat("Only a non-isolated policy should be able to use the application configuration", isIsolatedPolicy(), is(false));
    executeApplicationFlow("main");
    assertThat(invocationCount, equalTo(1));
  }

  @Test
  @Feature(CLASSLOADING_ISOLATION)
  public void policyWithExtensionUsingObjectStore() throws Exception {
    policyManager.registerPolicyTemplate(policyWithPluginUsingObjectStore().getArtifactFile());

    ArtifactPluginFileBuilder simpleExtensionPlugin = createSingleExtensionPlugin();
    ApplicationFileBuilder applicationFileBuilder = createExtensionApplicationWithServices(APP_WITH_SIMPLE_EXTENSION_CONFIG,
                                                                                           simpleExtensionPlugin);

    addPackedAppFromBuilder(applicationFileBuilder);

    startDeployment();
    assertApplicationDeploymentSuccess(applicationDeploymentListener, applicationFileBuilder.getId());

    policyManager.addPolicy(applicationFileBuilder.getId(), policyWithPluginUsingObjectStore().getArtifactId(),
                            new PolicyParametrization(BAR_POLICY_ID, s -> true, 1, emptyMap(),
                                                      getResourceFile("/policy-using-object-store.xml"),
                                                      emptyList()));

    executeApplicationFlow("main");
  }

  @Test
  @Issue("MULE-18682")
  @Feature(CLASSLOADING_ISOLATION)
  public void policyUpgradeOfPolicyWithExtensionUsingObjectStore() throws Exception {
    PolicyTemplateDescriptor templateDescriptor100 =
        policyManager.registerPolicyTemplate(policyWithPluginUsingObjectStore("1.0.0").getArtifactFile());
    PolicyTemplateDescriptor templateDescriptor101 =
        policyManager.registerPolicyTemplate(policyWithPluginUsingObjectStore("1.0.1").getArtifactFile());

    ArtifactPluginFileBuilder simpleExtensionPlugin = createSingleExtensionPlugin();
    ApplicationFileBuilder applicationFileBuilder = createExtensionApplicationWithServices(APP_WITH_SIMPLE_EXTENSION_CONFIG,
                                                                                           simpleExtensionPlugin);
    addPackedAppFromBuilder(applicationFileBuilder);

    startDeployment();
    assertApplicationDeploymentSuccess(applicationDeploymentListener, applicationFileBuilder.getId());

    PolicyParametrization parameters = new PolicyParametrization(BAR_POLICY_ID, s -> true, 1, emptyMap(),
                                                                 getResourceFile("/policy-using-object-store.xml"),
                                                                 emptyList());
    policyManager.addPolicy(applicationFileBuilder.getId(), templateDescriptor100, parameters);

    executeApplicationFlow("main");

    PolicyParametrization newParameters = new PolicyParametrization(FOO_POLICY_ID, s -> true, 1, emptyMap(),
                                                                    getResourceFile("/policy-using-object-store.xml"),
                                                                    emptyList());
    policyManager.addPolicy(applicationFileBuilder.getId(), templateDescriptor101, newParameters);
    policyManager.removePolicy(applicationFileBuilder.getId(), parameters.getId());

    executeApplicationFlow("main");
  }

  @Test
  public void redeployPolicyWithSecurityManagerDefined() throws Exception {
    ArtifactPluginFileBuilder simpleExtensionPlugin = createSingleExtensionPlugin();

    policyManager.registerPolicyTemplate(policyWithPluginAndResource().getArtifactFile());

    ApplicationFileBuilder applicationFileBuilder = createExtensionApplicationWithServices(APP_WITH_SIMPLE_EXTENSION_CONFIG,
                                                                                           simpleExtensionPlugin);

    addPackedAppFromBuilder(applicationFileBuilder);

    startDeployment();
    assertApplicationDeploymentSuccess(applicationDeploymentListener, applicationFileBuilder.getId());

    PolicyParametrization policy = new PolicyParametrization(BAR_POLICY_ID, s -> true, 1, emptyMap(),
                                                             getResourceFile("/policy-using-security-manager.xml"),
                                                             emptyList());

    policyManager.addPolicy(applicationFileBuilder.getId(), policyIncludingPluginFileBuilder.getArtifactId(), policy);

    executeApplicationFlow("main");
    assertThat(invocationCount, equalTo(1));

    policyManager.removePolicy(applicationFileBuilder.getId(), BAR_POLICY_ID);
    policyManager.addPolicy(applicationFileBuilder.getId(), policyIncludingPluginFileBuilder.getArtifactId(), policy);

    executeApplicationFlow("main");
    assertThat(invocationCount, equalTo(2));
  }

  @Test
  @Feature(POLICY_DEPLOYMENT)
  @Issue("W-17340911")
  public void appliesPolicyAndVerifiesClassLoaderIsolation() throws Exception {
    // domain with HTTP plugin
    DomainFileBuilder domainFileBuilder =
        new DomainFileBuilder("domain").definedBy("empty-domain-config.xml");
    domainFileBuilder.dependingOn(httpPlugin);

    // policy with HTTP plugin
    PolicyFileBuilder policyFileBuilder =
        new PolicyFileBuilder(FOO_POLICY_NAME).describedBy(new MulePolicyModel.MulePolicyModelBuilder()
            .setMinMuleVersion(MIN_MULE_VERSION)
            .setName(FOO_POLICY_NAME)
            .setRequiredProduct(MULE)
            .withBundleDescriptorLoader(
                                        createBundleDescriptorLoader(FOO_POLICY_NAME,
                                                                     MULE_POLICY_CLASSIFIER,
                                                                     PROPERTIES_BUNDLE_DESCRIPTOR_LOADER_ID))
            .withClassLoaderModelDescriptorLoader(
                                                  new MuleArtifactLoaderDescriptor(MULE_LOADER_ID, emptyMap()))
            .build());


    policyFileBuilder.dependingOn(httpPlugin);
    policyManager.registerPolicyTemplate(policyFileBuilder.getArtifactFile());

    addPackedDomainFromBuilder(domainFileBuilder);

    // deploy app that depends on the domain
    ApplicationFileBuilder applicationFileBuilder = createExtensionApplicationWithServices(APP_WITH_EXTENSION_PLUGIN_CONFIG,
                                                                                           helloExtensionV1Plugin)
                                                                                               .dependingOn(domainFileBuilder);
    addPackedAppFromBuilder(applicationFileBuilder);

    startDeployment();
    assertApplicationDeploymentSuccess(applicationDeploymentListener, applicationFileBuilder.getId());

    // apply policy to the application
    policyManager.addPolicy(applicationFileBuilder.getId(), policyFileBuilder.getArtifactId(),
                            new PolicyParametrization(FOO_POLICY_ID, parameters -> true, 1,
                                                      singletonMap(POLICY_PROPERTY_KEY, POLICY_PROPERTY_VALUE),
                                                      getResourceFile("/fooPolicy.xml"), emptyList()));

    RegionClassLoader appRegionClassLoader = findApp(applicationFileBuilder.getId(), 1).getRegionClassLoader();
    MuleDeployableArtifactClassLoader policyArtifactClassLoader = null;
    for (ArtifactClassLoader artifactLoader : appRegionClassLoader.getArtifactPluginClassLoaders()) {
      if (artifactLoader.getArtifactDescriptor() != null &&
          FOO_POLICY_NAME.equals(artifactLoader.getArtifactDescriptor().getName())) {
        policyArtifactClassLoader = (MuleDeployableArtifactClassLoader) artifactLoader;
      }
    }

    if (isClassLoaderHierarchyValid(policyArtifactClassLoader)) {
      ClassLoader policyClassLoader = policyArtifactClassLoader.getParent();
      ClassLoader newRegionClassLoader = policyClassLoader.getParent();

      if (isPolicyIsolationFullyEnabled(enablePolicyIsolationSystemProperty, separateCLforPolicyIsolationSystemProperty)) {
        assertThat(newRegionClassLoader instanceof IsolatedPolicyClassLoader, is(true));
        assertThat(((IsolatedPolicyClassLoader) newRegionClassLoader).getClassLoaderLookupPolicy()
            .getPackageLookupStrategy("org.mule.extension.http.api"), is(CHILD_FIRST));
      } else {
        assertThat(newRegionClassLoader instanceof RegionClassLoader, is(true));
        assertThat(((RegionClassLoader) newRegionClassLoader).getClassLoaderLookupPolicy()
            .getPackageLookupStrategy("org.mule.extension.http.api"), is(PARENT_FIRST));
      }
    }

    executeApplicationFlow("main");
    assertThat(invocationCount, equalTo(1));
  }

  private boolean isClassLoaderHierarchyValid(ClassLoader classLoader) {
    return classLoader != null && classLoader.getParent() != null && classLoader.getParent().getParent() != null;
  }

  private boolean isPolicyIsolationFullyEnabled(SystemProperty enablePolicyIsolationSystemProperty,
                                                SystemProperty separateCLforPolicyIsolationSystemProperty) {
    return enablePolicyIsolationSystemProperty.getValue().equals("true")
        && separateCLforPolicyIsolationSystemProperty.getValue().equals("true");
  }

  private void doApplicationPolicyExecutionTest(PolicyPointcut pointcut, int expectedPolicyInvocations,
                                                Object expectedPolicyParametrization)
      throws Exception {
    doApplicationPolicyExecutionTest(pointcut, expectedPolicyInvocations, expectedPolicyParametrization, "/fooPolicy.xml");
  }

  private void doApplicationPolicyExecutionTest(PolicyPointcut pointcut, int expectedPolicyInvocations,
                                                Object expectedPolicyParametrization, String policyFile)
      throws Exception {
    policyManager.registerPolicyTemplate(fooPolicyFileBuilder.getArtifactFile());

    ApplicationFileBuilder applicationFileBuilder = createExtensionApplicationWithServices(APP_WITH_EXTENSION_PLUGIN_CONFIG,
                                                                                           helloExtensionV1Plugin);
    addPackedAppFromBuilder(applicationFileBuilder);

    startDeployment();
    assertApplicationDeploymentSuccess(applicationDeploymentListener, applicationFileBuilder.getId());

    policyManager.addPolicy(applicationFileBuilder.getId(), fooPolicyFileBuilder.getArtifactId(),
                            new PolicyParametrization(FOO_POLICY_ID, pointcut, 1,
                                                      singletonMap(POLICY_PROPERTY_KEY, POLICY_PROPERTY_VALUE),
                                                      getResourceFile(policyFile), emptyList()));


    executeApplicationFlow("main");
    assertThat(invocationCount, equalTo(expectedPolicyInvocations));
    assertThat(policyParametrization, equalTo(expectedPolicyParametrization));
  }

  private PolicyFileBuilder policyWithPluginAndResource() {
    MulePolicyModel.MulePolicyModelBuilder mulePolicyModelBuilder = new MulePolicyModel.MulePolicyModelBuilder()
        .setMinMuleVersion(MIN_MULE_VERSION).setName(BAZ_POLICY_NAME)
        .setRequiredProduct(Product.MULE)
        .withBundleDescriptorLoader(createBundleDescriptorLoader(BAZ_POLICY_NAME, MULE_POLICY_CLASSIFIER,
                                                                 PROPERTIES_BUNDLE_DESCRIPTOR_LOADER_ID))
        .withClassLoaderModelDescriptorLoader(new MuleArtifactLoaderDescriptor(MULE_LOADER_ID, emptyMap()));
    return new PolicyFileBuilder(BAZ_POLICY_NAME).describedBy(mulePolicyModelBuilder
        .build())
        .containingClass(echoTestClassFile, "org/foo/EchoTest.class")
        .dependingOn(helloExtensionV1Plugin);
  }

  private PolicyFileBuilder policyWithPluginUsingObjectStore() {
    return policyWithPluginUsingObjectStore("1.0.0");
  }

  private PolicyFileBuilder policyWithPluginUsingObjectStore(String version) {
    MulePolicyModel.MulePolicyModelBuilder mulePolicyModelBuilder = new MulePolicyModel.MulePolicyModelBuilder()
        .setMinMuleVersion(MIN_MULE_VERSION).setName(BAZ_POLICY_NAME)
        .setRequiredProduct(Product.MULE)
        .withBundleDescriptorLoader(createBundleDescriptorLoader(BAZ_POLICY_NAME, MULE_POLICY_CLASSIFIER,
                                                                 PROPERTIES_BUNDLE_DESCRIPTOR_LOADER_ID, version))
        .withClassLoaderModelDescriptorLoader(new MuleArtifactLoaderDescriptor(MULE_LOADER_ID, emptyMap()));
    return new PolicyFileBuilder(BAZ_POLICY_NAME).describedBy(mulePolicyModelBuilder
        .build())
        .dependingOn(usingObjectStorePlugin)
        .withVersion(version);
  }

  private PolicyFileBuilder createInjectedPolicy() throws URISyntaxException {
    ArtifactPluginFileBuilder injectedExtension = createInjectedHelloExtensionPluginFileBuilder();

    return new PolicyFileBuilder(FOO_POLICY_NAME).describedBy(new MulePolicyModel.MulePolicyModelBuilder()
        .setMinMuleVersion(MIN_MULE_VERSION)
        .setName(FOO_POLICY_NAME)
        .setRequiredProduct(MULE)
        .withBundleDescriptorLoader(
                                    createBundleDescriptorLoader(FOO_POLICY_NAME,
                                                                 MULE_POLICY_CLASSIFIER,
                                                                 PROPERTIES_BUNDLE_DESCRIPTOR_LOADER_ID))
        .withClassLoaderModelDescriptorLoader(
                                              new MuleArtifactLoaderDescriptor(MULE_LOADER_ID,
                                                                               emptyMap()))
        .build())
        .dependingOn(injectedExtension);
  }

  private ArtifactPluginFileBuilder createSingleExtensionPlugin() {
    MulePluginModel.MulePluginModelBuilder mulePluginModelBuilder = new MulePluginModel.MulePluginModelBuilder()
        .setMinMuleVersion(MIN_MULE_VERSION).setName("simpleExtensionPlugin").setRequiredProduct(MULE)
        .withBundleDescriptorLoader(createBundleDescriptorLoader("simpleExtensionPlugin", MULE_PLUGIN_CLASSIFIER,
                                                                 PROPERTIES_BUNDLE_DESCRIPTOR_LOADER_ID, "1.0.0"));
    mulePluginModelBuilder.withClassLoaderModelDescriptorLoader(new MuleArtifactLoaderDescriptorBuilder().setId(MULE_LOADER_ID)
        .build());
    mulePluginModelBuilder.withExtensionModelDescriber().setId(JAVA_LOADER_ID)
        .addProperty("type", "org.foo.simple.SimpleExtension")
        .addProperty("version", "1.0.0");
    return new ArtifactPluginFileBuilder("simpleExtensionPlugin")
        .dependingOn(new JarFileBuilder("simpleExtension", simpleExtensionJarFile))
        .describedBy(mulePluginModelBuilder.build());
  }

  private ArtifactPluginFileBuilder createWithErrorDeclarationExtensionPlugin() {
    MulePluginModel.MulePluginModelBuilder mulePluginModelBuilder = new MulePluginModel.MulePluginModelBuilder()
        .setMinMuleVersion(MIN_MULE_VERSION).setName("withErrorDeclarationExtensionPlugin").setRequiredProduct(MULE)
        .withBundleDescriptorLoader(createBundleDescriptorLoader("withErrorDeclarationExtensionPlugin", MULE_PLUGIN_CLASSIFIER,
                                                                 PROPERTIES_BUNDLE_DESCRIPTOR_LOADER_ID, "1.0.0"));
    mulePluginModelBuilder.withClassLoaderModelDescriptorLoader(new MuleArtifactLoaderDescriptorBuilder().setId(MULE_LOADER_ID)
        .build());
    mulePluginModelBuilder.withExtensionModelDescriber().setId(JAVA_LOADER_ID)
        .addProperty("type", "org.foo.withErrorDeclaration.WithErrorDeclarationExtension")
        .addProperty("version", "1.0.0");
    return new ArtifactPluginFileBuilder("withErrorDeclarationExtensionPlugin")
        .dependingOn(new JarFileBuilder("withErrorDeclarationExtension", withErrorDeclarationExtensionJarFile))
        .describedBy(mulePluginModelBuilder.build());
  }

  private ArtifactPluginFileBuilder createPolicyDependencyInjectionExtensionDependencyPlugin() {
    MulePluginModel.MulePluginModelBuilder mulePluginModelBuilder = new MulePluginModel.MulePluginModelBuilder()
        .setMinMuleVersion(MIN_MULE_VERSION).setName("policyDependencyInjectionExtensionPlugin").setRequiredProduct(MULE)
        .withBundleDescriptorLoader(createBundleDescriptorLoader("policyDependencyInjectionExtensionPlugin",
                                                                 MULE_PLUGIN_CLASSIFIER,
                                                                 PROPERTIES_BUNDLE_DESCRIPTOR_LOADER_ID, "1.0.0"));
    mulePluginModelBuilder.withClassLoaderModelDescriptorLoader(new MuleArtifactLoaderDescriptorBuilder().setId(MULE_LOADER_ID)
        .build());
    mulePluginModelBuilder.withExtensionModelDescriber().setId(JAVA_LOADER_ID)
        .addProperty("type", "org.foo.policyIsolation.PolicyDependencyInjectionExtension")
        .addProperty("version", "1.0.0");
    return new ArtifactPluginFileBuilder("policyDependencyInjectionExtensionPlugin")
        .dependingOn(new JarFileBuilder("policyDependencyInjectionExtension", policyDependencyInjectionExtensionJarFile))
        .describedBy(mulePluginModelBuilder.build());
  }

  private ArtifactPluginFileBuilder createPolicyConfigurationExtensionDependencyPlugin() {
    MulePluginModel.MulePluginModelBuilder mulePluginModelBuilder = new MulePluginModel.MulePluginModelBuilder()
        .setMinMuleVersion(MIN_MULE_VERSION).setName("policyConfigurationExtensionPlugin").setRequiredProduct(MULE)
        .withBundleDescriptorLoader(createBundleDescriptorLoader("policyConfigurationExtensionPlugin",
                                                                 MULE_PLUGIN_CLASSIFIER,
                                                                 PROPERTIES_BUNDLE_DESCRIPTOR_LOADER_ID, "1.0.0"));
    mulePluginModelBuilder.withClassLoaderModelDescriptorLoader(new MuleArtifactLoaderDescriptorBuilder().setId(MULE_LOADER_ID)
        .build());
    mulePluginModelBuilder.withExtensionModelDescriber().setId(JAVA_LOADER_ID)
        .addProperty("type", "org.foo.policyIsolation.PolicyConfigurationExtension")
        .addProperty("version", "1.0.0");
    return new ArtifactPluginFileBuilder("policyConfigurationPlugin")
        .dependingOn(new JarFileBuilder("policyConfigurationExtension", policyConfigurationExtensionJarFile))
        .describedBy(mulePluginModelBuilder.build());
  }

  private ArtifactPluginFileBuilder createInjectedHelloExtensionPluginFileBuilder() throws URISyntaxException {
    File injectedHelloExtensionJarFile =
        new CompilerUtils.ExtensionCompiler().compiling(getResourceFile("/org/foo/injected/InjectedHelloExtension.java"),
                                                        getResourceFile("/org/foo/injected/InjectedHelloOperation.java"))
            .compile("mule-module-hello-1.0.jar", "1.0");

    MulePluginModel.MulePluginModelBuilder mulePluginModelBuilder = new MulePluginModel.MulePluginModelBuilder()
        .setMinMuleVersion(MIN_MULE_VERSION).setName("helloExtensionPlugin").setRequiredProduct(MULE)
        .withBundleDescriptorLoader(createBundleDescriptorLoader("helloExtensionPlugin", MULE_PLUGIN_CLASSIFIER,
                                                                 PROPERTIES_BUNDLE_DESCRIPTOR_LOADER_ID, "1.0.0"));
    mulePluginModelBuilder.withClassLoaderModelDescriptorLoader(new MuleArtifactLoaderDescriptorBuilder().setId(MULE_LOADER_ID)
        .addProperty(EXPORTED_RESOURCES,
                     asList("/", "META-INF/mule-hello.xsd",
                            "META-INF/spring.handlers",
                            "META-INF/spring.schemas"))
        .build());
    mulePluginModelBuilder.withExtensionModelDescriber().setId(JAVA_LOADER_ID)
        .addProperty("type", "org.foo.injected.InjectedHelloExtension")
        .addProperty("version", "1.0");
    return new ArtifactPluginFileBuilder("helloExtensionPlugin-1.0")
        .dependingOn(new JarFileBuilder("helloExtensionV1", injectedHelloExtensionJarFile))
        .describedBy((mulePluginModelBuilder.build()));
  }

  private PolicyFileBuilder createPolicyIncludingByePlugin() {
    MulePolicyModel.MulePolicyModelBuilder mulePolicyModelBuilder = new MulePolicyModel.MulePolicyModelBuilder()
        .setMinMuleVersion(MIN_MULE_VERSION).setName(BAZ_POLICY_NAME)
        .setRequiredProduct(Product.MULE)
        .withBundleDescriptorLoader(createBundleDescriptorLoader(BAZ_POLICY_NAME, MULE_POLICY_CLASSIFIER,
                                                                 PROPERTIES_BUNDLE_DESCRIPTOR_LOADER_ID))
        .withClassLoaderModelDescriptorLoader(new MuleArtifactLoaderDescriptor(MULE_LOADER_ID, emptyMap()));
    return new PolicyFileBuilder(BAZ_POLICY_NAME).describedBy(mulePolicyModelBuilder
        .build()).dependingOn(moduleUsingByeXmlExtensionPlugin);
  }

  private void configureSimpleAppAndPolicyWithErrorDeclarationExtensionAndErrorMapping() throws Exception {
    ArtifactPluginFileBuilder simpleExtensionPlugin = createSingleExtensionPlugin();
    ArtifactPluginFileBuilder withErrorDeclarationExtensionPlugin = createWithErrorDeclarationExtensionPlugin();
    PolicyFileBuilder withErrorDeclarationPolicyFileBuilder =
        policyIncludingPluginFileBuilder.dependingOn(withErrorDeclarationExtensionPlugin);
    if (isIsolatedPolicy()) {
      // Since the feature flag ENABLE_POLICY_ISOLATION is active, the policy must declare as dependencies all the extensions
      withErrorDeclarationPolicyFileBuilder = withErrorDeclarationPolicyFileBuilder.dependingOn(simpleExtensionPlugin);
    }
    policyManager.registerPolicyTemplate(withErrorDeclarationPolicyFileBuilder.getArtifactFile());

    ApplicationFileBuilder applicationFileBuilder = createExtensionApplicationWithServices("app-with-simple-extension-config.xml",
                                                                                           simpleExtensionPlugin);

    addPackedAppFromBuilder(applicationFileBuilder);

    startDeployment();
    assertApplicationDeploymentSuccess(applicationDeploymentListener, applicationFileBuilder.getId());

    policyManager.addPolicy(applicationFileBuilder.getId(), policyIncludingPluginFileBuilder.getArtifactId(),
                            new PolicyParametrization(BAR_POLICY_ID, s -> true, 1, emptyMap(),
                                                      getResourceFile("/policy-with-error-mapping.xml"),
                                                      emptyList()));
  }

  private void configureAppWithErrorDeclarationAndPolicyWithErrorMapping() throws Exception {
    ArtifactPluginFileBuilder simpleExtensionPlugin = createSingleExtensionPlugin();
    ArtifactPluginFileBuilder withErrorDeclarationExtensionPlugin = createWithErrorDeclarationExtensionPlugin();

    policyManager.registerPolicyTemplate(policyIncludingPluginFileBuilder.dependingOn(simpleExtensionPlugin).getArtifactFile());

    ApplicationFileBuilder applicationFileBuilder = createExtensionApplicationWithServices("app-with-simple-extension-config.xml",
                                                                                           withErrorDeclarationExtensionPlugin,
                                                                                           simpleExtensionPlugin);

    addPackedAppFromBuilder(applicationFileBuilder);

    startDeployment();
    assertApplicationDeploymentSuccess(applicationDeploymentListener, applicationFileBuilder.getId());

    policyManager.addPolicy(applicationFileBuilder.getId(), policyIncludingPluginFileBuilder.getArtifactId(),
                            new PolicyParametrization(BAR_POLICY_ID, s -> true, 1, emptyMap(),
                                                      getResourceFile("/policy-with-error-mapping.xml"),
                                                      emptyList()));
  }

  private boolean isIsolatedPolicy() {
    return parseBoolean(enablePolicyIsolationSystemProperty.getValue());
  }

  private void deployAppAndApplyPolicyWithDependencyInjectionExtension(String applicationConfigurationFileName,
                                                                       String policyConfigurationFileName)
      throws Exception {
    ArtifactPluginFileBuilder policyDependencyInjectionDependencyPlugin =
        createPolicyDependencyInjectionExtensionDependencyPlugin();
    policyManager.registerPolicyTemplate(policyWithPolicyIsolationFileBuilder
        .dependingOn(policyDependencyInjectionDependencyPlugin)
        .getArtifactFile());
    ApplicationFileBuilder applicationFileBuilder =
        createExtensionApplicationWithServices(applicationConfigurationFileName,
                                               policyDependencyInjectionDependencyPlugin);
    addPackedAppFromBuilder(applicationFileBuilder);
    startDeployment();
    assertApplicationDeploymentSuccess(applicationDeploymentListener, applicationFileBuilder.getId());
    policyManager.addPolicy(applicationFileBuilder.getId(), policyWithPolicyIsolationFileBuilder
        .dependingOn(policyDependencyInjectionDependencyPlugin).getArtifactId(),
                            new PolicyParametrization(ISOLATED_POLICY_NAME, s -> true, 1, emptyMap(),
                                                      getResourceFile("/" + policyConfigurationFileName),
                                                      emptyList()));
  }

  private void deployAppAndApplyPolicyWithPolicyConfigurationExtension(String applicationConfigurationFileName,
                                                                       String policyConfigurationFileName)
      throws Exception {
    ArtifactPluginFileBuilder policyConfigurationExtensionDependencyPlugin = createPolicyConfigurationExtensionDependencyPlugin();
    policyManager.registerPolicyTemplate(policyWithPolicyIsolationFileBuilder
        .dependingOn(policyConfigurationExtensionDependencyPlugin)
        .getArtifactFile());
    ApplicationFileBuilder applicationFileBuilder =
        createExtensionApplicationWithServices(applicationConfigurationFileName,
                                               policyConfigurationExtensionDependencyPlugin);
    addPackedAppFromBuilder(applicationFileBuilder);
    startDeployment();
    assertApplicationDeploymentSuccess(applicationDeploymentListener, applicationFileBuilder.getId());
    policyManager.addPolicy(applicationFileBuilder.getId(), policyWithPolicyIsolationFileBuilder
        .dependingOn(policyConfigurationExtensionDependencyPlugin).getArtifactId(),
                            new PolicyParametrization(ISOLATED_POLICY_NAME, s -> true, 1, emptyMap(),
                                                      getResourceFile("/" + policyConfigurationFileName),
                                                      emptyList()));
  }

  public static class TestSecurityProvider extends AbstractSecurityProvider {

    public TestSecurityProvider() {
      this("test-security-provider");
    }

    public TestSecurityProvider(String name) {
      super(name);
    }

    @Override
    public Authentication authenticate(Authentication authentication) throws SecurityException {
      return null;
    }
  }
}
