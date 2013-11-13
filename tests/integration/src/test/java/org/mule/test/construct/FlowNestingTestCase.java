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

import org.mule.DefaultMuleMessage;
import org.mule.api.MuleException;
import org.mule.api.MuleMessage;
import org.mule.api.client.MuleClient;
import org.mule.tck.junit4.FunctionalTestCase;
import org.mule.tck.junit4.rule.DynamicPort;
import org.mule.tck.testmodels.fruit.Apple;
import org.mule.tck.testmodels.fruit.Orange;

import org.junit.Rule;
import org.junit.Test;

public class FlowNestingTestCase extends FunctionalTestCase
{
    @Rule
    public DynamicPort dynamicPort = new DynamicPort("port1");

    @Override
    protected String getConfigFile()
    {
        return "org/mule/test/construct/flow-nesting-config.xml";
    }

    @Test
    public void testNestingFiltersAccepted() throws MuleException
    {
        MuleMessage request = new DefaultMuleMessage(new Orange(),  muleContext);
        request.setOutboundProperty("Currency", "MyCurrency");
        request.setOutboundProperty("AcquirerCountry", "MyCountry");
        request.setOutboundProperty("Amount", "4999");
               
        MuleClient client = muleContext.getClient();
        
        client.dispatch("vm://inFilter", request);
        MuleMessage result = client.request("vm://outFilter", 5000);
        assertNotNull(result);
    }
    
    @Test
    public void testNestingFiltersRejected() throws MuleException
    {
        MuleMessage request = new DefaultMuleMessage(new Apple(),  muleContext);
        request.setOutboundProperty("Currency", "MyCurrency");
        request.setOutboundProperty("AcquirerCountry", "MyCountry");
        request.setOutboundProperty("Amount", "4999");
               
        MuleClient client = muleContext.getClient();
        
        client.dispatch("vm://inFilter", request);
        MuleMessage result = client.request("vm://outFilter", 5000);
        assertNull(result);
    }
    
    @Test
    public void testNestingChoiceAccepted() throws Exception
    {
        MuleMessage request = new DefaultMuleMessage(new Apple(),  muleContext);
        request.setOutboundProperty("AcquirerCountry", "MyCountry");
        request.setOutboundProperty("Amount", "4999");
               
        MuleClient client = muleContext.getClient();
        
        client.dispatch("vm://inChoice", request);
        MuleMessage result = client.request("vm://outChoice", 5000);
        assertNotNull(result);
        assertEquals("ABC", result.getPayloadAsString());
    }
    
    @Test
    public void testNestingChoiceRejected() throws Exception
    {
        MuleMessage request = new DefaultMuleMessage(new Apple(),  muleContext);
        request.setOutboundProperty("AcquirerCountry", "MyCountry");
        request.setOutboundProperty("Amount", "5000");
               
        MuleClient client = muleContext.getClient();
        
        client.dispatch("vm://inChoice", request);
        MuleMessage result = client.request("vm://outChoice", 5000);
        assertNotNull(result);
        assertEquals("AB", result.getPayloadAsString());
    }
}


