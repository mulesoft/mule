/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.transport;

import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import org.mule.api.MuleException;
import org.mule.api.lifecycle.Disposable;
import org.mule.api.lifecycle.LifecycleState;
import org.mule.api.lifecycle.Startable;
import org.mule.api.lifecycle.Stoppable;
import org.mule.api.transport.MessageDispatcher;
import org.mule.tck.junit4.AbstractMuleTestCase;
import org.mule.tck.size.SmallTest;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

@SmallTest
@RunWith(MockitoJUnitRunner.class)
public class MessageDispatcherUtilsTestCase extends AbstractMuleTestCase
{

    @Mock
    private AbstractConnector connector;

    @Mock
    private ConnectorLifecycleManager connectorLifecycleManager;

    @Mock
    private MessageDispatcher dispatcher;

    @Mock
    private LifecycleState dispatcherLifecycleState;

    @Before
    public void setUp()
    {
        when(dispatcher.getConnector()).thenReturn(connector);
        when(dispatcher.getLifecycleState()).thenReturn(dispatcherLifecycleState);
        when(connector.getLifecycleManager()).thenReturn(connectorLifecycleManager);
    }

    @Test
    public void startsAndInitialisesDispatcherWhenStartingConnector() throws MuleException
    {
        when(connectorLifecycleManager.getCurrentPhase()).thenReturn(Startable.PHASE_NAME);
        when(dispatcherLifecycleState.isStarted()).thenReturn(false);
        when(dispatcherLifecycleState.isInitialised()).thenReturn(false);
        MessageDispatcherUtils.applyLifecycle(dispatcher);
        verify(dispatcher).initialise();
        verify(dispatcher).start();
    }

    @Test
    public void startsInitialisedDispatcherWhenStartingConnector() throws MuleException
    {
        when(connectorLifecycleManager.getCurrentPhase()).thenReturn(Startable.PHASE_NAME);
        when(dispatcherLifecycleState.isStarted()).thenReturn(false);
        when(dispatcherLifecycleState.isInitialised()).thenReturn(true);
        MessageDispatcherUtils.applyLifecycle(dispatcher);
        verify(dispatcher).start();
    }

    @Test
    public void doesntStartAlreadyStartedDispatcher() throws MuleException
    {
        when(connectorLifecycleManager.getCurrentPhase()).thenReturn(Startable.PHASE_NAME);
        when(dispatcherLifecycleState.isStarted()).thenReturn(true);
        MessageDispatcherUtils.applyLifecycle(dispatcher);
        verify(dispatcher, never()).start();
    }

    @Test
    public void stopsDispatcherWhenStoppingConnector() throws MuleException
    {
        when(connectorLifecycleManager.getCurrentPhase()).thenReturn(Stoppable.PHASE_NAME);
        when(dispatcherLifecycleState.isStarted()).thenReturn(true);
        MessageDispatcherUtils.applyLifecycle(dispatcher);
        verify(dispatcher).stop();
    }

    @Test
    public void doesntStopAlreadyStoppedDispatcher() throws MuleException
    {
        when(connectorLifecycleManager.getCurrentPhase()).thenReturn(Stoppable.PHASE_NAME);
        when(dispatcherLifecycleState.isStarted()).thenReturn(false);
        MessageDispatcherUtils.applyLifecycle(dispatcher);
        verify(dispatcher, never()).stop();
    }

    @Test
    public void disposesDispatcherWhenDisposingConnector() throws MuleException
    {
        when(connectorLifecycleManager.getCurrentPhase()).thenReturn(Disposable.PHASE_NAME);
        MessageDispatcherUtils.applyLifecycle(dispatcher);
        verify(dispatcher).dispose();
    }
}
