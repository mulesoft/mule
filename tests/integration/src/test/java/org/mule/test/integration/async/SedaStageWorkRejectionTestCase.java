/*
 * (c) 2003-2014 MuleSoft, Inc. This software is protected under international copyright
 * law. All use of this software is subject to MuleSoft's Master Subscription Agreement
 * (or other master license agreement) separately entered into in writing between you and
 * MuleSoft. If such an agreement is not in place, you may not use the software.
 */
package org.mule.test.integration.async;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import org.mule.api.MuleMessage;
import org.mule.api.client.MuleClient;
import org.mule.tck.AbstractServiceAndFlowTestCase;
import org.mule.transport.NullPayload;

import java.util.Arrays;
import java.util.Collection;

import org.junit.Test;
import org.junit.runners.Parameterized.Parameters;

public class SedaStageWorkRejectionTestCase extends AbstractServiceAndFlowTestCase
{
    public SedaStageWorkRejectionTestCase(ConfigVariant variant, String configResources)
    {
        super(variant, configResources);
    }

    @Parameters
    public static Collection<Object[]> parameters()
    {
        return Arrays.asList(new Object[][]{
            {ConfigVariant.SERVICE,
                "org/mule/test/integration/async/seda-stage-work-rejection-config-service.xml"},
            {ConfigVariant.FLOW, "org/mule/test/integration/async/seda-stage-work-rejection-config-flow.xml"}});
    }

    @Test
    public void handleRejectedEventWithExceptionStrategy() throws Exception
    {
        // Send 3 messages
        MuleClient client = muleContext.getClient();
        int nrMessages = 3;
        for (int i = 0; i < nrMessages; i++)
        {
            client.dispatch("vm://flow.in", "some data " + i, null);
        }

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
