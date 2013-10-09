/*
 * (c) 2003-2014 MuleSoft, Inc. This software is protected under international copyright
 * law. All use of this software is subject to MuleSoft's Master Subscription Agreement
 * (or other master license agreement) separately entered into in writing between you and
 * MuleSoft. If such an agreement is not in place, you may not use the software.
 */
package org.mule.module.launcher;

import org.mule.tck.junit4.AbstractMuleTestCase;

import org.junit.Before;
import org.junit.Test;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

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
}
