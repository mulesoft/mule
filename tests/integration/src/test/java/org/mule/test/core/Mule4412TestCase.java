/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.test.core;

import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertThat;
import org.mule.test.AbstractIntegrationTestCase;
import org.mule.runtime.core.api.MuleMessage;
import org.mule.runtime.core.api.client.MuleClient;
import org.mule.test.filters.FilterCounter;

import org.junit.Test;

/**
 * Test for MULE-4412 : selective-consumer filter is applied twice. We test that the
 * filter is only applied once in the positive case, plus make sure it doesn't get
 * filtered at all when the message does not meet the filter criteria
 */
public class Mule4412TestCase extends AbstractIntegrationTestCase
{
    @Override
    protected String getConfigFile()
    {
        return "mule-4412-flow.xml";
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
        MuleClient client = muleContext.getClient();
        flowRunner("AsyncRequest").withPayload(TEST_MESSAGE)
                                  .withInboundProperty("pass", "true")
                                  .asynchronously()
                                  .run();

        MuleMessage reply = client.request("test://asyncResponse", RECEIVE_TIMEOUT);
        int times = FilterCounter.counter.get();
        assertThat("did not filter one time as expected", times, is(1));
        assertNotNull(reply);
        assertThat("wrong message received", getPayloadAsString(reply), is(TEST_MESSAGE));
        assertThat("'pass' property value not correct", reply.getInboundProperty("pass"), is("true"));

        // make sure there are no more messages
        assertNull(client.request("test://asyncResponse", RECEIVE_TIMEOUT));
    }

    /**
     * Make sure the message does not get filtered when the property key is incorrect
     *
     * @throws Exception
     */
    @Test
    public void testWrongPropertyKey() throws Exception
    {
        MuleClient client = muleContext.getClient();
        flowRunner("AsyncRequest").withPayload(TEST_MESSAGE)
                                  .withInboundProperty("fail", "true")
                                  .asynchronously()
                                  .run();
        MuleMessage reply = client.request("test://asyncResponse", RECEIVE_TIMEOUT);
        assertNull(reply);
        assertThat("should not have filtered", FilterCounter.counter.get(), is(0));
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
        MuleClient client = muleContext.getClient();
        flowRunner("AsyncRequest").withPayload(TEST_MESSAGE)
                                  .withInboundProperty("pass", "false")
                                  .asynchronously()
                                  .run();

        MuleMessage reply = client.request("test://asyncResponse", RECEIVE_TIMEOUT);
        assertNull(reply);
        assertThat("should not have filtered", FilterCounter.counter.get(), is(0));
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
        MuleClient client = muleContext.getClient();
        flowRunner("AsyncRequest").withPayload(TEST_MESSAGE).asynchronously().run();
        MuleMessage reply = client.request("test://asyncResponse", RECEIVE_TIMEOUT);
        assertNull(reply);
        assertThat("should not have filtered", FilterCounter.counter.get(), is(0));
    }
}
