/* 
* $Id$
* ------------------------------------------------------------------------------------------------------
* 
* Copyright (c) SymphonySoft Limited. All rights reserved.
* http://www.symphonysoft.com
* 
* The software in this package is published under the terms of the BSD
* style license a copy of which has been included with this distribution in
* the LICENSE.txt file. 
*
*/
package org.mule.routing.response;

import org.mule.routing.inbound.EventGroup;
import org.mule.umo.UMOEvent;
import org.mule.umo.UMOMessage;
import org.mule.umo.routing.RoutingException;

/**
 * Handles single event responses from a replyTo address.  If multiple responses will
 * be received for a single invocation the ResponseCorrelationaggregator should be used.
 *
 * @author <a href="mailto:ross.mason@symphonysoft.com">Ross Mason</a>
 * @version $Revision$
 */
public class SingleResponseRouter extends AbstractResponseAggregator
{
    /**
     * Determines if the event group is ready to be aggregated. if the group is
     * ready to be aggregated (this is entirely up to the application. it could
     * be determined by volume, last modified time or some oher criteria based
     * on the last event received)
     *
     * Because this is a Single response router it will return true if the event group size is 1.
     * It will raise a warning if the event Group size is greater than 1.
     *
     * @param events
     * @return true if the event group size is 1 or greater
     */
    protected boolean shouldAggregate(EventGroup events)
    {
        int size = events.expectedSize();
        if(size > 1) {
            logger.warn("Correlation Group Size is not 1. The SingleResponse Aggregator will only handle single replyTo events for a response router.  If there will be multiple events for a single request use the 'ResponseCorrelationAggregator'");
        }
        return true;
    }

    /**
     * This method is invoked if the shouldAggregate method is called and
     * returns true. Once this method returns an aggregated message the event
     * group is removed from the router
     *
     * Because this is a Single response router it returns the first event in the event group.
     * It will raise a warning if the event Group size is greater than 1.
     *
     * @param events the event group for this request
     * @return an aggregated message
     * @throws org.mule.umo.routing.RoutingException
     *          if the aggregation fails. in this scenario the
     *          whole event group is removed and passed to the exception
     *          handler for this componenet
     */
    protected UMOMessage aggregateEvents(EventGroup events) throws RoutingException {
        return ((UMOEvent)events.iterator().next()).getMessage();
    }

}
