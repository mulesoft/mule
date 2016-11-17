/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.runtime.module.deployment.internal;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import org.mule.runtime.core.api.MuleContext;
import org.mule.runtime.module.deployment.api.DeploymentListener;
import org.mule.tck.junit4.AbstractMuleTestCase;
import org.mule.tck.size.SmallTest;

import org.junit.Test;

@SmallTest
public class MuleContextDeploymentListenerTestCase extends AbstractMuleTestCase {

  private static final String APP_NAME = "app";

  private MuleContext muleContext = mock(MuleContext.class);
  private final DeploymentListener deploymentListener = mock(DeploymentListener.class);
  private MuleContextDeploymentListener contextListener = new MuleContextDeploymentListener(APP_NAME, deploymentListener);

  @Test
  public void notifiesMuleContextCreated() throws Exception {
    contextListener.onCreation(muleContext);

    verify(deploymentListener).onMuleContextCreated(APP_NAME, muleContext);
  }

  @Test
  public void notifiesMuleContextInitialized() throws Exception {
    contextListener.onInitialization(muleContext);

    verify(deploymentListener).onMuleContextInitialised(APP_NAME, muleContext);
  }

  @Test
  public void notifiesMuleContextConfigured() throws Exception {
    contextListener.onConfiguration(muleContext);

    verify(deploymentListener).onMuleContextConfigured(APP_NAME, muleContext);
  }
}
