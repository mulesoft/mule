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

import org.mule.object.PrototypeObjectFactory;
import org.mule.tck.testmodels.fruit.Orange;
import org.mule.tck.testmodels.fruit.WaterMelon;

public class DefaultJavaComponentTestCase extends AbstractComponentTestCase
{

    public void testComponentCreation() throws Exception
    {
        PrototypeObjectFactory objectFactory = new PrototypeObjectFactory(Orange.class);
        objectFactory.setObjectClass(Orange.class);
        objectFactory.initialise();

        DefaultJavaComponent component = new DefaultJavaComponent(objectFactory);

        assertNotNull(component.getObjectFactory());
        assertEquals(objectFactory, component.getObjectFactory());
        assertEquals(Orange.class, component.getObjectFactory().getObjectClass());
        assertEquals(Orange.class, component.getObjectType());
    }

    public void testLifecycle() throws Exception
    {
        DefaultJavaComponent component = 
            new DefaultJavaComponent(new PrototypeObjectFactory(Orange.class));
        component.setService(getTestService());
        component.initialise();
        component.start();

        assertNotSame(component.borrowComponentLifecycleAdaptor(),
            component.borrowComponentLifecycleAdaptor());

        Object obj = component.getObjectFactory().getInstance();
        assertNotNull(obj);

        component.stop();
        component.start();

        assertNotSame(
            ((DefaultLifecycleAdapter) component.borrowComponentLifecycleAdaptor()).componentObject.get(),
            ((DefaultLifecycleAdapter) component.borrowComponentLifecycleAdaptor()).componentObject.get());

    }

    public void testComponentDisposal() throws Exception
    {
        DefaultJavaComponent component = new DefaultJavaComponent(
            new PrototypeObjectFactory(WaterMelon.class));
        
        component.setService(getTestService());
        component.initialise();
        component.start();

        DefaultLifecycleAdapter lifecycleAdapter = (DefaultLifecycleAdapter) 
            component.borrowComponentLifecycleAdaptor();
        component.returnComponentLifecycleAdaptor(lifecycleAdapter);
        component.dispose();

        assertNull(lifecycleAdapter.componentObject.get());
    }

}
