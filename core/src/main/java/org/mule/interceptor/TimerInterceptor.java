/*
 * (c) 2003-2014 MuleSoft, Inc. This software is protected under international copyright
 * law. All use of this software is subject to MuleSoft's Master Subscription Agreement
 * (or other master license agreement) separately entered into in writing between you and
 * MuleSoft. If such an agreement is not in place, you may not use the software.
 */
package org.mule.interceptor;

import org.mule.api.MuleEvent;
import org.mule.api.MuleException;
import org.mule.api.interceptor.Interceptor;
import org.mule.processor.AbstractInterceptingMessageProcessor;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

/**
 * <code>TimerInterceptor</code> simply times and displays the time taken to process
 * an event.
 */
public class TimerInterceptor extends AbstractInterceptingMessageProcessor implements Interceptor
{
    /**
     * logger used by this class
     */
    private static Log logger = LogFactory.getLog(TimerInterceptor.class);

    public MuleEvent process(MuleEvent event) throws MuleException
    {
        long startTime = System.currentTimeMillis();

        MuleEvent resultEvent = processNext(event);

        if (logger.isInfoEnabled())
        {
            long executionTime = System.currentTimeMillis() - startTime;
            logger.info(resultEvent.getFlowConstruct().getName() + " took " + executionTime
                        + "ms to process event [" + resultEvent.getId() + "]");
        }

        return resultEvent;
    }
}
