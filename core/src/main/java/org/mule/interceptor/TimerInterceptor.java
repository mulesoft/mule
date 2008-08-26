/*
 * $Id$
 * --------------------------------------------------------------------------------------
 * Copyright (c) MuleSource, Inc.  All rights reserved.  http://www.mulesource.com
 *
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.interceptor;

import org.mule.api.MuleException;
import org.mule.api.MuleMessage;
import org.mule.api.interceptor.Interceptor;
import org.mule.api.interceptor.Invocation;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

/**
 * <code>TimerInterceptor</code> simply times and displays the time taken to process
 * an event.
 */
public class TimerInterceptor implements Interceptor
{
    /**
     * logger used by this class
     */
    private static Log logger = LogFactory.getLog(TimerInterceptor.class);

    public MuleMessage intercept(Invocation invocation) throws MuleException
    {
        long startTime = System.currentTimeMillis();
        
        MuleMessage result = invocation.invoke();
        
        if (logger.isInfoEnabled())
        {
            long executionTime = System.currentTimeMillis() - startTime;
            logger.info(invocation.getService().getName() + " took " + executionTime + "ms to process event ["
                        + invocation.getEvent().getId() + "]");
        }
        
        return result;
    }
}
