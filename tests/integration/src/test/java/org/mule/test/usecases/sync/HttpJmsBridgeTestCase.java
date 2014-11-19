/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.test.usecases.sync;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.mule.module.http.api.HttpConstants.Methods.POST;
import static org.mule.module.http.api.client.HttpRequestOptionsBuilder.newOptions;

import org.mule.DefaultMuleMessage;
import org.mule.api.MuleMessage;
import org.mule.api.client.MuleClient;
import org.mule.tck.AbstractServiceAndFlowTestCase;
import org.mule.tck.junit4.rule.DynamicPort;

import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

import org.junit.Rule;
import org.junit.Test;
import org.junit.runners.Parameterized.Parameters;

public class HttpJmsBridgeTestCase extends AbstractServiceAndFlowTestCase
{

    @Rule
    public DynamicPort httpPort = new DynamicPort("port");

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
        MuleClient client = muleContext.getClient();
        String payload = "payload";

        Map<String, Object> headers = new HashMap<String, Object>();
        final String customHeader = "X-Custom-Header";
        headers.put(customHeader, "value");

        client.dispatch(String.format("http://localhost:%d/in", httpPort.getNumber()), new DefaultMuleMessage(payload, headers, muleContext), newOptions().method(POST.name()).build());

        MuleMessage msg = client.request("vm://out", 10000);
        assertNotNull(msg);
        assertEquals(payload, msg.getPayloadAsString());
        assertEquals("value", msg.getInboundProperty(customHeader));
    }
}
