/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.exception;

import static org.hamcrest.core.Is.is;
import static org.junit.Assert.assertThat;
import static org.mockito.Mockito.RETURNS_DEEP_STUBS;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import org.mule.api.MuleContext;
import org.mule.api.MuleException;
import org.mule.api.lifecycle.LifecycleCallback;
import org.mule.api.transport.Connector;
import org.mule.tck.junit4.AbstractMuleContextTestCase;
import org.mule.tck.testmodels.mule.TestConnector;
import org.mule.transport.ConnectException;
import org.mule.transport.ConnectorLifecycleManager;

import org.junit.Before;
import org.junit.Test;
import org.mockito.Matchers;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;

public class DefaultSystemExceptionStrategyTestCase extends AbstractMuleContextTestCase
{

    private FailureStopConnector connector;
    private final ConnectorLifecycleManager lifecycleManager = mock(ConnectorLifecycleManager.class, RETURNS_DEEP_STUBS);
    private final ConnectException connectException = mock(ConnectException.class);
    private DefaultSystemExceptionStrategy exceptionStrategy;

    @Before
    public void setUp() throws Exception
    {
        connector = new FailureStopConnector(muleContext, lifecycleManager);
        connector.setConnecting(false);
        connector.setConnected(true);
        exceptionStrategy = new DefaultSystemExceptionStrategy(muleContext);
        when(connectException.getStackTrace()).thenReturn(new StackTraceElement[] {});
        when(connectException.getFailed()).thenReturn(connector);
        doAnswer(new Answer<Void>()
        {
            @Override
            public Void answer(InvocationOnMock invocation) throws MuleException
            {
                @SuppressWarnings("unchecked")
                LifecycleCallback<Connector> lifecycleCallback = (LifecycleCallback<Connector>) invocation.getArguments()[0];
                lifecycleCallback.onTransition("stop", connector);
                return null;
            }
        }).when(lifecycleManager).fireStopPhase(Matchers.<LifecycleCallback<Connector>>any());
        when(lifecycleManager.getState().isStarted()).thenReturn(false);
    }

    @Test
    public void disconnectionIsAlwaysExecutedInReconnection()
    {
        exceptionStrategy.handleException(connectException);
        assertThat(connector.isConnected(), is(false));
    }

    private static class FailureStopConnector extends TestConnector
    {

        FailureStopConnector(MuleContext context, ConnectorLifecycleManager lifecycleManager)
        {
            super(context);
            this.lifecycleManager = lifecycleManager;
        }

        @Override
        protected void doStop()
        {
            throw new RuntimeException("Test exception");
        }

    }
}