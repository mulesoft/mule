/*
 * $Id$
 * --------------------------------------------------------------------------------------
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 *
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.source;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import org.mule.api.MuleContext;
import org.mule.api.construct.FlowConstruct;
import org.mule.api.lifecycle.Lifecycle;
import org.mule.api.lifecycle.LifecycleState;
import org.mule.api.processor.MessageProcessor;
import org.mule.api.source.ClusterizableMessageSource;
import org.mule.context.notification.ClusterNodeNotification;
import org.mule.tck.junit4.AbstractMuleTestCase;
import org.mule.tck.size.SmallTest;

import org.junit.Test;

@SmallTest
public class ClusterizableMessageSourceWrapperTestCase extends AbstractMuleTestCase
{

    private TestMessageSource messageSource = mock(TestMessageSource.class);
    private ClusterizableMessageSourceWrapper wrapper = new ClusterizableMessageSourceWrapper(messageSource);
    private MuleContext muleContext = mock(MuleContext.class);

    @Test
    public void delegatesSetListener()
    {
        MessageProcessor listener = mock(MessageProcessor.class);

        wrapper.setListener(listener);

        verify(messageSource, times(1)).setListener(listener);
    }

    @Test
    public void delegatesDispose() throws Exception
    {
        wrapper.dispose();

        verify(messageSource, times(1)).dispose();
    }

    @Test
    public void delegatesInitialise() throws Exception
    {
        wrapper.initialise();

        verify(messageSource, times(1)).initialise();
    }

    @Test
    public void ignoresStopIfNoStarted() throws Exception
    {
        wrapper.stop();

        verify(messageSource, times(0)).stop();
    }

    @Test
    public void delegatesStop() throws Exception
    {
        when(muleContext.isPrimaryPollingInstance()).thenReturn(true);
        wrapper.setMuleContext(muleContext);

        wrapper.start();
        wrapper.stop();

        verify(messageSource, times(1)).stop();
    }

    @Test
    public void registerNotificationListenerOnInitialization() throws Exception
    {
        wrapper.setMuleContext(muleContext);

        wrapper.initialise();

        verify(muleContext, times(1)).registerListener(wrapper);
    }

    @Test
    public void startsWhenIsPrimaryNode() throws Exception
    {
        when(muleContext.isPrimaryPollingInstance()).thenReturn(true);
        wrapper.setMuleContext(muleContext);

        wrapper.start();

        verify(messageSource, times(1)).start();
    }

    @Test
    public void ignoresStartWhenIsSecondaryNode() throws Exception
    {
        when(muleContext.isPrimaryPollingInstance()).thenReturn(false);
        wrapper.setMuleContext(muleContext);

        wrapper.start();

        verify(messageSource, times(0)).start();
    }

    @Test
    public void ignoresMessageSourceOnNotificationIfFlowIsStopped() throws Exception
    {
        when(muleContext.isPrimaryPollingInstance()).thenReturn(true);
        LifecycleState lifecycleState = mock(LifecycleState.class);
        when(lifecycleState.isStarted()).thenReturn(false);
        FlowConstruct flowConstruct = mock(FlowConstruct.class);
        when(flowConstruct.getLifecycleState()).thenReturn(lifecycleState);

        wrapper.setMuleContext(muleContext);
        wrapper.setFlowConstruct(flowConstruct);

        wrapper.onNotification(mock(ClusterNodeNotification.class));

        verify(messageSource, times(0)).start();
    }

    @Test
    public void startsMessageSourceOnNotificationIfFlowIsStarted() throws Exception
    {
        when(muleContext.isPrimaryPollingInstance()).thenReturn(true);
        LifecycleState lifecycleState = mock(LifecycleState.class);
        when(lifecycleState.isStarted()).thenReturn(true);
        FlowConstruct flowConstruct = mock(FlowConstruct.class);
        when(flowConstruct.getLifecycleState()).thenReturn(lifecycleState);

        wrapper.setMuleContext(muleContext);
        wrapper.setFlowConstruct(flowConstruct);

        wrapper.onNotification(mock(ClusterNodeNotification.class));

        verify(messageSource, times(1)).start();
    }

    @Test
    public void ignoresStartWhenWrappedMessageSourceIsAlreadyStarted() throws Exception
    {
        when(muleContext.isPrimaryPollingInstance()).thenReturn(true);
        wrapper.setMuleContext(muleContext);

        wrapper.start();
        wrapper.start();

        verify(messageSource, times(1)).start();
    }

    @Test
    public void unregistersListenerOnDispose() throws Exception
    {
        wrapper.setMuleContext(muleContext);

        wrapper.dispose();

        verify(muleContext, times(1)).unregisterListener(wrapper);
    }

    private interface TestMessageSource extends ClusterizableMessageSource, Lifecycle
    {

    }
}
