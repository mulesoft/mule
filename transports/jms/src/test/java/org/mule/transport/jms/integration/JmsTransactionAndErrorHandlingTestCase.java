/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.transport.jms.integration;

import org.junit.Test;
import org.mule.DefaultMuleMessage;
import org.mule.api.MuleMessage;
import org.mule.module.client.MuleClient;
import org.mule.transport.jms.ErrorTransformer;
import org.mule.transport.jms.FlowTransformer;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.mule.tck.functional.FlowAssert.verify;

/**
 * Tests the correct propagation of the correlation id property within the JMS transport. This test is related to MULE-6577.
 */
public class JmsTransactionAndErrorHandlingTestCase extends AbstractJmsFunctionalTestCase
{
    public static final int SHORT_TIMEOUT = 1000;
    public static final int MID_TIMEOUT = 2000;
    public static final int LONG_TIMEOUT = 5500;

    @Override
    protected String getConfigFile()
    {
        return "integration/jms-transaction-error-handling.xml";
    }

    @Test
    public void testEverythingWorks() throws Exception {
        FlowTransformer.crash = false;

        MuleClient client =  new MuleClient(muleContext);

        MuleMessage message = new DefaultMuleMessage("Hello", muleContext);
        client.dispatch("vm://in", message);
        MuleMessage result = client.request("vm://out", MID_TIMEOUT);
        assertNull(client.request("vm://error", SHORT_TIMEOUT));
        String payload = result.getPayloadAsString();
        assertEquals("Hello, world!", payload);

        // The transaction was not rolled back
        assertNotNull(client.request("vm://check-transacted", SHORT_TIMEOUT));
    }

    @Test
    public void testEverythingWorksSimple() throws Exception {
        FlowTransformer.crash = false;

        MuleClient client =  new MuleClient(muleContext);

        MuleMessage message = new DefaultMuleMessage("Hello", muleContext);
        client.dispatch("vm://in-simple", message);
        MuleMessage result = client.request("vm://out-simple", MID_TIMEOUT);
        assertNull(client.request("vm://error", LONG_TIMEOUT));
        String payload = result.getPayloadAsString();
        assertEquals("Hello, world!", payload);

        // The transaction was not rolled back
        assertNotNull(client.request("vm://check-transacted", SHORT_TIMEOUT));
    }

    @Test
    public void testErrorInUseCase() throws Exception {
        FlowTransformer.crash = true;
        ErrorTransformer.crash = false;

        MuleClient client =  new MuleClient(muleContext);

        MuleMessage message = new DefaultMuleMessage("Hello", muleContext);
        client.dispatch("vm://in", message);
        assertNull(client.request("vm://out", MID_TIMEOUT));
        MuleMessage result = client.request("vm://error", SHORT_TIMEOUT);
        String payload = result.getPayloadAsString();
        assertEquals("An error occurred!", payload);

        // The transaction was not rolled back
        assertNotNull(client.request("vm://check-transacted", SHORT_TIMEOUT));
    }

    /**
     * There should be one redelivery attempt, then the original message should
     * be routed to the ActiveMQ dead letter queue.
     */
    @Test
    public void testErrorInUseCaseSimple() throws Exception {
        FlowTransformer.crash = true;

        MuleClient client =  new MuleClient(muleContext);

        MuleMessage message = new DefaultMuleMessage("Hello", muleContext);
        client.dispatch("vm://in-simple", message);
        assertNull(client.request("vm://out-simple", MID_TIMEOUT));

        // The transaction was rolled back
        assertNull(client.request("vm://check-transacted", SHORT_TIMEOUT));
    }

    /**
     * There should be one redelivery attempt, then the original message should
     * be routed to the ActiveMQ dead letter queue.
     */
    @Test
    public void testErrorInUseCaseAndInExceptionHandler() throws Exception {
        FlowTransformer.crash = true;
        ErrorTransformer.crash = true;

        MuleClient client =  new MuleClient(muleContext);

        MuleMessage message = new DefaultMuleMessage("Hello", muleContext);
        client.dispatch("vm://in", message);
        assertNull(client.request("vm://out", SHORT_TIMEOUT));

        // The transaction was rolled back
        assertNull(client.request("vm://check-transacted", SHORT_TIMEOUT));
    }

    /**
     * There should be one redelivery attempt, then the original message should
     * be routed to the ActiveMQ dead letter queue.
     */
    @Test
    public void testErrorInUseCaseThroughFlowrefAndInExceptionHandler() throws Exception {
        FlowTransformer.crash = true;
        ErrorTransformer.crash = true;

        MuleClient client =  new MuleClient(muleContext);

        MuleMessage message = new DefaultMuleMessage("Hello", muleContext);
        client.dispatch("vm://in-flowref", message);
        assertNull(client.request("vm://out", SHORT_TIMEOUT));

        // The transaction was rolled back
        assertNull(client.request("vm://check-transacted", SHORT_TIMEOUT));
    }

}
