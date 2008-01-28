/*
 * $Id$
 * --------------------------------------------------------------------------------------
 * Copyright (c) MuleSource, Inc.  All rights reserved.  http://www.mulesource.com
 *
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.transport.ssl;

import org.mule.api.endpoint.Endpoint;
import org.mule.api.endpoint.ImmutableEndpoint;
import org.mule.api.lifecycle.InitialisationException;
import org.mule.api.service.Service;
import org.mule.api.transport.Connector;
import org.mule.tck.providers.AbstractConnectorTestCase;
import org.mule.tck.testmodels.fruit.Orange;

import java.io.IOException;

public class SslConnectorTestCase extends AbstractConnectorTestCase
{

    // @Override
    public Connector createConnector() throws Exception
    {
        return createConnector(true);
    }

    public static SslConnector createConnector(boolean initialised)
        throws InitialisationException, IOException
    {
        SslConnector cnn = new SslConnector();
        cnn.setName("SslConnector");
        cnn.setKeyStore("serverKeystore");
        cnn.setClientKeyStore("clientKeystore");
        cnn.setClientKeyStorePassword("mulepassword");
        cnn.setKeyPassword("mulepassword");
        cnn.setKeyStorePassword("mulepassword");
        cnn.setTrustStore("trustStore");
        cnn.setTrustStorePassword("mulepassword");
        cnn.getDispatcherThreadingProfile().setDoThreading(false);
        //TODO FIX URGENT
        // can this go?!  who added it?  when was it urgent?
//        if (initialised)
//        {
//            cnn.initialise(muleContext);
//        }
        return cnn;
    }

    public void testClientConnector() throws Exception
    {
        SslConnector cnn = new SslConnector();
        cnn.setClientKeyStore("clientKeystore");
        cnn.setClientKeyStorePassword("mulepassword");
        cnn.getDispatcherThreadingProfile().setDoThreading(false);
    }

    public String getTestEndpointURI()
    {
        return "ssl://localhost:56801";
    }

    public Object getValidMessage() throws Exception
    {
        return "Hello".getBytes();
    }

    public void testValidListener() throws Exception
    {
        Service service = getTestService("orange", Orange.class);
        Endpoint endpoint = getTestInboundEndpoint("Test");
        Connector connector = getConnector();

        try
        {
            endpoint.setEndpointURI(null);
            endpoint.setConnector(connector);
            connector.registerListener(service, endpoint);
            fail("cannot register with null endpointUri");
        }
        catch (Exception e)
        {
            //  expected
        }

        try
        {
            endpoint.setEndpointURI(null);
            connector.registerListener(service, endpoint);
            fail("cannot register with empty endpointUri");
        }
        catch (Exception e)
        {
            // expected
        }

        
        ImmutableEndpoint endpoint2 = muleContext.getRegistry()
            .lookupEndpointFactory()
            .getOutboundEndpoint("ssl://localhost:30303");

        connector.registerListener(service, endpoint2);
        try
        {
            connector.registerListener(service, endpoint2);
            fail("cannot register on the same endpointUri");
        }
        catch (Exception e)
        {
            // expected
        }
    }

    public void testProperties() throws Exception
    {
        SslConnector c = (SslConnector)getConnector();

        c.setSendBufferSize(1024);
        assertEquals(1024, c.getSendBufferSize());
        c.setSendBufferSize(0);
        assertEquals(SslConnector.DEFAULT_BUFFER_SIZE, c.getSendBufferSize());
    }

}
