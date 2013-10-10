/*
 * (c) 2003-2014 MuleSoft, Inc. This software is protected under international copyright
 * law. All use of this software is subject to MuleSoft's Master Subscription Agreement
 * (or other master license agreement) separately entered into in writing between you and
 * MuleSoft. If such an agreement is not in place, you may not use the software.
 */
package org.mule.routing;

import org.mule.DefaultMuleEvent;
import org.mule.api.MuleEvent;
import org.mule.api.routing.RoutingException;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

/**
 * <code>LoggingCatchAllStrategy</code> is a simple strategy that only logs any
 * events not caught by the router associated with this strategy. This should
 * <b>not</b> be used in production unless it is acceptable for events to be lost.
 */

public class LoggingCatchAllStrategy extends AbstractCatchAllStrategy
{
    private static final Log logger = LogFactory.getLog(DefaultMuleEvent.class);

    public MuleEvent doCatchMessage(MuleEvent event) throws RoutingException
    {
        logger.warn(String.format("Message was not dispatched. No routing path was defined for it. Message: %s", event.getMessage()));
        return event;
    }
}
