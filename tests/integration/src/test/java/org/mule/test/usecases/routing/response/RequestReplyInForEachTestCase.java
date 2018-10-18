/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.test.usecases.routing.response;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.junit.Assert.assertThat;
import static org.mockito.Mockito.mock;
import org.mule.DefaultMuleMessage;
import org.mule.api.MuleMessage;
import org.mule.api.client.MuleClient;
import org.mule.tck.junit4.FunctionalTestCase;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.junit.Before;
import org.junit.Test;

public abstract class RequestReplyInForEachTestCase extends FunctionalTestCase
{

    protected static final int TIMEOUT = 5000;
    private final List<String> values = new ArrayList<>();

    @Before
    public void setUp() throws Exception
    {
        values.add("value1");
        values.add("value2");
        values.add("value3");
    }

    @Test
    public void testRequestReplyWithForEach() throws Exception
    {
        MuleClient client = muleContext.getClient();
        MuleMessage message = new DefaultMuleMessage(values, mock(Map.class) , muleContext);
        client.dispatch("vm://foreach", message);
        for(String value : values )
        {
            assertResult(client, value + "-processed", "test-foreach-reply");
        }
    }

    @Test
    public void testRequestReplyInSequenceCall() throws Exception
    {
        MuleClient client = muleContext.getClient();
        MuleMessage message = new DefaultMuleMessage("test", mock(Map.class) , muleContext);
        client.dispatch("vm://sequence-call", message);
        assertResult(client, "first-call-processed", "test-sequence-reply");
        assertResult(client, "second-call-processed", "test-sequence-reply");
    }

    private void assertResult(MuleClient client, String payload, String queueName) throws Exception
    {
        MuleMessage reply = client.request("vm://" + queueName, TIMEOUT);
        assertThat(reply, is(notNullValue()));
        assertThat(reply.getPayloadAsString(), is(payload));
    }

}
