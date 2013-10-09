/*
 * (c) 2003-2014 MuleSoft, Inc. This software is protected under international copyright
 * law. All use of this software is subject to MuleSoft's Master Subscription Agreement
 * (or other master license agreement) separately entered into in writing between you and
 * MuleSoft. If such an agreement is not in place, you may not use the software.
 */
package org.mule.transport.jms.redelivery;


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


