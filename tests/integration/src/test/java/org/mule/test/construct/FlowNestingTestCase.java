/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.test.construct;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;

import org.mule.test.AbstractIntegrationTestCase;
import org.mule.runtime.core.api.MuleMessage;
import org.mule.runtime.core.api.client.MuleClient;
import org.mule.tck.junit4.rule.DynamicPort;
import org.mule.tck.testmodels.fruit.Apple;
import org.mule.tck.testmodels.fruit.Orange;

import java.io.Serializable;
import java.util.HashMap;
import java.util.Map;

import org.junit.Rule;
import org.junit.Test;

public class FlowNestingTestCase extends AbstractIntegrationTestCase
{
    @Rule
    public DynamicPort dynamicPort = new DynamicPort("port1");

    @Override
    protected String getConfigFile()
    {
        return "org/mule/test/construct/flow-nesting-config.xml";
    }

    @Test
    public void testNestingFiltersAccepted() throws Exception
    {
        Map<String, Serializable> inboundProperties = new HashMap<>();
        inboundProperties.put("Currency", "MyCurrency");
        inboundProperties.put("AcquirerCountry", "MyCountry");
        inboundProperties.put("Amount", "4999");
        flowRunner("NestedFilters").withPayload(new Orange()).withInboundProperties(inboundProperties).asynchronously().run();

        MuleClient client = muleContext.getClient();
        MuleMessage result = client.request("test://outFilter", RECEIVE_TIMEOUT);
        assertNotNull(result);
    }
    
    @Test
    public void testNestingFiltersRejected() throws Exception
    {
        Map<String, Serializable> inboundProperties = new HashMap<>();
        inboundProperties.put("Currency", "MyCurrency");
        inboundProperties.put("AcquirerCountry", "MyCountry");
        inboundProperties.put("Amount", "4999");
        flowRunner("NestedFilters").withPayload(new Apple()).withInboundProperties(inboundProperties).asynchronously().run();

        MuleClient client = muleContext.getClient();
        MuleMessage result = client.request("test://outFilter", RECEIVE_TIMEOUT);
        assertNull(result);
    }
    
    @Test
    public void testNestingChoiceAccepted() throws Exception
    {
        Map<String, Serializable> inboundProperties = new HashMap<>();
        inboundProperties.put("AcquirerCountry", "MyCountry");
        inboundProperties.put("Amount", "4999");
        flowRunner("NestedChoice").withPayload(new Apple()).withInboundProperties(inboundProperties).asynchronously().run();

        MuleClient client = muleContext.getClient();
        MuleMessage result = client.request("test://outChoice", RECEIVE_TIMEOUT);
        assertNotNull(result);
        assertEquals("ABC", getPayloadAsString(result));
    }
    
    @Test
    public void testNestingChoiceRejected() throws Exception
    {
        Map<String, Serializable> inboundProperties = new HashMap<>();
        inboundProperties.put("AcquirerCountry", "MyCountry");
        inboundProperties.put("Amount", "5000");
        flowRunner("NestedChoice").withPayload(new Apple()).withInboundProperties(inboundProperties).asynchronously().run();

        MuleClient client = muleContext.getClient();
        MuleMessage result = client.request("test://outChoice", RECEIVE_TIMEOUT);
        assertNotNull(result);
        assertEquals("AB", getPayloadAsString(result));
    }
}


