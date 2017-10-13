/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.runtime.module.deployment.internal;

import static java.util.Arrays.asList;
import static java.util.Collections.emptyList;
import static java.util.Collections.emptyMap;
import static java.util.Collections.singletonList;
import static java.util.Collections.singletonMap;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasItems;
import static org.hamcrest.Matchers.hasSize;
import static org.junit.Assert.fail;
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

import org.mule.runtime.api.deployment.meta.MuleArtifactLoaderDescriptor;
import org.mule.runtime.api.deployment.meta.MuleArtifactLoaderDescriptorBuilder;
import org.mule.runtime.api.deployment.meta.MulePluginModel;
import org.mule.runtime.api.deployment.meta.MulePolicyModel;
import org.mule.runtime.api.deployment.meta.Product;
import org.mule.runtime.api.notification.PolicyNotification;
import org.mule.runtime.api.notification.PolicyNotificationListener;
import org.mule.runtime.core.api.policy.PolicyParametrization;
import org.mule.runtime.core.api.policy.PolicyPointcut;
import org.mule.runtime.deployment.model.api.policy.PolicyRegistrationException;
import org.mule.runtime.module.deployment.impl.internal.builder.ApplicationFileBuilder;
import org.mule.runtime.module.deployment.impl.internal.builder.ArtifactPluginFileBuilder;
import org.mule.runtime.module.deployment.impl.internal.builder.JarFileBuilder;
import org.mule.runtime.module.deployment.impl.internal.builder.PolicyFileBuilder;
import org.mule.tck.probe.JUnitProbe;
import org.mule.tck.probe.PollingProber;
import org.mule.tck.util.CompilerUtils;

import java.io.File;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.List;

import org.junit.BeforeClass;
import org.junit.Test;

/**
 * Contains test for application deployment with policies on the default domain
 */
public class ApplicationPolicyDeploymentTestCase extends AbstractDeploymentTestCase {

  private static final int POLICY_NOTIFICATION_TIMEOUT = 5000;

  private static final String BAR_POLICY_ID = "barPolicy";
  private static final String POLICY_PROPERTY_VALUE = "policyPropertyValue";
  private static final String POLICY_PROPERTY_KEY = "policyPropertyKey";
  private static final String FOO_POLICY_NAME = "fooPolicy";

  // Policy artifact file builders
  private final PolicyFileBuilder fooPolicyFileBuilder =
      new PolicyFileBuilder(FOO_POLICY_NAME).describedBy(new MulePolicyModel.MulePolicyModelBuilder()
          .setMinMuleVersion(MIN_MULE_VERSION)
          .setName(FOO_POLICY_NAME)
          .setRequiredProduct(MULE)
          .withBundleDescriptorLoader(createBundleDescriptorLoader(FOO_POLICY_NAME, MULE_POLICY_CLASSIFIER,
                                                                   PROPERTIES_BUNDLE_DESCRIPTOR_LOADER_ID))
          .withClassLoaderModelDescriptorLoader(new MuleArtifactLoaderDescriptor(MULE_LOADER_ID, emptyMap()))
          .build());

  private static File simpleExtensionJarFile;


  @BeforeClass
  public static void compileTestClasses() throws Exception {
    simpleExtensionJarFile =
        new CompilerUtils.ExtensionCompiler().compiling(getResourceFile("/org/foo/simple/SimpleExtension.java"),
                                                        getResourceFile("/org/foo/simple/SimpleOperation.java"))
            .compile("mule-module-simple-4.0-SNAPSHOT.jar", "1.0.0");
  }

  public ApplicationPolicyDeploymentTestCase(boolean parallelDeployment) {
    super(parallelDeployment);
  }

  @Test
  public void appliesApplicationPolicy() throws Exception {
    doApplicationPolicyExecutionTest(parameters -> true, 1, POLICY_PROPERTY_VALUE);
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

  private void doApplicationPolicyExecutionTest(PolicyPointcut pointcut, int expectedPolicyInvocations,
                                                Object expectedPolicyParametrization)
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
                                                      getResourceFile("/fooPolicy.xml"), emptyList()));


    executeApplicationFlow("main");
    assertThat(invocationCount, equalTo(expectedPolicyInvocations));
    assertThat(policyParametrization, equalTo(expectedPolicyParametrization));
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
  public void appliesApplicationPolicyIncludingPlugin() throws Exception {
    ArtifactPluginFileBuilder simpleExtensionPlugin = createSingleExtensionPlugin();

    policyManager.registerPolicyTemplate(policyIncludingPluginFileBuilder.getArtifactFile());

    ApplicationFileBuilder applicationFileBuilder = createExtensionApplicationWithServices("app-with-simple-extension-config.xml",
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
  public void appliesApplicationPolicyDuplicatingPlugin() throws Exception {
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
    assertThat(invocationCount, equalTo(1));
  }

  @Test
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

    ApplicationFileBuilder applicationFileBuilder = createExtensionApplicationWithServices("app-with-simple-extension-config.xml",
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
  public void appliesPolicyThatUsesPolicyClassOnExpression() throws Exception {
    ArtifactPluginFileBuilder simpleExtensionPlugin = createSingleExtensionPlugin();

    policyManager.registerPolicyTemplate(policyWithPluginAndResource().getArtifactFile());

    ApplicationFileBuilder applicationFileBuilder = createExtensionApplicationWithServices("app-with-simple-extension-config.xml",
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
        .addProperty("type", "org.foo.hello.SimpleExtension")
        .addProperty("version", "1.0.0");
    return new ArtifactPluginFileBuilder("simpleExtensionPlugin")
        .dependingOn(new JarFileBuilder("simpleExtension", simpleExtensionJarFile))
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
}
