/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.component;

import org.mule.api.lifecycle.InitialisationException;
import org.mule.api.object.ObjectFactory;
import org.mule.api.service.Service;
import org.mule.lifecycle.LifecycleTrackerComponent;
import org.mule.model.seda.SedaService;
import org.mule.object.PrototypeObjectFactory;
import org.mule.tck.testmodels.fruit.Orange;

import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNotSame;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

public class DefaultJavaComponentTestCase extends AbstractComponentTestCase
{

    protected ObjectFactory createObjectFactory() throws InitialisationException
    {
        PrototypeObjectFactory objectFactory = new PrototypeObjectFactory(Orange.class);
        objectFactory.initialise();
        return objectFactory;
    }

    @Test
    public void testComponentCreation() throws Exception
    {
        ObjectFactory objectFactory = createObjectFactory();
        DefaultJavaComponent component = new DefaultJavaComponent(objectFactory);

        assertNotNull(component.getObjectFactory());
        assertEquals(objectFactory, component.getObjectFactory());
        assertEquals(Orange.class, component.getObjectFactory().getObjectClass());
        assertEquals(Orange.class, component.getObjectType());
    }

    @Test
    public void testLifecycle() throws Exception
    {
        DefaultJavaComponent component = new DefaultJavaComponent(createObjectFactory());
        component.setFlowConstruct(getTestService());
        component.setMuleContext(muleContext);
        component.initialise();
        component.start();

        assertNotSame(component.borrowComponentLifecycleAdaptor(),
            component.borrowComponentLifecycleAdaptor());

        Object obj = component.getObjectFactory().getInstance(muleContext);
        assertNotNull(obj);

        component.stop();
        component.start();

        assertNotSame(
            ((DefaultComponentLifecycleAdapter) component.borrowComponentLifecycleAdaptor()).componentObject,
            ((DefaultComponentLifecycleAdapter) component.borrowComponentLifecycleAdaptor()).componentObject);

    }

    @Test
    public void testComponentDisposal() throws Exception
    {
        DefaultJavaComponent component = new DefaultJavaComponent(
            createObjectFactory());
        
        component.setFlowConstruct(getTestService());
        component.setMuleContext(muleContext);
        component.initialise();
        component.start();

        DefaultComponentLifecycleAdapter lifecycleAdapter = (DefaultComponentLifecycleAdapter) 
            component.borrowComponentLifecycleAdaptor();
        component.returnComponentLifecycleAdaptor(lifecycleAdapter);
        component.stop();
        component.dispose();

        assertNull(lifecycleAdapter.componentObject);
    }
    
    @Test
    public void testServicePropagatedLifecycle() throws InitialisationException
    {
        Service service = new SedaService(muleContext);
        service.setName("service");
        service.setModel(muleContext.getRegistry().lookupSystemModel());
        LifecycleTrackerComponent component = new LifecycleTrackerComponent();
        service.setComponent(component);
        
        service.initialise();
        
        assertTrue(component.getTracker().contains("initialise"));
    }

}
