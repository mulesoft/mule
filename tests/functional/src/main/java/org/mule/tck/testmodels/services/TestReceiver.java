/*
 * (c) 2003-2014 MuleSoft, Inc. This software is protected under international copyright
 * law. All use of this software is subject to MuleSoft's Master Subscription Agreement
 * (or other master license agreement) separately entered into in writing between you and
 * MuleSoft. If such an agreement is not in place, you may not use the software.
 */
package org.mule.tck.testmodels.services;

import org.mule.RequestContext;
import org.mule.util.StringMessageUtils;

import java.util.concurrent.atomic.AtomicInteger;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

public class TestReceiver
{
    protected static final Log logger = LogFactory.getLog(TestComponent.class);

    protected AtomicInteger count = new AtomicInteger(0);

    public String receive(String message) throws Exception
    {
        if (logger.isDebugEnabled())
        {
            logger.debug(StringMessageUtils.getBoilerPlate("Received: " + message + " Number: " + inc()
                                                           + " in thread: "
                                                           + Thread.currentThread().getName()));
            logger.debug("Message ID is: " + RequestContext.getEventContext().getMessage().getCorrelationId());
        }

        return "Received: " + message;
    }

    protected int inc()
    {
        return count.incrementAndGet();
    }

}
