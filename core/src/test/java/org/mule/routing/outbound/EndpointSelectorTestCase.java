/*
 * $Id$
 * --------------------------------------------------------------------------------------
 * Copyright (c) MuleSource, Inc.  All rights reserved.  http://www.mulesource.com
 *
 * The software in this package is published under the terms of the MuleSource MPL
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.routing.outbound;

import org.mule.impl.MuleMessage;
import org.mule.tck.AbstractMuleTestCase;
import org.mule.tck.MuleTestUtils;
import org.mule.umo.UMOMessage;
import org.mule.umo.UMOSession;
import org.mule.umo.endpoint.UMOEndpoint;
import org.mule.umo.routing.CouldNotRouteOutboundMessageException;

import com.mockobjects.dynamic.C;
import com.mockobjects.dynamic.Mock;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Sample config:
 * 
 * <pre>
 * &lt;outbound-router&gt;
 *      &lt;router className=&quot;org.mule.routing.outbound.EndpointSelector&quot;&gt;
 *          &lt;endpoint name=&quot;dest1&quot; address=&quot;jms://queue1&quot; /&gt;
 *          &lt;endpoint name=&quot;dest2&quot; address=&quot;jms://queue2&quot; /&gt;
 *          &lt;endpoint name=&quot;dest3&quot; address=&quot;jms://queue3&quot; /&gt;
 *          &lt;properties&gt;
 *              &lt;property name=&quot;selector&quot; value=&quot;endpoint&quot; /&gt;
 *          &lt;/properties&gt;
 *      &lt;/router&gt;
 * &lt;/outbound-router&gt;
 * </pre>
 * 
 * </pre>
 */
public class EndpointSelectorTestCase extends AbstractMuleTestCase
{
    Mock session;
    UMOEndpoint dest1;
    UMOEndpoint dest2;
    UMOEndpoint dest3;
    EndpointSelector router;

    protected void doSetUp() throws Exception
    {
        super.doSetUp();
        session = MuleTestUtils.getMockSession();
        dest1 = getTestEndpoint("dest1", UMOEndpoint.ENDPOINT_TYPE_SENDER);
        dest2 = getTestEndpoint("dest2", UMOEndpoint.ENDPOINT_TYPE_SENDER);
        dest3 = getTestEndpoint("dest3", UMOEndpoint.ENDPOINT_TYPE_SENDER);

        List endpoints = new ArrayList();
        endpoints.add(dest1);
        endpoints.add(dest2);
        endpoints.add(dest3);

        router = new EndpointSelector();
        router.setEndpoints(endpoints);
    }

    public void testSelectEndpointDefaultProperty() throws Exception
    {
        Map props = new HashMap();
        props.put("apple", "red");
        props.put(router.getSelectorProperty(), "dest3");
        props.put("banana", "yellow");
        UMOMessage message = new MuleMessage("test event", props);

        assertTrue(router.isMatch(message));
        session.expect("dispatchEvent", C.eq(message, dest3));
        router.route(message, (UMOSession) session.proxy(), false);
        session.verify();
    }

    public void testSelectEndpointCustomProperty() throws Exception
    {
        // The "wayOut" property will determine which endpoint the message gets sent
        // to.
        router.setSelectorProperty("wayOut");

        Map props = new HashMap();
        props.put("apple", "red");
        props.put("wayOut", "dest2");
        props.put("banana", "yellow");
        UMOMessage message = new MuleMessage("test event", props);

        assertTrue(router.isMatch(message));
        session.expect("dispatchEvent", C.eq(message, dest2));
        router.route(message, (UMOSession) session.proxy(), false);
        session.verify();
    }

    public void testSelectEndpointNoMatch() throws Exception
    {
        Map props = new HashMap();
        props.put(router.getSelectorProperty(), "dest5");
        UMOMessage message = new MuleMessage("test event", props);

        try
        {
            router.route(message, (UMOSession) session.proxy(), false);
            fail("Router should have thrown an exception if endpoint was not found.");
        }
        catch (CouldNotRouteOutboundMessageException e)
        {
            // expected
        }
    }

    public void testSelectEndpointNoPropertySet() throws Exception
    {
        UMOMessage message = new MuleMessage("test event");

        try
        {
            router.route(message, (UMOSession) session.proxy(), false);
            fail("Router should have thrown an exception if no selector property was set on the message.");
        }
        catch (IllegalArgumentException e)
        {
            // expected
        }
    }
}
