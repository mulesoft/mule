/*
 * $Header$
 * $Revision$
 * $Date$
 * ------------------------------------------------------------------------------------------------------
 *
 * Copyright (c) Cubis Limited. All rights reserved.
 * http://www.cubis.co.uk
 *
 * The software in this package is published under the terms of the BSD
 * style license a copy of which has been included with this distribution in
 * the LICENSE.txt file.
 */
package org.mule.test.usecases.service;

import org.mule.routing.inbound.EventGroup;
import org.mule.routing.response.ResponseCorrelationAggregator;
import org.mule.umo.UMOEvent;
import org.mule.umo.UMOMessage;
import org.mule.umo.routing.RoutingException;
import org.mule.util.StringMessageHelper;

/**
 * @author <a href="mailto:ross.mason@cubis.co.uk">Ross Mason</a>
 * @version $Revision$
 */
public class DummyResponseAggregator extends ResponseCorrelationAggregator
{

    protected UMOMessage aggregateEvents(EventGroup events) throws RoutingException
    {
        logger.info(StringMessageHelper.getBoilerPlate("Response Agregator aggregating: " + events));
        return ((UMOEvent)events.getEvents().get(0)).getMessage();
    }
}
