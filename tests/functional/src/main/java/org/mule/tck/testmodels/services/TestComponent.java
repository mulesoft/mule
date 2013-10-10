/*
 * (c) 2003-2014 MuleSoft, Inc. This software is protected under international copyright
 * law. All use of this software is subject to MuleSoft's Master Subscription Agreement
 * (or other master license agreement) separately entered into in writing between you and
 * MuleSoft. If such an agreement is not in place, you may not use the software.
 */
package org.mule.tck.testmodels.services;

import java.util.concurrent.atomic.AtomicInteger;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

public class TestComponent implements ITestComponent
{
    public static final String EXCEPTION_MESSAGE = "Test Service fired an Exception";

    protected static final Log logger = LogFactory.getLog(TestComponent.class);

    protected AtomicInteger count = new AtomicInteger(0);

    public String receive(String message) throws Exception
    {
        logger.info("Received: " + message + " number: " + inc() + " in thread: "
                    + Thread.currentThread().getName());
        return "Received: " + message;
    }
    
    public String receiveBytes(byte[] message) throws Exception
    {
        return receive(new String(message)); 
    }

    public String throwsException(String message) throws Exception
    {
        throw new TestComponentException(EXCEPTION_MESSAGE);
    }

    protected int inc()
    {
        return count.incrementAndGet();
    }

}
