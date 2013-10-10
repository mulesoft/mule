/*
 * (c) 2003-2014 MuleSoft, Inc. This software is protected under international copyright
 * law. All use of this software is subject to MuleSoft's Master Subscription Agreement
 * (or other master license agreement) separately entered into in writing between you and
 * MuleSoft. If such an agreement is not in place, you may not use the software.
 */
package org.mule.test.usecases.sync;

import org.mule.api.MuleMessage;
import org.mule.module.client.MuleClient;
import org.mule.tck.AbstractServiceAndFlowTestCase;

import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

import org.junit.Test;
import org.junit.runners.Parameterized.Parameters;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

public class HttpJmsBridgeTestCase extends AbstractServiceAndFlowTestCase
{
    @Parameters
    public static Collection<Object[]> parameters()
    {
        return Arrays.asList(new Object[][]{
            {ConfigVariant.SERVICE, "org/mule/test/usecases/sync/http-jms-bridge-service.xml"},
            {ConfigVariant.FLOW, "org/mule/test/usecases/sync/http-jms-bridge-flow.xml"}
        });
    }

    public HttpJmsBridgeTestCase(ConfigVariant variant, String configResources)
    {
        super(variant, configResources);
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
