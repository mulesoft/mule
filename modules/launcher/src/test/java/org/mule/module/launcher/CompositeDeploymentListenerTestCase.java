/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.module.launcher;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import org.mule.api.MuleContext;
import org.mule.tck.junit4.AbstractMuleTestCase;

import org.junit.Before;
import org.junit.Test;

public class CompositeDeploymentListenerTestCase extends AbstractMuleTestCase
{
    private static final String APP_NAME = "foo";
    private static final Exception DEPLOYMENT_EXCEPTION = new Exception("Exception on foo");

    private CompositeDeploymentListener compositeDeploymentListener;
    private DeploymentListener listener1;
    private DeploymentListener listener2;

    @Before
    public void setUp() throws Exception
    {
        compositeDeploymentListener = new CompositeDeploymentListener();
        listener1 = mock(DeploymentListener.class);
        compositeDeploymentListener.addDeploymentListener(listener1);
        listener2 = mock(DeploymentListener.class);
        compositeDeploymentListener.addDeploymentListener(listener2);
    }

    @Test
    public void testNotifiesDeploymentStart() throws Exception
    {
        compositeDeploymentListener.onDeploymentStart(APP_NAME);

        verify(listener1, times(1)).onDeploymentStart(APP_NAME);
        verify(listener2, times(1)).onDeploymentStart(APP_NAME);
    }

    @Test
    public void testNotifiesDeploymentSuccess() throws Exception
    {
        compositeDeploymentListener.onDeploymentSuccess(APP_NAME);

        verify(listener1, times(1)).onDeploymentSuccess(APP_NAME);
        verify(listener2, times(1)).onDeploymentSuccess(APP_NAME);
    }

    @Test
    public void testNotifiesDeploymentFailure() throws Exception
    {
        compositeDeploymentListener.onDeploymentFailure(APP_NAME, DEPLOYMENT_EXCEPTION);

        verify(listener1, times(1)).onDeploymentFailure(APP_NAME, DEPLOYMENT_EXCEPTION);
        verify(listener2, times(1)).onDeploymentFailure(APP_NAME, DEPLOYMENT_EXCEPTION);
    }

    @Test
    public void testNotifiesUndeploymentStart() throws Exception
    {
        compositeDeploymentListener.onUndeploymentStart(APP_NAME);

        verify(listener1, times(1)).onUndeploymentStart(APP_NAME);
        verify(listener2, times(1)).onUndeploymentStart(APP_NAME);
    }

    @Test
    public void testNotifiesUndeploymentSuccess() throws Exception
    {
        compositeDeploymentListener.onUndeploymentSuccess(APP_NAME);

        verify(listener1, times(1)).onUndeploymentSuccess(APP_NAME);
        verify(listener2, times(1)).onUndeploymentSuccess(APP_NAME);
    }

    @Test
    public void testNotifiesUndeploymentFailure() throws Exception
    {
        compositeDeploymentListener.onUndeploymentFailure(APP_NAME, DEPLOYMENT_EXCEPTION);

        verify(listener1, times(1)).onUndeploymentFailure(APP_NAME, DEPLOYMENT_EXCEPTION);
        verify(listener2, times(1)).onUndeploymentFailure(APP_NAME, DEPLOYMENT_EXCEPTION);
    }

    @Test
    public void testNotifiesMuleContextCreated() throws Exception
    {
        MuleContext context = mock(MuleContext.class);
        compositeDeploymentListener.onMuleContextCreated(APP_NAME, context);

        verify(listener1, times(1)).onMuleContextCreated(APP_NAME, context);
        verify(listener2, times(1)).onMuleContextCreated(APP_NAME, context);
    }

    @Test
    public void testNotifiesMuleContextInitialised() throws Exception
    {
        MuleContext context = mock(MuleContext.class);
        compositeDeploymentListener.onMuleContextInitialised(APP_NAME, context);

        verify(listener1, times(1)).onMuleContextInitialised(APP_NAME, context);
        verify(listener2, times(1)).onMuleContextInitialised(APP_NAME, context);
    }

    @Test
    public void testNotifiesMuleContextConfigured() throws Exception
    {
        MuleContext context = mock(MuleContext.class);
        compositeDeploymentListener.onMuleContextConfigured(APP_NAME, context);

        verify(listener1, times(1)).onMuleContextConfigured(APP_NAME, context);
        verify(listener2, times(1)).onMuleContextConfigured(APP_NAME, context);
    }
}
