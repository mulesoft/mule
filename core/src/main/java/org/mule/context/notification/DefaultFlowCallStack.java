/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.context.notification;

import org.mule.api.context.notification.FlowCallStack;
import org.mule.api.context.notification.FlowStackElement;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Stack;

/**
 * Keeps context information about the executing flows and its callers
 * in order to provide augmented troubleshooting information for an application developer.
 */
public class DefaultFlowCallStack implements FlowCallStack
{
    private static final long serialVersionUID = 1299422751863396017L;

    private Stack<FlowStackElement> innerStack = new Stack<>();
    private List<String> executedProcessors = Collections.synchronizedList(new ArrayList<String>());

    public DefaultFlowCallStack()
    {
    }

    public DefaultFlowCallStack(FlowCallStack flowCallStack)
    {
        if (flowCallStack != null && flowCallStack instanceof DefaultFlowCallStack)
        {
            Collection<FlowStackElement> elementsCopy = flowCallStack.getElements();
            for (FlowStackElement flowStackElement : elementsCopy)
            {
                innerStack.push(((DefaultFlowStackElement) flowStackElement).clone());
            }
            // We want parallel paths of the same flows to contribute to this list and be available at the end, so we copy only the reference.
            executedProcessors = ((DefaultFlowCallStack) flowCallStack).executedProcessors;
        }
    }

    public void push(DefaultFlowStackElement flowStackElement)
    {
        innerStack.push(flowStackElement);
    }

    public void addInvokedMessageProcessor(String processorPath)
    {
        executedProcessors.add(processorPath);
        ((DefaultFlowStackElement) this.peek()).addInvokedMessageProcessor(processorPath);
    }

    public FlowStackElement pop()
    {
        return innerStack.pop();
    }

    @Override
    public FlowStackElement peek()
    {
        return innerStack.peek();
    }

    @Override
    public List<FlowStackElement> getElements()
    {
        List<FlowStackElement> ret = new ArrayList<>();
        for (FlowStackElement flowStackElement : innerStack)
        {
            ret.add(((DefaultFlowStackElement) flowStackElement).clone());
        }
        return ret;
    }

    @Override
    public List<String> getExecutedProcessors()
    {
        return new ArrayList<>(executedProcessors);
    }

    @Override
    public synchronized String toString()
    {
        StringBuilder ret = new StringBuilder();
        for (int i = innerStack.size() - 1; i >= 0; --i)
        {
            ret.append("at ").append(innerStack.get(i).toString());
            if (i != 0)
            {
                ret.append(System.lineSeparator());
            }
        }
        return ret.toString();
    }
}
