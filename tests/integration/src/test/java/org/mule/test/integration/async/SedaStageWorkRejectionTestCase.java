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
import org.mule.functional.junit4.FunctionalTestCase;
import org.mule.functional.listener.FlowExecutionListener;
import org.mule.api.message.NullPayload;

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
        testThirdMessageSendToExceptionStrategy("limitedThreadsFlow", flowExecutionListener);
    }

    @Test
    public void handleQueueFullWithExceptionStrategy() throws Exception
    {
        FlowExecutionListener flowExecutionListener = new FlowExecutionListener("limitedQueueFlow", muleContext).setTimeoutInMillis(5000).setNumberOfExecutionsRequired(3);
        testThirdMessageSendToExceptionStrategy("limitedQueueFlow", flowExecutionListener);
    }

    protected void testThirdMessageSendToExceptionStrategy(String flowName, FlowExecutionListener flowExecutionListener) throws Exception
    {
        // Send 3 messages
        MuleClient client = muleContext.getClient();
        int nrMessages = 3;
        for (int i = 0; i < nrMessages; i++)
        {
            flowRunner(flowName).withPayload(TEST_MESSAGE + i).asynchronously().run();
        }
        flowExecutionListener.waitUntilFlowIsComplete();
        // Receive 2 messages
        for (int i = 0; i < 2; i++)
        {
            MuleMessage result = client.request("test://out", RECEIVE_TIMEOUT);
            assertThat(result, is(notNullValue()));
            assertThat(result.getExceptionPayload(), is(nullValue()));
            assertThat(result.getPayload(), not(instanceOf(NullPayload.class)));
            assertThat(getPayloadAsString(result), containsString(TEST_MESSAGE));
        }

        // Third message doesn't arrive
        assertThat(client.request("test://out", RECEIVE_TIMEOUT / 5), is(nullValue()));

        // Third message was routed via exception strategy
        MuleMessage result = client.request("test://exception", RECEIVE_TIMEOUT);
        assertThat(result, is(notNullValue()));
    }
}
