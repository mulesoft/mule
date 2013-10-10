/*
 * (c) 2003-2014 MuleSoft, Inc. This software is protected under international copyright
 * law. All use of this software is subject to MuleSoft's Master Subscription Agreement
 * (or other master license agreement) separately entered into in writing between you and
 * MuleSoft. If such an agreement is not in place, you may not use the software.
 */
package org.mule.routing.outbound;

import org.mule.DefaultMuleMessage;
import org.mule.api.MuleMessage;
import org.mule.api.MuleSession;
import org.mule.api.endpoint.OutboundEndpoint;
import org.mule.api.processor.MessageProcessor;
import org.mule.api.routing.RoutingException;
import org.mule.tck.MuleTestUtils;
import org.mule.tck.junit4.AbstractMuleContextTestCase;

import com.mockobjects.dynamic.Mock;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.junit.Test;

import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

public class EndpointSelectorTestCase extends AbstractMuleContextTestCase
{
    public EndpointSelectorTestCase()
    {
        setStartContext(true);
    }

    Mock session;
    OutboundEndpoint dest1;
    OutboundEndpoint dest2;
    OutboundEndpoint dest3;
    EndpointSelector router;
    Mock mockendpoint1;
    Mock mockendpoint2;
    Mock mockendpoint3;


    @Override
    protected void doSetUp() throws Exception
    {
        super.doSetUp();
        session = MuleTestUtils.getMockSession();
        session.matchAndReturn("getFlowConstruct", getTestService());
        dest1 = getTestOutboundEndpoint("dest1");
        dest2 = getTestOutboundEndpoint("dest2");
        dest3 = getTestOutboundEndpoint("dest3");

        mockendpoint1 = RouterTestUtils.getMockEndpoint(dest1);
        mockendpoint2 = RouterTestUtils.getMockEndpoint(dest2);
        mockendpoint3 = RouterTestUtils.getMockEndpoint(dest3);

        List<MessageProcessor> endpoints = new ArrayList<MessageProcessor>();
        endpoints.add((OutboundEndpoint) mockendpoint1.proxy());
        endpoints.add((OutboundEndpoint) mockendpoint2.proxy());
        endpoints.add((OutboundEndpoint) mockendpoint3.proxy());

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
        mockendpoint3.expect("process", RouterTestUtils.getArgListCheckerMuleEvent());
        router.route(new OutboundRoutingTestEvent(message, (MuleSession) session.proxy(), muleContext));
        mockendpoint3.verify();
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
        mockendpoint2.expect("process", RouterTestUtils.getArgListCheckerMuleEvent());
        router.route(new OutboundRoutingTestEvent(message, (MuleSession) session.proxy(), muleContext));
        mockendpoint2.verify();
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
            router.route(new OutboundRoutingTestEvent(message, (MuleSession) session.proxy(), muleContext));
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
        mockendpoint3.expect("process", RouterTestUtils.getArgListCheckerMuleEvent());
        router.route(new OutboundRoutingTestEvent(message, (MuleSession) session.proxy(), muleContext));
        mockendpoint3.verify();
    }

    @Test
    public void testSelectEndpointNoPropertySet() throws Exception
    {
        MuleMessage message = new DefaultMuleMessage("test event", muleContext);

        try
        {
            router.route(new OutboundRoutingTestEvent(message, (MuleSession) session.proxy(), muleContext));
            fail("Router should have thrown an exception if no selector property was set on the message.");
        }
        catch (RoutingException e)
        {
            // expected
        }
    }
}
