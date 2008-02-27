/*
 * $Id$
 * --------------------------------------------------------------------------------------
 * Copyright (c) MuleSource, Inc.  All rights reserved.  http://www.mulesource.com
 *
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.test.components;

import org.mule.api.service.Service;
import org.mule.tck.FunctionalTestCase;
import org.mule.tck.testmodels.fruit.Orange;

public class ServiceDescriptorTestCase extends FunctionalTestCase
{
    protected String getConfigResources()
    {
        return "org/mule/test/components/service-factory-functional-test.xml";
    }

    public void testGenericObjectFactory() throws Exception
    {
        Service c = muleContext.getRegistry().lookupService("orange1");
        
        Object service =  c.getComponentFactory().getInstance();
        assertTrue("Service should be an Orange", service instanceof Orange);
        // Default values
        assertEquals(new Integer(10), ((Orange) service).getSegments());
    }
    
    public void testGenericObjectFactoryWithProperties() throws Exception
    {
        Service c = muleContext.getRegistry().lookupService("orange2");

        // Create an orange
        Object service =  c.getComponentFactory().getInstance();
        assertTrue("Service should be an Orange", service instanceof Orange);
        assertEquals(new Integer(8), ((Orange) service).getSegments());
        assertEquals("Florida Sunny", ((Orange) service).getBrand());

        // Create another orange
        service =  c.getComponentFactory().getInstance();
        assertTrue("Service should be an Orange", service instanceof Orange);
        assertEquals(new Integer(8), ((Orange) service).getSegments());
        assertEquals("Florida Sunny", ((Orange) service).getBrand());
    }
    
    public void testSingletonObjectFactory() throws Exception
    {
        Service c = muleContext.getRegistry().lookupService("orange3");
        Object service =  c.getComponentFactory().getInstance();
        assertTrue("Service should be an Orange", service instanceof Orange);
        // Default values
        assertEquals(new Integer(10), ((Orange) service).getSegments());
    }
    
    public void testSpringSingleton() throws Exception
    {
        Service c = muleContext.getRegistry().lookupService("orange4");
        Object service =  c.getComponentFactory().getInstance();
        assertTrue("Service should be an Orange", service instanceof Orange);
        // Default values
        assertEquals(new Integer(10), ((Orange) service).getSegments());
    }
    
    public void testSpringFactoryBean() throws Exception
    {
        Service c = muleContext.getRegistry().lookupService("orange5");
        Object service =  c.getComponentFactory().getInstance();
        assertNotNull(service);
        assertTrue("Service should be an Orange but is: " + service.getClass(), service instanceof Orange);
        assertEquals(new Integer(8), ((Orange) service).getSegments());
        assertEquals("Florida Sunny", ((Orange) service).getBrand());
    }

    public void testPojoAsFactoryBean() throws Exception
    {
        Service c = muleContext.getRegistry().lookupService("orange6");
        Object service =  c.getComponentFactory().getInstance();
        assertNotNull(service);
        assertTrue("Service should be an Orange but is: " + service.getClass(), service instanceof Orange);
        assertEquals("Florida Sunny", ((Orange) service).getBrand());
    }
}
