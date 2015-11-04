/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.context.notification;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.collection.IsCollectionWithSize.hasSize;
import static org.junit.Assert.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.withSettings;
import static org.mule.context.notification.MessageProcessingFlowTraceManager.FLOW_STACK_INFO_KEY;

import org.mule.api.AnnotatedObject;
import org.mule.api.MuleContext;
import org.mule.api.MuleEvent;
import org.mule.api.config.MuleConfiguration;
import org.mule.api.context.notification.FlowCallStack;
import org.mule.api.context.notification.ProcessorsTrace;
import org.mule.api.processor.MessageProcessor;
import org.mule.config.DefaultMuleConfiguration;
import org.mule.tck.junit4.AbstractMuleTestCase;
import org.mule.tck.size.SmallTest;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;

import javax.xml.namespace.QName;

import org.hamcrest.Description;
import org.hamcrest.Matcher;
import org.hamcrest.TypeSafeMatcher;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

@SmallTest
public class MessageProcessingFlowTraceManagerTestCase extends AbstractMuleTestCase
{
    private static QName docNameAttrName = new QName("http://www.mulesoft.org/schema/mule/documentation", "name");
    private static QName sourceFileNameAttrName = new QName("http://www.mulesoft.org/schema/mule/documentation", "sourceFileName");
    private static QName sourceFileLineAttrName = new QName("http://www.mulesoft.org/schema/mule/documentation", "sourceFileLine");

    private static final String NESTED_FLOW_NAME = "nestedFlow";
    private static final String ROOT_FLOW_NAME = "rootFlow";
    private static final String APP_ID = "MessageProcessingFlowTraceManagerTestCase";

    private static boolean originalFlowTrace;

    @BeforeClass
    public static void beforeClass()
    {
        originalFlowTrace = DefaultMuleConfiguration.flowTrace;
        DefaultMuleConfiguration.flowTrace = true;
    }

    @AfterClass
    public static void afterClass()
    {
        DefaultMuleConfiguration.flowTrace = originalFlowTrace;
    }

    private MessageProcessingFlowTraceManager manager;

    @Before
    public void before()
    {
        manager = new MessageProcessingFlowTraceManager();
        MuleContext context = mock(MuleContext.class);
        MuleConfiguration config = mock(MuleConfiguration.class);
        when(config.getId()).thenReturn(APP_ID);
        when(context.getConfiguration()).thenReturn(config);
        manager.setMuleContext(context);
    }

    @Test
    public void newFlowInvocation()
    {
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
        manager.onMessageProcessorNotificationPreInvoke(buildProcessorNotification(event, mock(MessageProcessor.class), NESTED_FLOW_NAME + "_ref"));

        PipelineMessageNotification pipelineNotificationNested = buildPipelineNotification(event, NESTED_FLOW_NAME);
        manager.onPipelineNotificationStart(pipelineNotificationNested);

        String rootEntry = "at " + flowName + "(" + NESTED_FLOW_NAME + "_ref @ " + APP_ID + ")";
        assertThat(getContextInfo(event), is("at " + NESTED_FLOW_NAME + System.lineSeparator() + rootEntry));

        manager.onPipelineNotificationComplete(pipelineNotificationNested);
        assertThat(getContextInfo(event), is(rootEntry));

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

        manager.onMessageProcessorNotificationPreInvoke(buildProcessorNotification(event, mock(MessageProcessor.class), "/comp"));
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

        AnnotatedObject annotatedMessageProcessor = (AnnotatedObject) mock(MessageProcessor.class, withSettings().extraInterfaces(AnnotatedObject.class));
        when(annotatedMessageProcessor.getAnnotation(docNameAttrName)).thenReturn("annotatedName");
        when(annotatedMessageProcessor.getAnnotation(sourceFileNameAttrName)).thenReturn("muleApp.xml");
        when(annotatedMessageProcessor.getAnnotation(sourceFileLineAttrName)).thenReturn(10);
        manager.onMessageProcessorNotificationPreInvoke(buildProcessorNotification(event, (MessageProcessor) annotatedMessageProcessor, "/comp"));
        assertThat(getContextInfo(event), is("at " + flowName + "(/comp @ " + APP_ID + ":muleApp.xml:10 (annotatedName))"));

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

        manager.onMessageProcessorNotificationPreInvoke(buildProcessorNotification(event, mock(MessageProcessor.class), "/comp"));
        assertThat(getContextInfo(event), is("at " + flowName + "(/comp @ " + APP_ID + ")"));

        MuleEvent eventCopy = buildEvent("newAnnotatedComponentCall", event.getFlowCallStack().clone(), event.getProcessorsTrace());
        assertThat(getContextInfo(eventCopy), is("at " + flowName + "(/comp @ " + APP_ID + ")"));

        manager.onPipelineNotificationComplete(pipelineNotification);
        assertThat(getContextInfo(event), is(""));

        String asyncFlowName = "asyncFlow";
        manager.onPipelineNotificationStart(buildPipelineNotification(eventCopy, asyncFlowName));

        manager.onMessageProcessorNotificationPreInvoke(buildProcessorNotification(eventCopy, mock(MessageProcessor.class), "/asyncComp"));
        assertThat(getContextInfo(eventCopy), is("at " + asyncFlowName + "(/asyncComp @ " + APP_ID + ")" + System.lineSeparator() + "at " + flowName + "(/comp @ " + APP_ID + ")"));
    }

