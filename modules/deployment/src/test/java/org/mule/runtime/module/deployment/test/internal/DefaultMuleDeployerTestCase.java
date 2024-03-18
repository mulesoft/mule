/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.module.deployment.test.internal;

import static org.junit.Assert.fail;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

import org.mule.runtime.api.scheduler.SchedulerService;
import org.mule.runtime.deployment.model.api.DeploymentException;
import org.mule.runtime.deployment.model.api.application.Application;
import org.mule.runtime.module.deployment.internal.DefaultArtifactDeployer;
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
