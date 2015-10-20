/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.context.notification;

import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.withSettings;
import static org.mule.context.notification.MessageProcessingFlowStackManager.FLOW_STACK_INFO_KEY;

import org.mule.api.AnnotatedObject;
import org.mule.api.MuleContext;
import org.mule.api.MuleEvent;
import org.mule.api.config.MuleConfiguration;
import org.mule.api.processor.MessageProcessor;
import org.mule.config.DefaultMuleConfiguration;
import org.mule.tck.junit4.AbstractMuleTestCase;
import org.mule.tck.size.SmallTest;
import org.mule.util.UUID;

import javax.xml.namespace.QName;

import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Ignore;
import org.junit.Test;

@SmallTest
public class MessageProcessingFlowStackManagerTestCase extends AbstractMuleTestCase
{

    private static final String NESTED_FLOW_NAME = "nestedFlow";
    private static final String ROOT_FLOW_NAME = "rootFlow";
    private static final String APP_ID = "MessageProcessingFlowStackManagerTestCase";

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

    private MessageProcessingFlowStackManager manager;

    @Before
    public void before()
    {
        manager = new MessageProcessingFlowStackManager();
        MuleContext context = mock(MuleContext.class);
        MuleConfiguration config = mock(MuleConfiguration.class);
        when(config.getId()).thenReturn(APP_ID);
        when(context.getConfiguration()).thenReturn(config);
        manager.setMuleContext(context);
    }
    
    @Test
    public void newFlowInvocation() {
        MuleEvent event = buildEvent("newFlowInvocation");
        String flowName = ROOT_FLOW_NAME;
        PipelineMessageNotification pipelineNotification = buildPipelineNotification(event, flowName);

        assertThat(getContextInfo(event), is(""));

        manager.onPipelineNotificationStart(pipelineNotification);

        assertThat(getContextInfo(event), is("at " + flowName));

        manager.onPipelineNotificationComplete(pipelineNotification);

        assertThat(getContextInfo(event), is(""));
    }

    @Test
    public void nestedFlowInvocations()
    {
        MuleEvent event = buildEvent("nestedFlowInvocations");
        String flowName = ROOT_FLOW_NAME;
        PipelineMessageNotification pipelineNotification = buildPipelineNotification(event, flowName);

        assertThat(getContextInfo(event), is(""));

        manager.onPipelineNotificationStart(pipelineNotification);

        PipelineMessageNotification pipelineNotificationNested = mock(PipelineMessageNotification.class);
        when(event.getId()).thenReturn("nestedFlowInvocations");
        when(pipelineNotificationNested.getSource()).thenReturn(event);
        when(pipelineNotificationNested.getResourceIdentifier()).thenReturn(NESTED_FLOW_NAME);

        manager.onPipelineNotificationStart(pipelineNotificationNested);

        assertThat(getContextInfo(event), is("at " + NESTED_FLOW_NAME + System.lineSeparator() + "at " + flowName));

        manager.onPipelineNotificationComplete(pipelineNotificationNested);

        assertThat(getContextInfo(event), is("at " + flowName));

        manager.onPipelineNotificationComplete(pipelineNotification);
        assertThat(getContextInfo(event), is(""));
    }

    @Test
    public void newComponentCall()
    {
        MuleEvent event = buildEvent("newComponentCall");
        String flowName = ROOT_FLOW_NAME;
        PipelineMessageNotification pipelineNotification = buildPipelineNotification(event, flowName);

        assertThat(getContextInfo(event), is(""));

        manager.onPipelineNotificationStart(pipelineNotification);

        assertThat(getContextInfo(event), is("at " + ROOT_FLOW_NAME));

        MessageProcessorNotification processorNotification = mock(MessageProcessorNotification.class);
        when(processorNotification.getProcessor()).thenReturn(mock(MessageProcessor.class));
        when(processorNotification.getProcessorPath()).thenReturn("/comp");
        when(processorNotification.getSource()).thenReturn(event);
        manager.onMessageProcessorNotificationPreInvoke(processorNotification);

        assertThat(getContextInfo(event), is("at " + ROOT_FLOW_NAME + "(/comp @ " + APP_ID + ")"));

        manager.onPipelineNotificationComplete(pipelineNotification);

        assertThat(getContextInfo(event), is(""));
    }

    protected String getContextInfo(MuleEvent event)
    {
        return (String) manager.getContextInfo(event, null).get(FLOW_STACK_INFO_KEY);
    }

    @Test
    public void newAnnotatedComponentCall()
    {
        MuleEvent event = buildEvent("newAnnotatedComponentCall");
        String flowName = ROOT_FLOW_NAME;
        PipelineMessageNotification pipelineNotification = buildPipelineNotification(event, flowName);

        assertThat(getContextInfo(event), is(""));

        manager.onPipelineNotificationStart(pipelineNotification);

        assertThat(getContextInfo(event), is("at " + flowName));

        MessageProcessorNotification processorNotification = mock(MessageProcessorNotification.class);
        AnnotatedObject annotatedMessageProcessor = (AnnotatedObject) mock(MessageProcessor.class, withSettings().extraInterfaces(AnnotatedObject.class));
        when(annotatedMessageProcessor.getAnnotation(any(QName.class))).thenReturn("annotatedName");
        when(processorNotification.getProcessor()).thenReturn((MessageProcessor) annotatedMessageProcessor);
        when(processorNotification.getProcessorPath()).thenReturn("/comp");
        when(processorNotification.getSource()).thenReturn(event);
        manager.onMessageProcessorNotificationPreInvoke(processorNotification);

        assertThat(getContextInfo(event), is("at " + flowName + "(/comp @ " + APP_ID + " (annotatedName))"));

        manager.onPipelineNotificationComplete(pipelineNotification);

        assertThat(getContextInfo(event), is(""));
    }

