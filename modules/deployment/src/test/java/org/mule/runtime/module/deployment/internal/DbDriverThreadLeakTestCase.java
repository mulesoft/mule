/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.runtime.module.deployment.internal;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.Matchers.greaterThanOrEqualTo;
import static org.junit.Assert.assertThat;
import static org.mule.runtime.module.deployment.internal.DeploymentDirectoryWatcher.CHANGE_CHECK_INTERVAL_PROPERTY;

import org.mule.runtime.deployment.model.api.application.Application;
import org.mule.runtime.module.artifact.api.classloader.ArtifactClassLoader;
import org.mule.runtime.module.deployment.api.DeploymentListener;
import org.mule.runtime.module.deployment.impl.internal.builder.ApplicationFileBuilder;
import org.mule.tck.junit4.rule.SystemProperty;
import org.mule.tck.probe.JUnitLambdaProbe;
import org.mule.tck.probe.PollingProber;

import org.junit.Rule;
import org.junit.Test;

import java.lang.ref.PhantomReference;
import java.lang.ref.ReferenceQueue;

public abstract class DbDriverThreadLeakTestCase extends AbstractDeploymentTestCase {

  @Rule
  public SystemProperty directoryWatcherChangeCheckInterval = new SystemProperty(CHANGE_CHECK_INTERVAL_PROPERTY, "5");

  private static final int PROBER_POLLING_INTERVAL = 500;
  private static final int PROBER_POLLING_TIMEOUT = 8000;
  public static final String ORACLE_DRIVER_TIMER_THREAD_NAME = "Timer-";
  public static final String ORACLE_DRIVER_TIMER_THREAD_CLASS_NAME = "TimerThread";

  private final String xmlFile;
  private final String appName;

  private TestDeploymentListener deploymentListener;

  public DbDriverThreadLeakTestCase(boolean parallellDeployment, String appName, String xmlFile) {
    super(parallellDeployment);
    this.appName = appName;
    this.xmlFile = xmlFile;
  }

  @Test
  public void oracleDriverTimerThreadsReleasedOnUndeploy() throws Exception {
    ApplicationFileBuilder applicationFileBuilder = getApplicationFileBuilder();

    addPackedAppFromBuilder(applicationFileBuilder);

    startDeployment();

    assertThat(getDeploymentListener().isAppDeployed(), is(true));

    new PollingProber(PROBER_POLLING_TIMEOUT, PROBER_POLLING_INTERVAL).check(new JUnitLambdaProbe(() -> {
      assertThat(countLiveThreadsWithName(ORACLE_DRIVER_TIMER_THREAD_NAME, ORACLE_DRIVER_TIMER_THREAD_CLASS_NAME),
                 is(greaterThanOrEqualTo(1)));
      return true;
    }));

    assertThat(removeAppAnchorFile(appName), is(true));

    new PollingProber(PROBER_POLLING_TIMEOUT, PROBER_POLLING_INTERVAL).check(new JUnitLambdaProbe(() -> {
      assertThat(getDeploymentListener().isAppUndeployed(), is(true));
      return true;
    }));

    assertThat(countLiveThreadsWithName(ORACLE_DRIVER_TIMER_THREAD_NAME, ORACLE_DRIVER_TIMER_THREAD_CLASS_NAME), is(0));
  }

  public int countLiveThreadsWithName(String threadName, String threadClassName) {
    int count = 0;
    Thread[] threads = new Thread[Thread.activeCount()];
    Thread.enumerate(threads);
    for (Thread thread : threads) {
      if (thread.getName().startsWith(threadName) && thread.getClass().getName().contains(threadClassName)) {
        count += 1;
      }
    }
    return count;
  }

  private ApplicationFileBuilder getApplicationFileBuilder() throws Exception {
    return createExtensionApplicationWithServices(xmlFile + ".xml",
                                                  oracleExtensionPlugin);
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

    private final DbDriverThreadLeakTestCase deploymentTestCase;

    protected MuleDeploymentService deploymentService;



    TestDeploymentListener(DbDriverThreadLeakTestCase deploymentTestCase, String appName) {
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
