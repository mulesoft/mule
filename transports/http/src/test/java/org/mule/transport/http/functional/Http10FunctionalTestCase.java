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

import org.apache.commons.httpclient.HttpClient;
import org.apache.commons.httpclient.HttpStatus;
import org.apache.commons.httpclient.HttpVersion;
import org.apache.commons.httpclient.methods.GetMethod;
import org.apache.commons.httpclient.params.HttpClientParams;

/**
 * Tests as per http://www.io.com/~maus/HttpKeepAlive.html
 */
public class Http10FunctionalTestCase extends FunctionalTestCase
{
    private static final String URL_WITHOUT_EP_OVERRIDE = "http://localhost:60213/http-in";
    private static final String URL_WITH_EP_OVERRIDE = "http://localhost:60214/http-in";
    
    private HttpClient httpClient;
    
    @Override
    protected void doSetUp() throws Exception
    {
        super.doSetUp();

        HttpClientParams params = new HttpClientParams();
        params.setVersion(HttpVersion.HTTP_1_0);
        httpClient = new HttpClient(params);        
    }

    @Override
    protected String getConfigResources()
    {
        return "http-keep-alive-config.xml";
    }
    
    public void testWithoutConnectionHeader() throws Exception
    {
        GetMethod request = new GetMethod(URL_WITHOUT_EP_OVERRIDE);
        request.removeRequestHeader(HttpConstants.HEADER_CONNECTION);
        runHttpMethodAndAssertConnectionHeader(request, "close");
    }
    
    public void testWithCloseConnectionHeader() throws Exception
    {
        GetMethod request = new GetMethod(URL_WITHOUT_EP_OVERRIDE);
        request.setRequestHeader(HttpConstants.HEADER_CONNECTION, "close");
        runHttpMethodAndAssertConnectionHeader(request, "close");
    }
    
    public void testKeepAlive() throws Exception
    {
        doTestKeepAlive(URL_WITHOUT_EP_OVERRIDE);
    }
    
    public void testKeepAliveWitEpOverride() throws Exception
    {
        doTestKeepAlive(URL_WITH_EP_OVERRIDE);
    }
    
    private void doTestKeepAlive(String url) throws Exception
    {
        GetMethod request = new GetMethod(url);
        request.addRequestHeader(HttpConstants.HEADER_CONNECTION, "Keep-Alive");
        runHttpMethodAndAssertConnectionHeader(request, "Keep-Alive");
        
        request.setRequestHeader(HttpConstants.HEADER_CONNECTION, "close");
        runHttpMethodAndAssertConnectionHeader(request, "close");
    }
    
    private void runHttpMethodAndAssertConnectionHeader(GetMethod request, String expectedConnectionHeaderValue) throws Exception
    {
        int status = httpClient.executeMethod(request);
        assertEquals(HttpStatus.SC_OK, status);
        String connectionHeader = request.getResponseHeader(HttpConstants.HEADER_CONNECTION).getValue();
        assertNotNull(connectionHeader);
        assertEquals(expectedConnectionHeaderValue, connectionHeader);
    }
}


