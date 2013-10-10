/*
 * (c) 2003-2014 MuleSoft, Inc. This software is protected under international copyright
 * law. All use of this software is subject to MuleSoft's Master Subscription Agreement
 * (or other master license agreement) separately entered into in writing between you and
 * MuleSoft. If such an agreement is not in place, you may not use the software.
 */
package org.mule.tck;

import org.mule.api.MuleEvent;
import org.mule.api.MuleException;
import org.mule.api.processor.MessageProcessor;
import org.mule.api.source.MessageSource;
import org.mule.util.ObjectUtils;

public class TriggerableMessageSource implements MessageSource
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
