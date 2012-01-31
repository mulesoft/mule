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

import static junit.framework.Assert.assertTrue;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

import org.mule.DefaultMuleMessage;
import org.mule.api.MuleEvent;
import org.mule.api.MuleMessage;
import org.mule.api.MuleMessageCollection;
import org.mule.api.MuleSession;
import org.mule.api.config.MuleProperties;
import org.mule.api.endpoint.OutboundEndpoint;
import org.mule.api.processor.MessageProcessor;
import org.mule.api.transport.PropertyScope;
import org.mule.routing.CorrelationMode;
import org.mule.routing.filters.RegExFilter;
import org.mule.tck.MuleTestUtils;
import org.mule.tck.junit4.AbstractMuleContextTestCase;

import com.mockobjects.dynamic.ConstraintMatcher;
import com.mockobjects.dynamic.Mock;

import java.util.ArrayList;
import java.util.List;

import org.junit.Test;

public class MulticastingRouterTestCase extends AbstractMuleContextTestCase
{
    public MulticastingRouterTestCase()
    {
        setStartContext(true);
    }

    @Test
    public void testMulticastingRouterAsync() throws Exception
    {
        Mock session = MuleTestUtils.getMockSession();
        session.matchAndReturn("getFlowConstruct", getTestService());
        RegExFilter filter = new RegExFilter("(.*) Message");

        OutboundEndpoint endpoint1 = getTestOutboundEndpoint("Test1Provider", "test://test1", null, filter, null);
        assertNotNull(endpoint1);

        OutboundEndpoint endpoint2 = getTestOutboundEndpoint("Test2Provider", "test://test2", null, filter, null);
        assertNotNull(endpoint2);
        Mock mockendpoint1 = RouterTestUtils.getMockEndpoint(endpoint1);
        Mock mockendpoint2 = RouterTestUtils.getMockEndpoint(endpoint2);

        MulticastingRouter router = createObject(MulticastingRouter.class);

        List<MessageProcessor> endpoints = new ArrayList<MessageProcessor>();
        endpoints.add((OutboundEndpoint) mockendpoint1.proxy());
        endpoints.add((OutboundEndpoint) mockendpoint2.proxy());
        router.setRoutes(endpoints);

        MuleMessage message = new DefaultMuleMessage(TEST_MESSAGE, muleContext);

        assertTrue(router.isMatch(message));

        mockendpoint1.expect("process", RouterTestUtils.getArgListCheckerMuleEvent());
        mockendpoint2.expect("process", RouterTestUtils.getArgListCheckerMuleEvent());
        router.route(new OutboundRoutingTestEvent(message, (MuleSession)session.proxy(), muleContext));
        mockendpoint1.verify();
        mockendpoint2.verify();

    }

    @Test
    public void testMulticastingRouterSync() throws Exception
    {
        Mock session = MuleTestUtils.getMockSession();
        session.matchAndReturn("getFlowConstruct", getTestService());
        session.matchAndReturn("setFlowConstruct", RouterTestUtils.getArgListCheckerFlowConstruct(), null);

        OutboundEndpoint endpoint1 = getTestOutboundEndpoint("Test1Provider", 
            "test://Test1Provider?exchangePattern=request-response");
        assertNotNull(endpoint1);

        OutboundEndpoint endpoint2 = getTestOutboundEndpoint("Test2Provider", 
            "test://Test2Provider?exchangePattern=request-response");
        assertNotNull(endpoint2);
        Mock mockendpoint1 = RouterTestUtils.getMockEndpoint(endpoint1);
        Mock mockendpoint2 = RouterTestUtils.getMockEndpoint(endpoint2);

        MulticastingRouter router = createObject(MulticastingRouter.class);
        RegExFilter filter = new RegExFilter("(.*) Message");
        router.setFilter(filter);
        List<MessageProcessor> endpoints = new ArrayList<MessageProcessor>();
        endpoints.add((OutboundEndpoint) mockendpoint1.proxy());
        endpoints.add((OutboundEndpoint) mockendpoint2.proxy());
        router.setRoutes(endpoints);

        assertEquals(filter, router.getFilter());

        MuleMessage message = new DefaultMuleMessage(TEST_MESSAGE, muleContext);

        assertTrue(router.isMatch(message));

        MuleEvent event = new OutboundRoutingTestEvent(message, null, muleContext);

        mockendpoint1.expectAndReturn("process", RouterTestUtils.getArgListCheckerMuleEvent(), event);
        mockendpoint2.expectAndReturn("process", RouterTestUtils.getArgListCheckerMuleEvent(), event);
        MuleEvent result = router.route(new OutboundRoutingTestEvent(message, (MuleSession)session.proxy(), muleContext));
        assertNotNull(result);
        MuleMessage resultMessage = result.getMessage();
        assertNotNull(resultMessage);
        assertTrue(resultMessage instanceof MuleMessageCollection);
        assertEquals(2, ((MuleMessageCollection)resultMessage).size());
        mockendpoint1.verify();
        mockendpoint2.verify();
    }

