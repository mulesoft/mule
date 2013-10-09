/*
 * (c) 2003-2014 MuleSoft, Inc. This software is protected under international copyright
 * law. All use of this software is subject to MuleSoft's Master Subscription Agreement
 * (or other master license agreement) separately entered into in writing between you and
 * MuleSoft. If such an agreement is not in place, you may not use the software.
 */
package org.mule.test.integration.exceptions;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.fail;

import org.mule.api.MuleException;
import org.mule.api.MuleMessage;

import java.util.concurrent.TimeUnit;

import org.junit.Test;

public class SynchronousMessagingExceptionStrategyTestCase extends AbstractExceptionStrategyTestCase
{

    @Override
    protected String getConfigResources()
    {
        return "org/mule/test/integration/exceptions/synch-messaging-exception-strategy.xml";
    }

    @Test
    public void testInboundTransformer() throws Exception
    {
        client.send("vm://in1", TEST_MESSAGE, null);
        latch.await(LATCH_AWAIT_TIMEOUT, TimeUnit.MILLISECONDS);
        assertEquals(1, serviceExceptionCounter.get());
        assertEquals(0, systemExceptionCounter.get());
    }

    @Test
    public void testInboundResponseTransformer() throws Exception
    {
        client.send("vm://in2", TEST_MESSAGE, null);
        latch.await(LATCH_AWAIT_TIMEOUT, TimeUnit.MILLISECONDS);
        assertEquals(1, serviceExceptionCounter.get());
        assertEquals(0, systemExceptionCounter.get());
    }

    @Test
    public void testOutboundTransformer() throws Exception
    {
        client.send("vm://in3", TEST_MESSAGE, null);
        latch.await(LATCH_AWAIT_TIMEOUT, TimeUnit.MILLISECONDS);
        assertEquals(1, serviceExceptionCounter.get());
        assertEquals(0, systemExceptionCounter.get());
        MuleMessage response = client.request("vm://out3", 500);
        assertNull(response);
    }

    @Test
    public void testOutboundResponseTransformer() throws Exception
    {
        client.send("vm://in4", TEST_MESSAGE, null);
        latch.await(LATCH_AWAIT_TIMEOUT, TimeUnit.MILLISECONDS);
        assertEquals(1, serviceExceptionCounter.get());
        assertEquals(0, systemExceptionCounter.get());
        MuleMessage response = client.request("vm://out4", 500);
        assertNull(response);
    }

    @Test
    public void testComponent() throws Exception
    {
        client.send("vm://in5", TEST_MESSAGE, null);
        latch.await(LATCH_AWAIT_TIMEOUT, TimeUnit.MILLISECONDS);
        assertEquals(1, serviceExceptionCounter.get());
        assertEquals(0, systemExceptionCounter.get());
    }

    @Test
    public void testInboundRouter() throws Exception
    {
        client.send("vm://in6", TEST_MESSAGE, null);
        latch.await(LATCH_AWAIT_TIMEOUT, TimeUnit.MILLISECONDS);
        assertEquals(1, serviceExceptionCounter.get());
        assertEquals(0, systemExceptionCounter.get());
    }

    @Test
    public void testOutboundRouter() throws Exception
    {
        client.send("vm://in7", TEST_MESSAGE, null);
        latch.await(LATCH_AWAIT_TIMEOUT, TimeUnit.MILLISECONDS);
        assertEquals(1, serviceExceptionCounter.get());
        assertEquals(0, systemExceptionCounter.get());
        MuleMessage response = client.request("vm://out7", 500);
        assertNull(response);
    }
}
