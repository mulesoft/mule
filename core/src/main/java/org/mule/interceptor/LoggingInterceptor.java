/*
 * (c) 2003-2014 MuleSoft, Inc. This software is protected under international copyright
 * law. All use of this software is subject to MuleSoft's Master Subscription Agreement
 * (or other master license agreement) separately entered into in writing between you and
 * MuleSoft. If such an agreement is not in place, you may not use the software.
 */
package org.mule.interceptor;

import org.mule.api.MuleEvent;
import org.mule.api.MuleException;
import org.mule.management.stats.ProcessingTime;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

/**
 * <code>LoggingInterceptor</code> is a simple interceptor that logs a message before
 * and after the event processing.
 */
public class LoggingInterceptor extends AbstractEnvelopeInterceptor
{
    /**
     * logger used by this class
     */
    private static Log logger = LogFactory.getLog(LoggingInterceptor.class);

    @Override
    public MuleEvent before(MuleEvent event)
    {
        if (logger.isDebugEnabled())
        {
            logger.debug("Started event processing for " + event.getFlowConstruct().getName());
        }
        return event;

    }

    @Override
    public MuleEvent after(MuleEvent event)
    {
        if (logger.isDebugEnabled() && event != null)
        {
            logger.debug("Finished event processing for " + event.getFlowConstruct().getName());
        }
        return event;
    }

    @Override
    public MuleEvent last(MuleEvent event, ProcessingTime time, long startTime, boolean exceptionWasThrown) throws MuleException
    {
        return event;
    }
}
