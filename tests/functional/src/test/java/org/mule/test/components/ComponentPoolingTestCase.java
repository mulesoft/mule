///*
// * $Id$
// * --------------------------------------------------------------------------------------
// * Copyright (c) MuleSource, Inc.  All rights reserved.  http://www.mulesource.com
// *
// * The software in this package is published under the terms of the CPAL v1.0
// * license, a copy of which has been included with this distribution in the
// * LICENSE.txt file.
// */
//
//package org.mule.test.components;
//
//import org.mule.api.service.Service;
//import org.mule.tck.FunctionalTestCase;
//import org.mule.tck.services.UniqueComponent;
//import org.mule.tck.testmodels.mule.TestSedaService;
//
//public class ComponentPoolingTestCase extends FunctionalTestCase
//{
//    protected String getConfigResources()
//    {
//        return "org/mule/test/components/component-pooling-functional-test.xml";
//    }
//
//    public void testConfigSanity() throws Exception
//    {
//        Service c = muleContext.getRegistry().lookupService("unique1");
//        assertTrue("Service should be a TestSedaService", c instanceof TestSedaService);
//        Object component = ((TestSedaService) c).getOrCreateService();
//        assertNotNull(component);
//        assertTrue("Service should be of type UniqueComponent but is of type " + component.getClass(), component instanceof UniqueComponent);
//        String id1 = ((UniqueComponent) component).getId();
//        assertNotNull(id1);
//    }
//
//    public void testSimpleFactory() throws Exception
//    {
//        Service c = muleContext.getRegistry().lookupService("unique1");        
//        
//        Object component = ((TestSedaService) c).getOrCreateService();
//        String id1 = ((UniqueComponent) component).getId();
//
//        component = ((TestSedaService) c).getOrCreateService();
//        String id2 = ((UniqueComponent) component).getId();
//        
//        assertFalse("Service IDs " + id1 + " and " + id2 + " should be different", id1.equals(id2));
//    }
//
//    public void testSingletonFactoryWithClassName() throws Exception
//    {
//        Service c = muleContext.getRegistry().lookupService("unique2");        
//        
//        Object component = ((TestSedaService) c).getOrCreateService();
//        String id1 = ((UniqueComponent) component).getId();
//
//        component = ((TestSedaService) c).getOrCreateService();
//        String id2 = ((UniqueComponent) component).getId();
//        
//        assertTrue("Service IDs " + id1 + " and " + id2 + " should be the same", id1.equals(id2));
//    }
//
//    public void testSingletonFactoryWithBean() throws Exception
//    {
//        Service c = muleContext.getRegistry().lookupService("unique3");        
//        
//        Object component = ((TestSedaService) c).getOrCreateService();
//        String id1 = ((UniqueComponent) component).getId();
//
//        component = ((TestSedaService) c).getOrCreateService();
//        String id2 = ((UniqueComponent) component).getId();
//        
//        assertTrue("Service IDs " + id1 + " and " + id2 + " should be the same", id1.equals(id2));
//    }
//
////    public void testPoolingDisabled() throws Exception
////    {
////        Model model = muleContext.getRegistry().lookupModel("main");
////        Service c = model.getComponent("unique4");        
////        
////        MuleProxy proxy = ((TestSedaService) c).getProxy();
////        Object service = ((TestMuleProxy) proxy).getComponent();
////        String id1 = ((UniqueComponent) service).getId();
////
////        proxy = ((TestSedaService) c).getProxy();
////        service = ((TestMuleProxy) proxy).getComponent();
////        String id2 = ((UniqueComponent) service).getId();
////        
////        assertTrue("Service IDs " + id1 + " and " + id2 + " should be the same", id1.equals(id2));
////    }
//
//}
//
//
