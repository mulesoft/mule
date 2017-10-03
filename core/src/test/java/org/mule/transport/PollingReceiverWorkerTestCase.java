/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.transport;

import static org.hamcrest.CoreMatchers.nullValue;
import static org.hamcrest.core.Is.is;
import static org.junit.Assert.assertThat;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import org.mule.OptimizedRequestContext;
import org.mule.RequestContext;
import org.mule.api.MuleContext;
import org.mule.api.MuleEvent;
import org.mule.api.construct.FlowConstruct;
import org.mule.tck.junit4.AbstractMuleTestCase;
import org.mule.tck.size.SmallTest;

import org.junit.Test;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;

@SmallTest
public class PollingReceiverWorkerTestCase extends AbstractMuleTestCase
{

    @Test
    public void skipsPollingWhenMuleContextIsStopping() throws Exception
    {
        AbstractPollingMessageReceiver messageReceiver = createPollingMessageReceiver(true);

        PollingReceiverWorker pollingReceiverWorker = new PollingReceiverWorker(messageReceiver);

        pollingReceiverWorker.run();

        verify(messageReceiver, never()).performPoll();
    }

    @Test
    public void executesPollingWhenMuleContextIsNotStopping() throws Exception
    {
        AbstractPollingMessageReceiver messageReceiver = createPollingMessageReceiver(false);

        PollingReceiverWorker pollingReceiverWorker = new PollingReceiverWorker(messageReceiver);

        pollingReceiverWorker.run();

        verify(messageReceiver).performPoll();
    }

    @Test
    public void requestContextIsCleanAfterPolling() throws Exception
    {
        AbstractPollingMessageReceiver messageReceiver = mockRequestContextSettingInPollingReceiver(createPollingMessageReceiver(false));

        PollingReceiverWorker pollingReceiverWorker = new PollingReceiverWorker(messageReceiver);

        pollingReceiverWorker.run();

        assertThat(RequestContext.getEvent(), is(nullValue()));
    }

    private AbstractPollingMessageReceiver createPollingMessageReceiver(boolean isStopping)
    {
        AbstractPollingMessageReceiver messageReceiver = mock(AbstractPollingMessageReceiver.class);
        when(messageReceiver.isStarted()).thenReturn(true);
        FlowConstruct flowConstruct = mock(FlowConstruct.class);
        MuleContext muleContext = mock(MuleContext.class);
        when(flowConstruct.getMuleContext()).thenReturn(muleContext);
        when(muleContext.isStopping()).thenReturn(isStopping);
        when(messageReceiver.getFlowConstruct()).thenReturn(flowConstruct);
        return messageReceiver;
    }

    private AbstractPollingMessageReceiver mockRequestContextSettingInPollingReceiver(AbstractPollingMessageReceiver pollingMessageReceiver) throws Exception
    {
        doAnswer(new Answer<Void>()
        {
            @Override
            public Void answer(InvocationOnMock invocation) throws Throwable
            {
                OptimizedRequestContext.criticalSetEvent(mock(MuleEvent.class));
                return null;
            }
        }).when(pollingMessageReceiver).performPoll();

        return pollingMessageReceiver;
    }

}
