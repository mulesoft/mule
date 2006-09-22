/*
 * $Id: EndpointSelectorTestCase.java 2736 2006-08-20 12:42:23 +0000 (Sun, 20 Aug 2006) holger $
 * --------------------------------------------------------------------------------------
 * Copyright (c) MuleSource, Inc.  All rights reserved.  http://www.mulesource.com
 *
 * The software in this package is published under the terms of the MuleSource MPL
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.test.routing.outbound;

import com.mockobjects.dynamic.C;
import com.mockobjects.dynamic.Mock;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.mule.impl.MuleMessage;
import org.mule.routing.outbound.EndpointSelector;
import org.mule.tck.AbstractMuleTestCase;
import org.mule.tck.MuleTestUtils;
import org.mule.umo.UMOMessage;
import org.mule.umo.UMOSession;
import org.mule.umo.endpoint.UMOEndpoint;
import org.mule.umo.routing.CouldNotRouteOutboundMessageException;

/**
 * @author <a href="mailto:carlson@hotpop.com">Travis Carlson</a>
 *
 * Sample config:
 *
 *   <outbound-router>
 *       <router className="org.mule.routing.outbound.EndpointSelector">
 *           <endpoint name="dest1" address="jms://queue1" />
 *           <endpoint name="dest2" address="jms://queue2" />
 *           <endpoint name="dest3" address="jms://queue3" />
 *           <properties>
 *               <property name="selector" value="endpoint" />
 *           </properties>
 *       </router>
 *   </outbound-router>
 */
public class EndpointSelectorTestCase extends AbstractMuleTestCase {
    Mock session;
    UMOEndpoint dest1;
    UMOEndpoint dest2;
    UMOEndpoint dest3;
    EndpointSelector router;

    protected void doSetUp() throws Exception {
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

    public void testSelectEndpointDefaultProperty() throws Exception {
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

    public void testSelectEndpointCustomProperty() throws Exception {
        // The "wayOut" property will determine which endpoint the message gets sent to.
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

    public void testSelectEndpointNoMatch() throws Exception {
        Map props = new HashMap();
        props.put(router.getSelectorProperty(), "dest5");
        UMOMessage message = new MuleMessage("test event", props);

        try {
            router.route(message, (UMOSession) session.proxy(), false);
            fail("Router should have thrown an exception if endpoint was not found.");
        } catch (CouldNotRouteOutboundMessageException e) {
            // expected
        }
    }

    public void testSelectEndpointNoPropertySet() throws Exception {
        UMOMessage message = new MuleMessage("test event");

        try {
            router.route(message, (UMOSession) session.proxy(), false);
            fail("Router should have thrown an exception if no selector property was set on the message.");
        } catch (IllegalArgumentException e) {
            // expected
        }
    }
}
