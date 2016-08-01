/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.functional.testmodels.services;

import org.mule.runtime.core.RequestContext;
import org.mule.runtime.core.util.StringMessageUtils;

import java.util.concurrent.atomic.AtomicInteger;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class TestReceiver
{
    protected static final Logger logger = LoggerFactory.getLogger(TestComponent.class);

    protected AtomicInteger count = new AtomicInteger(0);

    public String receive(String message) throws Exception
    {
        if (logger.isDebugEnabled())
        {
            logger.debug(StringMessageUtils.getBoilerPlate("Received: " + message + " Number: " + inc()
                                                           + " in thread: "
                                                           + Thread.currentThread().getName()));
            logger.debug("Message ID is: " + RequestContext.getEventContext().getMessage().getCorrelation().getId());
        }

        return "Received: " + message;
    }

    protected int inc()
    {
        return count.incrementAndGet();
    }

}