    @Test
    public void splitStackEnd()
    {
        MuleEvent event = buildEvent("newAnnotatedComponentCall");
        String flowName = ROOT_FLOW_NAME;
        PipelineMessageNotification pipelineNotification = buildPipelineNotification(event, flowName);

        manager.onPipelineNotificationStart(pipelineNotification);
        manager.onMessageProcessorNotificationPreInvoke(buildProcessorNotification(event, mock(MessageProcessor.class), "/comp"));
        FlowCallStack flowCallStack = event.getFlowCallStack();
        MuleEvent eventCopy = buildEvent("newAnnotatedComponentCall", flowCallStack.clone(), event.getProcessorsTrace());
        manager.onPipelineNotificationComplete(pipelineNotification);
        String asyncFlowName = "asyncFlow";
        manager.onPipelineNotificationStart(buildPipelineNotification(eventCopy, asyncFlowName));
        manager.onMessageProcessorNotificationPreInvoke(buildProcessorNotification(eventCopy, mock(MessageProcessor.class), "/asyncComp"));

        assertThat(event.getProcessorsTrace(), hasExecutedProcessors(
                "/comp @ " + APP_ID,
                "/asyncComp @ " + APP_ID));
    }

    @Test
    public void joinStack()
    {
        MuleEvent event = buildEvent("joinStack");
        String flowName = ROOT_FLOW_NAME;
        PipelineMessageNotification pipelineNotification = buildPipelineNotification(event, flowName);
        assertThat(getContextInfo(event), is(""));

        manager.onPipelineNotificationStart(pipelineNotification);
        assertThat(getContextInfo(event), is("at " + flowName));

        manager.onMessageProcessorNotificationPreInvoke(buildProcessorNotification(event, mock(MessageProcessor.class), "/scatter-gather"));
        assertThat(getContextInfo(event), is("at " + flowName + "(/scatter-gather @ " + APP_ID + ")"));

        MuleEvent eventCopy0 = buildEvent("joinStack_0", event.getFlowCallStack().clone(), event.getProcessorsTrace());
        MuleEvent eventCopy1 = buildEvent("joinStack_1", event.getFlowCallStack().clone(), event.getProcessorsTrace());

        manager.onMessageProcessorNotificationPreInvoke(buildProcessorNotification(eventCopy0, mock(MessageProcessor.class), "/route_0"));

        manager.onMessageProcessorNotificationPreInvoke(buildProcessorNotification(eventCopy1, mock(MessageProcessor.class), NESTED_FLOW_NAME + "_ref"));
        PipelineMessageNotification pipelineNotificationNested = buildPipelineNotification(eventCopy1, NESTED_FLOW_NAME);
        manager.onPipelineNotificationStart(pipelineNotificationNested);
        manager.onMessageProcessorNotificationPreInvoke(buildProcessorNotification(eventCopy1, mock(MessageProcessor.class), "/route_1"));
        assertThat(getContextInfo(eventCopy1),
                is("at " + NESTED_FLOW_NAME + "(/route_1 @ " + APP_ID + ")" + System.lineSeparator() + "at " + ROOT_FLOW_NAME + "(" + NESTED_FLOW_NAME + "_ref @ " + APP_ID + ")"));

        manager.onPipelineNotificationComplete(pipelineNotificationNested);

        assertThat(getContextInfo(event), is("at " + flowName + "(/scatter-gather @ " + APP_ID + ")"));

        manager.onPipelineNotificationComplete(pipelineNotification);
        assertThat(getContextInfo(event), is(""));
    }

