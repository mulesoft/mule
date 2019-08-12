package org.mule.runtime.module.deployment.internal;
/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;

import java.lang.ref.PhantomReference;
import java.lang.ref.ReferenceQueue;

import org.junit.Test;
import org.mule.runtime.deployment.model.api.application.Application;
import org.mule.runtime.module.artifact.api.classloader.ArtifactClassLoader;
import org.mule.runtime.module.deployment.api.DeploymentListener;
import org.mule.runtime.module.deployment.impl.internal.builder.ApplicationFileBuilder;
import org.mule.tck.probe.JUnitLambdaProbe;
import org.mule.tck.probe.PollingProber;

public abstract class ClassLoaderLeakTestCase extends AbstractDeploymentTestCase {


  private static final int PROBER_POLLING_INTERVAL = 100;

  private static final int PROBER_POLIING_TIMEOUT = 5000;

  private String appName;

  private String xmlFile;

  private boolean useEchoPluginInApp;

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

    private String appName;

    private ClassLoaderLeakTestCase deploymentTestCase;

    protected MuleDeploymentService deploymentService;



    TestDeploymentListener(ClassLoaderLeakTestCase deploymentTestCase, String appName) {
      this.deploymentTestCase = deploymentTestCase;
      this.appName = appName;
    }

    @Override
    public void onDeploymentSuccess(String artifactName) {
      Application app = deploymentTestCase.findApp(appName, 1);
      appDeployed = true;
      phantomReference = new PhantomReference<ArtifactClassLoader>(app.getArtifactClassLoader(), new ReferenceQueue<>());
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
