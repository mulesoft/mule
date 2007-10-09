/*
 * $Id$
 * --------------------------------------------------------------------------------------
 * Copyright (c) MuleSource, Inc.  All rights reserved.  http://www.mulesource.com
 *
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.providers.http;

import org.mule.impl.endpoint.MuleEndpointURI;
import org.mule.providers.tcp.TcpConnector;
import org.mule.tck.providers.AbstractConnectorTestCase;
import org.mule.tck.testmodels.fruit.Orange;
import org.mule.umo.UMOComponent;
import org.mule.umo.UMOManagementContext;
import org.mule.umo.endpoint.UMOEndpoint;
import org.mule.umo.lifecycle.InitialisationException;
import org.mule.umo.provider.UMOConnector;

import java.io.IOException;


public class HttpsConnectorTestCase extends AbstractConnectorTestCase
{

    // @Override
    public UMOConnector createConnector() throws Exception
    {
        return createConnector(managementContext, false);
    }

    public static HttpsConnector createConnector(UMOManagementContext context, boolean initialised)
        throws IOException, InitialisationException
    {
        HttpsConnector cnn = new HttpsConnector();
        cnn.setName("HttpsConnector");
        cnn.setKeyStore("serverKeystore");
        cnn.setClientKeyStore("clientKeystore");
        cnn.setClientKeyStorePassword("mulepassword");
        cnn.setKeyPassword("mulepassword");
        cnn.setKeyStorePassword("mulepassword");
        cnn.setTrustStore("trustStore");
        cnn.setTrustStorePassword("mulepassword");
        cnn.getDispatcherThreadingProfile().setDoThreading(false);

        if (initialised)
        {
            cnn.initialise();
        }
        return cnn;
    }

    public String getTestEndpointURI()
    {
        return "https://localhost:60127";
    }

    public Object getValidMessage() throws Exception
    {
        return "Hello".getBytes();
    }

    public void testValidListener() throws Exception
    {
        UMOComponent component = getTestComponent("orange", Orange.class);
        UMOEndpoint endpoint = (UMOEndpoint) managementContext.getRegistry().lookupEndpointFactory().createInboundEndpoint(
            getTestEndpointURI(), managementContext);

        try
        {
            endpoint.setEndpointURI(null);
            fail("cannot register with empty endpointUri");
        }
        catch (Exception e)
        {
            /* expected */
        }

        endpoint.setEndpointURI(new MuleEndpointURI(getTestEndpointURI()));
        getConnector().registerListener(component, endpoint);
    }

    public void testProperties() throws Exception
    {
        HttpsConnector c = (HttpsConnector)getConnector();

        c.setSendBufferSize(1024);
        assertEquals(1024, c.getSendBufferSize());
        c.setSendBufferSize(0);
        assertEquals(TcpConnector.DEFAULT_BUFFER_SIZE, c.getSendBufferSize());

        // all kinds of timeouts are tested in TcpConnectorTestCase now
    }
}
