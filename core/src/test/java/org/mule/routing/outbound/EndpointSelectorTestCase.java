/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.routing.outbound;

import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import org.mule.DefaultMuleMessage;
import org.mule.api.MuleEvent;
import org.mule.api.MuleMessage;
import org.mule.api.MuleSession;
import org.mule.api.endpoint.OutboundEndpoint;
import org.mule.api.processor.MessageProcessor;
import org.mule.api.routing.RoutingException;
import org.mule.tck.MuleEventCheckAnswer;
import org.mule.tck.junit4.AbstractMuleContextTestCase;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.junit.Test;

public class EndpointSelectorTestCase extends AbstractMuleContextTestCase
{
    private MuleSession session;
    private OutboundEndpoint dest1;
    private OutboundEndpoint dest2;
    private OutboundEndpoint dest3;
    private EndpointSelector router;
    private OutboundEndpoint mockEndpoint1;
    private OutboundEndpoint mockEndpoint2;
    private OutboundEndpoint mockEndpoint3;

    public EndpointSelectorTestCase()
    {
        setStartContext(true);
    }

    @Override
    protected void doSetUp() throws Exception
    {
        super.doSetUp();
        session = mock(MuleSession.class);
        dest1 = getTestOutboundEndpoint("dest1");
        dest2 = getTestOutboundEndpoint("dest2");
        dest3 = getTestOutboundEndpoint("dest3");

        mockEndpoint1 = RouterTestUtils.createMockEndpoint(dest1);
        mockEndpoint2 = RouterTestUtils.createMockEndpoint(dest2);
        mockEndpoint3 = RouterTestUtils.createMockEndpoint(dest3);

        List<MessageProcessor> endpoints = new ArrayList<MessageProcessor>();
        endpoints.add(mockEndpoint1);
        endpoints.add(mockEndpoint2);
        endpoints.add(mockEndpoint3);

        router = new EndpointSelector();
        router.setRoutes(endpoints);
        router.setMuleContext(muleContext);
        router.initialise();
    }

    @Test
    public void testSelectEndpointDefaultProperty() throws Exception
    {
        Map<String, Object> props = new HashMap<String, Object>();
        props.put("apple", "red");
        props.put(EndpointSelector.DEFAULT_SELECTOR_EXPRESSION, "dest3");
        props.put("banana", "yellow");
        MuleMessage message = new DefaultMuleMessage("test event", props, muleContext);

        assertTrue(router.isMatch(message));

        when(mockEndpoint3.process(any(MuleEvent.class))).thenAnswer(new MuleEventCheckAnswer());
        router.route(new OutboundRoutingTestEvent(message, session, muleContext));
    }

    @Test
    public void testSelectEndpointCustomProperty() throws Exception
    {
        // The "wayOut" property will determine which endpoint the message gets sent to.
        router.setExpression("wayOut");
        router.setEvaluator("header");

        Map<String, Object> props = new HashMap<String, Object>();
        props.put("apple", "red");
        props.put("wayOut", "dest2");
        props.put("banana", "yellow");
        MuleMessage message = new DefaultMuleMessage("test event", props, muleContext);
        assertTrue(router.isMatch(message));

        when(mockEndpoint2.process(any(MuleEvent.class))).thenAnswer(new MuleEventCheckAnswer());
        router.route(new OutboundRoutingTestEvent(message, session, muleContext));
    }

    @Test
    public void testSelectEndpointNoMatch() throws Exception
    {
        Map<String, Object> props = new HashMap<String, Object>();
        props.put(EndpointSelector.DEFAULT_SELECTOR_EXPRESSION, "dest5");

        try
        {
            // this test used to fail at the router; it now fails earlier when the message is
            // constructed.  i don't think this is a problem.
            MuleMessage message = new DefaultMuleMessage("test event", props, muleContext);
            router.route(new OutboundRoutingTestEvent(message, session, muleContext));
            fail("Router should have thrown an exception if endpoint was not found.");
        }
        catch (Exception e)
        {
            // expected
        }
    }

    @Test
    public void testSelectEndpointNoMatchUseDefault() throws Exception
    {
        MuleMessage message = new DefaultMuleMessage("test event", muleContext);
        router.setDefaultEndpointName("dest3");
        assertTrue(router.isMatch(message));

        when(mockEndpoint3.process(any(MuleEvent.class))).thenAnswer(new MuleEventCheckAnswer());
        router.route(new OutboundRoutingTestEvent(message, session, muleContext));
    }

    @Test
    public void testSelectEndpointNoPropertySet() throws Exception
    {
        MuleMessage message = new DefaultMuleMessage("test event", muleContext);

        try
        {
            router.route(new OutboundRoutingTestEvent(message, session, muleContext));
            fail("Router should have thrown an exception if no selector property was set on the message.");
        }
        catch (RoutingException e)
        {
            // expected
        }
    }
}
