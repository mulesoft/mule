/*
 * (c) 2003-2014 MuleSoft, Inc. This software is protected under international copyright
 * law. All use of this software is subject to MuleSoft's Master Subscription Agreement
 * (or other master license agreement) separately entered into in writing between you and
 * MuleSoft. If such an agreement is not in place, you may not use the software.
 */
package org.mule.transport.http.functional;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;

import org.mule.api.endpoint.InboundEndpoint;
import org.mule.module.client.MuleClient;
import org.mule.tck.AbstractServiceAndFlowTestCase;
import org.mule.tck.junit4.rule.DynamicPort;
import org.mule.transport.http.HttpConstants;

import java.util.Arrays;
import java.util.Collection;

import org.apache.commons.httpclient.HttpClient;
import org.apache.commons.httpclient.HttpVersion;
import org.apache.commons.httpclient.methods.GetMethod;
import org.apache.commons.httpclient.params.HttpClientParams;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runners.Parameterized.Parameters;

/**
 * Tests as per http://www.io.com/~maus/HttpKeepAlive.html
 */
public class Http10FunctionalTestCase extends AbstractServiceAndFlowTestCase
{
    @Rule
    public DynamicPort dynamicPort = new DynamicPort("port1");

    @Parameters
    public static Collection<Object[]> parameters()
    {
        return Arrays.asList(new Object[][]{
            {ConfigVariant.SERVICE, "http-10-config-service.xml"},
            {ConfigVariant.FLOW, "http-10-config-flow.xml"}
        });
    }      

    public Http10FunctionalTestCase(ConfigVariant variant, String configResources)
    {
        super(variant, configResources);    
    }
    
    private HttpClient setupHttpClient()
    {
        HttpClientParams params = new HttpClientParams();
        params.setVersion(HttpVersion.HTTP_1_0);
        return new HttpClient(params);
    }

    @Test
    public void testHttp10EnforceNonChunking() throws Exception
    {
        HttpClient client = setupHttpClient();
        MuleClient muleClient = new MuleClient(muleContext);
        GetMethod request = new GetMethod(((InboundEndpoint) muleClient.getMuleContext().getRegistry().lookupObject("inStreaming")).getAddress());
        client.executeMethod(request);
        assertEquals("hello", request.getResponseBodyAsString());
        
        assertNull(request.getResponseHeader(HttpConstants.HEADER_TRANSFER_ENCODING));
        assertNotNull(request.getResponseHeader(HttpConstants.HEADER_CONTENT_LENGTH));
    }
}
