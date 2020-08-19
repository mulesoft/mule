/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.runtime.module.deployment.internal;

import static java.lang.reflect.Modifier.FINAL;
import static java.util.Arrays.asList;
import static java.util.Collections.emptyList;
import static java.util.Collections.emptyMap;
import static java.util.Collections.singletonList;
import static java.util.Collections.singletonMap;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasItems;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.instanceOf;
import static org.hamcrest.core.Is.is;
import static org.junit.Assert.fail;
import static org.junit.rules.ExpectedException.none;
import static org.mule.runtime.api.deployment.meta.Product.MULE;
import static org.mule.runtime.api.notification.PolicyNotification.AFTER_NEXT;
import static org.mule.runtime.api.notification.PolicyNotification.BEFORE_NEXT;
import static org.mule.runtime.api.notification.PolicyNotification.PROCESS_END;
import static org.mule.runtime.api.notification.PolicyNotification.PROCESS_START;
import static org.mule.runtime.container.internal.ClasspathModuleDiscoverer.EXPORTED_RESOURCE_PROPERTY;
import static org.mule.runtime.core.internal.config.bootstrap.ClassLoaderRegistryBootstrapDiscoverer.BOOTSTRAP_PROPERTIES;
import static org.mule.runtime.deployment.model.api.artifact.ArtifactDescriptorConstants.EXPORTED_RESOURCES;
import static org.mule.runtime.deployment.model.api.artifact.ArtifactDescriptorConstants.MULE_LOADER_ID;
import static org.mule.runtime.module.deployment.impl.internal.policy.PropertiesBundleDescriptorLoader.PROPERTIES_BUNDLE_DESCRIPTOR_LOADER_ID;
import static org.mule.runtime.module.deployment.internal.TestPolicyProcessor.invocationCount;
import static org.mule.runtime.module.deployment.internal.TestPolicyProcessor.policyParametrization;
import static org.mule.runtime.module.extension.api.loader.java.DefaultJavaExtensionModelLoader.JAVA_LOADER_ID;
import static org.mule.test.allure.AllureConstants.ClassloadingIsolationFeature.CLASSLOADING_ISOLATION;

import java.io.File;
import java.lang.reflect.Field;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.List;

import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Ignore;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.junit.runners.Parameterized;
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
import org.mule.runtime.core.api.policy.PolicyParametrization;
import org.mule.runtime.core.api.policy.PolicyPointcut;
import org.mule.runtime.core.api.security.AbstractSecurityProvider;
import org.mule.runtime.deployment.model.api.policy.PolicyRegistrationException;
import org.mule.runtime.module.deployment.impl.internal.artifact.ArtifactContextBuilder;
import org.mule.runtime.deployment.model.api.policy.PolicyTemplateDescriptor;
import org.mule.runtime.module.deployment.impl.internal.builder.ApplicationFileBuilder;
import org.mule.runtime.module.deployment.impl.internal.builder.ArtifactPluginFileBuilder;
import org.mule.runtime.module.deployment.impl.internal.builder.JarFileBuilder;
import org.mule.runtime.module.deployment.impl.internal.builder.PolicyFileBuilder;
import org.mule.tck.probe.JUnitProbe;
import org.mule.tck.probe.PollingProber;
import org.mule.tck.util.CompilerUtils;

import io.qameta.allure.Issue;
import io.qameta.allure.Description;
import io.qameta.allure.Feature;

/**
 * Contains test for application deployment with policies on the default domain
 */
public class ApplicationPolicyDeploymentTestCase extends AbstractDeploymentTestCase {

  private static final String APP_WITH_SIMPLE_EXTENSION_CONFIG = "app-with-simple-extension-config.xml";
  private static final String APP_WITH_MODULE_BYE_CONFIG = "app-with-module-bye-config.xml";

  private static final int POLICY_NOTIFICATION_TIMEOUT = 5000;

  private static final String BAR_POLICY_ID = "barPolicy";
  private static final String POLICY_PROPERTY_VALUE = "policyPropertyValue";
  private static final String POLICY_PROPERTY_KEY = "policyPropertyKey";
  private static final String FOO_POLICY_NAME = "fooPolicy";
  private static File simpleExtensionJarFile;
  private static File withErrorDeclarationExtensionJarFile;

