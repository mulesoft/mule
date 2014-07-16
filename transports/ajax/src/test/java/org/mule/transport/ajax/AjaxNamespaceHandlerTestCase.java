/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.transport.ajax;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import org.mule.api.endpoint.EndpointBuilder;
import org.mule.api.endpoint.InboundEndpoint;
import org.mule.tck.junit4.FunctionalTestCase;
import org.mule.tck.junit4.rule.DynamicPort;
import org.mule.transport.ajax.container.AjaxServletConnector;
import org.mule.transport.ajax.embedded.AjaxConnector;

import java.net.URL;

import org.junit.Rule;
import org.junit.Test;

public class AjaxNamespaceHandlerTestCase extends FunctionalTestCase
{
    @Rule
    public DynamicPort dynamicPort1 = new DynamicPort("port1");

    @Rule
    public DynamicPort dynamicPort2 = new DynamicPort("port2");

    @Override
    protected String getConfigFile()
    {
        return "ajax-namespace-config.xml";
    }

    @Test
    public void testConnector1Properties() throws Exception
    {
        AjaxConnector connector =
                (AjaxConnector) muleContext.getRegistry().lookupConnector("connector1");

        assertNotNull(connector);

        assertTrue(connector.isJsonCommented());
        assertEquals(1000, connector.getInterval());
        assertEquals(1, connector.getLogLevel());
        assertEquals(10000, connector.getMaxInterval());
        assertEquals(3000, connector.getMultiFrameInterval());
        assertEquals(4000, connector.getRefsThreshold());
        assertEquals(50000, connector.getTimeout());
        assertEquals(new URL("http://0.0.0.0:" + dynamicPort1.getNumber() + "/service"), connector.getServerUrl());
        assertEquals("/foo/bar", connector.getResourceBase());
        // Test a abstract connector property (MULE-5776)
        assertTrue(connector.isValidateConnections());
    }

    @Test
    public void testSecureConnector2Properties() throws Exception
    {
        AjaxConnector connector =
                (AjaxConnector) muleContext.getRegistry().lookupConnector("connector2");

        assertNotNull(connector);

        assertTrue(connector.isJsonCommented());
        assertEquals(1000, connector.getInterval());
        assertEquals(1, connector.getLogLevel());
        assertEquals(10000, connector.getMaxInterval());
        assertEquals(3000, connector.getMultiFrameInterval());
        assertEquals(4000, connector.getRefsThreshold());
        assertEquals(50000, connector.getTimeout());
        assertEquals(new URL("https://0.0.0.0:" + dynamicPort2.getNumber() + "/service"), connector.getServerUrl());
        assertEquals("/foo/bar", connector.getResourceBase());

        //The full path gets resolved, we're just checkng that the property got set
        assertTrue(connector.getKeyStore().endsWith("/serverKeystore"));
        assertEquals("muleserver", connector.getKeyAlias());
        assertEquals("mulepassword", connector.getKeyPassword());
        assertEquals("mulepassword", connector.getKeyStorePassword());
        //The full path gets resolved, we're just checkng that the property got set
        assertTrue(connector.getClientKeyStore().endsWith("/clientKeystore"));
        assertEquals("mulepassword", connector.getClientKeyStorePassword());
        //The full path gets resolved, we're just checkng that the property got set
        assertTrue(connector.getTrustStore().endsWith("/trustStore"));
        assertEquals("mulepassword", connector.getTrustStorePassword());
        assertTrue(connector.isExplicitTrustStoreOnly());
        assertTrue(connector.isRequireClientAuthentication());
    }

    @Test
    public void testAjaxServletConnector() throws Exception
    {
        AjaxServletConnector connector = (AjaxServletConnector) muleContext.getRegistry().lookupConnector("connector3");
        assertNotNull(connector);
        //No properties
    }

    @Test
    public void testEmbeddedEndpoint() throws Exception
    {
        EndpointBuilder b = muleContext.getRegistry().lookupEndpointBuilder("endpoint1");
        assertNotNull(b);
        InboundEndpoint ep = b.buildInboundEndpoint();
        assertEquals("/request", ep.getEndpointURI().getPath());
    }

    @Test
    public void testServletEndpoint() throws Exception
    {
        EndpointBuilder b = muleContext.getRegistry().lookupEndpointBuilder("endpoint2");
        assertNotNull(b);
        InboundEndpoint ep = b.buildInboundEndpoint();
        assertEquals("/response", ep.getEndpointURI().getPath());
    }
}
