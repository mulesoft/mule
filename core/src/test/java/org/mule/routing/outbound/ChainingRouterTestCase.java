/*
 * (c) 2003-2014 MuleSoft, Inc. This software is protected under international copyright
 * law. All use of this software is subject to MuleSoft's Master Subscription Agreement
 * (or other master license agreement) separately entered into in writing between you and
 * MuleSoft. If such an agreement is not in place, you may not use the software.
 */
package org.mule.routing.outbound;

import org.mule.DefaultMuleMessage;
import org.mule.api.MuleEvent;
import org.mule.api.MuleMessage;
import org.mule.api.MuleSession;
import org.mule.api.endpoint.ImmutableEndpoint;
import org.mule.api.endpoint.OutboundEndpoint;
import org.mule.api.processor.MessageProcessor;
import org.mule.routing.LoggingCatchAllStrategy;
import org.mule.routing.filters.PayloadTypeFilter;
import org.mule.tck.MuleTestUtils;
import org.mule.tck.junit4.AbstractMuleContextTestCase;
import org.mule.tck.testmodels.fruit.Apple;

import com.mockobjects.dynamic.Mock;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

public class ChainingRouterTestCase extends AbstractMuleContextTestCase
{
    public ChainingRouterTestCase()
    {
        setStartContext(true);
    }

    private Mock session;
    private ChainingRouter router;
    private List<OutboundEndpoint> endpoints;
    private Mock mockendpoint1;
    private Mock mockendpoint2;
    private Mock mockendpoint3;

    @Override
    protected void doSetUp() throws Exception
    {
        super.doSetUp();
        session = MuleTestUtils.getMockSession();
        router = new ChainingRouter();
        router.setMuleContext(muleContext);

        DefaultOutboundRouterCollection messageRouter = new DefaultOutboundRouterCollection();
        messageRouter.setCatchAllStrategy(new LoggingCatchAllStrategy());

        OutboundEndpoint endpoint1 = getTestOutboundEndpoint("Test1Provider", 
            "test://test?exchangePattern=request-response");
        assertNotNull(endpoint1);

        OutboundEndpoint endpoint2 = getTestOutboundEndpoint("Test2Provider", 
            "test://test?exchangePattern=request-response");
        assertNotNull(endpoint2);

        mockendpoint1 = RouterTestUtils.getMockEndpoint(endpoint1);
        mockendpoint2 = RouterTestUtils.getMockEndpoint(endpoint2);
        PayloadTypeFilter filter = new PayloadTypeFilter(String.class);
        router.setFilter(filter);
        endpoints = new ArrayList<OutboundEndpoint>();
        endpoints.add((OutboundEndpoint) mockendpoint1.proxy());
        endpoints.add((OutboundEndpoint) mockendpoint2.proxy());
        router.setRoutes(new ArrayList<MessageProcessor>(endpoints));

        assertEquals(filter, router.getFilter());
        session.matchAndReturn("getFlowConstruct", getTestService("TEST", Apple.class));
    }

    @Test
    public void testChainingOutboundRouterSynchronous() throws Exception
    {
        MuleMessage message = new DefaultMuleMessage("test event", muleContext);
        assertTrue(router.isMatch(message));

        MuleEvent event = new OutboundRoutingTestEvent(message, (MuleSession)session.proxy(), muleContext);

        mockendpoint1.expectAndReturn("process", RouterTestUtils.getArgListCheckerMuleEvent(), event);
        mockendpoint2.expectAndReturn("process", RouterTestUtils.getArgListCheckerMuleEvent(), event);
        final MuleEvent result = router.route(new OutboundRoutingTestEvent(message, (MuleSession)session.proxy(), muleContext));
        assertNotNull("This is a sync call, we need a result returned.", result);
        assertEquals(message, result.getMessage());
        mockendpoint1.verify();
        mockendpoint2.verify();
    }

    @Test
    public void testChainingOutboundRouterSynchronousWithTemplate() throws Exception
    {
        OutboundEndpoint endpoint3 = getTestOutboundEndpoint("Test3Provider", 
            "test://foo?[barValue]&exchangePattern=request-response");
        assertNotNull(endpoint3);
        mockendpoint3 = RouterTestUtils.getMockEndpoint(endpoint3);
        router.addRoute((OutboundEndpoint) mockendpoint3.proxy());

        Map<String, Object> m = new HashMap<String, Object>();
        m.put("barValue", "bar");
        MuleMessage message = new DefaultMuleMessage("test event", m, muleContext);
        assertTrue(router.isMatch(message));

        MuleEvent event = new OutboundRoutingTestEvent(message, (MuleSession)session.proxy(), muleContext);

        ImmutableEndpoint ep = (ImmutableEndpoint) router.getRoute(2, event);
        assertEquals("test://foo?bar&exchangePattern=request-response", ep.getEndpointURI().toString());

        mockendpoint1.expectAndReturn("process", RouterTestUtils.getArgListCheckerMuleEvent(), event);
        mockendpoint2.expectAndReturn("process", RouterTestUtils.getArgListCheckerMuleEvent(), event);
        mockendpoint3.expectAndReturn("process", RouterTestUtils.getArgListCheckerMuleEvent(), event);
        final MuleEvent result = router.route(new OutboundRoutingTestEvent(message, (MuleSession)session.proxy(), muleContext));
        assertNotNull("This is a sync call, we need a result returned.", result);
        assertEquals(message, result.getMessage());
        mockendpoint1.verify();
        mockendpoint2.verify();
        mockendpoint3.verify();
    }

    @Test
    public void testChainingOutboundRouterAsynchronous() throws Exception
    {
        OutboundEndpoint endpoint1 = getTestOutboundEndpoint("Test1Provider", "test://test");
        assertNotNull(endpoint1);

        OutboundEndpoint endpoint2 = getTestOutboundEndpoint("Test2Provider", "test://test");
        assertNotNull(endpoint2);

        Mock mep1 = RouterTestUtils.getMockEndpoint(endpoint1);
        Mock mep2 = RouterTestUtils.getMockEndpoint(endpoint2);
        endpoints.clear();
        endpoints.add((OutboundEndpoint) mep1.proxy());
        endpoints.add((OutboundEndpoint) mep2.proxy());
        router.setRoutes(new ArrayList<MessageProcessor>(endpoints));
        MuleMessage message = new DefaultMuleMessage("test event", muleContext);
        assertTrue(router.isMatch(message));

        message = new DefaultMuleMessage("test event", muleContext);

        MuleEvent event = new OutboundRoutingTestEvent(message, (MuleSession)session.proxy(), muleContext);

        mep1.expectAndReturn("process", RouterTestUtils.getArgListCheckerMuleEvent(), event);
        mep2.expectAndReturn("process", RouterTestUtils.getArgListCheckerMuleEvent(), null);
        final MuleEvent result = router.route(new OutboundRoutingTestEvent(message, (MuleSession)session.proxy(), muleContext));
        assertNull("Async call shouldn't return any result.", result);
        mep1.verify();
        mep2.verify();
    }

    /**
     * One of the targets returns null and breaks the chain
     */
    @Test
    public void testBrokenChain() throws Exception
    {
        MuleMessage message = new DefaultMuleMessage("test event", muleContext);
        OutboundEndpoint endpoint1 = endpoints.get(0);
        mockendpoint1.expectAndReturn("process", RouterTestUtils.getArgListCheckerMuleEvent(), null);
        MuleEvent result = router.route(new OutboundRoutingTestEvent(message, (MuleSession)session.proxy(), muleContext));
        mockendpoint1.verify();
        assertNull(result);
    }
}
