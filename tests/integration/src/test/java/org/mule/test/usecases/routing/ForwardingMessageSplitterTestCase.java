/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.test.usecases.routing;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import org.mule.api.MuleMessage;
import org.mule.api.client.MuleClient;
import org.mule.tck.junit4.FunctionalTestCase;

import java.util.ArrayList;
import java.util.List;

import org.junit.Test;

public class ForwardingMessageSplitterTestCase extends FunctionalTestCase
{

    @Override
    protected String getConfigFile()
    {
        return "org/mule/test/usecases/routing/forwarding-message-splitter-flow.xml";
    }

    @Test
    public void testSyncResponse() throws Exception
    {
        MuleClient client = muleContext.getClient();

        List<Object> payload = new ArrayList<Object>();
        payload.add("hello");
        payload.add(new Integer(3));
        payload.add(new Exception());
        client.send("vm://in.queue", payload, null);
        MuleMessage m = client.request("vm://component.1", 2000);
        assertNotNull(m);
        assertTrue(m.getPayload() instanceof String);
        m = client.request("vm://component.2", 2000);
        assertNotNull(m);
        assertTrue(m.getPayload() instanceof Integer);

        m = client.request("vm://error.queue", 2000);
        assertNotNull(m);
        assertTrue(m.getPayload() instanceof Exception);
    }
}
