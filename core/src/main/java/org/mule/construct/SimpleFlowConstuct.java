/*
 * $Id$
 * --------------------------------------------------------------------------------------
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 *
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.construct;

import java.util.Collection;

import org.mule.api.MuleContext;
import org.mule.api.MuleException;
import org.mule.api.processor.MessageProcessor;
import org.mule.processor.NullMessageProcessor;
import org.mule.processor.builder.ChainMessageProcessorBuilder;

public class SimpleFlowConstuct extends AbstractFlowConstuct
{
    protected Collection<MessageProcessor> messageProcessors;

    public void setMessageProcessors(Collection<MessageProcessor> messageProcessors)
    {
        this.messageProcessors = messageProcessors;
    }

    public SimpleFlowConstuct(MuleContext muleContext, String name) throws MuleException
    {
        super(muleContext, name);
    }

    @Override
    protected void configureMessageProcessors(ChainMessageProcessorBuilder builder)
    {
        if (messageProcessors != null)
        {
            builder.chain(messageProcessors);
        }
        else
        {
            builder.chain(new NullMessageProcessor());
        }
    }
}
