/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.context.notification;

import org.mule.api.context.notification.FlowStackElement;

public class DefaultFlowStackElement implements FlowStackElement
{
    private static final long serialVersionUID = -851491195125245390L;

    private String flowName;
    private String currentProcessorPath;

    public DefaultFlowStackElement(String flowName)
    {
        this.flowName = flowName;
    }

    public void addInvokedMessageProcessor(String processorPath)
    {
        this.currentProcessorPath = processorPath;
    }

    @Override
    public String currentMessageProcessor()
    {
        return currentProcessorPath;
    }

    @Override
    public String getFlowName()
    {
        return flowName;
    }

    @Override
    public String toString()
    {
        if (currentProcessorPath == null)
        {
            return String.format("%s", flowName);
        }
        else
        {
            return String.format("%s(%s)", flowName, currentProcessorPath);
        }
    }

    @Override
    public DefaultFlowStackElement clone()
    {
        DefaultFlowStackElement defaultFlowStackElement = new DefaultFlowStackElement(flowName);
        defaultFlowStackElement.currentProcessorPath = currentProcessorPath;
        return defaultFlowStackElement;
    }
}
