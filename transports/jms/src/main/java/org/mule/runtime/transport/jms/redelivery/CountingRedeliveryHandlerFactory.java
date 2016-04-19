/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.transport.jms.redelivery;


import java.util.concurrent.atomic.AtomicReference;

/**
 * @see CountingRedeliveryHandler
 */
public class CountingRedeliveryHandlerFactory implements RedeliveryHandlerFactory
{

    protected AtomicReference<RedeliveryHandler> handler = new AtomicReference<RedeliveryHandler>(null);

    public RedeliveryHandler create()
    {
        RedeliveryHandler result;

        // initialize, accounting for concurrency
        if (handler.get() == null)
        {
            final CountingRedeliveryHandler newInstance = new CountingRedeliveryHandler();
            boolean ok = handler.compareAndSet(null, newInstance);
            if (!ok)
            {
                // someone was faster to initialize it, use this ref instead
                result = handler.get();
            }
            else
            {
                result = newInstance;
            }
        }
        else
        {
            // just re-use existing stateful handler
            result = handler.get();
        }

        return result;
    }

}


