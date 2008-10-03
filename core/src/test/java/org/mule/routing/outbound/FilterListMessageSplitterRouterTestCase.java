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
import org.mule.api.endpoint.OutboundEndpoint;
import org.mule.routing.filters.PayloadTypeFilter;
import org.mule.tck.AbstractMuleTestCase;
import org.mule.tck.MuleTestUtils;
import org.mule.tck.testmodels.fruit.Apple;
import org.mule.tck.testmodels.fruit.Orange;

import com.mockobjects.constraint.Constraint;
import com.mockobjects.dynamic.C;
import com.mockobjects.dynamic.Mock;

import java.util.ArrayList;
import java.util.List;

public class FilterListMessageSplitterRouterTestCase extends AbstractMuleTestCase
{

    public void testMessageSplitterRouter() throws Exception
    {
        Mock session = MuleTestUtils.getMockSession();
        session.matchAndReturn("getService", getTestService());

        OutboundEndpoint endpoint1 = getTestOutboundEndpoint("Test1endpoint", "test://endpointUri.1", null, new PayloadTypeFilter(Apple.class), null);
        OutboundEndpoint endpoint2 = getTestOutboundEndpoint("Test2Endpoint", "test://endpointUri.2", null, new PayloadTypeFilter(Orange.class), null);
        OutboundEndpoint endpoint3 = getTestOutboundEndpoint("Test3Endpoint", "test://endpointUri.3");

        ListMessageSplitter router = new ListMessageSplitter();
        router.setFilter(new PayloadTypeFilter(List.class));
        router.addEndpoint(endpoint1);
        router.addEndpoint(endpoint2);
        router.addEndpoint(endpoint3);

        List payload = new ArrayList();
        payload.add(new Apple());
        payload.add(new Apple());
        payload.add(new Orange());
        payload.add(new String());
        MuleMessage message = new DefaultMuleMessage(payload);

        assertTrue(router.isMatch(message));
        session.expect("dispatchEvent", C.args(new PayloadConstraint(Apple.class), C.eq(endpoint1)));
        session.expect("dispatchEvent", C.args(new PayloadConstraint(Apple.class), C.eq(endpoint1)));
        session.expect("dispatchEvent", C.args(new PayloadConstraint(Orange.class), C.eq(endpoint2)));
        session.expect("dispatchEvent", C.args(new PayloadConstraint(String.class), C.eq(endpoint3)));
        router.route(message, (MuleSession) session.proxy());
        session.verify();

        endpoint1 = getTestOutboundEndpoint("Test1endpoint", "test://endpointUri.1?synchronous=true", null, new PayloadTypeFilter(Apple.class), null);
        endpoint2 = getTestOutboundEndpoint("Test2Endpoint", "test://endpointUri.2?synchronous=true", null, new PayloadTypeFilter(Orange.class), null);
        endpoint3 = getTestOutboundEndpoint("Test3Endpoint", "test://endpointUri.3?synchronous=true");
        router = new ListMessageSplitter();
        router.setFilter(new PayloadTypeFilter(List.class));
        router.addEndpoint(endpoint1);
        router.addEndpoint(endpoint2);
        router.addEndpoint(endpoint3);

        message = new DefaultMuleMessage(payload);

        session.expectAndReturn("sendEvent", C.args(new PayloadConstraint(Apple.class), C.eq(endpoint1)),
                message);
        session.expectAndReturn("sendEvent", C.args(new PayloadConstraint(Apple.class), C.eq(endpoint1)),
                message);
        session.expectAndReturn("sendEvent", C.args(new PayloadConstraint(Orange.class), C.eq(endpoint2)),
                message);
        session.expectAndReturn("sendEvent", C.args(new PayloadConstraint(String.class), C.eq(endpoint3)),
                message);
        MuleMessage result = router.route(message, (MuleSession) session.proxy());
        assertNotNull(result);
        assertTrue(result.getPayload() instanceof List);
        assertEquals(((List) result.getPayload()).size(), 4);
        session.verify();
    }

    private class PayloadConstraint implements Constraint
    {
        private Class type;

        public PayloadConstraint(Class type)
        {
            this.type = type;
        }

        public boolean eval(Object o)
        {
            return ((MuleMessage) o).getPayload().getClass().equals(type);
        }
    }

}
