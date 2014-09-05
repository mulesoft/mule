/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.test.integration.async;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
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
    public void handleRejectedEventWithExceptionStrategy() throws Exception
    {
        FlowExecutionListener flowExecutionListener = new FlowExecutionListener("sedaFlowCrash", muleContext).setTimeoutInMillis(5000).setNumberOfExecutionsRequired(3);
        // Send 3 messages
        MuleClient client = muleContext.getClient();
        int nrMessages = 3;
        for (int i = 0; i < nrMessages; i++)
        {
            client.dispatch("vm://flow.in", "some data " + i, null);
        }
        flowExecutionListener.waitUntilFlowIsComplete();
        // Receive 2 messages
        for (int i = 0; i < 2; i++)
        {
            MuleMessage result = client.request("vm://flow.out", RECEIVE_TIMEOUT);
            assertNotNull(result);
            assertNull(result.getExceptionPayload());
            assertFalse(result.getPayload() instanceof NullPayload);

            assertTrue(result.getPayloadAsString().contains("some data"));
        }

        // Third message doesn't arrive
        assertNull(client.request("vm://flow.out", RECEIVE_TIMEOUT / 5));

        // Third message was router via exception strategy
        MuleMessage result = client.request("vm://flow.exception", RECEIVE_TIMEOUT);
        assertNotNull(result);
    }
}
