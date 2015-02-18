/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.module.cxf;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
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

public class WebServiceWrapperWithCxfTestCase extends AbstractServiceAndFlowTestCase
{
    private String testString = "test";

    @Rule
    public DynamicPort dynamicPort = new DynamicPort("port1");

    public WebServiceWrapperWithCxfTestCase(ConfigVariant variant, String configResources)
    {
        super(variant, configResources);
    }

    @Parameters
    public static Collection<Object[]> parameters()
    {
        return Arrays.asList(new Object[][]{
            {ConfigVariant.SERVICE, "mule-ws-wrapper-config-service.xml"},
            {ConfigVariant.FLOW, "mule-ws-wrapper-config-flow.xml"}
        });
    }

    @Test
    public void testWsCall() throws Exception
    {
        MuleClient client = muleContext.getClient();
        MuleMessage result = client.send("vm://testin", getTestMuleMessage(testString));
        assertNotNull(result.getPayload());
        assertEquals("Payload", testString, result.getPayloadAsString());
    }

    @Test
    public void testWsCallWithUrlFromMessage() throws Exception
    {
        MuleClient client = muleContext.getClient();
        Map<String, Object> props = new HashMap<String, Object>();
        props.put("ws.service.url", "http://localhost:" + dynamicPort.getNumber()
                                            + "/services/TestUMO?method=onReceive");
        MuleMessage result = client.send("vm://testin2", testString, props);
        assertNotNull(result.getPayload());
        assertEquals("Payload", testString, result.getPayloadAsString());
    }
}
