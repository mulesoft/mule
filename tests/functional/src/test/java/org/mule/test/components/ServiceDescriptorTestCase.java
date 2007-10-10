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

import org.mule.tck.FunctionalTestCase;
import org.mule.tck.testmodels.fruit.Orange;
import org.mule.umo.UMOComponent;

public class ServiceDescriptorTestCase extends FunctionalTestCase
{
    protected String getConfigResources()
    {
        return "org/mule/test/components/service-factory-functional-test.xml";
    }

    public void testGenericObjectFactory() throws Exception
    {
        UMOComponent c = managementContext.getRegistry().lookupComponent("orange1");
        
        Object service =  c.getServiceFactory().getOrCreate();
        assertTrue("Service should be an Orange", service instanceof Orange);
        // Default values
        assertEquals(new Integer(10), ((Orange) service).getSegments());
    }
    
    public void testGenericObjectFactoryWithProperties() throws Exception
    {
        UMOComponent c = managementContext.getRegistry().lookupComponent("orange2");

        // Create an orange
        Object service =  c.getServiceFactory().getOrCreate();
        assertTrue("Service should be an Orange", service instanceof Orange);
        assertEquals(new Integer(8), ((Orange) service).getSegments());
        assertEquals("Florida Sunny", ((Orange) service).getBrand());

        // Create another orange
        service =  c.getServiceFactory().getOrCreate();
        assertTrue("Service should be an Orange", service instanceof Orange);
        assertEquals(new Integer(8), ((Orange) service).getSegments());
        assertEquals("Florida Sunny", ((Orange) service).getBrand());
    }
    
    public void testSingletonObjectFactory() throws Exception
    {
        UMOComponent c = managementContext.getRegistry().lookupComponent("orange3");
        Object service =  c.getServiceFactory().getOrCreate();
        assertTrue("Service should be an Orange", service instanceof Orange);
        // Default values
        assertEquals(new Integer(10), ((Orange) service).getSegments());
    }
    
      // TODO MULE-2060  
//    public void testCustomFactory() throws Exception
//    {
//        UMOModel model = managementContext.getRegistry().lookupModel("main");
//        UMOComponent c = model.getComponent("orange4");
//        Object service = ComponentFactory.createService(c.getDescriptor());
//        assertTrue("Service should be an Orange", service instanceof Orange);
//        assertEquals(new Integer(8), ((Orange) service).getSegments());
//        assertEquals("Florida Sunny", ((Orange) service).getBrand());
//    }
}
