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

import org.mule.api.service.Service;
import org.mule.tck.junit4.FunctionalTestCase;
import org.mule.tck.testmodels.fruit.Orange;

import org.junit.Test;

public class ServiceDescriptorServiceTestCase extends FunctionalTestCase
{
    @Override
    protected String getConfigFile()
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
