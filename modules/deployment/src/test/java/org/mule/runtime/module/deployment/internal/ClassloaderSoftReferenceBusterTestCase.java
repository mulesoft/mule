/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.runtime.module.deployment.internal;

import static org.mule.runtime.module.artifact.api.classloader.MuleArtifactClassLoader.MULE_LEAK_PREVENTION;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.Matchers.hasSize;
import static org.junit.Assert.assertThat;
import java.lang.ref.PhantomReference;
import java.lang.ref.ReferenceQueue;

import org.hamcrest.CoreMatchers;
import org.junit.ClassRule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.mule.runtime.deployment.model.api.application.Application;
import org.mule.runtime.module.artifact.api.classloader.ArtifactClassLoader;
import org.mule.runtime.module.deployment.api.DeploymentListener;
import org.mule.tck.junit4.rule.SystemProperty;
import org.mule.tck.probe.JUnitLambdaProbe;
import org.mule.tck.probe.JUnitProbe;
import org.mule.tck.probe.PollingProber;

/**
 * Contains test for application deployment on the default domain
 */
@RunWith(Parameterized.class)
public class ClassloaderSoftReferenceBusterTestCase extends AbstractDeploymentTestCase {

  private static final int PROBER_POLLING_INTERVAL = 100;
  private static final int PROBER_POLIING_TIMEOUT = 5000;

  @ClassRule
  public static SystemProperty muleLeakPrevention = new SystemProperty(MULE_LEAK_PREVENTION, "true");

  private PhantomReference<ArtifactClassLoader> phantomReference;

  private boolean appDeployed;

  private boolean appUndeployed;

  private DeploymentListener deploymentListener = new DeploymentListener() {

    @Override
    public void onDeploymentSuccess(String artifactName) {
      Application app = findApp(emptyAppFileBuilder.getId(), 1);
      appDeployed = true;
      phantomReference =
          new PhantomReference<ArtifactClassLoader>(app.getArtifactClassLoader(), new ReferenceQueue<>());
    };

    @Override
    public void onUndeploymentSuccess(String artifactName) {
      appUndeployed = true;
    };
  };

  public ClassloaderSoftReferenceBusterTestCase(boolean parallellDeployment) {
    super(parallellDeployment);
    this.setUseMockedListeners(false);
  }


  @Test
  public void undeploysApplicationDoesNotLeakClassloader() throws Exception {
    deploymentService.addDeploymentListener(deploymentListener);
    addPackedAppFromBuilder(emptyAppFileBuilder);

    startDeployment();

    assertThat(appDeployed, is(true));

    deploymentListener = null;

    assertThat(removeAppAnchorFile(emptyAppFileBuilder.getId()), is(true));

    new PollingProber(PROBER_POLIING_TIMEOUT, PROBER_POLLING_INTERVAL).check(new JUnitLambdaProbe(() -> {
      assertThat(appUndeployed, is(true));
      return true;
    }));

    new PollingProber(PROBER_POLIING_TIMEOUT, PROBER_POLLING_INTERVAL).check(new JUnitLambdaProbe(() -> {
      System.gc();
      assertThat(phantomReference.isEnqueued(), is(true));
      return true;
    }));
  }
}
