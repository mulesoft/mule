/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.runtime.module.deployment.internal;

import static org.mule.runtime.module.deployment.internal.DeploymentDirectoryWatcher.CHANGE_CHECK_INTERVAL_PROPERTY;
import static org.mule.runtime.module.deployment.internal.TestArtifactsCatalog.echoTestClassFile;
import static org.mule.runtime.module.deployment.internal.TestArtifactsCatalog.withBrokenLifecycleListenerPlugin;
import static org.mule.runtime.module.deployment.internal.TestArtifactsCatalog.withLifecycleListenerPlugin;
import static org.mule.tck.junit4.rule.LogCleanup.clearAllLogs;
import static org.mule.test.allure.AllureConstants.JavaSdk.ArtifactLifecycleListener.ARTIFACT_LIFECYCLE_LISTENER;
import static org.mule.test.allure.AllureConstants.JavaSdk.JAVA_SDK;

import static java.lang.Thread.sleep;

import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;

import org.mule.runtime.deployment.model.api.application.Application;
import org.mule.runtime.deployment.model.api.domain.Domain;
import org.mule.runtime.module.artifact.api.classloader.ArtifactClassLoader;
import org.mule.runtime.module.deployment.api.DeploymentListener;
import org.mule.runtime.module.deployment.impl.internal.builder.ApplicationFileBuilder;
import org.mule.runtime.module.deployment.impl.internal.builder.DeployableFileBuilder;
import org.mule.runtime.module.deployment.impl.internal.builder.DomainFileBuilder;
import org.mule.sdk.api.artifact.lifecycle.ArtifactLifecycleListener;
import org.mule.tck.junit4.rule.SystemProperty;
import org.mule.tck.probe.JUnitLambdaProbe;
import org.mule.tck.probe.PollingProber;

import java.lang.ref.PhantomReference;
import java.lang.ref.ReferenceQueue;

import io.qameta.allure.Feature;
import io.qameta.allure.Story;
import org.junit.Rule;
import org.junit.Test;

/**
 * Tests that Extension's {@link ArtifactLifecycleListener}s are honoured for the different lifecycle phases.
 * <p>
 * Currently, only artifact disposal is supported.
 */
@Feature(JAVA_SDK)
@Story(ARTIFACT_LIFECYCLE_LISTENER)
public class ArtifactLifecycleListenerTestCase extends AbstractDeploymentTestCase {

  private static final int PROBER_POLLING_INTERVAL = 100;
  private static final int PROBER_POLLING_TIMEOUT = 5000;
  private static final String APP_NAME = "app-with-lifecycle-listener-extension";
  private static final String DOMAIN_NAME = "domain-with-lifecycle-listener-plugin";

  @Rule
  public SystemProperty directoryWatcherChangeCheckInterval = new SystemProperty(CHANGE_CHECK_INTERVAL_PROPERTY, "5");

  private final TestApplicationDeploymentListener appDeploymentListener;
  private final TestDomainDeploymentListener domainDeploymentListener;

  public ArtifactLifecycleListenerTestCase(boolean parallelDeployment) {
    super(parallelDeployment);
    this.appDeploymentListener = new TestApplicationDeploymentListener(new ApplicationFileBuilder(APP_NAME));
    this.domainDeploymentListener = new TestDomainDeploymentListener(new DomainFileBuilder(DOMAIN_NAME));
  }

