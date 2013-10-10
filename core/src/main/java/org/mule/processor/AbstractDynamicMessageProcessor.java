/*
 * (c) 2003-2014 MuleSoft, Inc. This software is protected under international copyright
 * law. All use of this software is subject to MuleSoft's Master Subscription Agreement
 * (or other master license agreement) separately entered into in writing between you and
 * MuleSoft. If such an agreement is not in place, you may not use the software.
 */
package org.mule.processor;

import org.mule.api.MuleEvent;
import org.mule.api.MuleException;
import org.mule.api.processor.MessageProcessor;
import org.mule.util.ObjectUtils;

/**
 * Implementation of {@link MessageProcessor} that dynamically chooses and uses
 * another {@link MessageProcessor}
 */
public abstract class AbstractDynamicMessageProcessor implements MessageProcessor
{
    public MuleEvent process(MuleEvent event) throws MuleException
    {
        return resolveMessageProcessor(event).process(event);
    }

    /**
     * Determines which MessageProcessor should be used. Implementations may choose
     * to use a message property, configure this
     */
    protected abstract MessageProcessor resolveMessageProcessor(MuleEvent event) throws MuleException;

    @Override
    public String toString()
    {
        return ObjectUtils.toString(this);
    }
}
