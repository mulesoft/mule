/*
 * $Id$
 * --------------------------------------------------------------------------------------
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 *
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.routing;

import org.mule.DefaultMuleEvent;
import org.mule.DefaultMuleMessage;
import org.mule.MessageExchangePattern;
import org.mule.api.MuleEvent;
import org.mule.api.MuleMessage;
import org.mule.api.MuleSession;
import org.mule.api.endpoint.ImmutableEndpoint;
import org.mule.api.service.Service;
import org.mule.tck.AbstractMuleTestCase;
import org.mule.tck.MuleTestUtils;
import org.mule.tck.testmodels.fruit.Apple;

import java.util.List;

public class SimpleCollectionAggregatorTestCase extends AbstractMuleTestCase
{

    public SimpleCollectionAggregatorTestCase()
    {
        setStartContext(true);
    }

    public void testMessageProcessor() throws Exception
    {
        MuleSession session = getTestSession(getTestService(), muleContext);
        Service testService = getTestService("test", Apple.class);
        assertNotNull(testService);

        SimpleCollectionAggregator router = new SimpleCollectionAggregator();
        router.setMuleContext(muleContext);
        router.setFlowConstruct(testService);
        router.initialise();
        
        MuleMessage message1 = new DefaultMuleMessage("test event A", muleContext);
        MuleMessage message2 = new DefaultMuleMessage("test event B", muleContext);
        MuleMessage message3 = new DefaultMuleMessage("test event C", muleContext);
        message1.setCorrelationId(message1.getUniqueId());
        message2.setCorrelationId(message1.getUniqueId());
        message3.setCorrelationId(message1.getUniqueId());
        message1.setCorrelationGroupSize(3);

        ImmutableEndpoint endpoint = MuleTestUtils.getTestOutboundEndpoint(MessageExchangePattern.ONE_WAY, muleContext);
        MuleEvent event1 = new DefaultMuleEvent(message1, endpoint, session);
        MuleEvent event2 = new DefaultMuleEvent(message2, endpoint, session);
        MuleEvent event3 = new DefaultMuleEvent(message3, endpoint, session);

        assertNull(router.process(event1));
        assertNull(router.process(event2));
        MuleEvent resultEvent = router.process(event3);
        assertNotNull(resultEvent);
        MuleMessage resultMessage = resultEvent.getMessage();
        assertNotNull(resultMessage);
        List<String> payload = (List<String>)resultMessage.getPayload();
        assertEquals(3, payload.size());
        assertEquals("test event A", payload.get(0));
        assertEquals("test event B", payload.get(1));
        assertEquals("test event C", payload.get(2));
    }

}
