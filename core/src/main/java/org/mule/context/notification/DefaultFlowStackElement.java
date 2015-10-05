/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.context.notification;

import org.mule.api.context.notification.FlowStackElement;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

public class DefaultFlowStackElement implements FlowStackElement, Serializable
{
    private static final long serialVersionUID = -2950239332824176742L;

    private String flowName;
    private List<String> processorPaths;

    public DefaultFlowStackElement(String flowName)
    {
        this.flowName = flowName;
        this.processorPaths = new ArrayList<>();
    }

    @Override
    public void addInvokedMessageProcessor(String processorPath)
    {
        this.processorPaths.add(processorPath);
    }

    @Override
    public String toString()
    {
        if (processorPaths.isEmpty())
        {
            return String.format("%s", flowName);
        }
        else
        {
            return String.format("%s(%s)", flowName, processorPaths.get(processorPaths.size() - 1));
        }
    }

    @Override
    public DefaultFlowStackElement clone()
    {
        DefaultFlowStackElement defaultFlowStackElement = new DefaultFlowStackElement(flowName);
        defaultFlowStackElement.processorPaths.addAll(this.processorPaths);
        return defaultFlowStackElement;
    }
}
