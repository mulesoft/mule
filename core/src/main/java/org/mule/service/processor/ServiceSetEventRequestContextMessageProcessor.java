/*
 * (c) 2003-2014 MuleSoft, Inc. This software is protected under international copyright
 * law. All use of this software is subject to MuleSoft's Master Subscription Agreement
 * (or other master license agreement) separately entered into in writing between you and
 * MuleSoft. If such an agreement is not in place, you may not use the software.
 */
package org.mule.service.processor;

import org.mule.OptimizedRequestContext;
import org.mule.api.MuleEvent;
import org.mule.api.MuleException;
import org.mule.api.processor.MessageProcessor;

public class ServiceSetEventRequestContextMessageProcessor implements MessageProcessor
{

    public MuleEvent process(MuleEvent event) throws MuleException
    {
        // DF: I've no idea why we only do this for sync
        if (event.getExchangePattern().hasResponse())
        {
            event = OptimizedRequestContext.unsafeSetEvent(event);
        }

        return event;
    }
}