    @Test
    public void joinStackEnd()
    {
        MuleEvent event = buildEvent("joinStack");
        String flowName = ROOT_FLOW_NAME;
        PipelineMessageNotification pipelineNotification = buildPipelineNotification(event, flowName);

        manager.onPipelineNotificationStart(pipelineNotification);
        manager.onMessageProcessorNotificationPreInvoke(buildProcessorNotification(event, mock(MessageProcessor.class), "/scatter-gather"));

        FlowCallStack flowCallStack = event.getFlowCallStack();
        MuleEvent eventCopy0 = buildEvent("joinStack_0", flowCallStack.clone(), event.getProcessorsTrace());
        MuleEvent eventCopy1 = buildEvent("joinStack_1", flowCallStack.clone(), event.getProcessorsTrace());

        manager.onMessageProcessorNotificationPreInvoke(buildProcessorNotification(eventCopy0, mock(MessageProcessor.class), "/route_0"));

        manager.onMessageProcessorNotificationPreInvoke(buildProcessorNotification(eventCopy1, mock(MessageProcessor.class), NESTED_FLOW_NAME + "_ref"));
        PipelineMessageNotification pipelineNotificationNested = buildPipelineNotification(eventCopy1, NESTED_FLOW_NAME);
        manager.onPipelineNotificationStart(pipelineNotificationNested);
        manager.onMessageProcessorNotificationPreInvoke(buildProcessorNotification(eventCopy1, mock(MessageProcessor.class), "/route_1"));
        manager.onPipelineNotificationComplete(pipelineNotificationNested);

        manager.onPipelineNotificationComplete(pipelineNotification);

        assertThat(event.getProcessorsTrace(), hasExecutedProcessors(
                "/scatter-gather @ " + APP_ID,
                "/route_0 @ " + APP_ID,
                NESTED_FLOW_NAME + "_ref @ " + APP_ID,
                "/route_1 @ " + APP_ID));
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
        return buildEvent(eventId, new DefaultFlowCallStack(), new DefaultProcessorsTrace());
    }

    protected MuleEvent buildEvent(String eventId, FlowCallStack flowStack, ProcessorsTrace processorsTrace)
    {
        MuleEvent event = mock(MuleEvent.class);
        when(event.getId()).thenReturn(eventId);
        when(event.getFlowCallStack()).thenReturn(flowStack);
        when(event.getProcessorsTrace()).thenReturn(processorsTrace);
        return event;
    }

    protected MessageProcessorNotification buildProcessorNotification(MuleEvent event, MessageProcessor processor, String processorPath)
    {
        MessageProcessorNotification processorNotification = mock(MessageProcessorNotification.class);
        when(processorNotification.getProcessor()).thenReturn(processor);
        when(processorNotification.getProcessorPath()).thenReturn(processorPath);
        when(processorNotification.getSource()).thenReturn(event);
        return processorNotification;
    }

    protected PipelineMessageNotification buildPipelineNotification(MuleEvent event, String flowName)
    {
        PipelineMessageNotification pipelineNotification = mock(PipelineMessageNotification.class);
        when(pipelineNotification.getSource()).thenReturn(event);
        when(pipelineNotification.getResourceIdentifier()).thenReturn(flowName);
        return pipelineNotification;
    }

    private Matcher<ProcessorsTrace> hasExecutedProcessors(final String... expectedProcessors)
    {
        return new TypeSafeMatcher<ProcessorsTrace>()
        {
            private List<Matcher> failed = new ArrayList<>();

            @Override
            protected boolean matchesSafely(ProcessorsTrace processorsTrace)
            {
                Matcher<Collection<? extends Object>> sizeMatcher = hasSize(expectedProcessors.length);
                if (!sizeMatcher.matches(processorsTrace.getExecutedProcessors()))
                {
                    failed.add(sizeMatcher);
                }

                int i = 0;
                for (String expectedProcessor : expectedProcessors)
                {
                    Matcher processorItemMatcher = is(expectedProcessor);
                    if (!processorItemMatcher.matches(processorsTrace.getExecutedProcessors().get(i)))
                    {
                        failed.add(processorItemMatcher);
                    }
                    ++i;
                }

                return failed.isEmpty();
            }

            @Override
            public void describeTo(Description description)
            {
                description.appendValue(Arrays.asList(expectedProcessors));
            }

            @Override
            protected void describeMismatchSafely(ProcessorsTrace item, Description description)
            {
                description.appendText("was ").appendValue(item.getExecutedProcessors());
            }
        };
    }

}
