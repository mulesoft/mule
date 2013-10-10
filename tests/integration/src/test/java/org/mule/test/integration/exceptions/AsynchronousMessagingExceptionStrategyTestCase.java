/*
 * (c) 2003-2014 MuleSoft, Inc. This software is protected under international copyright
 * law. All use of this software is subject to MuleSoft's Master Subscription Agreement
 * (or other master license agreement) separately entered into in writing between you and
 * MuleSoft. If such an agreement is not in place, you may not use the software.
 */
package org.mule.test.integration.exceptions;

import org.mule.api.MuleMessage;

import java.util.concurrent.TimeUnit;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;

public class AsynchronousMessagingExceptionStrategyTestCase extends AbstractExceptionStrategyTestCase
{
    
    @Override
    protected String getConfigResources()
    {
        return "org/mule/test/integration/exceptions/asynch-messaging-exception-strategy.xml";
    }
    
    @Test
    public void testInboundTransformer() throws Exception
    {
        client.dispatch("vm://in1", TEST_MESSAGE, null);
        latch.await(LATCH_AWAIT_TIMEOUT, TimeUnit.MILLISECONDS); 
        assertEquals(1, serviceExceptionCounter.get());
        assertEquals(0, systemExceptionCounter.get());
    }

    @Test
    public void testInboundResponseTransformer() throws Exception
    {
        client.dispatch("vm://in2", TEST_MESSAGE, null);
        latch.await(LATCH_AWAIT_TIMEOUT, TimeUnit.MILLISECONDS); 
        // No exception expected because response transformer is not applied for an asynchronous endpoint
        assertEquals(0, serviceExceptionCounter.get());
        assertEquals(0, systemExceptionCounter.get());
    }
    
    @Test
    public void testOutboundTransformer() throws Exception
    {
        client.dispatch("vm://in3", TEST_MESSAGE, null);
        latch.await(LATCH_AWAIT_TIMEOUT, TimeUnit.MILLISECONDS); 
        assertEquals(1, serviceExceptionCounter.get());
        assertEquals(0, systemExceptionCounter.get());
        MuleMessage response = client.request("vm://out3", 500);
        assertNull(response);
    }
    
    @Test
    public void testOutboundResponseTransformer() throws Exception
    {
        client.dispatch("vm://in4", TEST_MESSAGE, null);
        latch.await(LATCH_AWAIT_TIMEOUT, TimeUnit.MILLISECONDS); 
        // No exception expected because response transformer is not applied for an asynchronous endpoint
        assertEquals(0, serviceExceptionCounter.get());
        assertEquals(0, systemExceptionCounter.get());
        MuleMessage response = client.request("vm://out4", 500);
        assertNotNull(response);
    }
    
    @Test
    public void testComponent() throws Exception
    {
        client.dispatch("vm://in5", TEST_MESSAGE, null);
        latch.await(LATCH_AWAIT_TIMEOUT, TimeUnit.MILLISECONDS); 
        assertEquals(1, serviceExceptionCounter.get());
        assertEquals(0, systemExceptionCounter.get());
    }

    @Test
    public void testInboundRouter() throws Exception
    {
        client.dispatch("vm://in6", TEST_MESSAGE, null);
        latch.await(LATCH_AWAIT_TIMEOUT, TimeUnit.MILLISECONDS); 
        assertEquals(1, serviceExceptionCounter.get());
        assertEquals(0, systemExceptionCounter.get());
    }
    
    @Test
    public void testOutboundRouter() throws Exception
    {
        client.dispatch("vm://in7", TEST_MESSAGE, null);
        latch.await(LATCH_AWAIT_TIMEOUT, TimeUnit.MILLISECONDS); 
        assertEquals(1, serviceExceptionCounter.get());
        assertEquals(0, systemExceptionCounter.get());
        MuleMessage response = client.request("vm://out7", 500);
        assertNull(response);
    }
}


