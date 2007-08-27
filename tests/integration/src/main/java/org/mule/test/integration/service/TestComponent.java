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

import javax.jms.TextMessage;

import edu.emory.mathcs.backport.java.util.concurrent.atomic.AtomicInteger;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

public class TestComponent implements ITestComponent
{
    public static final String EXCEPTION_MESSAGE = "Test Component fired an Exception";

    private static final Log logger = LogFactory.getLog(TestComponent.class);

    private AtomicInteger count = new AtomicInteger(0);

    public String receive(String message) throws Exception
    {
        logger.info("Received: " + message + " number: " + inc() + " in thread: "
                    + Thread.currentThread().getName());
        return "Received: " + message;
    }

    public String throwsException(String message) throws Exception
    {
        throw new TestComponentException(EXCEPTION_MESSAGE);
    }

    public String receiveJms(TextMessage message) throws Exception
    {
        logger.info("Received: " + message.getText() + " Number: " + inc() + " in thread: "
                    + Thread.currentThread().getName());
        return "Received: " + message.getText();
    }

    protected int inc()
    {
        return count.incrementAndGet();
    }

}
