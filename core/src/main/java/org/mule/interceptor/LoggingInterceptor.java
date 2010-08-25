/*
 * $Id$
 * --------------------------------------------------------------------------------------
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 *
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.interceptor;

import org.mule.api.MuleEvent;

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

    public MuleEvent before(MuleEvent event)
    {
        if (logger.isInfoEnabled())
        {
            logger.info("About to process event for " + event.getFlowConstruct().getName());
        }
        return event;

    }

    public MuleEvent after(MuleEvent event)
    {
        if (logger.isInfoEnabled())
        {
            logger.info("Processed event for " + event.getFlowConstruct().getName());
        }
        return event;
    }

}
