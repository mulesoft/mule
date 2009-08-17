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

import org.apache.commons.httpclient.HttpClient;
import org.apache.commons.httpclient.HttpVersion;
import org.apache.commons.httpclient.methods.GetMethod;
import org.apache.commons.httpclient.params.HttpClientParams;
import org.mule.tck.FunctionalTestCase;
import org.mule.transport.http.HttpConstants;

/**
 * Tests as per http://www.io.com/~maus/HttpKeepAlive.html
 */
public class Http10FunctionalTestCase extends FunctionalTestCase
{
    private HttpClient setupHttpClient()
    {
        HttpClientParams params = new HttpClientParams();
        params.setVersion(HttpVersion.HTTP_1_0);
        return new HttpClient(params);
    }

    @Override
    protected String getConfigResources()
    {
        return "http-10-config.xml";
    }
    
    public void testHttp10EnforceNonChunking() throws Exception
    {
    	HttpClient client = setupHttpClient();
    	
        GetMethod request = new GetMethod("http://localhost:60213/streaming");
        client.executeMethod(request);
        assertEquals("hello", request.getResponseBodyAsString());
        
        assertNull(request.getResponseHeader(HttpConstants.HEADER_TRANSFER_ENCODING));
        assertNotNull(request.getResponseHeader(HttpConstants.HEADER_CONTENT_LENGTH));
    }
}


