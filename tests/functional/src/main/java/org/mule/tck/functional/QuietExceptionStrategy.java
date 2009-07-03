/*
 * $Id$
 * --------------------------------------------------------------------------------------
 * Copyright (c) MuleSource, Inc.  All rights reserved.  http://www.mulesource.com
 *
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.tck.functional;

import org.mule.AbstractExceptionListener;
import org.mule.api.MuleMessage;
import org.mule.api.endpoint.ImmutableEndpoint;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

/**
 * Restrict exceptions to debug log messages
 */
public class QuietExceptionStrategy extends AbstractExceptionListener
{

    protected transient Log logger = LogFactory.getLog(getClass());

    public void handleMessagingException(MuleMessage message, Throwable e)
    {
        logger.debug("Ignoring", e);
    }

    public void handleRoutingException(MuleMessage message, ImmutableEndpoint endpoint, Throwable e)
    {
        logger.debug("Ignoring", e);
    }

    public void handleLifecycleException(Object component, Throwable e)
    {
        logger.debug("Ignoring", e);
    }

    public void handleStandardException(Throwable e)
    {
        logger.debug("Ignoring", e);
    }

    @Override
    protected void logException(Throwable t)
    {
        logger.debug("Ignoring", t);
    }

}