  @Test
  public void whenExtensionIsInAppThenLifecycleListenerGetsCalledOnAppUndeploy() throws Exception {
    // Prepares an application that depends on an extension that declares an ArtifactLifecycleListener
    ApplicationFileBuilder applicationFileBuilder = getApplicationFileBuilder()
        .dependingOn(withLifecycleListenerPlugin);
    addPackedAppFromBuilder(applicationFileBuilder);

    // Starts the deployment
    startDeployment();
    triggerDirectoryWatcher();

    assertThat(appDeploymentListener.isArtifactDeployed(), is(true));

    // Executes a flow with an operation that leaks the application's ClassLoader
    executeApplicationFlow("main");

    // Undeploys the application
    assertThat(removeAppAnchorFile(applicationFileBuilder.getId()), is(true));
    triggerDirectoryWatcher();
    assertThat(appDeploymentListener.isArtifactUndeployed(), is(true));

    // After some time, the application's ClassLoader should be collectable, thanks to the extension's LifecycleListener
    assertClassLoaderIsCollectable(appDeploymentListener.getPhantomReference());
  }

  @Test
  public void whenExtensionIsInDomainThenLifecycleListenerGetsCalledOnDomainUndeploy() throws Exception {
    // Prepares a domain that depends on an extension that declares an ArtifactLifecycleListener
    DomainFileBuilder domainFileBuilder = getDomainDependingOnExtensionWithLifecycleListenerFileBuilder();
    addPackedDomainFromBuilder(domainFileBuilder);

    ApplicationFileBuilder applicationFileBuilder = getApplicationFileBuilder()
        .dependingOn(domainFileBuilder);
    addPackedAppFromBuilder(applicationFileBuilder);

    // Starts the deployment
    startDeployment();
    triggerDirectoryWatcher();

    assertThat(domainDeploymentListener.isArtifactDeployed(), is(true));
    assertThat(appDeploymentListener.isArtifactDeployed(), is(true));

    // Executes a flow with an operation that leaks the domain's ClassLoader
    executeApplicationFlow("main");

    // Undeploys the application
    assertThat(removeAppAnchorFile(applicationFileBuilder.getId()), is(true));
    triggerDirectoryWatcher();
    assertThat(appDeploymentListener.isArtifactUndeployed(), is(true));
    assertThat(domainDeploymentListener.isArtifactUndeployed(), is(false));

    // The LifecycleListener should not have been called
    assertLifecycleListenerNotExecuted(appDeploymentListener.getPhantomReference());

    // Undeploys the domain
    assertThat(removeDomainAnchorFile(domainFileBuilder.getId()), is(true));
    triggerDirectoryWatcher();
    assertThat(domainDeploymentListener.isArtifactUndeployed(), is(true));

    // After some time, both the app's and the domain's ClassLoaders should be collectable, thanks to the extension's
    // LifecycleListener
    assertClassLoaderIsCollectable(appDeploymentListener.getPhantomReference());
    assertClassLoaderIsCollectable(domainDeploymentListener.getPhantomReference());
  }

  @Test
  public void whenOneListenerFailsThenOtherListenersAreStillCalled() throws Exception {
    // Prepares an application that depends on two extensions with lifecycle listeners, one which fails and another one which
    // is necessary to avoid a leak
    ApplicationFileBuilder applicationFileBuilder = getApplicationFileBuilder()
        .dependingOn(withLifecycleListenerPlugin)
        .dependingOn(withBrokenLifecycleListenerPlugin);
    addPackedAppFromBuilder(applicationFileBuilder);

    // Starts the deployment
    startDeployment();
    triggerDirectoryWatcher();

    assertThat(appDeploymentListener.isArtifactDeployed(), is(true));

    // Executes a flow with an operation that leaks the application's ClassLoader
    executeApplicationFlow("main");

    // Undeploys the application
    assertThat(removeAppAnchorFile(applicationFileBuilder.getId()), is(true));
    triggerDirectoryWatcher();
    assertThat(appDeploymentListener.isArtifactUndeployed(), is(true));

    // After some time, the application's ClassLoader should be collectable, thanks to the extension's LifecycleListener
    assertClassLoaderIsCollectable(appDeploymentListener.getPhantomReference());
  }

  private void assertClassLoaderIsCollectable(PhantomReference<ArtifactClassLoader> classLoaderReference) {
    new PollingProber(PROBER_POLLING_TIMEOUT, PROBER_POLLING_INTERVAL).check(new JUnitLambdaProbe(() -> {
      clearAllLogs();
      System.gc();
      assertThat(classLoaderReference.isEnqueued(), is(true));
      return true;
    }));
  }

