/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.test.usecases.sync;

import org.mule.api.MuleMessage;
import org.mule.module.client.MuleClient;
import org.mule.tck.junit4.FunctionalTestCase;

import java.util.HashMap;
import java.util.Map;

import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

public class HttpJmsBridgeTestCase extends FunctionalTestCase
{

    @Override
    protected String getConfigResources()
    {
        return "org/mule/test/usecases/sync/http-jms-bridge.xml";
    }

    @Test
    public void testBridge() throws Exception
    {
        MuleClient client = new MuleClient(muleContext);
        String payload = "payload";

        Map<String, Object> headers = new HashMap<String, Object>();
        final String customHeader = "X-Custom-Header";
        headers.put(customHeader, "value");

        client.sendNoReceive("http://localhost:4444/in", payload, headers);

        MuleMessage msg = client.request("vm://out", 10000);
        assertNotNull(msg);
        assertEquals(payload, msg.getPayloadAsString());
        assertEquals("value", msg.getInboundProperty(customHeader));
    }
}