    @Test
    public void splitStack()
    {
        MuleEvent event = buildEvent("newAnnotatedComponentCall");
        String flowName = ROOT_FLOW_NAME;
        PipelineMessageNotification pipelineNotification = buildPipelineNotification(event, flowName);

        assertThat(getContextInfo(event), is(""));

        manager.onPipelineNotificationStart(pipelineNotification);

        assertThat(getContextInfo(event), is("at " + flowName));

        MessageProcessorNotification processorNotification = mock(MessageProcessorNotification.class);
        when(processorNotification.getProcessor()).thenReturn(mock(MessageProcessor.class));
        when(processorNotification.getProcessorPath()).thenReturn("/comp");
        when(processorNotification.getSource()).thenReturn(event);
        manager.onMessageProcessorNotificationPreInvoke(processorNotification);

        assertThat(getContextInfo(event), is("at " + flowName + "(/comp @ " + APP_ID + ")"));

        MuleEvent eventCopy = mock(MuleEvent.class);
        when(eventCopy.getId()).thenReturn("newAnnotatedComponentCall");
        DefaultFlowCallStack copyFlowStack = new DefaultFlowCallStack(event.getFlowCallStack());
        when(eventCopy.getFlowCallStack()).thenReturn(copyFlowStack);

        manager.onPipelineNotificationComplete(pipelineNotification);

        assertThat(getContextInfo(event), is(""));

        String asyncFlowName = "asyncFlow";
        PipelineMessageNotification asyncPipelineNotification = buildPipelineNotification(eventCopy, asyncFlowName);
        manager.onPipelineNotificationStart(asyncPipelineNotification);

        MessageProcessorNotification asyncProcessorNotification = mock(MessageProcessorNotification.class);
        when(asyncProcessorNotification.getProcessor()).thenReturn(mock(MessageProcessor.class));
        when(asyncProcessorNotification.getProcessorPath()).thenReturn("/asyncComp");
        when(asyncProcessorNotification.getSource()).thenReturn(eventCopy);
        manager.onMessageProcessorNotificationPreInvoke(asyncProcessorNotification);

        assertThat(getContextInfo(eventCopy), is("at " + asyncFlowName + "(/asyncComp @ " + APP_ID + ")" + System.lineSeparator() + "at " + flowName + "(/comp @ " + APP_ID + ")"));
    }

    @Test
    public void mixedEvents()
    {
        MuleEvent event1 = buildEvent("mixedEvents_1");
        MuleEvent event2 = buildEvent("mixedEvents_2");

        String flowName = ROOT_FLOW_NAME;
        PipelineMessageNotification pipelineNotification1 = buildPipelineNotification(event1, flowName);
        PipelineMessageNotification pipelineNotification2 = buildPipelineNotification(event2, flowName);

        assertThat(getContextInfo(event1), is(""));
        assertThat(getContextInfo(event2), is(""));

        manager.onPipelineNotificationStart(pipelineNotification1);

        assertThat(getContextInfo(event1), is("at " + flowName));
        assertThat(getContextInfo(event2), is(""));

        manager.onPipelineNotificationStart(pipelineNotification2);

        assertThat(getContextInfo(event1), is("at " + flowName));
        assertThat(getContextInfo(event2), is("at " + flowName));

        manager.onPipelineNotificationComplete(pipelineNotification1);

        assertThat(getContextInfo(event1), is(""));
        assertThat(getContextInfo(event2), is("at " + flowName));

        manager.onPipelineNotificationComplete(pipelineNotification2);

        assertThat(getContextInfo(event1), is(""));
        assertThat(getContextInfo(event2), is(""));
    }

    protected MuleEvent buildEvent(String eventId)
    {
        MuleEvent event = mock(MuleEvent.class);
        when(event.getId()).thenReturn(eventId);
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

    @Ignore
    @Test
    public void footPrint()
    {
        Runtime runtime = Runtime.getRuntime();

        runtime.gc();
        long memBefore = runtime.totalMemory() - runtime.freeMemory();

        runtime.gc();
        long memMiddle = runtime.totalMemory() - runtime.freeMemory();
        System.out.println(memMiddle - memBefore);

        for (int i = 0; i < 5000; ++i)
        {
            MuleEvent event = buildEvent(UUID.getUUID());
            manager.onPipelineNotificationStart(buildPipelineNotification(event, ROOT_FLOW_NAME));
            for (int j = 0; j < 10; ++j)
            {
                manager.onPipelineNotificationStart(buildPipelineNotification(event, NESTED_FLOW_NAME + j));
            }
        }

        runtime.gc();
        long memAfter = runtime.totalMemory() - runtime.freeMemory();
        System.out.println(memAfter - memMiddle);
    }

    
}
