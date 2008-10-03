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
import org.mule.tck.AbstractMuleTestCase;
import org.mule.tck.MuleTestUtils;

import com.mockobjects.dynamic.C;
import com.mockobjects.dynamic.Mock;

import java.util.ArrayList;
import java.util.List;
import java.util.StringTokenizer;

public class MessageSplitterRouterTestCase extends AbstractMuleTestCase
{
    public void testMessageSplitterRouter() throws Exception
    {
        Mock session = MuleTestUtils.getMockSession();
        session.matchAndReturn("getService", getTestService());

        //Async endpoints
        OutboundEndpoint endpoint1 = getTestOutboundEndpoint("Test1Endpoint", "test://endpointUri.1");
        OutboundEndpoint endpoint2 = getTestOutboundEndpoint("Test2Endpoint", "test://endpointUri.2");
        OutboundEndpoint endpoint3 = getTestOutboundEndpoint("Test3Endpoint", "test://endpointUri.3");

        //Sync endpoints
        OutboundEndpoint endpoint4 = getTestOutboundEndpoint("Test4Endpoint", "test://endpointUri.4?synchronous=true");
        OutboundEndpoint endpoint5 = getTestOutboundEndpoint("Test5Endpoint", "test://endpointUri.5?synchronous=true");
        OutboundEndpoint endpoint6 = getTestOutboundEndpoint("Test6Endpoint", "test://endpointUri.6?synchronous=true");

        // Dummy message splitter
        AbstractMessageSplitter router = new AbstractMessageSplitter()
        {
            protected SplitMessage getMessageParts(MuleMessage message, List endpoints)
            {
                int i = 0;
                SplitMessage splitMessage = new SplitMessage();
                for (StringTokenizer tokenizer = new StringTokenizer(message.getPayload().toString(), ","); tokenizer.hasMoreTokens(); i++)
                {
                    String s = tokenizer.nextToken();
                    splitMessage.addPart(s, (OutboundEndpoint) endpoints.get(i));
                }
                return splitMessage;
            }
        };

        List endpoints = new ArrayList();
        endpoints.add(endpoint1);
        endpoints.add(endpoint2);
        endpoints.add(endpoint3);
        router.setEndpoints(endpoints);

        MuleMessage message = new DefaultMuleMessage("test,mule,message");

        assertTrue(router.isMatch(message));
        session.expect("dispatchEvent", C.args(C.isA(MuleMessage.class), C.eq(endpoint1)));
        session.expect("dispatchEvent", C.args(C.isA(MuleMessage.class), C.eq(endpoint2)));
        session.expect("dispatchEvent", C.args(C.isA(MuleMessage.class), C.eq(endpoint3)));
        router.route(message, (MuleSession) session.proxy());
        session.verify();

        endpoints = new ArrayList();
        endpoints.add(endpoint4);
        endpoints.add(endpoint5);
        endpoints.add(endpoint6);
        router.getEndpoints().clear();
        router.setEndpoints(endpoints);

        message = new DefaultMuleMessage("test,mule,message");

        session.expectAndReturn("sendEvent", C.args(C.isA(MuleMessage.class), C.eq(endpoint4)), message);
        session.expectAndReturn("sendEvent", C.args(C.isA(MuleMessage.class), C.eq(endpoint5)), message);
        session.expectAndReturn("sendEvent", C.args(C.isA(MuleMessage.class), C.eq(endpoint6)), message);
        MuleMessage result = router.route(message, (MuleSession) session.proxy());
        assertNotNull(result);
        assertTrue(result instanceof MuleMessageCollection);
        assertEquals(3, ((MuleMessageCollection) result).size());
        session.verify();
    }
}
