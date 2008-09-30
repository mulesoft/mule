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
import org.mule.module.client.MuleClient;
import org.mule.tck.FunctionalTestCase;

import java.util.Map;
import java.util.HashMap;
import java.util.List;
import java.util.ArrayList;

public class ExpressionRecipientListAsyncTestCase extends FunctionalTestCase
{
    protected String getConfigResources()
    {
        return "org/mule/test/integration/routing/outbound/expression-recipient-list-async-test.xml";
    }

    public void testRecipientList() throws Exception
    {
        String message = "test";
        MuleClient client = new MuleClient();
        Map props = new HashMap(3);
        props.put("recipient1", "vm://service1.queue");
        props.put("recipient2", "vm://service2.queue");
        props.put("recipient3", "vm://service3.queue");
        client.dispatch("vm://distributor.queue", message, props);

        List results = new ArrayList(3);

        MuleMessage result = client.request("vm://collector.queue", 5000);
        assertNotNull(result);
        results.add(result.getPayload());

        result = client.request("vm://collector.queue", 3000);
        assertNotNull(result);
        results.add(result.getPayload());

        result = client.request("vm://collector.queue", 3000);
        assertNotNull(result);
        results.add(result.getPayload());

        assertTrue(results.contains("test 1 Received"));
        assertTrue(results.contains("test 2 Received"));
        assertTrue(results.contains("test 3 Received"));
    }
}