/*
 * (c) 2003-2014 MuleSoft, Inc. This software is protected under international copyright
 * law. All use of this software is subject to MuleSoft's Master Subscription Agreement
 * (or other master license agreement) separately entered into in writing between you and
 * MuleSoft. If such an agreement is not in place, you may not use the software.
 */
package org.mule.module.ws.construct.builder;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import org.mule.api.MuleException;
import org.mule.api.config.ConfigurationException;
import org.mule.exception.DefaultMessagingExceptionStrategy;
import org.mule.module.ws.construct.WSProxy;
import org.mule.tck.junit4.AbstractMuleContextTestCase;
import org.mule.transformer.compression.GZipCompressTransformer;
import org.mule.transformer.simple.ObjectToByteArray;
import org.mule.transformer.simple.StringAppendTransformer;

import java.io.File;
import java.net.URI;
import java.net.URISyntaxException;

import org.junit.Test;

public class WSProxyBuilderTestCase extends AbstractMuleContextTestCase
{

    @Test
    public void testConfigurationInvalidFileWsdl()
    {
        try
        {
            new WSProxyBuilder().name("test-ws-proxy-invalid-file-wsdl")
                .wsdlFile(new File("missing_file.foo"))
                .inboundAddress("test://foo")
                .outboundAddress("test://bar")
                .build(muleContext);
            fail("should have raised a MuleException");
        }
        catch (final MuleException me)
        {
            assertTrue(me instanceof ConfigurationException);
        }
    }

    @Test
    public void testFullConfigurationFileWsdl() throws Exception
    {
        final WSProxy wsProxy = new WSProxyBuilder().name("test-ws-proxy-full-file-wsdl")
            .wsdlFile(new File(getTestWsdlUri()))
            .inboundAddress("test://foo")
            .outboundAddress("test://bar")
            .transformers(new StringAppendTransformer("bar"))
            .responseTransformers(new ObjectToByteArray(), new GZipCompressTransformer())
            .exceptionStrategy(new DefaultMessagingExceptionStrategy(muleContext))
            .build(muleContext);

        assertEquals("test-ws-proxy-full-file-wsdl", wsProxy.getName());
    }

    @Test
    public void testConfigurationUriWsdl() throws Exception
    {
        final WSProxy wsProxy = new WSProxyBuilder().name("test-ws-proxy-uri-wsdl").wsldLocation(
            getTestWsdlUri()).inboundAddress("test://foo").outboundAddress("test://bar").build(muleContext);

        assertEquals("test-ws-proxy-uri-wsdl", wsProxy.getName());
    }

    @Test
    public void testConfigurationNoWsdl() throws Exception
    {
        final WSProxy wsProxy = new WSProxyBuilder().name("test-ws-proxy-no-wsdl").inboundAddress(
            "test://foo").outboundAddress("test://bar").build(muleContext);

        assertEquals("test-ws-proxy-no-wsdl", wsProxy.getName());
    }

    private URI getTestWsdlUri() throws URISyntaxException
    {
        return Thread.currentThread().getContextClassLoader().getResource("weather-forecaster.wsdl").toURI();
    }
}
