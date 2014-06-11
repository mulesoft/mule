/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.test.integration.endpoints;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;

import org.mule.DefaultMuleMessage;
import org.mule.api.MuleMessage;
import org.mule.api.transformer.DataType;
import org.mule.tck.AbstractServiceAndFlowTestCase;
import org.mule.tck.junit4.rule.DynamicPort;

import java.util.Arrays;
import java.util.Collection;

import org.junit.Rule;
import org.junit.Test;
import org.junit.runners.Parameterized.Parameters;

public class DynamicEndpointConfigTestCase extends AbstractServiceAndFlowTestCase
{

    @Rule
    public DynamicPort dynamicPort = new DynamicPort("port1");

    @Rule
    public DynamicPort dynamicPort2 = new DynamicPort("port2");

    @Parameters
    public static Collection<Object[]> parameters()
    {
        return Arrays.asList(new Object[][]{
            {ConfigVariant.SERVICE, "org/mule/test/integration/endpoints/dynamic-endpoint-config-service.xml"},
            {ConfigVariant.FLOW, "org/mule/test/integration/endpoints/dynamic-endpoint-config-flow.xml"}
        });
    }

    public DynamicEndpointConfigTestCase(ConfigVariant variant, String configResources)
    {
        super(variant, configResources);
    }

    @Test
    public void testName() throws Exception
    {
        MuleMessage msg = new DefaultMuleMessage("Data", muleContext);
        msg.setOutboundProperty("testProp", "testPath");
        MuleMessage response = muleContext.getClient().send("vm://in1", msg);
        assertNotNull(response);
        assertNull(response.getExceptionPayload());
        assertEquals("Data Received", response.getPayload(DataType.STRING_DATA_TYPE));

        response = muleContext.getClient().send("vm://in2", msg);
        assertNotNull(response);
        assertNull(response.getExceptionPayload());
        assertEquals("Data Received", response.getPayload(DataType.STRING_DATA_TYPE));

        response = muleContext.getClient().send("vm://in3", msg);
        assertNotNull(response);
        assertNull(response.getExceptionPayload());
        String payload = response.getPayload(DataType.STRING_DATA_TYPE);
        assertEquals("Data Also Received", payload);
    }
}
