/*
 * $Id$
 * --------------------------------------------------------------------------------------
 * Copyright (c) MuleSource, Inc.  All rights reserved.  http://www.mulesource.com
 *
 * The software in this package is published under the terms of the MuleSource MPL
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.interceptors;

import org.mule.umo.Invocation;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

/**
 * <code>LoggingInterceptor</code> is a simple interceptor that logs a message before
 * and after the event processing.
 */
public class LoggingInterceptor extends EnvelopeInterceptor
{
    /**
     * logger used by this class
     */
    private static Log logger = LogFactory.getLog(LoggingInterceptor.class);

    /*
     * (non-Javadoc)
     * 
     * @see org.mule.interceptors.EnvelopeInterceptor#before(org.mule.umo.Invocation)
     */
    public void before(Invocation event)
    {
        logger.info("About to process event for " + event.getDescriptor().getName());

    }

    /*
     * (non-Javadoc)
     * 
     * @see org.mule.interceptors.EnvelopeInterceptor#after(org.mule.umo.Invocation)
     */
    public void after(Invocation event)
    {
        logger.info("Processed event for " + event.getDescriptor().getName());
    }

}
