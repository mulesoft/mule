/*
 * $Id: AsyncReplyNoTimeoutTestCase.java 11514 2008-03-30 21:13:10Z rossmason $
 * --------------------------------------------------------------------------------------
 * Copyright (c) MuleSource, Inc.  All rights reserved.  http://www.mulesource.com
 *
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.test.integration.routing.outbound;

import org.mule.api.MuleMessage;
import org.mule.api.MuleMessageCollection;
import org.mule.module.client.MuleClient;
import org.mule.tck.FunctionalTestCase;

import java.util.Map;
import java.util.HashMap;

public class ExpressionRecipientListSyncTestCase extends FunctionalTestCase
{
    protected String getConfigResources()
    {
        return "org/mule/test/integration/routing/outbound/expression-recipient-list-sync-test.xml";
    }

    public void testRecipientList() throws Exception
    {
        String message = "test";
        MuleClient client = new MuleClient();
        Map props = new HashMap(3);
        props.put("recipient1", "vm://service1.queue");
        props.put("recipient2", "vm://service2.queue");
        props.put("recipient3", "vm://service3.queue");
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