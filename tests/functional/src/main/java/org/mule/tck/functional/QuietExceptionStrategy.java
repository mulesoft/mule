/*
 * $Id$
 * --------------------------------------------------------------------------------------
 * Copyright (c) MuleSource, Inc.  All rights reserved.  http://www.mulesource.com
 *
 * The software in this package is published under the terms of the MuleSource MPL
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.tck.functional;

import org.mule.impl.AbstractExceptionListener;
import org.mule.umo.UMOMessage;
import org.mule.umo.endpoint.UMOImmutableEndpoint;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

/**
 * Restrict exceptions to debug log messages
 */
public class QuietExceptionStrategy extends AbstractExceptionListener
{

    protected transient Log logger = LogFactory.getLog(getClass());

    public void handleMessagingException(UMOMessage message, Throwable e)
    {
        logger.debug("Ignoring", e);
    }

    public void handleRoutingException(UMOMessage message, UMOImmutableEndpoint endpoint, Throwable e)
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

}
