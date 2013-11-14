/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.test.integration.exceptions;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;

import org.mule.api.MuleMessage;

import java.util.concurrent.TimeUnit;

import org.junit.Test;

public class AsynchronousMessagingExceptionStrategyTestCase extends AbstractExceptionStrategyTestCase
{
    @Override
    protected String getConfigFile()
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


