/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.tck;

import org.mule.api.MuleEvent;
import org.mule.api.MuleException;
import org.mule.api.processor.MessageProcessor;
import org.mule.api.source.MessageSource;
import org.mule.api.source.NonBlockingMessageSource;
import org.mule.util.ObjectUtils;

public class TriggerableMessageSource implements NonBlockingMessageSource
{
    protected MessageProcessor listener;

    public TriggerableMessageSource()
    {
        // empty
    }

    public TriggerableMessageSource(MessageProcessor listener)
    {
        this.listener = listener;
    }

    public MuleEvent trigger(MuleEvent event) throws MuleException
    {
        return listener.process(event);
    }

    public void setListener(MessageProcessor listener)
    {
        this.listener = listener;
    }
    
    @Override
    public String toString()
    {
        return ObjectUtils.toString(this);
    }
}
