/*
 * $Header$
 * $Revision$
 * $Date$
 * ------------------------------------------------------------------------------------------------------
 *
 * Copyright (c) SymphonySoft Limited. All rights reserved.
 * http://www.symphonysoft.com
 *
 * The software in this package is published under the terms of the BSD
 * style license a copy of which has been included with this distribution in
 * the LICENSE.txt file.
 */
package org.mule.test.routing.inbound;

import org.mule.impl.MuleEvent;
import org.mule.impl.MuleMessage;
import org.mule.routing.LoggingCatchAllStrategy;
import org.mule.routing.inbound.AbstractEventAggregator;
import org.mule.routing.inbound.EventGroup;
import org.mule.routing.inbound.InboundMessageRouter;
import org.mule.tck.AbstractMuleTestCase;
import org.mule.tck.testmodels.fruit.Apple;
import org.mule.umo.UMOComponent;
import org.mule.umo.UMOEvent;
import org.mule.umo.UMOException;
import org.mule.umo.UMOMessage;
import org.mule.umo.UMOSession;
import org.mule.umo.endpoint.UMOEndpoint;
import org.mule.umo.routing.RoutingException;
import org.mule.umo.routing.UMOInboundMessageRouter;

import java.util.Iterator;

/**
 * @author <a href="mailto:ross.mason@symphonysoft.com">Ross Mason</a>
 * @version $Revision$
 */

public class EventAggregatorTestCase extends AbstractMuleTestCase
{
    public void testMessageAggregator() throws Exception
    {
        UMOComponent testComponent = getTestComponent(getTestDescriptor("test", Apple.class.getName()));
        UMOSession session = getTestSession(testComponent);

        UMOInboundMessageRouter messageRouter = new InboundMessageRouter();
        SimpleEventAggregator router = new SimpleEventAggregator(3);
        messageRouter.addRouter(router);
        messageRouter.setCatchAllStrategy(new LoggingCatchAllStrategy());

        UMOMessage message1 = new MuleMessage("test event A");
        UMOMessage message2 = new MuleMessage("test event B");
        UMOMessage message3 = new MuleMessage("test event C");

        UMOEndpoint endpoint = getTestEndpoint("Test1Provider", UMOEndpoint.ENDPOINT_TYPE_SENDER);
        UMOEvent event1 = new MuleEvent(message1, endpoint, session, false);
        UMOEvent event2 = new MuleEvent(message2, endpoint, session, false);
        UMOEvent event3 = new MuleEvent(message3, endpoint, session, false);
        assertTrue(router.isMatch(event1));
        assertTrue(router.isMatch(event2));
        assertTrue(router.isMatch(event3));

        assertNull(router.process(event1));
        assertNull(router.process(event2));

        UMOEvent[] results = router.process(event3);
        assertNotNull(results);
        assertEquals(1, results.length);
        assertEquals("test event A test event B test event C ", results[0].getMessageAsString());
    }

    public static class SimpleEventAggregator extends AbstractEventAggregator
    {
        private int eventCount = 0;
        private int eventThreshold = 1;

        public SimpleEventAggregator(int eventThreshold)
        {
            this.eventThreshold = eventThreshold;
        }

        protected boolean shouldAggregate(EventGroup events)
        {
            eventCount++;
            if (eventCount == eventThreshold) {
                eventCount = 0;
                return true;
            }
            return false;
        }

        protected UMOMessage aggregateEvents(EventGroup events) throws RoutingException
        {
            StringBuffer newPayload = new StringBuffer();
            UMOEvent event = null;
            for (Iterator iterator = events.getEvents().iterator(); iterator.hasNext();) {
                event = (UMOEvent) iterator.next();
                try {
                    newPayload.append(event.getMessageAsString()).append(" ");
                } catch (UMOException e) {
                    throw new RoutingException(event.getMessage(), event.getEndpoint(), e);
                }
            }
            return new MuleMessage(newPayload.toString(), event.getMessage());
        }
    }
}
