/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.context.notification;

import org.mule.api.MuleContext;
import org.mule.api.MuleEvent;
import org.mule.api.context.MuleContextAware;
import org.mule.api.execution.LocationExecutionContextProvider;
import org.mule.api.lifecycle.Initialisable;
import org.mule.api.lifecycle.InitialisationException;
import org.mule.api.processor.MessageProcessor;
import org.mule.config.DefaultMuleConfiguration;
import org.mule.processor.chain.SubFlowMessageProcessor;

import java.util.Collections;
import java.util.Map;

/**
 * Manager for handling message processing stacks.
 */
public class MessageProcessingFlowStackManager extends LocationExecutionContextProvider implements MuleContextAware, Initialisable
{
    public static final String FLOW_STACK_INFO_KEY = "FlowStack";

    private final FlowNotificationTextDebugger pipelineProcessorDebugger;
    private final MessageProcessorTextDebugger messageProcessorTextDebugger;

    private MuleContext muleContext;

    public MessageProcessingFlowStackManager()
    {
        messageProcessorTextDebugger = new MessageProcessorTextDebugger(this);
        pipelineProcessorDebugger = new FlowNotificationTextDebugger(this);

    }

    public void onMessageProcessorNotificationPreInvoke(MessageProcessorNotification notification)
    {
        if (DefaultMuleConfiguration.isFlowCallStacks())
        {
            ((DefaultFlowCallStack) notification.getSource().getFlowCallStack()).addInvokedMessageProcessor(
                    resolveProcessorRepresentation(muleContext.getConfiguration().getId(), notification.getProcessorPath(), notification.getProcessor()));

            if (notification.getProcessor() instanceof SubFlowMessageProcessor)
            {
                onFlowStart(notification.getSource(), ((SubFlowMessageProcessor) notification.getProcessor()).getSubFlowName());
            }
        }
    }

    public void onMessageProcessorNotificationPostInvoke(MessageProcessorNotification notification)
    {
        if (DefaultMuleConfiguration.isFlowCallStacks())
        {
            if (notification.getProcessor() instanceof SubFlowMessageProcessor)
            {
                onFlowComplete(notification.getSource());
            }
        }
    }

    @Override
    public void setMuleContext(MuleContext context)
    {
        this.muleContext = context;
    }


    @Override
    public void initialise() throws InitialisationException
    {
        muleContext.getNotificationManager().addListener(messageProcessorTextDebugger);
        muleContext.getNotificationManager().addListener(pipelineProcessorDebugger);
    }

    public void onPipelineNotificationComplete(PipelineMessageNotification notification)
    {
        onFlowComplete((MuleEvent) notification.getSource());
    }

    public void onPipelineNotificationStart(PipelineMessageNotification notification)
    {
        onFlowStart((MuleEvent) notification.getSource(), notification.getResourceIdentifier());
    }

    public void onFlowStart(MuleEvent muleEvent, String flowName)
    {
        if (DefaultMuleConfiguration.isFlowCallStacks())
        {
            ((DefaultFlowCallStack) muleEvent.getFlowCallStack()).push(new DefaultFlowStackElement(flowName));
        }
    }

    public void onFlowComplete(MuleEvent muleEvent)
    {
        if (DefaultMuleConfiguration.isFlowCallStacks())
        {
            ((DefaultFlowCallStack) muleEvent.getFlowCallStack()).pop();
        }
    }

    @Override
    public Map<String, Object> getContextInfo(MuleEvent muleEvent, MessageProcessor lastProcessed)
    {
        if (DefaultMuleConfiguration.isFlowCallStacks())
        {
            return Collections.<String, Object> singletonMap(FLOW_STACK_INFO_KEY, muleEvent.getFlowCallStack().toString());
        }
        else
        {
            return Collections.<String, Object> emptyMap();
        }
    }
}
