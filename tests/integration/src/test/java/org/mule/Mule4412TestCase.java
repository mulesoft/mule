/*
 * (c) 2003-2014 MuleSoft, Inc. This software is protected under international copyright
 * law. All use of this software is subject to MuleSoft's Master Subscription Agreement
 * (or other master license agreement) separately entered into in writing between you and
 * MuleSoft. If such an agreement is not in place, you may not use the software.
 */
package org.mule;

import org.mule.api.MuleMessage;
import org.mule.api.transport.PropertyScope;
import org.mule.module.client.MuleClient;
import org.mule.tck.AbstractServiceAndFlowTestCase;
import org.mule.test.filters.FilterCounter;

import java.util.Arrays;
import java.util.Collection;

import org.junit.Test;
import org.junit.runners.Parameterized.Parameters;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

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
        DefaultMuleMessage msg = new DefaultMuleMessage(TEST_MESSAGE, muleContext);
        msg.setOutboundProperty("pass", "true");
        MuleClient client = new MuleClient(muleContext);
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
        DefaultMuleMessage msg = new DefaultMuleMessage(TEST_MESSAGE, muleContext);
        msg.setProperty("fail", "true", PropertyScope.INVOCATION);
        MuleClient client = new MuleClient(muleContext);
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
        DefaultMuleMessage msg = new DefaultMuleMessage(TEST_MESSAGE, muleContext);
        msg.setInboundProperty("pass", "false");
        MuleClient client = new MuleClient(muleContext);
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
        DefaultMuleMessage msg = new DefaultMuleMessage(TEST_MESSAGE, muleContext);
        MuleClient client = new MuleClient(muleContext);
        client.send("vm://async", msg);
        MuleMessage reply = client.request("vm://asyncResponse", RECEIVE_TIMEOUT_MS);
        assertNull(reply);
        assertTrue("should not have filtered", FilterCounter.counter.get() == 0);
    }
}
