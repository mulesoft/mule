/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.context.notification;

import org.mule.api.context.notification.FlowCallStack;
import org.mule.api.context.notification.FlowStackElement;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Stack;

/**
 * Keeps context information about the executing flows and its callers
 * in order to provide augmented troubleshooting information for an application developer.
 */
public class DefaultFlowCallStack implements FlowCallStack, Serializable
{
    private static final long serialVersionUID = -4540003678773678613L;

    private Stack<FlowStackElement> innerStack = new Stack<>();

    public DefaultFlowCallStack()
    {
    }

    public DefaultFlowCallStack(FlowCallStack flowCallStack)
    {
        if(flowCallStack != null)
        {
            Collection<FlowStackElement> elementsCopy = flowCallStack.getElementsCopy();
            for (FlowStackElement flowStackElement : elementsCopy)
            {
                innerStack.push(flowStackElement.clone());
            }
        }
    }

    @Override
    public void push(FlowStackElement flowStackElement)
    {
        innerStack.push(flowStackElement);
    }

    @Override
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
    public Collection<FlowStackElement> getElementsCopy()
    {
        List<FlowStackElement> ret = new ArrayList<>();
        for (FlowStackElement flowStackElement : innerStack)
        {
            ret.add(flowStackElement.clone());
        }
        return ret;
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
