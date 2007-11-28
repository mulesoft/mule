/*
 * $Id$
 * --------------------------------------------------------------------------------------
 * Copyright (c) MuleSource, Inc.  All rights reserved.  http://www.mulesource.com
 *
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.test.integration.providers.jdbc;


import org.mule.impl.MuleEvent;
import org.mule.impl.MuleMessage;
import org.mule.impl.model.seda.SedaComponent;
import org.mule.routing.inbound.InboundRouterCollection;
import org.mule.routing.outbound.FilteringOutboundRouter;
import org.mule.routing.outbound.OutboundRouterCollection;
import org.mule.tck.MuleTestUtils;
import org.mule.tck.functional.EventCallback;
import org.mule.umo.UMOComponent;
import org.mule.umo.UMOMessage;
import org.mule.umo.UMOSession;
import org.mule.umo.endpoint.UMOImmutableEndpoint;
import org.mule.umo.routing.UMOInboundRouterCollection;
import org.mule.umo.routing.UMOOutboundRouter;
import org.mule.umo.routing.UMOOutboundRouterCollection;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

public class JdbcNonTransactionalFunctionalTestCase extends AbstractJdbcFunctionalTestCase
{

    /*
     * org.apache.commons.dbutils.ResultSetHandler (called by QueryRunner which is
     * called by JdbcMessageReceiver) allows either null or a List of 0 rows to be
     * returned so we check for both.
     */
    protected static void assertResultSetEmpty(UMOMessage message)
    {
        assertNotNull(message);
        Object payload = message.getPayload();
        assertTrue(payload instanceof java.util.List);
        List list = (List)payload;
        assertEquals(0, list.size());
    }

    protected static void assertResultSetNotEmpty(UMOMessage message)
    {
        assertNotNull(message);
        Object payload = message.getPayload();
        assertTrue(payload instanceof java.util.List);
        List list = (List)payload;
        assertFalse(list.isEmpty());
    }

    public void testDirectSql() throws Exception
    {
        // Start the server
        managementContext.start();

        UMOImmutableEndpoint muleEndpoint = managementContext.getRegistry().lookupEndpointFactory().getInboundEndpoint(
            "jdbc://?sql=SELECT * FROM TEST");
        UMOMessage message = muleEndpoint.request(1000);
        assertResultSetEmpty(message);
        
        execSqlUpdate("INSERT INTO TEST(TYPE, DATA, ACK, RESULT) VALUES (1, '" + DEFAULT_MESSAGE
            + "', NULL, NULL)");
        message = muleEndpoint.request(1000);
        assertResultSetNotEmpty(message);
    }

    public void testSend() throws Exception
    {
        // Start the server
        managementContext.start();

        UMOImmutableEndpoint endpoint = managementContext.getRegistry().lookupEndpointFactory().getOutboundEndpoint(
            DEFAULT_OUT_URI);
        
        UMOMessage message = new MuleMessage(DEFAULT_MESSAGE);
        UMOSession session = MuleTestUtils.getTestSession();
        MuleEvent event = new MuleEvent(message, endpoint, session, true);
        session.dispatchEvent(event);

        Object[] obj2 = execSqlQuery("SELECT DATA FROM TEST WHERE TYPE = 2");
        assertNotNull(obj2);
        assertEquals(1, obj2.length);
        assertEquals(DEFAULT_MESSAGE, obj2[0]);
    }

    public void testReceive() throws Exception
    {
        // Start the server
        managementContext.start();

        UMOImmutableEndpoint muleEndpoint = managementContext.getRegistry().lookupEndpointFactory().getInboundEndpoint(
            DEFAULT_IN_URI);
        
        UMOMessage message = muleEndpoint.request(1000);

        assertResultSetEmpty(message);

        execSqlUpdate("INSERT INTO TEST(TYPE, DATA, ACK, RESULT) VALUES (1, '" + DEFAULT_MESSAGE
            + "', NULL, NULL)");

        message = muleEndpoint.request(1000);
        assertResultSetNotEmpty(message);
    }

    public void testReceiveAndSend() throws Exception
    {
        // Start the server
        initialiseComponent(null);
        managementContext.start();

        execSqlUpdate("INSERT INTO TEST(TYPE, DATA, ACK, RESULT) VALUES (1, '" + DEFAULT_MESSAGE
                        + "', NULL, NULL)");

        long t0 = System.currentTimeMillis();
        while (System.currentTimeMillis() - t0 < 20000)
        {
            Object[] rs = execSqlQuery("SELECT COUNT(*) FROM TEST WHERE TYPE = 2");
            assertNotNull(rs);
            assertEquals(1, rs.length);
            if (((Number)rs[0]).intValue() > 0)
            {
                break;
            }
            Thread.sleep(100);
        }

        Object[] obj2 = execSqlQuery("SELECT DATA FROM TEST WHERE TYPE = 2");
        assertNotNull(obj2);
        assertEquals(1, obj2.length);
        assertEquals(DEFAULT_MESSAGE + " Received", obj2[0]);
    }

    public void initialiseComponent(EventCallback callback) throws Exception
    {
        UMOComponent component = new SedaComponent();
        HashMap props = new HashMap();
        props.put("eventCallback", callback);
        component.setProperties(props);
        component.setName("testComponent");
        
        UMOImmutableEndpoint in = managementContext.getRegistry().lookupEndpointFactory().getInboundEndpoint(
            getInDest());
        UMOImmutableEndpoint out = managementContext.getRegistry().lookupEndpointFactory().getOutboundEndpoint(
            getOutDest());

        UMOInboundRouterCollection inboundRouterCollection = new InboundRouterCollection();
        inboundRouterCollection.addEndpoint(in);
        component.setInboundRouter(inboundRouterCollection);

        UMOOutboundRouterCollection outboundRouterCollection = new OutboundRouterCollection();
        UMOOutboundRouter outboundRouter = new FilteringOutboundRouter();
        List endpoints = new ArrayList();
        endpoints.add(out);
        outboundRouter.setEndpoints(endpoints);
        outboundRouterCollection.addRouter(outboundRouter);

        component.setInboundRouter(inboundRouterCollection);
        component.setOutboundRouter(outboundRouterCollection);

        managementContext.getRegistry().registerComponent(component);
    }

}