  @Rule
  public ExpectedException expectedEx = none();

  @Parameterized.Parameters(name = "Parallel: {0}")
  public static List<Boolean> params() {
    // Only run without parallel deployment since this configuration does not affect policy deployment at all
    return asList(false);
  }

  @Before
  public void setup() throws NoSuchFieldException, IllegalAccessException {
    setShareErrorTypeRepository(false);
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
          .withClassLoaderModelDescriptorLoader(new MuleArtifactLoaderDescriptor(MULE_LOADER_ID, emptyMap()))
          .build());

  public ApplicationPolicyDeploymentTestCase(boolean parallelDeployment) {
    super(parallelDeployment);
  }

  @BeforeClass
  public static void compileTestClasses() throws Exception {
    simpleExtensionJarFile =
        new CompilerUtils.ExtensionCompiler().compiling(getResourceFile("/org/foo/simple/SimpleExtension.java"),
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

    executeApplicationFlow("main");
    assertThat(invocationCount, equalTo(2));
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
    PolicyNotificationListener<PolicyNotification> notificationListener =
        notification -> notificationListenerActionIds.add(notification.getAction().getActionId());

    policyManager.addPolicy(applicationFileBuilder.getId(), fooPolicyFileBuilder.getArtifactId(),
                            new PolicyParametrization(FOO_POLICY_ID, pointparameters -> true, 1,
                                                      singletonMap(POLICY_PROPERTY_KEY, POLICY_PROPERTY_VALUE),
                                                      getResourceFile("/fooPolicy.xml"), singletonList(notificationListener)));

    executeApplicationFlow("main");
    assertThat(invocationCount, equalTo(1));
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
            .setMinMuleVersion(MIN_MULE_VERSION).setName(BAR_POLICY_NAME).setRequiredProduct(MULE)
            .withBundleDescriptorLoader(createBundleDescriptorLoader(BAR_POLICY_NAME, MULE_POLICY_CLASSIFIER,
                                                                     PROPERTIES_BUNDLE_DESCRIPTOR_LOADER_ID))
            .withClassLoaderModelDescriptorLoader(new MuleArtifactLoaderDescriptor(MULE_LOADER_ID, emptyMap()))
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

    executeApplicationFlow("main");
    assertThat(invocationCount, equalTo(1));

    policyManager.removePolicy(applicationFileBuilder.getId(), FOO_POLICY_ID);

    executeApplicationFlow("main");
    assertThat("Policy is still applied on the application", invocationCount, equalTo(1));
  }


  @Test
  @Feature(CLASSLOADING_ISOLATION)
  public void appliesApplicationPolicyUsingAppPlugin() throws Exception {
    policyManager.registerPolicyTemplate(policyUsingAppPluginFileBuilder.getArtifactFile());

    ApplicationFileBuilder applicationFileBuilder = createExtensionApplicationWithServices(APP_WITH_EXTENSION_PLUGIN_CONFIG,
                                                                                           helloExtensionV1Plugin);
    addPackedAppFromBuilder(applicationFileBuilder);

    startDeployment();
    assertApplicationDeploymentSuccess(applicationDeploymentListener, applicationFileBuilder.getId());

    policyManager.addPolicy(applicationFileBuilder.getId(), policyUsingAppPluginFileBuilder.getArtifactId(),
                            new PolicyParametrization(BAR_POLICY_ID, s -> true, 1, emptyMap(),
                                                      getResourceFile("/appPluginPolicy.xml"), emptyList()));

    executeApplicationFlow("main");
    assertThat(invocationCount, equalTo(1));
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
  @Description("If the application doesn't have the ErrorType that is needed by the policy, but the policy already has it in its own ErrorType repository")
  public void appliesPolicyWithoutSharingErrorTypeRepositoryWithSimpleApp() throws Exception {
    setShareErrorTypeRepository(false);

    configureSimpleAppAndPolicyWithErrorDeclarationExtensionAndErrorMapping();

    executeApplicationFlow("main");
    assertThat(invocationCount, equalTo(1));
  }

  @Test
  @Issue("MULE-18196")
  @Description("When policy and app share ErrorType repository, everything should work")
  public void appliesPolicySharingErrorTypeRepositoryWithApp() throws Exception {
    setShareErrorTypeRepository(true);

    configureAppWithErrorDeclarationAndPolicyWithErrorMapping();

    executeApplicationFlow("main");
    assertThat(invocationCount, equalTo(1));
  }


  @Test
  @Issue("MULE-18196")
  @Description("When policy ErrorType repository is isolated, the policy can't use any ErrorType that is not in its own repository")
  public void appliesPolicyWithoutSharingErrorTypeRepositoryWithApp() throws Exception {
    setShareErrorTypeRepository(false);

    expectedEx.expect(PolicyRegistrationException.class);
    expectedEx.expectMessage("Error occured registering policy 'barPolicy'");
    expectedEx.expectCause(instanceOf(InitialisationException.class));

    configureAppWithErrorDeclarationAndPolicyWithErrorMapping();

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
        .withBundleDescriptorLoader(createBundleDescriptorLoader(FOO_POLICY_NAME, MULE_POLICY_CLASSIFIER,
                                                                 PROPERTIES_BUNDLE_DESCRIPTOR_LOADER_ID))
        .withClassLoaderModelDescriptorLoader(new MuleArtifactLoaderDescriptor(MULE_LOADER_ID, emptyMap()))
        .build())
        .dependingOn(injectedExtension);
  }

  private ArtifactPluginFileBuilder createSingleExtensionPlugin() {
    MulePluginModel.MulePluginModelBuilder mulePluginModelBuilder = new MulePluginModel.MulePluginModelBuilder()
        .setMinMuleVersion(MIN_MULE_VERSION).setName("simpleExtensionPlugin").setRequiredProduct(MULE)
        .withBundleDescriptorLoader(createBundleDescriptorLoader("simpleExtensionPlugin", MULE_EXTENSION_CLASSIFIER,
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
        .withBundleDescriptorLoader(createBundleDescriptorLoader("withErrorDeclarationExtensionPlugin", MULE_EXTENSION_CLASSIFIER,
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

  private ArtifactPluginFileBuilder createInjectedHelloExtensionPluginFileBuilder() throws URISyntaxException {
    File injectedHelloExtensionJarFile =
        new CompilerUtils.ExtensionCompiler().compiling(getResourceFile("/org/foo/injected/InjectedHelloExtension.java"),
                                                        getResourceFile("/org/foo/injected/InjectedHelloOperation.java"))
            .compile("mule-module-hello-1.0.jar", "1.0");

    MulePluginModel.MulePluginModelBuilder mulePluginModelBuilder = new MulePluginModel.MulePluginModelBuilder()
        .setMinMuleVersion(MIN_MULE_VERSION).setName("helloExtensionPlugin").setRequiredProduct(MULE)
        .withBundleDescriptorLoader(createBundleDescriptorLoader("helloExtensionPlugin", MULE_EXTENSION_CLASSIFIER,
                                                                 PROPERTIES_BUNDLE_DESCRIPTOR_LOADER_ID, "1.0.0"));
    mulePluginModelBuilder.withClassLoaderModelDescriptorLoader(new MuleArtifactLoaderDescriptorBuilder().setId(MULE_LOADER_ID)
        .addProperty(EXPORTED_RESOURCES,
                     asList("/", "META-INF/mule-hello.xsd", "META-INF/spring.handlers", "META-INF/spring.schemas"))
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

    policyManager.registerPolicyTemplate(policyIncludingPluginFileBuilder.dependingOn(withErrorDeclarationExtensionPlugin)
        .getArtifactFile());

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

  private void setShareErrorTypeRepository(boolean value) throws NoSuchFieldException, IllegalAccessException {
    Field field = ArtifactContextBuilder.class.getDeclaredField("SHARE_ERROR_TYPE_REPOSITORY");
    Field modifiersField = Field.class.getDeclaredField("modifiers");
    modifiersField.setAccessible(true);
    modifiersField.setInt(field, field.getModifiers() & ~FINAL);
    field.setAccessible(true);
    field.set(null, value);
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
