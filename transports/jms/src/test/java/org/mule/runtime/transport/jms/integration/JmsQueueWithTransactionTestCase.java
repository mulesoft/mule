/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.transport.jms.integration;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

import org.mule.runtime.core.api.MuleMessage;
import org.mule.runtime.core.api.client.MuleClient;

import org.junit.Test;

public class JmsQueueWithTransactionTestCase extends AbstractJmsFunctionalTestCase
{
    @Override
    protected String getConfigFile()
    {
        return "integration/jms-queue-with-transaction.xml";
    }

    @Test
    public void testOutboundJmsTransaction() throws Exception
    {
        MuleClient client = muleContext.getClient();
        client.send("vm://in", getTestMuleMessage(DEFAULT_INPUT_MESSAGE));

        MuleMessage response = client.request("vm://out", getTimeout());
        assertNotNull(response);
        assertEquals(DEFAULT_INPUT_MESSAGE, getPayloadAsString(response));
    }
}
