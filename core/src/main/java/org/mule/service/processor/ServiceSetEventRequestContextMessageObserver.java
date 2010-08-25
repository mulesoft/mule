/*
 * $Id$
 * --------------------------------------------------------------------------------------
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 *
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.service.processor;

import org.mule.OptimizedRequestContext;
import org.mule.api.MuleEvent;
import org.mule.processor.AbstractMessageObserver;

public class ServiceSetEventRequestContextMessageObserver extends AbstractMessageObserver
{
    @Override
    public void observe(MuleEvent event)
    {
        // DF: I've no idea why we only do this for sync
        if (event.getEndpoint().getExchangePattern().hasResponse())
        {
            event = OptimizedRequestContext.unsafeSetEvent(event);
        }
    }
}
