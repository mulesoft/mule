/*
 * $Id$
 * --------------------------------------------------------------------------------------
 * Copyright (c) MuleSource, Inc.  All rights reserved.  http://www.mulesource.com
 *
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.component;

import org.mule.api.lifecycle.InitialisationException;
import org.mule.api.object.ObjectFactory;
import org.mule.object.PrototypeObjectFactory;
import org.mule.tck.testmodels.fruit.Orange;

public class DefaultJavaComponentTestCase extends AbstractComponentTestCase
{

    protected ObjectFactory createObjectFactory() throws InitialisationException
    {
        PrototypeObjectFactory objectFactory = new PrototypeObjectFactory(Orange.class);
        objectFactory.initialise();
        return objectFactory;
    }

    public void testComponentCreation() throws Exception
    {
        ObjectFactory objectFactory = createObjectFactory();
        DefaultJavaComponent component = new DefaultJavaComponent(objectFactory);

        assertNotNull(component.getObjectFactory());
        assertEquals(objectFactory, component.getObjectFactory());
        assertEquals(Orange.class, component.getObjectFactory().getObjectClass());
        assertEquals(Orange.class, component.getObjectType());
    }

    public void testLifecycle() throws Exception
    {
        DefaultJavaComponent component = new DefaultJavaComponent(createObjectFactory());
        component.setService(getTestService());
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
            ((DefaultComponentLifecycleAdapter) component.borrowComponentLifecycleAdaptor()).componentObject.get(),
            ((DefaultComponentLifecycleAdapter) component.borrowComponentLifecycleAdaptor()).componentObject.get());

    }

    public void testComponentDisposal() throws Exception
    {
        DefaultJavaComponent component = new DefaultJavaComponent(
            createObjectFactory());
        
        component.setService(getTestService());
        component.setMuleContext(muleContext);
        component.initialise();
        component.start();

        DefaultComponentLifecycleAdapter lifecycleAdapter = (DefaultComponentLifecycleAdapter) 
            component.borrowComponentLifecycleAdaptor();
        component.returnComponentLifecycleAdaptor(lifecycleAdapter);
        component.dispose();

        assertNull(lifecycleAdapter.componentObject.get());
    }

}
