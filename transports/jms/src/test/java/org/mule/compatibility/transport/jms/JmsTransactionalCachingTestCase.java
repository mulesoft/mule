/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.compatibility.transport.jms;

import static org.hamcrest.core.IsCollectionContaining.hasItems;
import static org.hamcrest.core.IsEqual.equalTo;
import static org.junit.Assert.assertThat;

import org.mule.functional.junit4.FunctionalTestCase;
import org.mule.runtime.api.message.NullPayload;
import org.mule.runtime.core.api.MuleMessage;
import org.mule.runtime.core.api.client.MuleClient;

import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;

import org.junit.Test;

/**
 * Tests that JMS message are correctly sent when caching elements and use transactions
 */
public class JmsTransactionalCachingTestCase extends FunctionalTestCase
{

    public static final String TEST_MESSAGE_1 = "test1";
    public static final String TEST_MESSAGE_2 = "test2";
    public static final String TEST_MESSAGE_3 = "test3";

    @Override
    protected String getConfigFile()
    {
        return "jms-transactional-caching-config.xml";
    }

    @Test
    public void cachesSession() throws Exception
    {
        MuleClient client = muleContext.getClient();

        MuleMessage response = client.send("vm://testInput", TEST_MESSAGE_1, null);
        assertThat(TEST_MESSAGE_1, equalTo(getPayloadAsString(response)));
        response = client.send("vm://testInput", TEST_MESSAGE_2, null);
        assertThat(NullPayload.getInstance(), equalTo(response.getPayload()));
        response = client.send("vm://testInput", TEST_MESSAGE_3, null);
        assertThat(TEST_MESSAGE_3, equalTo(getPayloadAsString(response)));

        Set<String> responses = new HashSet<String>();
        response = client.request("vm://testOut", RECEIVE_TIMEOUT);
        responses.add(getPayloadAsString(response));
        response = client.request("vm://testOut", RECEIVE_TIMEOUT);
        responses.add(getPayloadAsString(response));

        assertThat(responses, hasItems(equalTo(TEST_MESSAGE_1), equalTo(TEST_MESSAGE_3)));
    }

    public static class AbortMessageOnEventCount
    {
        private static AtomicInteger counter = new AtomicInteger(0);

        public Object process(Object payload)
        {
            final int currentCounter = counter.incrementAndGet();

            if (currentCounter % 2 == 0)
            {
                throw new RuntimeException("Expected exception to abort the transaction during the test");
            }

            return payload;
        }
    }

}
