/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.tck.functional;

import org.mule.api.MuleEvent;
import org.mule.exception.AbstractMessagingExceptionStrategy;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

/**
 * Restrict exceptions to debug log messages
 */
public class QuietExceptionStrategy extends AbstractMessagingExceptionStrategy
{
    protected transient Log logger = LogFactory.getLog(getClass());

    @Override
    protected void doHandleException(Exception e, MuleEvent event)
    {
        logger.debug("Ignoring", e);
    }

    @Override
    protected void logException(Throwable t)
    {
        logger.debug("Ignoring", t);
    }
}
