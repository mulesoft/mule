/*
 * $Id$
 * --------------------------------------------------------------------------------------
 * Copyright (c) MuleSource, Inc.  All rights reserved.  http://www.mulesource.com
 *
 * The software in this package is published under the terms of the MuleSource MPL
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.test.components;

import org.mule.impl.model.MuleProxy;
import org.mule.tck.FunctionalTestCase;
import org.mule.tck.services.UniqueComponent;
import org.mule.tck.testmodels.mule.TestMuleProxy;
import org.mule.tck.testmodels.mule.TestSedaComponent;
import org.mule.tck.testmodels.mule.TestSedaModel;
import org.mule.umo.UMOComponent;
import org.mule.umo.model.UMOModel;

public class ComponentPoolingTestCase extends FunctionalTestCase
{
    protected String getConfigResources()
    {
        return "org/mule/test/components/component-pooling-functional-test.xml";
    }

    public void testConfigSanity() throws Exception
    {
        UMOModel model = managementContext.getRegistry().lookupModel("main");
        assertTrue("Model should be a TestSedaModel", model instanceof TestSedaModel);
        UMOComponent c = model.getComponent("unique1");
        assertTrue("Component should be a TestSedaComponent", c instanceof TestSedaComponent);
        try
        {
            c.getInstance();
            fail("Should not be able to call getInstance() for a SedaComponent");
        }
        catch (Exception e)
        {
            // expected
        }
        
        MuleProxy proxy = ((TestSedaComponent) c).getProxy();
        assertTrue("Proxy should be a TestMuleProxy", proxy instanceof TestMuleProxy);
        Object component = ((TestMuleProxy) proxy).getComponent();
        assertNotNull(component);
        assertTrue("Component should be of type UniqueComponent", component instanceof UniqueComponent);
        String id1 = ((UniqueComponent) component).getId();
        assertNotNull(id1);
    }

    public void testSimpleFactory() throws Exception
    {
        UMOModel model = managementContext.getRegistry().lookupModel("main");
        UMOComponent c = model.getComponent("unique1");        
        
        MuleProxy proxy = ((TestSedaComponent) c).getProxy();
        Object component = ((TestMuleProxy) proxy).getComponent();
        String id1 = ((UniqueComponent) component).getId();

        proxy = ((TestSedaComponent) c).getProxy();
        component = ((TestMuleProxy) proxy).getComponent();
        String id2 = ((UniqueComponent) component).getId();
        
        assertFalse("Component IDs " + id1 + " and " + id2 + " should be different", id1.equals(id2));
    }

    public void testSingletonFactoryWithClassName() throws Exception
    {
        UMOModel model = managementContext.getRegistry().lookupModel("main");
        UMOComponent c = model.getComponent("unique2");        
        
        MuleProxy proxy = ((TestSedaComponent) c).getProxy();
        Object component = ((TestMuleProxy) proxy).getComponent();
        String id1 = ((UniqueComponent) component).getId();

        proxy = ((TestSedaComponent) c).getProxy();
        component = ((TestMuleProxy) proxy).getComponent();
        String id2 = ((UniqueComponent) component).getId();
        
        assertTrue("Component IDs " + id1 + " and " + id2 + " should be the same", id1.equals(id2));
    }

    public void testSingletonFactoryWithBean() throws Exception
    {
        UMOModel model = managementContext.getRegistry().lookupModel("main");
        UMOComponent c = model.getComponent("unique3");        
        
        MuleProxy proxy = ((TestSedaComponent) c).getProxy();
        Object component = ((TestMuleProxy) proxy).getComponent();
        String id1 = ((UniqueComponent) component).getId();

        proxy = ((TestSedaComponent) c).getProxy();
        component = ((TestMuleProxy) proxy).getComponent();
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


