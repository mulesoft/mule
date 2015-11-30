/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.routing.outbound;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNotSame;
import static org.junit.Assert.assertTrue;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import org.mule.DefaultMuleMessage;
import org.mule.api.MuleEvent;
import org.mule.api.MuleMessage;
import org.mule.api.MuleSession;
import org.mule.api.endpoint.OutboundEndpoint;
import org.mule.api.processor.MessageProcessor;
import org.mule.api.transformer.Transformer;
import org.mule.api.transformer.TransformerException;
import org.mule.routing.filters.PayloadTypeFilter;
import org.mule.tck.MuleEventCheckAnswer;
import org.mule.tck.junit4.AbstractMuleContextTestCase;
import org.mule.transformer.AbstractTransformer;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.junit.Test;

public class FilteringOutboundRouterTestCase extends AbstractMuleContextTestCase
{
    public FilteringOutboundRouterTestCase()
    {
        setStartContext(true);
    }

    @Test
    public void testFilteringOutboundRouterAsync() throws Exception
    {
        OutboundEndpoint endpoint1 = getTestOutboundEndpoint("Test1Provider",
            "test://Test1Provider?exchangePattern=request-response");
        assertNotNull(endpoint1);
        OutboundEndpoint mockEndpoint = RouterTestUtils.createMockEndpoint(endpoint1);

        FilteringOutboundRouter router = new FilteringOutboundRouter();
        PayloadTypeFilter filter = new PayloadTypeFilter(String.class);
        router.setMuleContext(muleContext);
        router.setFilter(filter);
        List<MessageProcessor> endpoints = new ArrayList<MessageProcessor>();
        endpoints.add(mockEndpoint);
        router.setRoutes(endpoints);

        // Default is now true
        assertTrue(router.isUseTemplates());
        assertEquals(filter, router.getFilter());

        MuleMessage message = new DefaultMuleMessage("test event", muleContext);

        assertTrue(router.isMatch(message));

        when(mockEndpoint.process(any(MuleEvent.class))).thenAnswer(new MuleEventCheckAnswer());
        MuleSession session = mock(MuleSession.class);
        router.route(new OutboundRoutingTestEvent(message, session, muleContext));

        //Test with transform
        message = new DefaultMuleMessage(new Exception("test event"), muleContext);

        assertTrue(!router.isMatch(message));

        router.setTransformers(
              Arrays.<Transformer>asList(
                new AbstractTransformer()
                {
                    @Override
                    public Object doTransform(Object src, String outputEncoding) throws TransformerException
                    {
                        return ((Exception)src).getMessage();
                    }
                }
            )
        );

        assertTrue(router.isMatch(message));
    }

    @Test
    public void testFilteringOutboundRouterSync() throws Exception
    {
        OutboundEndpoint endpoint1 = getTestOutboundEndpoint("Test1Provider",
            "test://Test1Provider?exchangePattern=request-response");
        assertNotNull(endpoint1);
        OutboundEndpoint mockEndpoint = RouterTestUtils.createMockEndpoint(endpoint1);

        FilteringOutboundRouter router = new FilteringOutboundRouter();
        PayloadTypeFilter filter = new PayloadTypeFilter(String.class);
        router.setFilter(filter);
        List<OutboundEndpoint> endpoints = new ArrayList<OutboundEndpoint>();
        endpoints.add(mockEndpoint);
        router.setRoutes(new ArrayList<MessageProcessor>(endpoints));

        // Default is now true
        assertTrue(router.isUseTemplates());
        assertEquals(filter, router.getFilter());

        MuleMessage message = new DefaultMuleMessage("test event", muleContext);
        MuleEvent event = new OutboundRoutingTestEvent(message, null, muleContext);
        when(mockEndpoint.process(any(MuleEvent.class))).thenAnswer(new MuleEventCheckAnswer(event));

        MuleSession session = mock(MuleSession.class);
        MuleEvent result = router.route(new OutboundRoutingTestEvent(message, session, muleContext));
        assertNotNull(result);
        assertEquals(message, result.getMessage());
    }

    @Test
    public void testFilteringOutboundRouterWithTemplates() throws Exception
    {
        OutboundEndpoint endpoint1 = getTestOutboundEndpoint("Test1Provider", "test://foo?[barValue]");
        assertNotNull(endpoint1);

        FilteringOutboundRouter router = new FilteringOutboundRouter();
        router.setMuleContext(muleContext);
        PayloadTypeFilter filter = new PayloadTypeFilter(String.class);
        router.setFilter(filter);
        List<OutboundEndpoint> endpoints = new ArrayList<OutboundEndpoint>();
        endpoints.add(endpoint1);
        router.setRoutes(new ArrayList<MessageProcessor>(endpoints));

        assertTrue(router.isUseTemplates());
        assertEquals(filter, router.getFilter());

        Map<String, Object> m = new HashMap<String, Object>();
        m.put("barValue", "bar");
        MuleMessage message = new DefaultMuleMessage("test event", m, muleContext);
        MuleEvent event = new OutboundRoutingTestEvent(message, null, muleContext);

        assertTrue(router.isMatch(message));
        OutboundEndpoint ep = (OutboundEndpoint) router.getRoute(0, event);
        // MULE-2690: assert that templated targets are not mutated
        assertNotSame(endpoint1, ep);
        // assert that the returned endpoint has a resolved URI
        assertEquals("test://foo?bar", ep.getEndpointURI().toString());
    }
}