  private void assertLifecycleListenerNotExecuted(PhantomReference<ArtifactClassLoader> classLoaderReference)
      throws InterruptedException {
    clearAllLogs();
    System.gc();
    sleep(500);
    assertThat(classLoaderReference.isEnqueued(), is(false));
  }

  private ApplicationFileBuilder getApplicationFileBuilder() {
    return appDeploymentListener.getBaseFileBuilder()
        .definedBy("app-with-lifecycle-listener-extension.xml")
        // Adds a random class to the artifact's ClassLoader, so we can try loading it during the listener's execution.
        .containingClass(echoTestClassFile, "org/foo/EchoTest.class");
  }

  private DomainFileBuilder getDomainDependingOnExtensionWithLifecycleListenerFileBuilder() {
    return domainDeploymentListener.getBaseFileBuilder()
        .definedBy("empty-domain-config.xml")
        .dependingOn(withLifecycleListenerPlugin)
        // Adds a random class to the artifact's ClassLoader, so we can try loading it during the listener's execution.
        .containingClass(echoTestClassFile, "org/foo/EchoTest.class");
  }

  @Override
  protected void configureDeploymentService() {
    deploymentService.addDeploymentListener(appDeploymentListener);
    deploymentService.addDomainDeploymentListener(domainDeploymentListener);
  }

  private abstract static class TestArtifactDeploymentListener implements DeploymentListener {

    private PhantomReference<ArtifactClassLoader> phantomReference;

    private boolean artifactDeployed;

    private boolean artifactUndeployed;

    protected final DeployableFileBuilder<?> deployableFileBuilder;

    TestArtifactDeploymentListener(DeployableFileBuilder<?> baseFileBuilder) {
      this.deployableFileBuilder = baseFileBuilder;
    }

    @Override
    public void onDeploymentSuccess(String artifactName) {
      if (deployableFileBuilder.getId().equals(artifactName)) {
        artifactDeployed = true;
        phantomReference =
            new PhantomReference<>(getArtifactClassLoader(deployableFileBuilder.getId()), new ReferenceQueue<>());
      }
    }

    @Override
    public void onUndeploymentSuccess(String artifactName) {
      if (deployableFileBuilder.getId().equals(artifactName)) {
        artifactUndeployed = true;
      }
    }

    public PhantomReference<ArtifactClassLoader> getPhantomReference() {
      return phantomReference;
    }

    public boolean isArtifactDeployed() {
      return artifactDeployed;
    }

    public boolean isArtifactUndeployed() {
      return artifactUndeployed;
    }

    protected abstract ArtifactClassLoader getArtifactClassLoader(String artifactName);
  }

  private class TestApplicationDeploymentListener extends TestArtifactDeploymentListener {

    TestApplicationDeploymentListener(ApplicationFileBuilder baseFileBuilder) {
      super(baseFileBuilder);
    }

    @Override
    protected ArtifactClassLoader getArtifactClassLoader(String appName) {
      Application app = findApp(appName, 1);
      return app.getArtifactClassLoader();
    }

    public ApplicationFileBuilder getBaseFileBuilder() {
      return (ApplicationFileBuilder) deployableFileBuilder;
    }
  }

  private class TestDomainDeploymentListener extends TestArtifactDeploymentListener {

    TestDomainDeploymentListener(DomainFileBuilder baseFileBuilder) {
      super(baseFileBuilder);
    }

    @Override
    protected ArtifactClassLoader getArtifactClassLoader(String domainName) {
      Domain domain = findADomain(domainName);
      return domain.getArtifactClassLoader();
    }

    public DomainFileBuilder getBaseFileBuilder() {
      return (DomainFileBuilder) deployableFileBuilder;
    }
  }
}
