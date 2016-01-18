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
import org.mule.api.MuleMessage;
import org.mule.api.client.MuleClient;
import org.mule.api.transport.PropertyScope;
import org.mule.functional.junit4.FunctionalTestCase;
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
    public void testNestingFiltersAccepted() throws Exception
    {
        MuleMessage request = new DefaultMuleMessage(new Orange(),  muleContext);
        request.setProperty("Currency", "MyCurrency", PropertyScope.INBOUND);
        request.setProperty("AcquirerCountry", "MyCountry", PropertyScope.INBOUND);
        request.setProperty("Amount", "4999", PropertyScope.INBOUND);
               
        MuleClient client = muleContext.getClient();
        
        flowRunner("NestedFilters").withPayload(request).asynchronously().run();
        MuleMessage result = client.request("test://outFilter", RECEIVE_TIMEOUT);
        assertNotNull(result);
    }
    
    @Test
    public void testNestingFiltersRejected() throws Exception
    {
        MuleMessage request = new DefaultMuleMessage(new Apple(),  muleContext);
        request.setProperty("Currency", "MyCurrency", PropertyScope.INBOUND);
        request.setProperty("AcquirerCountry", "MyCountry", PropertyScope.INBOUND);
        request.setProperty("Amount", "4999", PropertyScope.INBOUND);
               
        MuleClient client = muleContext.getClient();
        
        flowRunner("NestedFilters").withPayload(request).asynchronously().run();
        MuleMessage result = client.request("test://outFilter", RECEIVE_TIMEOUT);
        assertNull(result);
    }
    
    @Test
    public void testNestingChoiceAccepted() throws Exception
    {
        MuleMessage request = new DefaultMuleMessage(new Apple(),  muleContext);
        request.setProperty("AcquirerCountry", "MyCountry", PropertyScope.INBOUND);
        request.setProperty("Amount", "4999", PropertyScope.INBOUND);
               
        MuleClient client = muleContext.getClient();
        
        flowRunner("NestedChoice").withPayload(request).asynchronously().run();
        MuleMessage result = client.request("test://outChoice", RECEIVE_TIMEOUT);
        assertNotNull(result);
        assertEquals("ABC", getPayloadAsString(result));
    }
    
    @Test
    public void testNestingChoiceRejected() throws Exception
    {
        MuleMessage request = new DefaultMuleMessage(new Apple(),  muleContext);
        request.setProperty("AcquirerCountry", "MyCountry", PropertyScope.INBOUND);
        request.setProperty("Amount", "5000", PropertyScope.INBOUND);
               
        MuleClient client = muleContext.getClient();
        
        flowRunner("NestedChoice").withPayload(request).asynchronously().run();
        MuleMessage result = client.request("test://outChoice", RECEIVE_TIMEOUT);
        assertNotNull(result);
        assertEquals("AB", getPayloadAsString(result));
    }
}


