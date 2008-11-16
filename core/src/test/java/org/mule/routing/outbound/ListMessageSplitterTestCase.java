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
import org.mule.api.MuleMessageCollection;
import org.mule.api.MuleSession;
import org.mule.api.endpoint.OutboundEndpoint;
import org.mule.api.service.Service;
import org.mule.routing.filters.PayloadTypeFilter;
import org.mule.tck.AbstractMuleTestCase;
import org.mule.tck.MuleTestUtils;
import org.mule.tck.testmodels.fruit.Apple;
import org.mule.tck.testmodels.fruit.Orange;
import org.mule.util.mock.PayloadClassConstraint;

import com.mockobjects.dynamic.C;
import com.mockobjects.dynamic.Mock;

import java.util.ArrayList;
import java.util.List;

public class ListMessageSplitterTestCase extends AbstractMuleTestCase
{
    public void testCorrelationGroupSizePropertySet() throws Exception
    {
        Service testService = getTestService("test", Apple.class);
        MuleSession session = getTestSession(testService, muleContext);

        OutboundEndpoint endpoint = getTestOutboundEndpoint("Test1Endpoint", "test://endpoint?synchronous=true");

        ListMessageSplitter router = new ListMessageSplitter();
        router.setFilter(null);
        router.addEndpoint(endpoint);

        List payload = new ArrayList();
        payload.add("one");
        payload.add("two");
        payload.add("three");
        payload.add("four");

        MuleMessage message = new DefaultMuleMessage(payload);

        MuleMessage result = router.route(message, session);
        assertNotNull(result);
        assertTrue(result instanceof MuleMessageCollection);
        assertEquals("There should be 4 results for 4 split messages.", 4, ((MuleMessageCollection) result).size());
    }

    public void testMessageSplitterRouter() throws Exception
    {
        Mock session = MuleTestUtils.getMockSession();

        OutboundEndpoint endpoint1 = getTestOutboundEndpoint("Test1endpoint", "test://endpointUri.1", null, new PayloadTypeFilter(Apple.class), null);
        OutboundEndpoint endpoint2 = getTestOutboundEndpoint("Test2Endpoint", "test://endpointUri.2", null, new PayloadTypeFilter(Orange.class), null);
        OutboundEndpoint endpoint3 = getTestOutboundEndpoint("Test3Endpoint", "test://endpointUri.3");

        OutboundEndpoint endpoint4 = getTestOutboundEndpoint("Test4endpoint", "test://endpointUri.4?synchronous=true", null, new PayloadTypeFilter(Apple.class), null);
        OutboundEndpoint endpoint5 = getTestOutboundEndpoint("Test5Endpoint", "test://endpointUri.5?synchronous=true", null, new PayloadTypeFilter(Orange.class), null);
        OutboundEndpoint endpoint6 = getTestOutboundEndpoint("Test6Endpoint", "test://endpointUri.6?synchronous=true");


        ListMessageSplitter asyncSplitter = new ListMessageSplitter();
        asyncSplitter.setDisableRoundRobin(true);
        asyncSplitter.setFilter(new PayloadTypeFilter(List.class));
        asyncSplitter.addEndpoint(endpoint1);
        asyncSplitter.addEndpoint(endpoint2);
        asyncSplitter.addEndpoint(endpoint3);

        ListMessageSplitter syncSplitter = new ListMessageSplitter();
        syncSplitter.setDisableRoundRobin(true);
        syncSplitter.setFilter(new PayloadTypeFilter(List.class));
        syncSplitter.addEndpoint(endpoint4);
        syncSplitter.addEndpoint(endpoint5);
        syncSplitter.addEndpoint(endpoint6);

        List payload = new ArrayList();
        payload.add(new Apple());
        payload.add(new Apple());
        payload.add(new Orange());
        payload.add(new String());
        MuleMessage message = new DefaultMuleMessage(payload);

        assertTrue(asyncSplitter.isMatch(message));
        session.expectAndReturn("getService", getTestService());
        session.expectAndReturn("getService", getTestService());
        session.expectAndReturn("getService", getTestService());
        session.expectAndReturn("getService", getTestService());
        session.expect("dispatchEvent", C.args(new PayloadClassConstraint(Apple.class), C.eq(endpoint1)));
        session.expect("dispatchEvent", C.args(new PayloadClassConstraint(Apple.class), C.eq(endpoint1)));
        session.expect("dispatchEvent", C.args(new PayloadClassConstraint(Orange.class), C.eq(endpoint2)));
        session.expect("dispatchEvent", C.args(new PayloadClassConstraint(String.class), C.eq(endpoint3)));
        asyncSplitter.route(message, (MuleSession) session.proxy());
        session.verify();

        message = new DefaultMuleMessage(payload);

        session.expectAndReturn("getService", getTestService());
        session.expectAndReturn("getService", getTestService());
        session.expectAndReturn("getService", getTestService());
        session.expectAndReturn("getService", getTestService());

        session.expectAndReturn("sendEvent", C.args(new PayloadClassConstraint(Apple.class), C.eq(endpoint4)),
                message);
        session.expectAndReturn("sendEvent", C.args(new PayloadClassConstraint(Apple.class), C.eq(endpoint4)),
                message);
        session.expectAndReturn("sendEvent", C.args(new PayloadClassConstraint(Orange.class), C.eq(endpoint5)),
                message);
        session.expectAndReturn("sendEvent", C.args(new PayloadClassConstraint(String.class), C.eq(endpoint6)),
                message);
        MuleMessage result = syncSplitter.route(message, (MuleSession) session.proxy());
        assertNotNull(result);
        assertTrue(result instanceof MuleMessageCollection);
        assertEquals(4, ((MuleMessageCollection) result).size());
        session.verify();
    }
}
