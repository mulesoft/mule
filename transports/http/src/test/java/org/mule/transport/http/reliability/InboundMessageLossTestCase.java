/*
 * $Id$
 * --------------------------------------------------------------------------------------
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 *
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.transport.http.reliability;

import org.mule.exception.DefaultSystemExceptionStrategy;
import org.mule.routing.filters.WildcardFilter;
import org.mule.tck.DynamicPortTestCase;
import org.mule.transport.http.HttpConstants;

import org.apache.commons.httpclient.HttpClient;
import org.apache.commons.httpclient.HttpMethodBase;
import org.apache.commons.httpclient.methods.GetMethod;

public class InboundMessageLossTestCase extends DynamicPortTestCase
{
    protected HttpClient httpClient = new HttpClient();
    
    @Override
    protected String getConfigResources()
    {
        return "reliability/inbound-message-loss.xml";
    }

    @Override
    protected void doSetUp() throws Exception
    {
        super.doSetUp();
        
        // Set SystemExceptionStrategy to redeliver messages (this can only be configured programatically for now)
        ((DefaultSystemExceptionStrategy) muleContext.getExceptionListener()).setRollbackTxFilter(new WildcardFilter("*"));
    }

    public void testNoException() throws Exception
    {
        HttpMethodBase request = createRequest(getBaseUri() + "/noException");
        int status = httpClient.executeMethod(request);
        assertEquals(HttpConstants.SC_OK, status);
        assertEquals("Here you go", request.getResponseBodyAsString());
    }
    
    public void testTransformerException() throws Exception
    {
        HttpMethodBase request = createRequest(getBaseUri() + "/transformerException");
        int status = httpClient.executeMethod(request);
        assertEquals(HttpConstants.SC_INTERNAL_SERVER_ERROR, status);
        assertTrue(request.getResponseBodyAsString().contains("Failure"));
    }
    
    public void testRouterException() throws Exception
    {
        HttpMethodBase request = createRequest(getBaseUri() + "/routerException");
        int status = httpClient.executeMethod(request);
        assertEquals(HttpConstants.SC_INTERNAL_SERVER_ERROR, status);
        assertTrue(request.getResponseBodyAsString().contains("Failure"));
    }
    
    public void testComponentException() throws Exception
    {
        HttpMethodBase request = createRequest(getBaseUri() + "/componentException");
        int status = httpClient.executeMethod(request);
        // Component exception occurs after the SEDA queue for an asynchronous request, but since 
        // this request is synchronous, the failure propagates back to the client.
        assertEquals(HttpConstants.SC_INTERNAL_SERVER_ERROR, status);
        assertTrue(request.getResponseBodyAsString().contains("exception"));
    }    

    protected HttpMethodBase createRequest(String uri)
    {
        return new GetMethod(uri);
    }
    
    protected String getBaseUri()
    {
        return "http://localhost:" + getPorts().get(0);
    }
    
    @Override
    protected int getNumPortsToFind()
    {
        return 1;
    }
}
