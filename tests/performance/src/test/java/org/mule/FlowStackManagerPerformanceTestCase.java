/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import org.mule.api.MuleEvent;
import org.mule.config.DefaultMuleConfiguration;
import org.mule.context.notification.DefaultFlowCallStack;
import org.mule.context.notification.MessageProcessingFlowStackManager;
import org.mule.context.notification.PipelineMessageNotification;
import org.mule.tck.junit4.AbstractMuleContextTestCase;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;

import org.databene.contiperf.PerfTest;
import org.databene.contiperf.junit.ContiPerfRule;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Rule;
import org.junit.Test;

public class FlowStackManagerPerformanceTestCase extends AbstractMuleContextTestCase
{
    @Rule
    public ContiPerfRule rule = new ContiPerfRule();

    private static boolean originalFlowCallStacks;

    @BeforeClass
    public static void beforeClass()
    {
        originalFlowCallStacks = DefaultMuleConfiguration.flowCallStacks;
        DefaultMuleConfiguration.flowCallStacks = true;
    }

    @AfterClass
    public static void afterClass()
    {
        DefaultMuleConfiguration.flowCallStacks = originalFlowCallStacks;
    }

    private MessageProcessingFlowStackManager manager = new MessageProcessingFlowStackManager();

    @Test
    @PerfTest(duration = 15000, threads = 1, warmUp = 5000)
    public void testFlowStack() throws Exception
    {
        MuleEvent event = buildEvent();
        PipelineMessageNotification pipelineNotification = buildPipelineNotification(event, "rootFlow");
        PipelineMessageNotification pipelineNotificationNested = buildPipelineNotification(event, "nestedFlow");

        for (int i = 0; i < 1000; ++i)
        {
            manager.onPipelineNotificationStart(pipelineNotification);
            manager.onPipelineNotificationStart(pipelineNotificationNested);
            manager.onPipelineNotificationComplete(pipelineNotificationNested);
            manager.onPipelineNotificationComplete(pipelineNotification);
        }
    }

    @Test
    @PerfTest(duration = 15000, threads = 1, warmUp = 5000)
    public void testSerializeDeserializeFlowStack() throws Exception
    {
        MuleEvent event = buildEvent();
        manager.onPipelineNotificationStart(buildPipelineNotification(event, "rootFlow"));
        for (int j = 0; j < 10; ++j)
        {
            manager.onPipelineNotificationStart(buildPipelineNotification(event, "nestedFlow" + j));
        }

        for (int i = 0; i < 1000; ++i)
        {
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            new ObjectOutputStream(baos).writeObject(event.getFlowCallStack());

            new ObjectInputStream(new ByteArrayInputStream(baos.toByteArray())).readObject();
        }
    }

    @Test
    @PerfTest(duration = 15000, threads = 1, warmUp = 5000)
    public void testSerializeDeserializeEmptyFlowStack() throws Exception
    {
        MuleEvent event = buildEvent();

        for (int i = 0; i < 1000; ++i)
        {
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            new ObjectOutputStream(baos).writeObject(event.getFlowCallStack());

            new ObjectInputStream(new ByteArrayInputStream(baos.toByteArray())).readObject();
        }
    }

    protected MuleEvent buildEvent()
    {
        MuleEvent event = mock(MuleEvent.class);
        when(event.getFlowCallStack()).thenReturn(new DefaultFlowCallStack());
        return event;
    }

    protected PipelineMessageNotification buildPipelineNotification(MuleEvent event, String flowName)
    {
        PipelineMessageNotification pipelineNotification = mock(PipelineMessageNotification.class);
        when(pipelineNotification.getSource()).thenReturn(event);
        when(pipelineNotification.getResourceIdentifier()).thenReturn(flowName);
        return pipelineNotification;
    }
}
