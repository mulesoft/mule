/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.runtime.module.deployment.internal;

import static org.mule.runtime.api.deployment.meta.Product.MULE;
import static org.mule.runtime.deployment.model.api.artifact.ArtifactDescriptorConstants.MULE_LOADER_ID;
import static org.mule.runtime.module.artifact.api.descriptor.BundleDescriptor.MULE_PLUGIN_CLASSIFIER;
import static org.mule.runtime.module.deployment.impl.internal.policy.PropertiesBundleDescriptorLoader.PROPERTIES_BUNDLE_DESCRIPTOR_LOADER_ID;
import static org.mule.runtime.module.deployment.internal.DeploymentDirectoryWatcher.CHANGE_CHECK_INTERVAL_PROPERTY;
import static org.mule.runtime.module.extension.internal.loader.java.DefaultJavaExtensionModelLoader.JAVA_LOADER_ID;

import static java.util.Collections.emptyList;
import static java.util.Collections.emptyMap;
import static java.util.Collections.singletonMap;

import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

import org.mule.runtime.api.deployment.meta.MuleArtifactLoaderDescriptor;
import org.mule.runtime.api.deployment.meta.MuleArtifactLoaderDescriptorBuilder;
import org.mule.runtime.api.deployment.meta.MulePluginModel;
import org.mule.runtime.api.deployment.meta.MulePolicyModel;
import org.mule.runtime.api.exception.MuleException;
import org.mule.runtime.api.exception.MuleRuntimeException;
import org.mule.runtime.core.api.policy.PolicyParametrization;
import org.mule.runtime.deployment.model.api.application.Application;
import org.mule.runtime.module.artifact.api.classloader.ArtifactClassLoader;
import org.mule.runtime.module.deployment.api.DeploymentListener;
import org.mule.runtime.module.deployment.impl.internal.builder.ApplicationFileBuilder;
import org.mule.runtime.module.deployment.impl.internal.builder.ArtifactPluginFileBuilder;
import org.mule.runtime.module.deployment.impl.internal.builder.JarFileBuilder;
import org.mule.runtime.module.deployment.impl.internal.builder.PolicyFileBuilder;
import org.mule.tck.junit4.rule.SystemProperty;
import org.mule.tck.probe.JUnitLambdaProbe;
import org.mule.tck.probe.PollingProber;
import org.mule.tck.report.HeapDumper;
import org.mule.tck.util.CompilerUtils;

import java.io.File;
import java.lang.ref.PhantomReference;
import java.lang.ref.ReferenceQueue;
import java.util.concurrent.atomic.AtomicReference;

import org.junit.BeforeClass;
import org.junit.Rule;
import org.junit.Test;

import io.qameta.allure.Description;
import io.qameta.allure.Issue;

public abstract class ClassLoaderLeakTestCase extends AbstractDeploymentTestCase {

  @Rule
  public SystemProperty directoryWatcherChangeCheckInterval = new SystemProperty(CHANGE_CHECK_INTERVAL_PROPERTY, "5");

  private static File simpleExtensionJarFile;

  private static final String POLICY_PROPERTY_VALUE = "policyPropertyValue";
  private static final String POLICY_PROPERTY_KEY = "policyPropertyKey";
  private static final String FOO_POLICY_NAME = "fooPolicy";

  private static final int PROBER_POLLING_INTERVAL = 100;
  private static final int PROBER_POLIING_TIMEOUT = 5000;

  private final String appName;

  private final String xmlFile;

  private final boolean useEchoPluginInApp;

  @BeforeClass
  public static void compileTestClasses() throws Exception {
    simpleExtensionJarFile =
        new CompilerUtils.ExtensionCompiler().compiling(getResourceFile("/org/foo/simple/SimpleExtension.java"),
                                                        getResourceFile("/org/foo/simple/SimpleOperation.java"))
            .compile("mule-module-simple-4.0-SNAPSHOT.jar", "1.0.0");
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
          .build())
          .dependingOn(createSingleExtensionPlugin());

  private TestDeploymentListener deploymentListener;

  public ClassLoaderLeakTestCase(boolean parallellDeployment, String appName, String xmlFile, boolean useEchoPluginInApp) {
    super(parallellDeployment);
    this.appName = appName;
    this.useEchoPluginInApp = useEchoPluginInApp;
    this.xmlFile = xmlFile;
  }

