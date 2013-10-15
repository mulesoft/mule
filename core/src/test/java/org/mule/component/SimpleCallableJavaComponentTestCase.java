/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.component;

import org.mule.api.DefaultMuleException;
import org.mule.object.PrototypeObjectFactory;
import org.mule.tck.testmodels.fruit.Apple;
import org.mule.tck.testmodels.fruit.Orange;

import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertSame;

public class SimpleCallableJavaComponentTestCase extends AbstractComponentTestCase
{

    @Test
    public void testComponentCreationWithObjectFactory() throws Exception
    {
        PrototypeObjectFactory objectFactory = new PrototypeObjectFactory(
            Apple.class);
        objectFactory.setObjectClass(Apple.class);
        objectFactory.initialise();

        SimpleCallableJavaComponent component = new SimpleCallableJavaComponent(
            objectFactory);

        assertNotNull(component.getObjectFactory());
        assertEquals(objectFactory, component.getObjectFactory());
        assertEquals(Apple.class, component.getObjectFactory().getObjectClass());
        assertEquals(Apple.class, component.getObjectType());

        objectFactory = new PrototypeObjectFactory(Orange.class);
        objectFactory.setObjectClass(Orange.class);
        objectFactory.initialise();

        try
        {
            component = new SimpleCallableJavaComponent(objectFactory);
        }
        catch (Exception e)
        {
            assertSame(DefaultMuleException.class, e.getClass());
        }
    }

    @Test
    public void testDirectComponentCreation() throws Exception
    {
        SimpleCallableJavaComponent component = new SimpleCallableJavaComponent(Apple.class);

        assertNotNull(component.getObjectFactory());
        assertEquals(Apple.class, component.getObjectFactory().getObjectClass());
        assertEquals(Apple.class, component.getObjectType());

        try
        {
            component = new SimpleCallableJavaComponent(Orange.class);
        }
        catch (Exception e)
        {
            assertSame(DefaultMuleException.class, e.getClass());
        }
    }

    @Test
    public void testSimpleComponentCreation() throws Exception
    {
        SimpleCallableJavaComponent component = new SimpleCallableJavaComponent(
            new Apple());

        assertNotNull(component.getObjectFactory());
        assertEquals(Apple.class, component.getObjectFactory().getObjectClass());
        assertEquals(Apple.class, component.getObjectType());

        try
        {
            component = new SimpleCallableJavaComponent(new Orange());
        }
        catch (Exception e)
        {
            assertSame(DefaultMuleException.class, e.getClass());
        }
    }

    @Test
    public void testLifecycle() throws Exception
    {
        SimpleCallableJavaComponent component = new SimpleCallableJavaComponent(
            new Apple());
        component.setFlowConstruct(getTestService());
        component.setMuleContext(muleContext);
        component.initialise();
        component.start();

        assertNull(component.borrowComponentLifecycleAdaptor());

        Object obj = component.getObjectFactory().getInstance(muleContext);
        assertNotNull(obj);

        component.stop();
        component.dispose();
//        try
//        {
//            component.checkDisposed();
//        }
//        catch (Exception e)
//        {
//            assertSame(DisposeException.class, e.getClass());
//        }

    }

}
