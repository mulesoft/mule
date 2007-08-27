/*
 * $Id$
 * --------------------------------------------------------------------------------------
 * Copyright (c) MuleSource, Inc.  All rights reserved.  http://www.mulesource.com
 *
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.test.integration.service;

import org.mule.impl.RequestContext;
import org.mule.util.StringMessageUtils;

import javax.jms.TextMessage;

import edu.emory.mathcs.backport.java.util.concurrent.atomic.AtomicInteger;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

public class TestReceiver
{
    private static final Log logger = LogFactory.getLog(TestComponent.class);

    private AtomicInteger count = new AtomicInteger(0);

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

    public String receive(TextMessage message) throws Exception
    {
        if (logger.isDebugEnabled())
        {
            logger.debug("Received: " + message.getText() + " Number: " + inc() + " in thread: "
                         + Thread.currentThread().getName());
            logger.debug("Message ID is: " + message.getJMSMessageID());
        }

        return "Received: " + message.getText();
    }

    protected int inc()
    {
        return count.incrementAndGet();
    }

}
