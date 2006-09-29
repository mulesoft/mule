/*
 * $Id$
 * --------------------------------------------------------------------------------------
 * Copyright (c) MuleSource, Inc.  All rights reserved.  http://www.mulesource.com
 *
 * The software in this package is published under the terms of the MuleSource MPL
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.test.integration.service;

import javax.jms.TextMessage;

/**
 * @author <a href="mailto:ross.mason@symphonysoft.com">Ross Mason</a>
 * @version $Revision$
 */
public class TestComponent implements ITestComponent
{
    private int count = 0;
    public static final String EXCEPTION_MESSAGE = "Test Component fired an Exception";

    public String receive(String message) throws Exception
    {
        System.out.println("Received: " + message + " Number: " + inc() + " in thread: "
                + Thread.currentThread().getName());
        return "Received: " + message;
    }
    
    public String throwsException(String message) throws Exception
    {
        throw new TestComponentException(EXCEPTION_MESSAGE);
    }

    public String receiveJms(TextMessage message) throws Exception
    {
        System.out.println("Received: " + message.getText() + " Number: " + inc() + " in thread: "
                + Thread.currentThread().getName());
        return "Received: " + message.getText();
    }

    protected int inc()
    {
        count++;
        return count;
    }
}
