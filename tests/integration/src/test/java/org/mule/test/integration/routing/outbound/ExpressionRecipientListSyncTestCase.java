/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.test.integration.routing.outbound;

import org.mule.api.MuleMessage;
import org.mule.api.MuleMessageCollection;
import org.mule.module.client.MuleClient;
import org.mule.tck.junit4.FunctionalTestCase;

import java.util.HashMap;
import java.util.Map;

import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

public class ExpressionRecipientListSyncTestCase extends FunctionalTestCase
{

    @Override
    protected String getConfigResources()
    {
        return "org/mule/test/integration/routing/outbound/expression-recipient-list-sync-test.xml";
    }

    @Test
    public void testRecipientList() throws Exception
    {
        String message = "test";
        MuleClient client = new MuleClient(muleContext);
        Map<String, Object> props = new HashMap<String, Object>(3);
        props.put("recipient1", "vm://service1.queue?exchangePattern=request-response");
        props.put("recipient2", "vm://service2.queue?exchangePattern=request-response");
        props.put("recipient3", "vm://service3.queue?exchangePattern=request-response");
        MuleMessage result = client.send("vm://distributor.queue", message, props);

        assertNotNull(result);
        
        assertTrue(result instanceof MuleMessageCollection);
        MuleMessageCollection coll = (MuleMessageCollection)result;
        assertEquals(3, coll.size());
        MuleMessage[] results = coll.getMessagesAsArray();

        for (int i = 0; i < results.length; i++)
        {
            MuleMessage muleMessage = results[i];
            assertEquals("test " + (i+1) + " Received", muleMessage.getPayload());
        }
    }
}