  @Test
  public void undeploysApplicationDoesNotLeakClassloader() throws Exception {

    ApplicationFileBuilder applicationFileBuilder = getApplicationFileBuilder();

    addPackedAppFromBuilder(applicationFileBuilder);

    startDeployment();

    assertThat(getDeploymentListener().isAppDeployed(), is(true));

    assertThat(removeAppAnchorFile(appName), is(true));

    new PollingProber(PROBER_POLIING_TIMEOUT, PROBER_POLLING_INTERVAL).check(new JUnitLambdaProbe(() -> {
      assertThat(getDeploymentListener().isAppUndeployed(), is(true));
      return true;
    }));

    new PollingProber(PROBER_POLIING_TIMEOUT, PROBER_POLLING_INTERVAL).check(new JUnitLambdaProbe(() -> {
      System.gc();
      assertThat(getDeploymentListener().getPhantomReference().isEnqueued(), is(true));
      return true;
    }));
  }

  @Test
  @Issue("MULE-18480")
  public void undeploysApplicationWithPoliciesDoesNotLeakClassloader() throws Exception {
    policyManager.registerPolicyTemplate(fooPolicyFileBuilder.getArtifactFile());

    ApplicationFileBuilder applicationFileBuilder = getApplicationFileBuilder();

    addPackedAppFromBuilder(applicationFileBuilder);

    startDeployment();

    assertThat(getDeploymentListener().isAppDeployed(), is(true));

    policyManager.addPolicy(applicationFileBuilder.getId(), fooPolicyFileBuilder.getArtifactId(),
                            new PolicyParametrization(FOO_POLICY_ID, pointparameters -> true, 1,
                                                      singletonMap(POLICY_PROPERTY_KEY, POLICY_PROPERTY_VALUE),
                                                      getResourceFile("/fooPolicy.xml"), emptyList()));

    assertThat(removeAppAnchorFile(appName), is(true));

    new PollingProber(PROBER_POLIING_TIMEOUT, PROBER_POLLING_INTERVAL).check(new JUnitLambdaProbe(() -> {
      assertThat(getDeploymentListener().isAppUndeployed(), is(true));
      return true;
    }));

    new PollingProber(PROBER_POLIING_TIMEOUT, PROBER_POLLING_INTERVAL).check(new JUnitLambdaProbe(() -> {
      System.gc();
      assertThat(getDeploymentListener().getPhantomReference().isEnqueued(), is(true));
      return true;
    }));
  }

  @Test
  @Issue("MULE-18480")
  @Description("When an artifact is redeployed by changing it in the filesystem, objects associated to the original deployment are released befroe deploying the new one.")
  public void redeployByConfigChangePreviousAppEagerlyGCd() throws Exception {
    DeploymentListener mockDeploymentListener = spy(new DeploymentStatusTracker());
    AtomicReference<Throwable> redeploymentSuccessThrown = new AtomicReference<>();

    ApplicationFileBuilder applicationFileBuilder = getApplicationFileBuilder();

    prepareScenario(applicationFileBuilder, mockDeploymentListener, redeploymentSuccessThrown);

    File configFile = new File(appsDir + "/" + applicationFileBuilder.getDeployedPath(),
                               getConfigFilePathWithinArtifact(MULE_CONFIG_XML_FILE));
    configFile.setLastModified(configFile.lastModified() + FILE_TIMESTAMP_PRECISION_MILLIS);

    assertRededeployment(mockDeploymentListener, redeploymentSuccessThrown);
  }

  @Test
  @Issue("MULE-18480")
  @Description("When an artifact is redeployed through the deployment service by uri, objects associated to the original deployment are released befroe deploying the new one.")
  public void redeployPreviousAppEagerlyGCd() throws Exception {
    DeploymentListener mockDeploymentListener = spy(new DeploymentStatusTracker());
    AtomicReference<Throwable> redeploymentSuccessThrown = new AtomicReference<>();

    ApplicationFileBuilder applicationFileBuilder = getApplicationFileBuilder();

    prepareScenario(applicationFileBuilder, mockDeploymentListener, redeploymentSuccessThrown);

    deploymentService.redeploy(applicationFileBuilder.getArtifactFile().toURI());

    assertRededeployment(mockDeploymentListener, redeploymentSuccessThrown);
  }

