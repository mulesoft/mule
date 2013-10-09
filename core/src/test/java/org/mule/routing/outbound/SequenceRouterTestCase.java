/*
 * (c) 2003-2014 MuleSoft, Inc. This software is protected under international copyright
 * law. All use of this software is subject to MuleSoft's Master Subscription Agreement
 * (or other master license agreement) separately entered into in writing between you and
 * MuleSoft. If such an agreement is not in place, you may not use the software.
 */
/*
* $Id$
* --------------------------------------------------------------------------------------
* Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
*
* The software in this package is published under the terms of the CPAL v1.0
* license, a copy of which has been included with this distribution in the
* LICENSE.txt file.
*/

package org.mule.routing.outbound;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;

import org.mule.DefaultMuleMessage;
import org.mule.VoidMuleEvent;
import org.mule.api.MuleEvent;
import org.mule.api.MuleMessage;
import org.mule.api.MuleMessageCollection;
import org.mule.api.MuleSession;
import org.mule.api.endpoint.OutboundEndpoint;
import org.mule.api.processor.MessageProcessor;
import org.mule.tck.MuleTestUtils;
import org.mule.tck.junit4.AbstractMuleContextTestCase;

import com.mockobjects.dynamic.Mock;

import java.util.ArrayList;
import java.util.List;

import org.junit.Test;

public class SequenceRouterTestCase extends AbstractMuleContextTestCase
{

    private Mock session;
    private SequenceRouter router;
    private Mock mockEndpoint1;
    private Mock mockEndpoint2;

    @Override
    protected void doSetUp() throws Exception
    {
        session = MuleTestUtils.getMockSession();
        session.matchAndReturn("getFlowConstruct", getTestService());
        session.matchAndReturn("setFlowConstruct", RouterTestUtils.getArgListCheckerFlowConstruct(), null);

        OutboundEndpoint endpoint1 = getTestOutboundEndpoint("Test1Provider",
                                            "test://Test1Provider?exchangePattern=request-response");

        OutboundEndpoint endpoint2 = getTestOutboundEndpoint("Test2Provider",
                                            "test://Test2Provider?exchangePattern=request-response");
        mockEndpoint1 = RouterTestUtils.getMockEndpoint(endpoint1);
        mockEndpoint2 = RouterTestUtils.getMockEndpoint(endpoint2);

        router = createObject(SequenceRouter.class);
        List<MessageProcessor> endpoints = new ArrayList<MessageProcessor>();
        endpoints.add((OutboundEndpoint) mockEndpoint1.proxy());
        endpoints.add((OutboundEndpoint) mockEndpoint2.proxy());
        router.setRoutes(endpoints);
    }

    @Test
    public void testSyncEndpointsOk() throws Exception
    {
        MuleMessage message = new DefaultMuleMessage(TEST_MESSAGE, muleContext);
        MuleEvent event = new OutboundRoutingTestEvent(message, null, muleContext);

        mockEndpoint1.expectAndReturn("process", RouterTestUtils.getArgListCheckerMuleEvent(), event);
        mockEndpoint2.expectAndReturn("process", RouterTestUtils.getArgListCheckerMuleEvent(), event);

        MuleEvent result = router.route(new OutboundRoutingTestEvent(message, (MuleSession)session.proxy(), muleContext));

        assertNotNull(result);
        MuleMessage resultMessage = result.getMessage();
        assertNotNull(resultMessage);
        assertTrue(resultMessage instanceof MuleMessageCollection);
        assertEquals(2, ((MuleMessageCollection)resultMessage).size());
        mockEndpoint1.verify();
        mockEndpoint2.verify();
    }

    @Test
    public void testSyncEndpointsWithFirstOneFailing() throws Exception
    {
        MuleMessage message = new DefaultMuleMessage(TEST_MESSAGE, muleContext);
        MuleEvent eventWithNullMessage = new OutboundRoutingTestEvent(null, null, muleContext);

        mockEndpoint1.expectAndReturn("process", RouterTestUtils.getArgListCheckerMuleEvent(), eventWithNullMessage);

        MuleEvent result = router.route(new OutboundRoutingTestEvent(message, (MuleSession)session.proxy(), muleContext));

        assertSame(VoidMuleEvent.getInstance(), result);
    }

    @Test
    public void testSyncEndpointsWithLastOneFailing() throws Exception
    {
        MuleMessage message = new DefaultMuleMessage(TEST_MESSAGE, muleContext);

        MuleEvent event = new OutboundRoutingTestEvent(message, null, muleContext);
        MuleEvent eventWithNullMessage = new OutboundRoutingTestEvent(null, null, muleContext);

        mockEndpoint1.expectAndReturn("process", RouterTestUtils.getArgListCheckerMuleEvent(), event);
        mockEndpoint2.expectAndReturn("process", RouterTestUtils.getArgListCheckerMuleEvent(), eventWithNullMessage);

        MuleEvent result = router.route(new OutboundRoutingTestEvent(message, (MuleSession)session.proxy(), muleContext));

        assertNotNull(result);
        assertTrue(result instanceof MuleEvent);
    }
}
