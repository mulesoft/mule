/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.module.ibeans.config;

import java.util.Collections;
import java.util.List;

import org.mule.api.MuleContext;
import org.mule.api.processor.MessageProcessor;
import org.mule.construct.SimpleFlowConstruct;

/**
 * This is an empty flow construct that is used to host an iBean as a component with
 * one or more component bindings. Each method annotated with
 * {@link org.ibeans.annotation.Call} or {@link org.ibeans.annotation.Template} has
 * an associated component binding associated with it.
 * 
 * @see org.mule.module.ibeans.config.CallInterfaceBinding
 */
public class IBeanFlowConstruct extends SimpleFlowConstruct
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
