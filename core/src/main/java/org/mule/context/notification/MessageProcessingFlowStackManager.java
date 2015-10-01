/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.context.notification;

import org.mule.api.AnnotatedObject;
import org.mule.api.MuleContext;
import org.mule.api.MuleEvent;
import org.mule.api.context.MuleContextAware;
import org.mule.api.execution.ExceptionContextProvider;
import org.mule.api.lifecycle.Initialisable;
import org.mule.api.lifecycle.InitialisationException;
import org.mule.config.DefaultMuleConfiguration;

import java.util.Collections;
import java.util.Map;

import javax.xml.namespace.QName;

/**
 * Manager for handling message processing stacks.
 */
public class MessageProcessingFlowStackManager implements ExceptionContextProvider, MuleContextAware, Initialisable
{
    private static final QName NAME_ANNOTATION_KEY = new QName("http://www.mulesoft.org/schema/mule/documentation", "name");

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
        if (DefaultMuleConfiguration.flowCallStacks)
        {
            notification.getSource().getFlowCallStack().peek().addInvokedMessageProcessor(resolveProcessorName(notification));
        }
    }

    private String resolveProcessorName(MessageProcessorNotification notification)
    {
        if (notification.getProcessor() instanceof AnnotatedObject)
        {
            Object docName = ((AnnotatedObject) notification.getProcessor()).getAnnotation(NAME_ANNOTATION_KEY);
            if (docName != null)
            {
                return String.format("%s @ %s (%s)", notification.getProcessorPath(), muleContext.getConfiguration().getId(), docName.toString());
            }
            else
            {
                return String.format("%s @ %s", notification.getProcessorPath(), muleContext.getConfiguration().getId());
            }
        }
        else
        {
            return String.format("%s @ %s", notification.getProcessorPath(), muleContext.getConfiguration().getId());
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
        if (DefaultMuleConfiguration.flowCallStacks)
        {
            muleEvent.getFlowCallStack().push(new DefaultFlowStackElement(flowName));
        }
    }

    public void onFlowComplete(MuleEvent muleEvent)
    {
        if (DefaultMuleConfiguration.flowCallStacks)
        {
            muleEvent.getFlowCallStack().pop();
        }
    }

    @Override
    public Map<String, Object> getContextInfo(MuleEvent muleEvent)
    {
        if (DefaultMuleConfiguration.flowCallStacks)
        {
            return Collections.<String, Object> singletonMap(FLOW_STACK_INFO_KEY, muleEvent.getFlowCallStack().toString());
        }
        else
        {
            return Collections.<String, Object> emptyMap();
        }
    }
}
