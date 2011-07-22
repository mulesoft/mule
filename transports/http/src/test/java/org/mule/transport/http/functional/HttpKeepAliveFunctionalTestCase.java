/*
 * $Id$
 * --------------------------------------------------------------------------------------
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 *
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.transport.http.functional;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

import org.mule.api.endpoint.InboundEndpoint;
import org.mule.module.client.MuleClient;
import org.mule.tck.AbstractServiceAndFlowTestCase;
import org.mule.tck.junit4.rule.DynamicPort;
import org.mule.transport.http.HttpConstants;

import java.util.Arrays;
import java.util.Collection;

import org.apache.commons.httpclient.Header;
import org.apache.commons.httpclient.HttpClient;
import org.apache.commons.httpclient.HttpMethod;
import org.apache.commons.httpclient.HttpStatus;
import org.apache.commons.httpclient.HttpVersion;
import org.apache.commons.httpclient.methods.GetMethod;
import org.apache.commons.httpclient.params.HttpClientParams;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runners.Parameterized.Parameters;

/**
 * Tests as per http://www.io.com/~maus/HttpKeepAlive.html
 */
public class HttpKeepAliveFunctionalTestCase extends AbstractServiceAndFlowTestCase
{
    
    private HttpClient http10Client;
    private HttpClient http11Client;
    private MuleClient client = null;

    @Rule
    public DynamicPort dynamicPort1 = new DynamicPort("port1");

    @Rule
    public DynamicPort dynamicPort2 = new DynamicPort("port2");
    
    public HttpKeepAliveFunctionalTestCase(ConfigVariant variant, String configResources)
    {
        super(variant, configResources);
    }

    @Parameters
    public static Collection<Object[]> parameters()
    {
        return Arrays.asList(new Object[][]{
            {ConfigVariant.SERVICE, "http-keep-alive-config-service.xml"},
            {ConfigVariant.FLOW, "http-keep-alive-config-flow.xml"}
        });
    }      
    
    @Override
    protected void doSetUp() throws Exception
    {
        super.doSetUp();

        http10Client = setupHttpClient(HttpVersion.HTTP_1_0);
        http11Client = setupHttpClient(HttpVersion.HTTP_1_1);
        client = new MuleClient(muleContext);
    }
    
    private HttpClient setupHttpClient(HttpVersion version)
    {
        HttpClientParams params = new HttpClientParams();
        params.setVersion(version);
        return new HttpClient(params);
    }

    @Test
    public void testHttp10WithoutConnectionHeader() throws Exception
    {
        GetMethod request = new GetMethod(((InboundEndpoint) client.getMuleContext().getRegistry().lookupObject("inWithoutEndpointOverride")).getAddress());
        request.removeRequestHeader(HttpConstants.HEADER_CONNECTION);
        runHttp10MethodAndAssertConnectionHeader(request, "close");
    }
    
    @Test
    public void testHttp10WithCloseConnectionHeader() throws Exception
    {
        GetMethod request = new GetMethod(((InboundEndpoint) client.getMuleContext().getRegistry().lookupObject("inWithoutEndpointOverride")).getAddress());
        request.setRequestHeader(HttpConstants.HEADER_CONNECTION, "close");
        runHttp10MethodAndAssertConnectionHeader(request, "close");
    }
    
    @Test
    public void testHttp10KeepAlive() throws Exception
    {
        doTestKeepAlive(((InboundEndpoint) client.getMuleContext().getRegistry().lookupObject("inWithoutEndpointOverride")).getAddress());
    }
    
    @Test
    public void testHttp10KeepAliveWitEpOverride() throws Exception
    {
        doTestKeepAlive(((InboundEndpoint) client.getMuleContext().getRegistry().lookupObject("inWithoutEndpointOverride")).getAddress());
    }
    
    private void doTestKeepAlive(String url) throws Exception
    {
        GetMethod request = new GetMethod(url);
        request.addRequestHeader(HttpConstants.HEADER_CONNECTION, "Keep-Alive");
        runHttp10MethodAndAssertConnectionHeader(request, "Keep-Alive");
        
        request.setRequestHeader(HttpConstants.HEADER_CONNECTION, "close");
        runHttp10MethodAndAssertConnectionHeader(request, "close");
    }
    
    private void runHttp10MethodAndAssertConnectionHeader(HttpMethod request, String expectedConnectionHeaderValue) throws Exception
    {
        int status = http10Client.executeMethod(request);
        assertEquals(HttpStatus.SC_OK, status);
        String connectionHeader = request.getResponseHeader(HttpConstants.HEADER_CONNECTION).getValue();
        assertNotNull(connectionHeader);
        assertEquals(expectedConnectionHeaderValue, connectionHeader);
    }
    
    @Test
    public void testHttp11KeepAlive() throws Exception
    {
        doTestHttp11KeepAlive(((InboundEndpoint) client.getMuleContext().getRegistry().lookupObject("inWithoutEndpointOverride")).getAddress());
    }
    
    @Test
    public void testHttp11KeepAliveWithEpOverride() throws Exception
    {
        doTestHttp11KeepAlive(((InboundEndpoint) client.getMuleContext().getRegistry().lookupObject("inWithoutEndpointOverride")).getAddress());
    }
    
    public void doTestHttp11KeepAlive(String url) throws Exception
    {
        GetMethod request = new GetMethod(url);
        runHttp11MethodAndAssert(request);
        
        // the connection should be still open, send another request and terminate the connection
        request = new GetMethod(url);
        request.setRequestHeader(HttpConstants.HEADER_CONNECTION, "close");
        runHttp11MethodAndAssert(request);
        
        Header connectHeader = request.getResponseHeader(HttpConstants.HEADER_CONNECTION);
        assertNotNull(connectHeader);
        assertEquals("close", connectHeader.getValue());
    }
        
    private void runHttp11MethodAndAssert(HttpMethod request) throws Exception
    {
        int status = http11Client.executeMethod(request);
        assertEquals(HttpStatus.SC_OK, status);
        assertEquals("/http-in", request.getResponseBodyAsString());
    }

}