    @Test
    public void testMulticastingRouterMixedSyncAsync() throws Exception
    {
        Mock session = MuleTestUtils.getMockSession();
        session.matchAndReturn("getFlowConstruct", getTestService());
        session.matchAndReturn("setFlowConstruct", RouterTestUtils.getArgListCheckerFlowConstruct(), null);

        OutboundEndpoint endpoint1 = getTestOutboundEndpoint("Test1Provider", 
            "test://Test1Provider?exchangePattern=request-response");
        assertNotNull(endpoint1);

        OutboundEndpoint endpoint2 = getTestOutboundEndpoint("Test2Provider", 
            "test://Test2Provider?exchangePattern=request-response");
        assertNotNull(endpoint2);

        Mock mockendpoint1 = RouterTestUtils.getMockEndpoint(endpoint1);
        Mock mockendpoint2 = RouterTestUtils.getMockEndpoint(endpoint2);

        MulticastingRouter router = createObject(MulticastingRouter.class);
        
        List<MessageProcessor> endpoints = new ArrayList<MessageProcessor>();
        endpoints.add((OutboundEndpoint) mockendpoint1.proxy());
        endpoints.add((OutboundEndpoint) mockendpoint2.proxy());
        router.setRoutes(endpoints);


        MuleMessage message = new DefaultMuleMessage(TEST_MESSAGE, muleContext);

        assertTrue(router.isMatch(message));
        MuleEvent event = new OutboundRoutingTestEvent(message, null, muleContext);

        mockendpoint1.expectAndReturn("process", RouterTestUtils.getArgListCheckerMuleEvent(), event);
        mockendpoint2.expect("process", RouterTestUtils.getArgListCheckerMuleEvent());
        MuleEvent result = router.route(new OutboundRoutingTestEvent(message, (MuleSession)session.proxy(), muleContext));
        assertNotNull(result);
        assertEquals(getPayload(message), getPayload(result.getMessage()));
        mockendpoint1.verify();
        mockendpoint2.verify();
    }
    
    @Test
    public void testMulticastingRouterCorrelationIdPropagation() throws Exception
    {
        Mock session = MuleTestUtils.getMockSession();
        session.matchAndReturn("getFlowConstruct", getTestService());
        RegExFilter filter = new RegExFilter("(.*) Message");

        OutboundEndpoint endpoint1 = getTestOutboundEndpoint("Test1Provider", "test://test1", null, filter, null);
        assertNotNull(endpoint1);

        OutboundEndpoint endpoint2 = getTestOutboundEndpoint("Test2Provider", "test://test2", null, filter, null);
        assertNotNull(endpoint2);
        Mock mockendpoint1 = RouterTestUtils.getMockEndpoint(endpoint1);
        Mock mockendpoint2 = RouterTestUtils.getMockEndpoint(endpoint2);

        MulticastingRouter router = createObject(MulticastingRouter.class);

        List<MessageProcessor> endpoints = new ArrayList<MessageProcessor>();
        endpoints.add((OutboundEndpoint) mockendpoint1.proxy());
        endpoints.add((OutboundEndpoint) mockendpoint2.proxy());
        router.setRoutes(endpoints);
        router.setEnableCorrelation(CorrelationMode.NEVER);

        MuleMessage message = new DefaultMuleMessage(TEST_MESSAGE, muleContext);
        message.setProperty(MuleProperties.MULE_CORRELATION_ID_PROPERTY, "MyCustomCorrelationId", PropertyScope.INBOUND);

        assertTrue(router.isMatch(message));

        ConstraintMatcher expectedCorrelationId = new ConstraintMatcher(){
            public boolean matches(Object[] args)
            {
                boolean argsMatch=args.length == 1 && args[0] instanceof MuleEvent;
                if(argsMatch)
                {
                    MuleEvent event=(MuleEvent) args[0];
                    String correlationId=event.getMessage().getOutboundProperty(MuleProperties.MULE_CORRELATION_ID_PROPERTY);
                    return correlationId!=null && correlationId.equals("MyCustomCorrelationId");
                }
                return false;
            }

            public Object[] getConstraints()
            {
                return new String[] {"Outbound Correlation ID property should be set."};
            }
        };
        mockendpoint1.expect("process", expectedCorrelationId);
        mockendpoint2.expect("process", expectedCorrelationId);
        router.route(new OutboundRoutingTestEvent(message, (MuleSession)session.proxy(), muleContext));
        mockendpoint1.verify();
        mockendpoint2.verify();

    }


    private String getPayload(MuleMessage message) throws Exception
    {
        Object payload = message.getPayload();
        if (payload instanceof List)
        {
            payload = ((List<?>) payload).get(0);
        }
        return payload.toString();
    }
}
