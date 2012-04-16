/*
 * $Id$
 * --------------------------------------------------------------------------------------
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 *
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
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
