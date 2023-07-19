/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 */
package org.mule.runtime.module.deployment.internal;

import static org.junit.Assert.fail;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

import org.mule.runtime.api.scheduler.SchedulerService;
import org.mule.runtime.deployment.model.api.DeploymentException;
import org.mule.runtime.deployment.model.api.application.Application;
import org.mule.tck.junit4.AbstractMuleTestCase;
import org.mule.tck.size.SmallTest;

import org.junit.Test;

@SmallTest
public class DefaultMuleDeployerTestCase extends AbstractMuleTestCase {

  @Test
  public void disposesAppOnDeployFailure() throws Exception {
    DefaultArtifactDeployer deployer = new DefaultArtifactDeployer(() -> mock(SchedulerService.class));
    Application app = mock(Application.class);
    doThrow(new IllegalStateException()).when(app).init();

    try {
      deployer.deploy(app);
      fail("Deployment is supposed to fail");
    } catch (DeploymentException expected) {
    }

    verify(app, times(1)).dispose();
  }
}
