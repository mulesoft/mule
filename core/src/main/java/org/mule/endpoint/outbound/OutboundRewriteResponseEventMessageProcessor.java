/*
 * (c) 2003-2014 MuleSoft, Inc. This software is protected under international copyright
 * law. All use of this software is subject to MuleSoft's Master Subscription Agreement
 * (or other master license agreement) separately entered into in writing between you and
 * MuleSoft. If such an agreement is not in place, you may not use the software.
 */
package org.mule.endpoint.outbound;

import org.mule.DefaultMuleEvent;
import org.mule.VoidMuleEvent;
import org.mule.OptimizedRequestContext;
import org.mule.api.MuleEvent;
import org.mule.api.MuleException;
import org.mule.api.processor.MessageProcessor;
import org.mule.util.ObjectUtils;


public class OutboundRewriteResponseEventMessageProcessor implements MessageProcessor
{
    public MuleEvent process(MuleEvent event) throws MuleException
    {
        if (event != null && !VoidMuleEvent.getInstance().equals(event))
        { // See MULE-2692
            // RM* This actually performs the function of adding properties from the
            // request to the response message I think this could be done without the
            // performance hit. Or we could provide a way to set the request message
            // as
            // the OriginalAdapter on the message And provide access to the request
            // properties that way
            return new DefaultMuleEvent(OptimizedRequestContext.unsafeRewriteEvent(event.getMessage()), event);
        }
        else
        {
            return null;
        }
    }

    @Override
    public String toString()
    {
        return ObjectUtils.toString(this);
    }
}
