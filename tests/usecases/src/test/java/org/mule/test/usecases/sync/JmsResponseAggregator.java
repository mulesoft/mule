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
package org.mule.test.usecases.sync;

import org.mule.routing.inbound.EventGroup;
import org.mule.routing.response.ResponseCorrelationAggregator;
import org.mule.umo.UMOEvent;
import org.mule.umo.UMOMessage;
import org.mule.umo.routing.RoutingException;

/**
 * @author <a href="mailto:gnt@codehaus.org">Guillaume Nodet</a>
 * @version $Revision$
 */
public class JmsResponseAggregator extends ResponseCorrelationAggregator {

	/* (non-Javadoc)
	 * @see org.mule.routing.response.AbstractResponseAggregator#aggregateEvents(org.mule.routing.inbound.EventGroup)
	 */
	protected UMOMessage aggregateEvents(EventGroup events) throws RoutingException {
		UMOEvent event = (UMOEvent) events.getEvents().get(0);
		return event.getMessage();
	}

}
