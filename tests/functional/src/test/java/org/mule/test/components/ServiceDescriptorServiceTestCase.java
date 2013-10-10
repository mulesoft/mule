/*
 * (c) 2003-2014 MuleSoft, Inc. This software is protected under international copyright
 * law. All use of this software is subject to MuleSoft's Master Subscription Agreement
 * (or other master license agreement) separately entered into in writing between you and
 * MuleSoft. If such an agreement is not in place, you may not use the software.
 */
package org.mule.test.components;

import org.mule.api.service.Service;
import org.mule.tck.junit4.FunctionalTestCase;
import org.mule.tck.testmodels.fruit.Orange;

import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

public class ServiceDescriptorServiceTestCase extends FunctionalTestCase
{

    @Override
    protected String getConfigResources()
    {
        return "org/mule/test/components/service-factory-functional-test-service.xml";
    }

    @Test
    public void testGenericObjectFactory() throws Exception
    {
        Service c = muleContext.getRegistry().lookupService("orange1");
        
        Object service =  getComponent(c);
        assertTrue("Service should be an Orange", service instanceof Orange);
        // Default values
        assertEquals(new Integer(10), ((Orange) service).getSegments());
    }
    
    @Test
    public void testGenericObjectFactoryWithProperties() throws Exception
    {
        Service c = muleContext.getRegistry().lookupService("orange2");

        // Create an orange
        Object service =  getComponent(c);
        assertTrue("Service should be an Orange", service instanceof Orange);
        assertEquals(new Integer(8), ((Orange) service).getSegments());
        assertEquals("Florida Sunny", ((Orange) service).getBrand());

        // Create another orange
        service =  getComponent(c);
        assertTrue("Service should be an Orange", service instanceof Orange);
        assertEquals(new Integer(8), ((Orange) service).getSegments());
        assertEquals("Florida Sunny", ((Orange) service).getBrand());
    }
    
    @Test
    public void testSingletonObjectFactory() throws Exception
    {
        Service c = muleContext.getRegistry().lookupService("orange3");
        Object service =  getComponent(c);
        assertTrue("Service should be an Orange", service instanceof Orange);
        // Default values
        assertEquals(new Integer(10), ((Orange) service).getSegments());
    }
    
    @Test
    public void testSpringSingleton() throws Exception
    {
        Service c = muleContext.getRegistry().lookupService("orange4");
        Object service =  getComponent(c);
        assertTrue("Service should be an Orange", service instanceof Orange);
        // Default values
        assertEquals(new Integer(10), ((Orange) service).getSegments());
    }
    
    @Test
    public void testSpringFactoryBean() throws Exception
    {
        Service c = muleContext.getRegistry().lookupService("orange5");
        Object service =  getComponent(c);
        assertNotNull(service);
        assertTrue("Service should be an Orange but is: " + service.getClass(), service instanceof Orange);
        assertEquals(new Integer(8), ((Orange) service).getSegments());
        assertEquals("Florida Sunny", ((Orange) service).getBrand());
    }

    @Test
    public void testPojoAsFactoryBean() throws Exception
    {
        Service c = muleContext.getRegistry().lookupService("orange6");
        Object service =  getComponent(c);
        assertNotNull(service);
        assertTrue("Service should be an Orange but is: " + service.getClass(), service instanceof Orange);
        assertEquals("Florida Sunny", ((Orange) service).getBrand());
    }
}
