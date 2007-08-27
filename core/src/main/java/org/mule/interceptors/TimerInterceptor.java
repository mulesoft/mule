/*
 * $Id$
 * --------------------------------------------------------------------------------------
 * Copyright (c) MuleSource, Inc.  All rights reserved.  http://www.mulesource.com
 *
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.interceptors;

import org.mule.umo.Invocation;
import org.mule.umo.UMOException;
import org.mule.umo.UMOInterceptor;
import org.mule.umo.UMOMessage;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

/**
 * <code>TimerInterceptor</code> simply times and displays the time taken to process an
 * event.
 */
public class TimerInterceptor implements UMOInterceptor
{
    /**
     * logger used by this class
     */
    private static Log logger = LogFactory.getLog(TimerInterceptor.class);

    /*
     * (non-Javadoc)
     * 
     * @see org.mule.umo.UMOInterceptor#intercept(org.mule.umo.UMOEvent)
     */
    public UMOMessage intercept(Invocation invocation) throws UMOException
    {
        long startTime = System.currentTimeMillis();
        UMOMessage result = invocation.execute();
        long executionTime = System.currentTimeMillis() - startTime;
        logger.info(invocation.getDescriptor().getName() + " took " + executionTime + "ms to process event ["
                    + invocation.getEvent().getId() + "]");
        return result;
    }
}
