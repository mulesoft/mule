/*
 * $Id$
 * --------------------------------------------------------------------------------------
 * Copyright (c) MuleSource, Inc.  All rights reserved.  http://www.mulesource.com
 *
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.routing.outbound;

import org.mule.DefaultMuleMessage;
import org.mule.api.MuleMessage;
import org.mule.api.MuleSession;
import org.mule.api.endpoint.ImmutableEndpoint;
import org.mule.api.routing.RoutingException;
import org.mule.tck.AbstractMuleTestCase;
import org.mule.tck.MuleTestUtils;

import com.mockobjects.dynamic.C;
import com.mockobjects.dynamic.Mock;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class EndpointSelectorTestCase extends AbstractMuleTestCase
{
    Mock session;
    ImmutableEndpoint dest1;
    ImmutableEndpoint dest2;
    ImmutableEndpoint dest3;
    EndpointSelector router;

    protected void doSetUp() throws Exception
    {
        super.doSetUp();
        session = MuleTestUtils.getMockSession();
        dest1 = getTestOutboundEndpoint("dest1");
        dest2 = getTestOutboundEndpoint("dest2");
        dest3 = getTestOutboundEndpoint("dest3");

        List endpoints = new ArrayList();
        endpoints.add(dest1);
        endpoints.add(dest2);
        endpoints.add(dest3);

        router = new EndpointSelector();
        router.setEndpoints(endpoints);
        router.setMuleContext(muleContext);
    }

    public void testSelectEndpointDefaultProperty() throws Exception
    {
        Map props = new HashMap();
        props.put("apple", "red");
        props.put(router.DEFAULT_SELECTOR_EXPRESSION, "dest3");
        props.put("banana", "yellow");
        MuleMessage message = new DefaultMuleMessage("test event", props);

        assertTrue(router.isMatch(message));
        session.expect("dispatchEvent", C.eq(message, dest3));
        router.route(message, (MuleSession) session.proxy(), false);
        session.verify();
    }

    public void testSelectEndpointCustomProperty() throws Exception
    {
        // The "wayOut" property will determine which endpoint the message gets sent
        // to.
        router.setExpression("wayOut");
        router.setEvaluator("header");

        Map props = new HashMap();
        props.put("apple", "red");
        props.put("wayOut", "dest2");
        props.put("banana", "yellow");
        MuleMessage message = new DefaultMuleMessage("test event", props);

        assertTrue(router.isMatch(message));
        session.expect("dispatchEvent", C.eq(message, dest2));
        router.route(message, (MuleSession) session.proxy(), false);
        session.verify();
    }

    public void testSelectEndpointNoMatch() throws Exception
    {
        Map props = new HashMap();
        props.put(router.DEFAULT_SELECTOR_EXPRESSION, "dest5");

        try
        {
            // this test used to fail at the router; it now fails earlier when the message is
            // constructed.  i don't think this is a problem.
            MuleMessage message = new DefaultMuleMessage("test event", props);
            router.route(message, (MuleSession) session.proxy(), false);
            fail("Router should have thrown an exception if endpoint was not found.");
        }
        catch (Exception e)
        {
            // expected
        }
    }

    public void testSelectEndpointNoPropertySet() throws Exception
    {
        MuleMessage message = new DefaultMuleMessage("test event");

        try
        {
            router.route(message, (MuleSession) session.proxy(), false);
            fail("Router should have thrown an exception if no selector property was set on the message.");
        }
        catch (RoutingException e)
        {
            // expected
        }
    }
}