  @Test
  @Issue("MULE-18480")
  @Description("When an artifact is redeployed through the deployment service by name, objects associated to the original deployment are released befroe deploying the new one.")
  public void redeployByNamePreviousAppEagerlyGCd() throws Exception {
    DeploymentListener mockDeploymentListener = spy(new DeploymentStatusTracker());
    AtomicReference<Throwable> redeploymentSuccessThrown = new AtomicReference<>();

    ApplicationFileBuilder applicationFileBuilder = getApplicationFileBuilder();

    prepareScenario(applicationFileBuilder, mockDeploymentListener, redeploymentSuccessThrown);

    deploymentService.redeploy(appName);

    assertRededeployment(mockDeploymentListener, redeploymentSuccessThrown);
  }

  private void prepareScenario(ApplicationFileBuilder applicationFileBuilder, DeploymentListener mockDeploymentListener,
                               AtomicReference<Throwable> redeploymentSuccessThrown)
      throws Exception, MuleException {
    redeploymentSuccessThrown.set(new Exception("Leak check not done."));

    deploymentService.addDeploymentListener(mockDeploymentListener);

    addPackedAppFromBuilder(applicationFileBuilder);
    startDeployment();
    assertThat(getDeploymentListener().isAppDeployed(), is(true));

    final PhantomReference<Application> firstAppRef =
        new PhantomReference<>(deploymentService.findApplication(appName), new ReferenceQueue<>());

    doAnswer(invocation -> {
      try {
        new PollingProber(PROBER_POLIING_TIMEOUT, PROBER_POLLING_INTERVAL).check(new JUnitLambdaProbe(() -> {
          System.gc();
          assertThat(firstAppRef.isEnqueued(), is(true));
          return true;
        }));
        redeploymentSuccessThrown.set(null);
      } catch (Throwable t) {
        HeapDumper.main(new String[0]);
        redeploymentSuccessThrown.set(t);
      }

      return null;
    }).when(mockDeploymentListener).onRedeploymentSuccess(appName);
  }

  private void assertRededeployment(DeploymentListener mockDeploymentListener,
                                    AtomicReference<Throwable> redeploymentSuccessThrown) {
    new PollingProber(PROBER_POLIING_TIMEOUT + 1000, PROBER_POLLING_INTERVAL).check(new JUnitLambdaProbe(() -> {
      if (redeploymentSuccessThrown.get() != null) {
        throw new MuleRuntimeException(redeploymentSuccessThrown.get());
      }
      return true;
    }));

    verify(mockDeploymentListener, times(1)).onRedeploymentSuccess(appName);
  }

  private ApplicationFileBuilder getApplicationFileBuilder() throws Exception {
    if (useEchoPluginInApp) {
      return createExtensionApplicationWithServices(xmlFile + ".xml",
                                                    helloExtensionV1Plugin);
    } else {
      return new ApplicationFileBuilder(xmlFile)
          .definedBy(xmlFile + ".xml");
    }
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

  @Override
  protected void configureDeploymentService() {
    deploymentService.addDeploymentListener(getDeploymentListener());
  }

  protected TestDeploymentListener getDeploymentListener() {
    if (deploymentListener == null) {
      deploymentListener = new TestDeploymentListener(this, appName);
    }
    return deploymentListener;
  }

  static class TestDeploymentListener implements DeploymentListener {

    private PhantomReference<ArtifactClassLoader> phantomReference;

    private boolean appDeployed;

    private boolean appUndeployed;

    private final String appName;

    private final ClassLoaderLeakTestCase deploymentTestCase;

    protected MuleDeploymentService deploymentService;



    TestDeploymentListener(ClassLoaderLeakTestCase deploymentTestCase, String appName) {
      this.deploymentTestCase = deploymentTestCase;
      this.appName = appName;
    }

    @Override
    public void onDeploymentSuccess(String artifactName) {
      Application app = deploymentTestCase.findApp(appName, 1);
      appDeployed = true;
      phantomReference = new PhantomReference<>(app.getArtifactClassLoader(), new ReferenceQueue<>());
    };

    @Override
    public void onUndeploymentSuccess(String artifactName) {
      appUndeployed = true;
    }

    public PhantomReference<ArtifactClassLoader> getPhantomReference() {
      return phantomReference;
    }

    public boolean isAppDeployed() {
      return appDeployed;
    }

    public boolean isAppUndeployed() {
      return appUndeployed;
    }
  };
}
