/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.routing;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import org.mule.DefaultMuleEvent;
import org.mule.DefaultMuleMessage;
import org.mule.MessageExchangePattern;
import org.mule.api.MuleEvent;
import org.mule.api.MuleMessage;
import org.mule.api.MuleSession;
import org.mule.api.endpoint.InboundEndpoint;
import org.mule.api.service.Service;
import org.mule.tck.MuleTestUtils;
import org.mule.tck.junit4.AbstractMuleContextTestCase;
import org.mule.tck.testmodels.fruit.Apple;

import org.junit.Test;

public class MessageChunkAggregatorTestCase extends AbstractMuleContextTestCase
{

    public MessageChunkAggregatorTestCase()
    {
        setStartContext(true);
    }

    @Test
    public void testMessageProcessor() throws Exception
    {
        MuleSession session = getTestSession(null, muleContext);
        Service testService = getTestService("test", Apple.class);
        assertNotNull(testService);

        MessageChunkAggregator router = new MessageChunkAggregator();
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

        InboundEndpoint endpoint = MuleTestUtils.getTestInboundEndpoint(MessageExchangePattern.ONE_WAY,
            muleContext);
        MuleEvent event1 = new DefaultMuleEvent(message1, endpoint, getTestService(), session);
        MuleEvent event2 = new DefaultMuleEvent(message2, endpoint, getTestService(), session);
        MuleEvent event3 = new DefaultMuleEvent(message3, endpoint, getTestService(), session);

        assertNull(router.process(event1));
        assertNull(router.process(event2));
        MuleEvent resultEvent = router.process(event3);
        assertNotNull(resultEvent);
        MuleMessage resultMessage = resultEvent.getMessage();
        assertNotNull(resultMessage);
        String payload = resultMessage.getPayloadAsString();

        assertTrue(payload.contains("test event A"));
        assertTrue(payload.contains("test event B"));
        assertTrue(payload.contains("test event C"));
        assertTrue(payload.matches("test event [A,B,C]test event [A,B,C]test event [A,B,C]"));
    }
}
