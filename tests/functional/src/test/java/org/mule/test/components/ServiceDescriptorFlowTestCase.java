/*
 * (c) 2003-2014 MuleSoft, Inc. This software is protected under international copyright
 * law. All use of this software is subject to MuleSoft's Master Subscription Agreement
 * (or other master license agreement) separately entered into in writing between you and
 * MuleSoft. If such an agreement is not in place, you may not use the software.
 */
package org.mule.test.components;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import org.junit.Test;
import org.mule.api.construct.FlowConstruct;
import org.mule.tck.junit4.FunctionalTestCase;
import org.mule.tck.testmodels.fruit.Orange;

public class ServiceDescriptorFlowTestCase extends FunctionalTestCase
{

    @Override
    protected String getConfigResources()
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
