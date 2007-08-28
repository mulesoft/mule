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

import org.mule.impl.model.ComponentFactory;
import org.mule.tck.FunctionalTestCase;
import org.mule.tck.testmodels.fruit.Orange;
import org.mule.umo.UMOComponent;
import org.mule.umo.model.UMOModel;

public class ServiceDescriptorTestCase extends FunctionalTestCase
{
    protected String getConfigResources()
    {
        return "org/mule/test/components/service-factory-functional-test.xml";
    }

    public void testConfigSanity() throws Exception
    {
        // nop
    }
    
    public void testGenericObjectFactory() throws Exception
    {
        UMOModel model = managementContext.getRegistry().lookupModel("main");
        UMOComponent c = model.getComponent("orange1");
        Object service = ComponentFactory.createService(c.getDescriptor());
        assertTrue("Service should be an Orange", service instanceof Orange);
        // Default values
        assertEquals(new Integer(10), ((Orange) service).getSegments());
    }
    
    public void testGenericObjectFactoryWithProperties() throws Exception
    {
        UMOModel model = managementContext.getRegistry().lookupModel("main");
        UMOComponent c = model.getComponent("orange2");

        // Create an orange
        Object service = ComponentFactory.createService(c.getDescriptor());
        assertTrue("Service should be an Orange", service instanceof Orange);
        assertEquals(new Integer(8), ((Orange) service).getSegments());
        assertEquals("Florida Sunny", ((Orange) service).getBrand());

        // Create another orange
        service = ComponentFactory.createService(c.getDescriptor());
        assertTrue("Service should be an Orange", service instanceof Orange);
        assertEquals(new Integer(8), ((Orange) service).getSegments());
        assertEquals("Florida Sunny", ((Orange) service).getBrand());
    }
    
    public void testSingletonObjectFactory() throws Exception
    {
        UMOModel model = managementContext.getRegistry().lookupModel("main");
        UMOComponent c = model.getComponent("orange3");
        Object service = ComponentFactory.createService(c.getDescriptor());
        assertTrue("Service should be an Orange", service instanceof Orange);
        // Default values
        assertEquals(new Integer(10), ((Orange) service).getSegments());
    }
    
    public void testCustomFactory() throws Exception
    {
        UMOModel model = managementContext.getRegistry().lookupModel("main");
        UMOComponent c = model.getComponent("orange4");
        Object service = ComponentFactory.createService(c.getDescriptor());
        assertTrue("Service should be an Orange", service instanceof Orange);
        assertEquals(new Integer(8), ((Orange) service).getSegments());
        assertEquals("Florida Sunny", ((Orange) service).getBrand());
    }
}
