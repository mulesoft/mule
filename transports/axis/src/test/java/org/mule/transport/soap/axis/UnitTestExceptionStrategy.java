/*
 * (c) 2003-2014 MuleSoft, Inc. This software is protected under international copyright
 * law. All use of this software is subject to MuleSoft's Master Subscription Agreement
 * (or other master license agreement) separately entered into in writing between you and
 * MuleSoft. If such an agreement is not in place, you may not use the software.
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
    protected void logException(Throwable t)
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


