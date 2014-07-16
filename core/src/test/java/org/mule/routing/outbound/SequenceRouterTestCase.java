/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.routing.outbound;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import org.mule.DefaultMuleMessage;
import org.mule.VoidMuleEvent;
import org.mule.api.MuleEvent;
import org.mule.api.MuleMessage;
import org.mule.api.MuleMessageCollection;
import org.mule.api.MuleSession;
import org.mule.api.endpoint.OutboundEndpoint;
import org.mule.api.processor.MessageProcessor;
import org.mule.tck.MuleEventCheckAnswer;
import org.mule.tck.junit4.AbstractMuleContextTestCase;

import java.util.ArrayList;
import java.util.List;

import org.junit.Test;

public class SequenceRouterTestCase extends AbstractMuleContextTestCase
{
    private MuleSession session;
    private SequenceRouter router;
    private OutboundEndpoint mockEndpoint1;
    private OutboundEndpoint mockEndpoint2;

    @Override
    protected void doSetUp() throws Exception
    {
        session = mock(MuleSession.class);

        OutboundEndpoint endpoint1 = getTestOutboundEndpoint("Test1Provider",
                                            "test://Test1Provider?exchangePattern=request-response");
        mockEndpoint1 = RouterTestUtils.createMockEndpoint(endpoint1);

        OutboundEndpoint endpoint2 = getTestOutboundEndpoint("Test2Provider",
                                            "test://Test2Provider?exchangePattern=request-response");
        mockEndpoint2 = RouterTestUtils.createMockEndpoint(endpoint2);

        router = createObject(SequenceRouter.class);
        List<MessageProcessor> endpoints = new ArrayList<MessageProcessor>();
        endpoints.add(mockEndpoint1);
        endpoints.add(mockEndpoint2);
        router.setRoutes(endpoints);
    }

    @Test
    public void testSyncEndpointsOk() throws Exception
    {
        MuleMessage message = new DefaultMuleMessage(TEST_MESSAGE, muleContext);
        MuleEvent event = new OutboundRoutingTestEvent(message, null, muleContext);

        when(mockEndpoint1.process(any(MuleEvent.class))).thenAnswer(new MuleEventCheckAnswer(event));
        when(mockEndpoint2.process(any(MuleEvent.class))).thenAnswer(new MuleEventCheckAnswer(event));

        MuleEvent result = router.route(new OutboundRoutingTestEvent(message, session, muleContext));
        assertNotNull(result);

        MuleMessage resultMessage = result.getMessage();
        assertNotNull(resultMessage);
        assertTrue(resultMessage instanceof MuleMessageCollection);
        assertEquals(2, ((MuleMessageCollection)resultMessage).size());
    }

    @Test
    public void testSyncEndpointsWithFirstOneFailing() throws Exception
    {
        MuleMessage message = new DefaultMuleMessage(TEST_MESSAGE, muleContext);
        MuleEvent eventWithNullMessage = new OutboundRoutingTestEvent(null, null, muleContext);

        when(mockEndpoint1.process(any(MuleEvent.class))).thenAnswer(new MuleEventCheckAnswer(eventWithNullMessage));

        MuleEvent result = router.route(new OutboundRoutingTestEvent(message, session, muleContext));
        assertSame(VoidMuleEvent.getInstance(), result);
    }

    @Test
    public void testSyncEndpointsWithLastOneFailing() throws Exception
    {
        MuleMessage message = new DefaultMuleMessage(TEST_MESSAGE, muleContext);

        MuleEvent event = new OutboundRoutingTestEvent(message, null, muleContext);
        MuleEvent eventWithNullMessage = new OutboundRoutingTestEvent(null, null, muleContext);

        when(mockEndpoint1.process(any(MuleEvent.class))).thenAnswer(new MuleEventCheckAnswer(event));
        when(mockEndpoint2.process(any(MuleEvent.class))).thenAnswer(new MuleEventCheckAnswer(eventWithNullMessage));

        MuleEvent result = router.route(new OutboundRoutingTestEvent(message, session, muleContext));
        assertNotNull(result);
    }
}
