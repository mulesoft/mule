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
import org.mule.api.MuleEvent;
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
    public MessageSplitterRouterTestCase()
    {
        setStartContext(true);
    }

    public void testMessageSplitterRouter() throws Exception
    {
        Mock session = MuleTestUtils.getMockSession();
        session.matchAndReturn("getFlowConstruct", getTestService());

        //Async endpoints
        OutboundEndpoint endpoint1 = getTestOutboundEndpoint("Test1Endpoint", "test://endpointUri.1");
        OutboundEndpoint endpoint2 = getTestOutboundEndpoint("Test2Endpoint", "test://endpointUri.2");
        OutboundEndpoint endpoint3 = getTestOutboundEndpoint("Test3Endpoint", "test://endpointUri.3");
        Mock mockendpoint1 = RouterTestUtils.getMockEndpoint(endpoint1);
        Mock mockendpoint2 = RouterTestUtils.getMockEndpoint(endpoint2);
        Mock mockendpoint3 = RouterTestUtils.getMockEndpoint(endpoint3);

        //Sync endpoints  org.python.core.__builtin__
        OutboundEndpoint endpoint4 = getTestOutboundEndpoint("Test4Endpoint", "test://endpointUri.4?synchronous=true");
        OutboundEndpoint endpoint5 = getTestOutboundEndpoint("Test5Endpoint", "test://endpointUri.5?synchronous=true");
        OutboundEndpoint endpoint6 = getTestOutboundEndpoint("Test6Endpoint", "test://endpointUri.6?synchronous=true");
        Mock mockendpoint4 = RouterTestUtils.getMockEndpoint(endpoint4);
        Mock mockendpoint5 = RouterTestUtils.getMockEndpoint(endpoint5);
        Mock mockendpoint6 = RouterTestUtils.getMockEndpoint(endpoint6);


        // Dummy message splitter
        AbstractMessageSplitter router = new AbstractMessageSplitter()
        {
            @Override
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

        router.setMuleContext(muleContext);

        List<OutboundEndpoint> endpoints = new ArrayList<OutboundEndpoint>();
        endpoints.add((OutboundEndpoint) mockendpoint1.proxy());
        endpoints.add((OutboundEndpoint) mockendpoint2.proxy());
        endpoints.add((OutboundEndpoint) mockendpoint3.proxy());
        router.setEndpoints(endpoints);

        MuleMessage message = new DefaultMuleMessage("test,mule,message", muleContext);

        assertTrue(router.isMatch(message));
        mockendpoint1.expect("process", RouterTestUtils.getArgListCheckerMuleEvent());
        mockendpoint2.expect("process", RouterTestUtils.getArgListCheckerMuleEvent());
        mockendpoint3.expect("process", RouterTestUtils.getArgListCheckerMuleEvent());
        router.route(new OutboundRoutingTestEvent(message, (MuleSession) session.proxy()));
        mockendpoint1.verify();
        mockendpoint2.verify();
        mockendpoint3.verify();

        endpoints = new ArrayList<OutboundEndpoint>();
        endpoints.add((OutboundEndpoint) mockendpoint4.proxy());
        endpoints.add((OutboundEndpoint) mockendpoint5.proxy());
        endpoints.add((OutboundEndpoint) mockendpoint6.proxy());
        router.getEndpoints().clear();
        router.setEndpoints(endpoints);

        message = new DefaultMuleMessage("test,mule,message", muleContext);
        MuleEvent event = new OutboundRoutingTestEvent(message, null);

        mockendpoint4.expectAndReturn("process", RouterTestUtils.getArgListCheckerMuleEvent(), event);
        mockendpoint5.expectAndReturn("process", RouterTestUtils.getArgListCheckerMuleEvent(), event);
        mockendpoint6.expectAndReturn("process", RouterTestUtils.getArgListCheckerMuleEvent(), event);
        MuleMessage result = router.route(new OutboundRoutingTestEvent(message, (MuleSession) session.proxy()));
        assertNotNull(result);
        assertTrue(result instanceof MuleMessageCollection);
        assertEquals(3, ((MuleMessageCollection) result).size());
        mockendpoint4.verify();
        mockendpoint5.verify();
        mockendpoint6.verify();
    }
}
