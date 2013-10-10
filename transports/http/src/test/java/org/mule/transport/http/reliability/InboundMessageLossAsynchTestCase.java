/*
 * (c) 2003-2014 MuleSoft, Inc. This software is protected under international copyright
 * law. All use of this software is subject to MuleSoft's Master Subscription Agreement
 * (or other master license agreement) separately entered into in writing between you and
 * MuleSoft. If such an agreement is not in place, you may not use the software.
 */
package org.mule.transport.http.reliability;

import org.junit.Test;
import org.mule.transport.http.HttpConstants;

import org.apache.commons.httpclient.HttpMethodBase;
import org.apache.commons.httpclient.methods.PostMethod;

import static org.junit.Assert.assertEquals;

/**
 * Verify that no inbound messages are lost when exceptions occur.  
 * The message must either make it all the way to the SEDA queue (in the case of 
 * an asynchronous inbound endpoint), or be restored/rolled back at the source.
 * 
 * In the case of the HTTP transport, there is no way to restore the source message
 * so an exception is simply returned to the client.
 */
public class InboundMessageLossAsynchTestCase extends InboundMessageLossTestCase
{
    @Override
    protected String getConfigResources()
    {
        return "reliability/inbound-message-loss-asynch.xml";
    }

    @Test
    @Override
    public void testNoException() throws Exception
    {
        HttpMethodBase request = createRequest(getBaseUri() + "/noException");
        int status = httpClient.executeMethod(request);
        assertEquals(HttpConstants.SC_OK, status);
    }

    @Test
    @Override
    public void testHandledTransformerException() throws Exception
    {
        HttpMethodBase request = createRequest(getBaseUri() + "/handledTransformerException");
        int status = httpClient.executeMethod(request);
        assertEquals(HttpConstants.SC_OK, status);    }

    @Test
    @Override
    public void testComponentException() throws Exception
    {
        HttpMethodBase request = createRequest(getBaseUri() + "/componentException");
        int status = httpClient.executeMethod(request);
        // Component exception occurs after the SEDA queue for an asynchronous request, so from the client's
        // perspective, the message has been delivered successfully.
        assertEquals(HttpConstants.SC_OK, status);
    }    

    @Override
    protected HttpMethodBase createRequest(String uri)
    {
        return new PostMethod(uri);
    }    
}
