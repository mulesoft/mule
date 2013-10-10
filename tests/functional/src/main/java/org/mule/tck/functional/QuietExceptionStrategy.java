/*
 * (c) 2003-2014 MuleSoft, Inc. This software is protected under international copyright
 * law. All use of this software is subject to MuleSoft's Master Subscription Agreement
 * (or other master license agreement) separately entered into in writing between you and
 * MuleSoft. If such an agreement is not in place, you may not use the software.
 */
package org.mule.tck.functional;

import org.mule.api.MuleEvent;
import org.mule.exception.AbstractMessagingExceptionStrategy;

/**
 * Restrict exceptions to debug log messages
 */
public class QuietExceptionStrategy extends AbstractMessagingExceptionStrategy
{
    public QuietExceptionStrategy()
    {
        super(null);
    }

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

    public boolean isRedeliver()
    {
        return false;
    }
}
