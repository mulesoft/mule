/*
 * $Id$
 * --------------------------------------------------------------------------------------
 * Copyright (c) MuleSource, Inc.  All rights reserved.  http://www.mulesource.com
 *
 * The software in this package is published under the terms of the MuleSource MPL
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.providers.http;

import org.mule.impl.MuleDescriptor;
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
    public UMOConnector getConnector() throws Exception
    {
        return createConnector(managementContext, true);
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
        cnn.setStorePassword("mulepassword");
        cnn.setTrustStore("trustStore");
        cnn.setTrustStorePassword("mulepassword");
        cnn.getDispatcherThreadingProfile().setDoThreading(false);

        if (initialised)
        {
            cnn.initialise(context);
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
        HttpsConnector connector = (HttpsConnector)getConnector();

        MuleDescriptor d = getTestDescriptor("orange", Orange.class.getName());
        UMOComponent component = getTestComponent(d);
        UMOEndpoint endpoint = getTestEndpoint("Test", UMOEndpoint.ENDPOINT_TYPE_RECEIVER);
        endpoint.setEndpointURI(null);
        endpoint.setConnector(connector);

        try
        {
            connector.registerListener(component, endpoint);
            fail("cannot register with null endpointUri");
        }
        catch (Exception e)
        { /* expected */
        }
        endpoint.setEndpointURI(null);
        try
        {
            connector.registerListener(component, endpoint);
            fail("cannot register with empty endpointUri");
        }
        catch (Exception e)
        { /* expected */
        }

        endpoint.setEndpointURI(new MuleEndpointURI("https://localhost:0"));
        connector.registerListener(component, endpoint);
        try
        {
            connector.registerListener(component, endpoint);
            fail("cannot register on the same endpointUri");
        }
        catch (Exception e)
        { /* expected */
        }
        connector.dispose();
    }

    public void testProperties() throws Exception
    {
        HttpsConnector c = (HttpsConnector)getConnector();

        c.setBufferSize(1024);
        assertEquals(1024, c.getBufferSize());
        c.setBufferSize(0);
        assertEquals(TcpConnector.DEFAULT_BUFFER_SIZE, c.getBufferSize());

        c.dispose();
        // all kinds of timeouts are tested in TcpConnectorTestCase now
    }
}
