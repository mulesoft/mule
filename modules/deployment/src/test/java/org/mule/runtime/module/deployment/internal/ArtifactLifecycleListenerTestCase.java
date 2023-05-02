/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.runtime.module.deployment.internal;

import static org.mule.runtime.module.deployment.internal.DeploymentDirectoryWatcher.CHANGE_CHECK_INTERVAL_PROPERTY;
import static org.mule.runtime.module.deployment.internal.TestArtifactsCatalog.echoTestClassFile;
import static org.mule.runtime.module.deployment.internal.TestArtifactsCatalog.withLifecycleListenerPlugin;
import static org.mule.tck.junit4.rule.LogCleanup.clearAllLogs;

import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;

import org.mule.runtime.deployment.model.api.application.Application;
import org.mule.runtime.module.artifact.api.classloader.ArtifactClassLoader;
import org.mule.runtime.module.deployment.api.DeploymentListener;
import org.mule.runtime.module.deployment.impl.internal.builder.ApplicationFileBuilder;
import org.mule.tck.junit4.rule.SystemProperty;
import org.mule.tck.probe.JUnitLambdaProbe;
import org.mule.tck.probe.PollingProber;

import java.lang.ref.PhantomReference;
import java.lang.ref.ReferenceQueue;

import org.junit.Rule;
import org.junit.Test;

public class ArtifactLifecycleListenerTestCase extends AbstractDeploymentTestCase {

  private static final int PROBER_POLLING_INTERVAL = 100;
  private static final int PROBER_POLLING_TIMEOUT = 5000;
  private static final String APP_CONFIG_FILE = "app-with-lifecycle-listener-declaration-extension";
  private static final String APP_NAME = "appWithExtensionPlugin-1.0.0-mule-application";

  @Rule
  public SystemProperty directoryWatcherChangeCheckInterval = new SystemProperty(CHANGE_CHECK_INTERVAL_PROPERTY, "5");

  private final TestDeploymentListener deploymentListener;

  public ArtifactLifecycleListenerTestCase(boolean parallelDeployment) {
    super(parallelDeployment);
    this.deploymentListener = new TestDeploymentListener(this, APP_NAME);
  }

  @Test
  public void lifecycleListenerGetsCalledOnUndeploy() throws Exception {
    ApplicationFileBuilder applicationFileBuilder = getApplicationFileBuilder()
        // Adds a random class to the application's ClassLoader, so we can try loading it during the listener's execution.
        .containingClass(echoTestClassFile, "org/foo/EchoTest.class");

    addPackedAppFromBuilder(applicationFileBuilder);

    startDeployment();
    triggerDirectoryWatcher();

    assertThat(deploymentListener.isAppDeployed(), is(true));

    executeApplicationFlow("main");

    assertThat(removeAppAnchorFile(APP_NAME), is(true));
    triggerDirectoryWatcher();
    assertThat(deploymentListener.isAppUndeployed(), is(true));

    new PollingProber(PROBER_POLLING_TIMEOUT, PROBER_POLLING_INTERVAL).check(new JUnitLambdaProbe(() -> {
      clearAllLogs();
      System.gc();
      assertThat(deploymentListener.getPhantomReference().isEnqueued(), is(true));
      return true;
    }));
  }

  private ApplicationFileBuilder getApplicationFileBuilder() throws Exception {
    return createExtensionApplicationWithServices(APP_CONFIG_FILE + ".xml", withLifecycleListenerPlugin);
  }

  @Override
  protected void configureDeploymentService() {
    deploymentService.addDeploymentListener(deploymentListener);
  }

  private static class TestDeploymentListener implements DeploymentListener {

    private PhantomReference<ArtifactClassLoader> phantomReference;

    private boolean appDeployed;

    private boolean appUndeployed;

    private final String appName;

    private final ArtifactLifecycleListenerTestCase deploymentTestCase;

    TestDeploymentListener(ArtifactLifecycleListenerTestCase deploymentTestCase, String appName) {
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
