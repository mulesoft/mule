/*
 * $Id$
 * --------------------------------------------------------------------------------------
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 *
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.routing.outbound;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.mock;

import org.mule.DefaultMuleMessage;
import org.mule.api.MuleEvent;
import org.mule.api.MuleMessage;
import org.mule.api.MuleMessageCollection;
import org.mule.api.MuleSession;
import org.mule.api.endpoint.OutboundEndpoint;
import org.mule.api.processor.MessageProcessor;
import org.mule.tck.junit4.AbstractMuleContextTestCase;

import com.mockobjects.dynamic.Mock;

import java.util.ArrayList;
import java.util.List;
import java.util.StringTokenizer;

import org.junit.Test;

public class MessageSplitterRouterTestCase extends AbstractMuleContextTestCase
{
    public MessageSplitterRouterTestCase()
    {
        setStartContext(true);
    }

    @Test
    public void testMessageSplitterRouter() throws Exception
    {
        //Async targets
        OutboundEndpoint endpoint1 = getTestOutboundEndpoint("Test1Endpoint", "test://endpointUri.1");
        OutboundEndpoint endpoint2 = getTestOutboundEndpoint("Test2Endpoint", "test://endpointUri.2");
        OutboundEndpoint endpoint3 = getTestOutboundEndpoint("Test3Endpoint", "test://endpointUri.3");
        Mock mockendpoint1 = RouterTestUtils.getMockEndpoint(endpoint1);
        Mock mockendpoint2 = RouterTestUtils.getMockEndpoint(endpoint2);
        Mock mockendpoint3 = RouterTestUtils.getMockEndpoint(endpoint3);

        //Sync targets  org.python.core.__builtin__
        OutboundEndpoint endpoint4 = getTestOutboundEndpoint("Test4Endpoint",
            "test://endpointUri.4?exchangePattern=request-response");
        OutboundEndpoint endpoint5 = getTestOutboundEndpoint("Test5Endpoint",
            "test://endpointUri.5?exchangePattern=request-response");
        OutboundEndpoint endpoint6 = getTestOutboundEndpoint("Test6Endpoint",
            "test://endpointUri.6?exchangePattern=request-response");
        Mock mockendpoint4 = RouterTestUtils.getMockEndpoint(endpoint4);
        Mock mockendpoint5 = RouterTestUtils.getMockEndpoint(endpoint5);
        Mock mockendpoint6 = RouterTestUtils.getMockEndpoint(endpoint6);


        // Dummy message splitter
        AbstractMessageSplitter router = new AbstractMessageSplitter()
        {
            @Override
            protected SplitMessage getMessageParts(MuleMessage message, List<MessageProcessor> endpoints)
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

        List<MessageProcessor> endpoints = new ArrayList<MessageProcessor>();
        endpoints.add((OutboundEndpoint) mockendpoint1.proxy());
        endpoints.add((OutboundEndpoint) mockendpoint2.proxy());
        endpoints.add((OutboundEndpoint) mockendpoint3.proxy());
        router.setRoutes(endpoints);

        MuleMessage message = new DefaultMuleMessage("test,mule,message", muleContext);

        assertTrue(router.isMatch(message));
        mockendpoint1.expect("process", RouterTestUtils.getArgListCheckerMuleEvent());
        mockendpoint2.expect("process", RouterTestUtils.getArgListCheckerMuleEvent());
        mockendpoint3.expect("process", RouterTestUtils.getArgListCheckerMuleEvent());

        MuleSession session = mock(MuleSession.class);
        router.route(new OutboundRoutingTestEvent(message, session, muleContext));
        mockendpoint1.verify();
        mockendpoint2.verify();
        mockendpoint3.verify();

        endpoints = new ArrayList<MessageProcessor>();
        endpoints.add((OutboundEndpoint) mockendpoint4.proxy());
        endpoints.add((OutboundEndpoint) mockendpoint5.proxy());
        endpoints.add((OutboundEndpoint) mockendpoint6.proxy());
        router.getRoutes().clear();
        router.setRoutes(endpoints);

        message = new DefaultMuleMessage("test,mule,message", muleContext);
        MuleEvent event = new OutboundRoutingTestEvent(message, null, muleContext);

        mockendpoint4.expectAndReturn("process", RouterTestUtils.getArgListCheckerMuleEvent(), event);
        mockendpoint5.expectAndReturn("process", RouterTestUtils.getArgListCheckerMuleEvent(), event);
        mockendpoint6.expectAndReturn("process", RouterTestUtils.getArgListCheckerMuleEvent(), event);

        MuleEvent result = router.route(new OutboundRoutingTestEvent(message, session, muleContext));
        assertNotNull(result);
        MuleMessage resultMessage = result.getMessage();
        assertNotNull(resultMessage);
        assertTrue(resultMessage instanceof MuleMessageCollection);
        assertEquals(3, ((MuleMessageCollection) resultMessage).size());
        mockendpoint4.verify();
        mockendpoint5.verify();
        mockendpoint6.verify();
    }
}
