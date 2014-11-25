/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.test.integration.async;

import static org.hamcrest.CoreMatchers.containsString;
import static org.hamcrest.CoreMatchers.instanceOf;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.not;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.hamcrest.CoreMatchers.nullValue;
import static org.junit.Assert.assertThat;
import org.mule.api.MuleMessage;
import org.mule.api.client.MuleClient;
import org.mule.tck.junit4.FunctionalTestCase;
import org.mule.tck.listener.FlowExecutionListener;
import org.mule.transport.NullPayload;

import org.junit.Test;

public class SedaStageWorkRejectionTestCase extends FunctionalTestCase
{

    @Override
    protected String getConfigFile()
    {
        return "org/mule/test/integration/async/seda-stage-work-rejection-config-flow.xml";
    }

    @Test
    public void handleWorkRejectedWithExceptionStrategy() throws Exception
    {
        FlowExecutionListener flowExecutionListener = new FlowExecutionListener("limitedThreadsFlow", muleContext).setTimeoutInMillis(5000).setNumberOfExecutionsRequired(3);
        testThirdMessageSendToExceptionStrategy("vm://flow1.in", flowExecutionListener);
    }

    @Test
    public void handleQueueFullWithExceptionStrategy() throws Exception
    {
        FlowExecutionListener flowExecutionListener = new FlowExecutionListener("limitedQueueFlow", muleContext).setTimeoutInMillis(5000).setNumberOfExecutionsRequired(3);
        testThirdMessageSendToExceptionStrategy("vm://flow2.in", flowExecutionListener);
    }

    protected void testThirdMessageSendToExceptionStrategy(String inUrl, FlowExecutionListener flowExecutionListener) throws Exception
    {
        // Send 3 messages
        MuleClient client = muleContext.getClient();
        int nrMessages = 3;
        for (int i = 0; i < nrMessages; i++)
        {
            client.dispatch(inUrl, TEST_MESSAGE + i, null);
        }
        flowExecutionListener.waitUntilFlowIsComplete();
        // Receive 2 messages
        for (int i = 0; i < 2; i++)
        {
            MuleMessage result = client.request("vm://out", RECEIVE_TIMEOUT);
            assertThat(result, is(notNullValue()));
            assertThat(result.getExceptionPayload(), is(nullValue()));
            assertThat(result.getPayload(), not(instanceOf(NullPayload.class)));
            assertThat(result.getPayloadAsString(), containsString(TEST_MESSAGE));
        }

        // Third message doesn't arrive
        assertThat(client.request("vm://out", RECEIVE_TIMEOUT / 5), is(nullValue()));

        // Third message was routed via exception strategy
        MuleMessage result = client.request("vm://exception", RECEIVE_TIMEOUT);
        assertThat(result, is(notNullValue()));
    }
}
