/*
 * $Id$
 * --------------------------------------------------------------------------------------
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 *
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.module.ws.construct.builder;

import java.io.File;
import java.net.URI;
import java.net.URISyntaxException;

import org.mule.api.MuleException;
import org.mule.api.config.ConfigurationException;
import org.mule.exception.DefaultMessagingExceptionStrategy;
import org.mule.module.ws.construct.WSProxy;
import org.mule.tck.AbstractMuleTestCase;
import org.mule.transformer.compression.GZipCompressTransformer;
import org.mule.transformer.simple.ObjectToByteArray;
import org.mule.transformer.simple.StringAppendTransformer;

public class WSProxyBuilderTestCase extends AbstractMuleTestCase
{
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

    public void testFullConfigurationFileWsdl() throws Exception
    {
        final WSProxy wsProxy = new WSProxyBuilder().name("test-ws-proxy-full-file-wsdl")
            .wsdlFile(new File(getTestWsdlUri()))
            .inboundAddress("test://foo")
            .outboundAddress("test://bar")
            .transformers(new StringAppendTransformer("bar"))
            .responseTransformers(new ObjectToByteArray(), new GZipCompressTransformer())
            .exceptionStrategy(new DefaultMessagingExceptionStrategy(muleContext, true))
            .build(muleContext);

        assertEquals("test-ws-proxy-full-file-wsdl", wsProxy.getName());
    }

    public void testConfigurationUriWsdl() throws Exception
    {
        final WSProxy wsProxy = new WSProxyBuilder().name("test-ws-proxy-uri-wsdl").wsldLocation(
            getTestWsdlUri()).inboundAddress("test://foo").outboundAddress("test://bar").build(muleContext);

        assertEquals("test-ws-proxy-uri-wsdl", wsProxy.getName());
    }

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
