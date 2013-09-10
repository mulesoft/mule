/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.service.processor;

import org.mule.OptimizedRequestContext;
import org.mule.api.MuleEvent;
import org.mule.api.MuleException;
import org.mule.api.processor.MessageProcessor;
@Deprecated
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
