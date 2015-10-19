/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.routing.outbound;

import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.instanceOf;
import static org.junit.Assert.assertThat;
import org.mule.api.MuleMessage;
import org.mule.api.client.LocalMuleClient;
import org.mule.tck.junit4.FunctionalTestCase;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;

import org.junit.Test;

public class AggregationTimeoutTestCase extends FunctionalTestCase
{

    private static final CountDownLatch blockExecution = new CountDownLatch(1);
    public static final String PROCESS_EVENT = "process";
    public static final String BLOCK_EVENT = "block";
    public static final String PROCESSED_EVENT = "processed";

    @Override
    protected String getConfigFile()
    {
        return "org/mule/test/integration/routing/outbound/aggregation-timeout-config.xml";
    }

    @Test
    public void timeoutsAggregationWithPersistentStore() throws Exception
    {
        List<String> inputData = new ArrayList<>();
        inputData.add(PROCESS_EVENT);
        inputData.add(BLOCK_EVENT);

        try
        {
            LocalMuleClient client = muleContext.getClient();
            client.dispatch("vm://testIn", inputData, null);

            MuleMessage response = client.request("vm://testOut", RECEIVE_TIMEOUT);
            assertThat(response.getPayload(), instanceOf(List.class));

            List<String> payload = (List) response.getPayload();
            assertThat(payload.size(), equalTo(1));
            assertThat(payload, hasItem(PROCESSED_EVENT));
        }
        finally
        {
            // Release the blocked thread
            blockExecution.countDown();
        }
    }

    public static class BlockExecutionComponent
    {
        public Object onCall(Object payload) throws Exception
        {
            if (payload.equals(BLOCK_EVENT))
            {
                blockExecution.await();
            }

            return PROCESSED_EVENT;
        }
    }
}
