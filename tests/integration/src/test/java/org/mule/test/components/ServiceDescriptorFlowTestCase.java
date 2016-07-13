/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.test.components;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import org.mule.test.AbstractIntegrationTestCase;
import org.mule.runtime.core.api.construct.FlowConstruct;
import org.mule.tck.testmodels.fruit.Orange;

import org.junit.Test;

public class ServiceDescriptorFlowTestCase extends AbstractIntegrationTestCase
{
    @Override
    protected String getConfigFile()
    {
        return "org/mule/test/components/service-factory-functional-test-flow.xml";
    }

    @Test
    public void testGenericObjectFactory() throws Exception
    {
        FlowConstruct c = muleContext.getRegistry().lookupFlowConstruct("orange1");
        
        Object flow =  getComponent(c);
        assertTrue("Flow should be an Orange", flow instanceof Orange);
        // Default values
        assertEquals(new Integer(10), ((Orange) flow).getSegments());
    }
    
    @Test
    public void testGenericObjectFactoryWithProperties() throws Exception
    {
        FlowConstruct c = muleContext.getRegistry().lookupFlowConstruct("orange2");

        // Create an orange
        Object flow = getComponent(c);
        assertTrue("Flow should be an Orange", flow instanceof Orange);
        assertEquals(new Integer(8), ((Orange) flow).getSegments());
        assertEquals("Florida Sunny", ((Orange) flow).getBrand());

        // Create another orange
        flow = getComponent(c);
        assertTrue("Service should be an Orange", flow instanceof Orange);
        assertEquals(new Integer(8), ((Orange) flow).getSegments());
        assertEquals("Florida Sunny", ((Orange) flow).getBrand());
    }
    
    @Test
    public void testSingletonObjectFactory() throws Exception
    {
        FlowConstruct c = muleContext.getRegistry().lookupFlowConstruct("orange3");
        
        Object flow =  getComponent(c);
        assertTrue("Flow should be an Orange", flow instanceof Orange);
        // Default values
        assertEquals(new Integer(10), ((Orange) flow).getSegments());
    }
    
    @Test
    public void testSpringSingleton() throws Exception
    {
        FlowConstruct c = muleContext.getRegistry().lookupFlowConstruct("orange4");
        
        Object flow =  getComponent(c);
        assertTrue("Flow should be an Orange", flow instanceof Orange);
        // Default values
        assertEquals(new Integer(10), ((Orange) flow).getSegments());
    }
    
    @Test
    public void testSpringFactoryBean() throws Exception
    {
        FlowConstruct c = muleContext.getRegistry().lookupFlowConstruct("orange5");
        
        Object flow =  getComponent(c);
        assertNotNull(flow);
        assertTrue("Flow should be an Orange but is: " + flow.getClass(), flow instanceof Orange);
        assertEquals(new Integer(8), ((Orange) flow).getSegments());
        assertEquals("Florida Sunny", ((Orange) flow).getBrand());
    }

    @Test
    public void testPojoAsFactoryBean() throws Exception
    {
        FlowConstruct c = muleContext.getRegistry().lookupFlowConstruct("orange6");
        
        Object flow =  getComponent(c);
        assertNotNull(flow);
        assertTrue("Flow should be an Orange but is: " + flow.getClass(), flow instanceof Orange);
        assertEquals("Florida Sunny", ((Orange) flow).getBrand());
    }
}
