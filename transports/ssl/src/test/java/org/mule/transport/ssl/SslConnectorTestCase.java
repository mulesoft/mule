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

import org.mule.api.endpoint.InboundEndpoint;
import org.mule.api.service.Service;
import org.mule.api.transport.Connector;
import org.mule.tck.testmodels.fruit.Orange;
import org.mule.transport.AbstractConnectorTestCase;

public class SslConnectorTestCase extends AbstractConnectorTestCase
{
    // @Override
    public Connector createConnector() throws Exception
    {
        SslConnector cnn = new SslConnector();
        cnn.setMuleContext(muleContext);
        cnn.setName("SslConnector");
        cnn.setKeyStore("serverKeystore");
        cnn.setClientKeyStore("clientKeystore");
        cnn.setClientKeyStorePassword("mulepassword");
        cnn.setKeyPassword("mulepassword");
        cnn.setKeyStorePassword("mulepassword");
        cnn.setTrustStore("trustStore");
        cnn.setTrustStorePassword("mulepassword");
        cnn.getDispatcherThreadingProfile().setDoThreading(false);
        return cnn;
    }

    public void testClientConnector() throws Exception
    {
        SslConnector cnn = new SslConnector();
        cnn.setMuleContext(muleContext);
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
        Connector connector = getConnector();
        
        InboundEndpoint endpoint2 = muleContext.getRegistry()
            .lookupEndpointFactory()
            .getInboundEndpoint("ssl://localhost:30303");

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
