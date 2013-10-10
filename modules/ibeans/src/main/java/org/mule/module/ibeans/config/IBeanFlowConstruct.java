/*
 * (c) 2003-2014 MuleSoft, Inc. This software is protected under international copyright
 * law. All use of this software is subject to MuleSoft's Master Subscription Agreement
 * (or other master license agreement) separately entered into in writing between you and
 * MuleSoft. If such an agreement is not in place, you may not use the software.
 */
package org.mule.module.ibeans.config;

import java.util.Collections;
import java.util.List;

import org.mule.api.MuleContext;
import org.mule.api.processor.MessageProcessor;
import org.mule.construct.Flow;

/**
 * This is an empty flow construct that is used to host an iBean as a component with
 * one or more component bindings. Each method annotated with
 * {@link org.ibeans.annotation.Call} or {@link org.ibeans.annotation.Template} has
 * an associated component binding associated with it.
 * 
 * @see org.mule.module.ibeans.config.CallInterfaceBinding
 */
public class IBeanFlowConstruct extends Flow
{
    public IBeanFlowConstruct(String name, MuleContext muleContext)
    {
        super(name, muleContext);
        final List<MessageProcessor> messageProcessors = Collections.emptyList();
        setMessageProcessors(messageProcessors);
    }

    @Override
    public String getConstructType()
    {
        return "Ibeans-Flow";
    }
}
