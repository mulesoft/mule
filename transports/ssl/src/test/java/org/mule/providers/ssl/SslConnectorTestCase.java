/*
 * $Id$
 * --------------------------------------------------------------------------------------
 * Copyright (c) MuleSource, Inc.  All rights reserved.  http://www.mulesource.com
 *
 * The software in this package is published under the terms of the MuleSource MPL
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.providers.ssl;

import org.mule.impl.MuleDescriptor;
import org.mule.impl.endpoint.MuleEndpoint;
import org.mule.impl.endpoint.MuleEndpointURI;
import org.mule.tck.providers.AbstractConnectorTestCase;
import org.mule.tck.testmodels.fruit.Orange;
import org.mule.umo.UMOComponent;
import org.mule.umo.endpoint.UMOEndpoint;
import org.mule.umo.lifecycle.InitialisationException;
import org.mule.umo.provider.UMOConnector;

import java.io.IOException;

public class SslConnectorTestCase extends AbstractConnectorTestCase
{

    public UMOConnector getConnector() throws Exception
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
//            cnn.initialise(managementContext);
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
        MuleDescriptor d = getTestDescriptor("orange", Orange.class.getName());
        UMOComponent component = getTestComponent(d);
        UMOEndpoint endpoint = getTestEndpoint("Test", UMOEndpoint.ENDPOINT_TYPE_RECEIVER);

        try
        {
            endpoint.setEndpointURI(null);
            endpoint.setConnector(connector);
            connector.registerListener(component, endpoint);
            fail("cannot register with null endpointUri");
        }
        catch (Exception e)
        {
            /* expected */
        }

        try
        {
            endpoint.setEndpointURI(null);
            connector.registerListener(component, endpoint);
            fail("cannot register with empty endpointUri");
        }
        catch (Exception e)
        {
            /* expected */
        }

        endpoint = new MuleEndpoint();
        MuleEndpointURI uri = new MuleEndpointURI("ssl://localhost:30303");
        uri.initialise();
        endpoint.setEndpointURI(uri);
        connector.registerListener(component, endpoint);
        try
        {
            connector.registerListener(component, endpoint);
            fail("cannot register on the same endpointUri");
        }
        catch (Exception e)
        {
            /* expected */
        }
    }

    public void testProperties() throws Exception
    {
        SslConnector c = (SslConnector)connector;

        c.setSendBufferSize(1024);
        assertEquals(1024, c.getSendBufferSize());
        c.setSendBufferSize(0);
        assertEquals(SslConnector.DEFAULT_BUFFER_SIZE, c.getSendBufferSize());
    }

}
