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
import org.mule.tck.services.UniqueComponent;
import org.mule.tck.testmodels.mule.TestSedaComponent;
import org.mule.umo.UMOComponent;

public class ComponentPoolingTestCase extends FunctionalTestCase
{
    protected String getConfigResources()
    {
        return "org/mule/test/components/component-pooling-functional-test.xml";
    }

    public void testConfigSanity() throws Exception
    {
        UMOComponent c = managementContext.getRegistry().lookupComponent("unique1");
        assertTrue("Component should be a TestSedaComponent", c instanceof TestSedaComponent);
        Object component = ((TestSedaComponent) c).getOrCreateService();
        assertNotNull(component);
        assertTrue("Component should be of type UniqueComponent", component instanceof UniqueComponent);
        String id1 = ((UniqueComponent) component).getId();
        assertNotNull(id1);
    }

    public void testSimpleFactory() throws Exception
    {
        UMOComponent c = managementContext.getRegistry().lookupComponent("unique1");        
        
        Object component = ((TestSedaComponent) c).getOrCreateService();
        String id1 = ((UniqueComponent) component).getId();

        component = ((TestSedaComponent) c).getOrCreateService();
        String id2 = ((UniqueComponent) component).getId();
        
        assertFalse("Component IDs " + id1 + " and " + id2 + " should be different", id1.equals(id2));
    }

    public void testSingletonFactoryWithClassName() throws Exception
    {
        UMOComponent c = managementContext.getRegistry().lookupComponent("unique2");        
        
        Object component = ((TestSedaComponent) c).getOrCreateService();
        String id1 = ((UniqueComponent) component).getId();

        component = ((TestSedaComponent) c).getOrCreateService();
        String id2 = ((UniqueComponent) component).getId();
        
        assertTrue("Component IDs " + id1 + " and " + id2 + " should be the same", id1.equals(id2));
    }

    public void testSingletonFactoryWithBean() throws Exception
    {
        UMOComponent c = managementContext.getRegistry().lookupComponent("unique3");        
        
        Object component = ((TestSedaComponent) c).getOrCreateService();
        String id1 = ((UniqueComponent) component).getId();

        component = ((TestSedaComponent) c).getOrCreateService();
        String id2 = ((UniqueComponent) component).getId();
        
        assertTrue("Component IDs " + id1 + " and " + id2 + " should be the same", id1.equals(id2));
    }

//    public void testPoolingDisabled() throws Exception
//    {
//        UMOModel model = managementContext.getRegistry().lookupModel("main");
//        UMOComponent c = model.getComponent("unique4");        
//        
//        MuleProxy proxy = ((TestSedaComponent) c).getProxy();
//        Object component = ((TestMuleProxy) proxy).getComponent();
//        String id1 = ((UniqueComponent) component).getId();
//
//        proxy = ((TestSedaComponent) c).getProxy();
//        component = ((TestMuleProxy) proxy).getComponent();
//        String id2 = ((UniqueComponent) component).getId();
//        
//        assertTrue("Component IDs " + id1 + " and " + id2 + " should be the same", id1.equals(id2));
//    }

}


