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
import org.mule.api.processor.MessageProcessorChain;
import org.mule.util.ObjectUtils;

import java.util.Collections;
import java.util.List;


public class NullMessageProcessor implements MessageProcessorChain
{
    public MuleEvent process(MuleEvent event) throws MuleException
    {
        return event;
    }

    @Override
    public String toString()
    {
        return ObjectUtils.toString(this);
    }

    public List<MessageProcessor> getMessageProcessors()
    {
        return Collections.emptyList();
    }

    public String getName()
    {
        return null;
    }

}
