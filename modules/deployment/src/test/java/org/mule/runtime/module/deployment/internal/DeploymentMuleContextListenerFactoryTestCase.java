/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.runtime.module.deployment.internal;

import static org.mockito.Matchers.any;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

import org.mule.runtime.api.config.custom.CustomizationService;
import org.mule.runtime.core.api.MuleContext;
import org.mule.runtime.core.api.context.notification.MuleContextListener;
import org.mule.runtime.module.deployment.api.DeploymentListener;
import org.mule.tck.junit4.AbstractMuleTestCase;
import org.mule.tck.size.SmallTest;

import org.junit.Test;

@SmallTest
public class DeploymentMuleContextListenerFactoryTestCase extends AbstractMuleTestCase {

  public static final String APP_NAME = "app";
  private final DeploymentListener deploymentListener = mock(DeploymentListener.class);
  private final DeploymentMuleContextListenerFactory factory = new DeploymentMuleContextListenerFactory(deploymentListener);

  @Test
  public void createsContextListener() throws Exception {
    MuleContextListener contextListener = factory.create(APP_NAME);
    MuleContext context = mock(MuleContext.class);

    contextListener.onCreation(context);

    verify(deploymentListener).onArtifactCreated(eq(APP_NAME), any(CustomizationService.class));
  }
}
