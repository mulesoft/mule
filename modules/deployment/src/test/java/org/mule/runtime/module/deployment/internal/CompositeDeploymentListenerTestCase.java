/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.module.deployment.internal;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import org.mule.runtime.api.artifact.Registry;
import org.mule.runtime.api.config.custom.CustomizationService;
import org.mule.runtime.core.api.MuleContext;
import org.mule.runtime.module.deployment.api.DeploymentListener;
import org.mule.tck.junit4.AbstractMuleTestCase;
import org.mule.tck.size.SmallTest;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

@SmallTest
@RunWith(MockitoJUnitRunner.class)
public class CompositeDeploymentListenerTestCase extends AbstractMuleTestCase {

  private static final String APP_NAME = "foo";
  private static final Exception DEPLOYMENT_EXCEPTION = new Exception("Exception on foo");

  private CompositeDeploymentListener compositeDeploymentListener;

  @Mock
  private DeploymentListener listener1;

  @Mock
  private DeploymentListener listener2;

  @Mock
  private MuleContext muleContext;

  @Mock
  private CustomizationService customizationService;

  @Before
  public void setUp() throws Exception {
    when(muleContext.getCustomizationService()).thenReturn(customizationService);
    compositeDeploymentListener = new CompositeDeploymentListener();
    compositeDeploymentListener.addDeploymentListener(listener1);
    compositeDeploymentListener.addDeploymentListener(listener2);
  }

  @Test
  public void testNotifiesDeploymentStart() throws Exception {
    compositeDeploymentListener.onDeploymentStart(APP_NAME);

    verify(listener1, times(1)).onDeploymentStart(APP_NAME);
    verify(listener2, times(1)).onDeploymentStart(APP_NAME);
  }

  @Test
  public void testNotifiesDeploymentSuccess() throws Exception {
    compositeDeploymentListener.onDeploymentSuccess(APP_NAME);

    verify(listener1, times(1)).onDeploymentSuccess(APP_NAME);
    verify(listener2, times(1)).onDeploymentSuccess(APP_NAME);
  }

  @Test
  public void testNotifiesDeploymentFailure() throws Exception {
    compositeDeploymentListener.onDeploymentFailure(APP_NAME, DEPLOYMENT_EXCEPTION);

    verify(listener1, times(1)).onDeploymentFailure(APP_NAME, DEPLOYMENT_EXCEPTION);
    verify(listener2, times(1)).onDeploymentFailure(APP_NAME, DEPLOYMENT_EXCEPTION);
  }

  @Test
  public void testNotifiesUndeploymentStart() throws Exception {
    compositeDeploymentListener.onUndeploymentStart(APP_NAME);

    verify(listener1, times(1)).onUndeploymentStart(APP_NAME);
    verify(listener2, times(1)).onUndeploymentStart(APP_NAME);
  }

  @Test
  public void testNotifiesUndeploymentSuccess() throws Exception {
    compositeDeploymentListener.onUndeploymentSuccess(APP_NAME);

    verify(listener1, times(1)).onUndeploymentSuccess(APP_NAME);
    verify(listener2, times(1)).onUndeploymentSuccess(APP_NAME);
  }

  @Test
  public void testNotifiesUndeploymentFailure() throws Exception {
    compositeDeploymentListener.onUndeploymentFailure(APP_NAME, DEPLOYMENT_EXCEPTION);

    verify(listener1, times(1)).onUndeploymentFailure(APP_NAME, DEPLOYMENT_EXCEPTION);
    verify(listener2, times(1)).onUndeploymentFailure(APP_NAME, DEPLOYMENT_EXCEPTION);
  }

  @Test
  public void testNotifiesMuleContextCreated() throws Exception {
    compositeDeploymentListener.onArtifactCreated(APP_NAME, customizationService);

    verify(listener1, times(1)).onArtifactCreated(APP_NAME, customizationService);
    verify(listener2, times(1)).onArtifactCreated(APP_NAME, customizationService);
  }

  @Test
  public void testNotifiesMuleContextInitialised() throws Exception {
    Registry registry = mock(Registry.class);
    compositeDeploymentListener.onArtifactInitialised(APP_NAME, registry);

    verify(listener1, times(1)).onArtifactInitialised(APP_NAME, registry);
    verify(listener2, times(1)).onArtifactInitialised(APP_NAME, registry);
  }
}
