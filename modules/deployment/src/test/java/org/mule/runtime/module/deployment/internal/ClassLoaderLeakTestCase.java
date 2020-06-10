package org.mule.runtime.module.deployment.internal;
/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.Matchers.nullValue;
import static org.junit.Assert.assertThat;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

import org.mule.runtime.deployment.model.api.application.Application;
import org.mule.runtime.module.artifact.api.classloader.ArtifactClassLoader;
import org.mule.runtime.module.deployment.api.DeploymentListener;
import org.mule.runtime.module.deployment.impl.internal.builder.ApplicationFileBuilder;
import org.mule.tck.probe.JUnitLambdaProbe;
import org.mule.tck.probe.PollingProber;

import java.lang.ref.PhantomReference;
import java.lang.ref.ReferenceQueue;
import java.util.concurrent.atomic.AtomicReference;

import org.junit.Test;

import io.qameta.allure.Description;
import io.qameta.allure.Issue;

public abstract class ClassLoaderLeakTestCase extends AbstractDeploymentTestCase {


  private static final int PROBER_POLLING_INTERVAL = 100;

  private static final int PROBER_POLIING_TIMEOUT = 5000;

  private final String appName;

  private final String xmlFile;

  private final boolean useEchoPluginInApp;

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
  @Description("When an artifact is redeployed, objects associated to the original deployment are released befroe deploying the new one.")
  public void redeployPreviousAppEagerlyGCd() throws Exception {
    DeploymentListener mockDeploymentListener = spy(new DeploymentStatusTracker());
    deploymentService.addDeploymentListener(mockDeploymentListener);

    ApplicationFileBuilder applicationFileBuilder = getApplicationFileBuilder();
    addPackedAppFromBuilder(applicationFileBuilder);
    startDeployment();
    assertThat(getDeploymentListener().isAppDeployed(), is(true));

    final PhantomReference<Application> firstAppRef =
        new PhantomReference<>(deploymentService.findApplication(appName), new ReferenceQueue<>());

    AtomicReference<Throwable> redeploymentSuccessThrown = new AtomicReference<>();

    doAnswer(invocation -> {
      try {
        new PollingProber(PROBER_POLIING_TIMEOUT, PROBER_POLLING_INTERVAL).check(new JUnitLambdaProbe(() -> {
          System.gc();
          assertThat(firstAppRef.isEnqueued(), is(true));
          return true;
        }));
      } catch (Throwable t) {
        redeploymentSuccessThrown.set(t);
      }

      return null;
    }).when(mockDeploymentListener).onRedeploymentSuccess(appName);

    deploymentService.redeploy(appName);

    // Application was redeployed
    new PollingProber(PROBER_POLIING_TIMEOUT, PROBER_POLLING_INTERVAL).check(new JUnitLambdaProbe(() -> {
      verify(mockDeploymentListener, times(1)).onRedeploymentSuccess(appName);
      return true;
    }));
    assertThat(redeploymentSuccessThrown.get(), is(nullValue()));
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
