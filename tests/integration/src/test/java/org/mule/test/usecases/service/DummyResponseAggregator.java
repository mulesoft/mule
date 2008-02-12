/*
 * $Id$
 * --------------------------------------------------------------------------------------
 * Copyright (c) MuleSource, Inc.  All rights reserved.  http://www.mulesource.com
 *
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.test.usecases.service;

import org.mule.api.MuleEvent;
import org.mule.api.MuleMessage;
import org.mule.api.routing.RoutingException;
import org.mule.routing.inbound.EventGroup;
import org.mule.routing.response.ResponseCorrelationAggregator;
import org.mule.util.StringMessageUtils;

public class DummyResponseAggregator extends ResponseCorrelationAggregator
{

    protected MuleMessage aggregateEvents(EventGroup events) throws RoutingException
    {
        logger.info(StringMessageUtils.getBoilerPlate("Response Agregator aggregating: " + events));
        return ((MuleEvent)events.iterator().next()).getMessage();
    }
}
