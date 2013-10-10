/*
 * (c) 2003-2014 MuleSoft, Inc. This software is protected under international copyright
 * law. All use of this software is subject to MuleSoft's Master Subscription Agreement
 * (or other master license agreement) separately entered into in writing between you and
 * MuleSoft. If such an agreement is not in place, you may not use the software.
 */
package org.mule.transport.ssl;

import org.mule.api.endpoint.InboundEndpoint;
import org.mule.api.service.Service;
import org.mule.api.transport.Connector;
import org.mule.tck.testmodels.fruit.Orange;
import org.mule.transport.AbstractConnectorTestCase;

import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;

public class SslConnectorTestCase extends AbstractConnectorTestCase
{
    @Override
    public Connector createConnector() throws Exception
    {
        SslConnector cnn = new SslConnector(muleContext);
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

    @Test
    public void testClientConnector() throws Exception
    {
        SslConnector cnn = new SslConnector(muleContext);
        cnn.setClientKeyStore("clientKeystore");
        cnn.setClientKeyStorePassword("mulepassword");
        cnn.getDispatcherThreadingProfile().setDoThreading(false);
    }

    @Override
    public String getTestEndpointURI()
    {
        return "ssl://localhost:56801";
    }

    @Override
    public Object getValidMessage() throws Exception
    {
        return "Hello".getBytes();
    }

    @Test
    public void testValidListener() throws Exception
    {
        Service service = getTestService("orange", Orange.class);
        Connector connector = getConnector();

        InboundEndpoint endpoint2 =
            muleContext.getEndpointFactory().getInboundEndpoint("ssl://localhost:30303");

        connector.registerListener(endpoint2, getSensingNullMessageProcessor(), service);
        try
        {
            connector.registerListener(endpoint2, getSensingNullMessageProcessor(), service);
            fail("cannot register on the same endpointUri");
        }
        catch (Exception e)
        {
            // expected
        }
    }

    @Test
    public void testProperties() throws Exception
    {
        SslConnector c = (SslConnector)getConnector();

        c.setSendBufferSize(1024);
        assertEquals(1024, c.getSendBufferSize());
        c.setSendBufferSize(0);
        assertEquals(SslConnector.DEFAULT_BUFFER_SIZE, c.getSendBufferSize());
    }

}
