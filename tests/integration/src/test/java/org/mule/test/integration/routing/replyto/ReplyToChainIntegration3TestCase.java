/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.test.integration.routing.replyto;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

import org.mule.api.MuleMessage;
import org.mule.api.client.MuleClient;
import org.mule.tck.junit4.FunctionalTestCase;

import org.junit.Test;

public class ReplyToChainIntegration3TestCase extends FunctionalTestCase
{
    @Override
    protected String getConfigResources()
    {
        return "org/mule/test/integration/routing/replyto/replyto-chain-integration-test-3.xml";
    }

    @Test
    public void testReplyToChain() throws Exception
    {
        String message = "test";

        MuleClient client = muleContext.getClient();
        client.dispatch("vm://pojo1", message, null);
        MuleMessage result = client.request("jms://response", 10000);
        assertNotNull(result);
        assertEquals("Received: " + message, result.getPayload());
    }
}
