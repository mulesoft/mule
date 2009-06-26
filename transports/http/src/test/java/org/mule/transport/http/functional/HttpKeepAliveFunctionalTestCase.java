/*
 * $Id$
 * --------------------------------------------------------------------------------------
 * Copyright (c) MuleSource, Inc.  All rights reserved.  http://www.mulesource.com
 *
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.transport.http.functional;

import org.mule.tck.FunctionalTestCase;
import org.mule.transport.http.HttpConstants;

import org.apache.commons.httpclient.Header;
import org.apache.commons.httpclient.HttpClient;
import org.apache.commons.httpclient.HttpMethod;
import org.apache.commons.httpclient.HttpStatus;
import org.apache.commons.httpclient.HttpVersion;
import org.apache.commons.httpclient.methods.GetMethod;
import org.apache.commons.httpclient.params.HttpClientParams;

/**
 * Tests as per http://www.io.com/~maus/HttpKeepAlive.html
 */
public class HttpKeepAliveFunctionalTestCase extends FunctionalTestCase
{
    private static final String URL_WITHOUT_EP_OVERRIDE = "http://localhost:60213/http-in";
    private static final String URL_WITH_EP_OVERRIDE = "http://localhost:60214/http-in";
    
    private HttpClient http10Client;
    private HttpClient http11Client;
    
    @Override
    protected void doSetUp() throws Exception
    {
        super.doSetUp();

        http10Client = setupHttpClient(HttpVersion.HTTP_1_0);
        http11Client = setupHttpClient(HttpVersion.HTTP_1_1);
    }
    
    private HttpClient setupHttpClient(HttpVersion version)
    {
        HttpClientParams params = new HttpClientParams();
        params.setVersion(version);
        return new HttpClient(params);
    }

    @Override
    protected String getConfigResources()
    {
        return "http-keep-alive-config.xml";
    }
    
    public void testHttp10WithoutConnectionHeader() throws Exception
    {
        GetMethod request = new GetMethod(URL_WITHOUT_EP_OVERRIDE);
        request.removeRequestHeader(HttpConstants.HEADER_CONNECTION);
        runHttp10MethodAndAssertConnectionHeader(request, "close");
    }
    
    public void testHttp10WithCloseConnectionHeader() throws Exception
    {
        GetMethod request = new GetMethod(URL_WITHOUT_EP_OVERRIDE);
        request.setRequestHeader(HttpConstants.HEADER_CONNECTION, "close");
        runHttp10MethodAndAssertConnectionHeader(request, "close");
    }
    
    public void testHttp10KeepAlive() throws Exception
    {
        doTestKeepAlive(URL_WITHOUT_EP_OVERRIDE);
    }
    
    public void testHttp10KeepAliveWitEpOverride() throws Exception
    {
        doTestKeepAlive(URL_WITH_EP_OVERRIDE);
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
    
    public void testHttp11KeepAlive() throws Exception
    {
        doTestHttp11KeepAlive(URL_WITHOUT_EP_OVERRIDE);
    }
    
    public void testHttp11KeepAliveWithEpOverride() throws Exception
    {
        doTestHttp11KeepAlive(URL_WITH_EP_OVERRIDE);
    }
    
    public void doTestHttp11KeepAlive(String url) throws Exception
    {
        GetMethod request = new GetMethod(url);
        runHttp11MethodAndAssert(request);
        
        // the connection should be still open, send another request and terminate the connection
        request = new GetMethod(URL_WITHOUT_EP_OVERRIDE);
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


