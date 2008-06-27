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
import org.mule.tck.AbstractMuleTestCase;
import org.mule.tck.MuleTestUtils;
import org.mule.util.StringUtils;

import com.mockobjects.dynamic.C;
import com.mockobjects.dynamic.Mock;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class MessageSplitterRouterTestCase extends AbstractMuleTestCase
{

    public void testMessageSplitterRouter() throws Exception
    {
        Mock session = MuleTestUtils.getMockSession();
        session.matchAndReturn("getService", getTestService());

        OutboundEndpoint endpoint1 = getTestOutboundEndpoint("Test1Provider", "test://endpointUri.1");
        OutboundEndpoint endpoint2 = getTestOutboundEndpoint("Test2Provider", "test://endpointUri.2");
        OutboundEndpoint endpoint3 = getTestOutboundEndpoint("Test3Provider", "test://endpointUri.3");

        // Dummy message splitter
        AbstractMessageSplitter router = new AbstractMessageSplitter()
        {
            private List parts;

            protected void initialise(MuleMessage message)
            {
                multimatch = false;
                parts = Arrays.asList(StringUtils.splitAndTrim(message.getPayload().toString(), ","));
            }

            protected MuleMessage getMessagePart(MuleMessage message, OutboundEndpoint endpoint)
            {
                if (endpoint.getEndpointURI().getAddress().equals("endpointUri.1"))
                {
                    return new DefaultMuleMessage(parts.get(0));
                }
                else if (endpoint.getEndpointURI().getAddress().equals("endpointUri.2"))
                {
                    return new DefaultMuleMessage(parts.get(1));
                }
                else if (endpoint.getEndpointURI().getAddress().equals("endpointUri.3"))
                {
                    return new DefaultMuleMessage(parts.get(2));
                }
                else
                {
                    return null;
                }
            }
            
            protected void cleanup()
            {
                parts = null;
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
        router.route(message, (MuleSession)session.proxy(), false);
        session.verify();

        message = new DefaultMuleMessage("test,mule,message");

        session.expectAndReturn("sendEvent", C.args(C.isA(MuleMessage.class), C.eq(endpoint1)), message);
        session.expectAndReturn("sendEvent", C.args(C.isA(MuleMessage.class), C.eq(endpoint2)), message);
        session.expectAndReturn("sendEvent", C.args(C.isA(MuleMessage.class), C.eq(endpoint3)), message);
        MuleMessage result = router.route(message, (MuleSession)session.proxy(), true);
        assertNotNull(result);
        assertEquals(message, result);
        session.verify();
    }
}
