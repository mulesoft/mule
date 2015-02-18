/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import org.mule.api.MuleMessage;
import org.mule.api.client.MuleClient;
import org.mule.api.transport.PropertyScope;
import org.mule.tck.AbstractServiceAndFlowTestCase;
import org.mule.test.filters.FilterCounter;

import java.util.Arrays;
import java.util.Collection;

import org.junit.Test;
import org.junit.runners.Parameterized.Parameters;

/**
 * Test for MULE-4412 : selective-consumer filter is applied twice. We test that the
 * filter is only applied once in the positive case, plus make sure it doesn't get
 * filtered at all when the message does not meet the filter criteria
 */
public class Mule4412TestCase extends AbstractServiceAndFlowTestCase
{
    private int RECEIVE_TIMEOUT_MS = 3000;

    @Parameters
    public static Collection<Object[]> parameters()
    {
        return Arrays.asList(new Object[][]{{ConfigVariant.SERVICE, "mule-4412-service.xml"},
            {ConfigVariant.FLOW, "mule-4412-flow.xml"}});
    }

    public Mule4412TestCase(ConfigVariant variant, String configResources)
    {
        super(variant, configResources);
    }

    @Override
    protected void doSetUp() throws Exception
    {
        super.doSetUp();
        // reset the counter for every test
        FilterCounter.counter.set(0);
    }

    @Override
    protected void doTearDown() throws Exception
    {
        super.doTearDown();
        // reset the counter for every test
        FilterCounter.counter.set(0);
    }

    /**
     * Make sure that the message only gets filtered once
     *
     * @throws Exception
     */
    @Test
    public void testFilterOnce() throws Exception
    {
        MuleMessage msg = getTestMuleMessage(TEST_MESSAGE);
        msg.setOutboundProperty("pass", "true");

        MuleClient client = muleContext.getClient();
        client.send("vm://async", msg);
        MuleMessage reply = client.request("vm://asyncResponse", RECEIVE_TIMEOUT_MS);
        int times = FilterCounter.counter.get();
        assertTrue("did not filter one time as expected, times filtered " + times, times == 1);
        assertNotNull(reply);
        assertEquals("wrong message received : " + reply.getPayloadAsString(), TEST_MESSAGE,
            reply.getPayloadAsString());
        assertEquals("'pass' property value not correct", "true", reply.getInboundProperty("pass"));

        // make sure there are no more messages
        assertNull(client.request("vm://asyncResponse", RECEIVE_TIMEOUT_MS));
    }

    /**
     * Make sure the message does not get filtered when the property key is incorrect
     *
     * @throws Exception
     */
    @Test
    public void testWrongPropertyKey() throws Exception
    {
        MuleMessage msg = getTestMuleMessage(TEST_MESSAGE);
        msg.setProperty("fail", "true", PropertyScope.INVOCATION);

        MuleClient client = muleContext.getClient();
        client.send("vm://async", msg);
        MuleMessage reply = client.request("vm://asyncResponse", RECEIVE_TIMEOUT_MS);
        assertNull(reply);
        assertTrue("should not have filtered", FilterCounter.counter.get() == 0);
    }

    /**
     * Make sure the message does not get filtered when the property value is not as
     * expected
     *
     * @throws Exception
     */
    @Test
    public void testWrongPropertyValue() throws Exception
    {
        MuleMessage msg = getTestMuleMessage(TEST_MESSAGE);
        msg.setProperty("pass", "false", PropertyScope.INBOUND);

        MuleClient client = muleContext.getClient();
        client.send("vm://async", msg);
        MuleMessage reply = client.request("vm://asyncResponse", RECEIVE_TIMEOUT_MS);
        assertNull(reply);
        assertTrue("should not have filtered", FilterCounter.counter.get() == 0);
    }

    /**
     * Make sure the message does not get filtered at all when the expected property
     * is not defined
     *
     * @throws Exception
     */
    @Test
    public void testNoProperty() throws Exception
    {
        MuleMessage msg = getTestMuleMessage(TEST_MESSAGE);

        MuleClient client = muleContext.getClient();
        client.send("vm://async", msg);
        MuleMessage reply = client.request("vm://asyncResponse", RECEIVE_TIMEOUT_MS);
        assertNull(reply);
        assertTrue("should not have filtered", FilterCounter.counter.get() == 0);
    }
}
