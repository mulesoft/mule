/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.transport.soap.axis;

import org.mule.api.MuleContext;
import org.mule.api.MuleEvent;
import org.mule.exception.DefaultMessagingExceptionStrategy;

import java.util.ArrayList;
import java.util.List;

public class UnitTestExceptionStrategy extends DefaultMessagingExceptionStrategy
{
    /**
     * Record all exceptions that this ExceptionStrategy handles so Unit Test
     * can query them and make their assertions.
     */
    private List<Throwable> messagingExceptions = null;
    
    public UnitTestExceptionStrategy(MuleContext muleContext)
    {
        super(muleContext);
        messagingExceptions = new ArrayList<Throwable>();
    }
    
    @Override
    protected void logFatal(MuleEvent event, Throwable t)
    {
        logger.debug("logFatal", t);
    }

    @Override
    protected void doLogException(Throwable t)
    {
        logger.debug("logException", t);
    }

    @Override
    protected void doHandleException(Exception e, MuleEvent event)
    {
        messagingExceptions.add(e);
        super.doHandleException(e, event);
    }
    
    public List<Throwable> getMessagingExceptions()
    {
        return messagingExceptions;
    }
}


