/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
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
