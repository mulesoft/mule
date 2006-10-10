/*
 * $Id$
 * --------------------------------------------------------------------------------------
 * Copyright (c) MuleSource, Inc.  All rights reserved.  http://www.mulesource.com
 *
 * The software in this package is published under the terms of the MuleSource MPL
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.test.util;

import java.lang.reflect.Method;
import java.net.URL;
import java.util.Enumeration;
import java.util.List;

import junit.framework.TestCase;

import org.mule.tck.testmodels.fruit.AbstractFruit;
import org.mule.tck.testmodels.fruit.Apple;
import org.mule.tck.testmodels.fruit.Banana;
import org.mule.tck.testmodels.fruit.Fruit;
import org.mule.tck.testmodels.fruit.FruitBowl;
import org.mule.tck.testmodels.fruit.Orange;
import org.mule.tck.testmodels.fruit.WaterMelon;
import org.mule.util.ClassUtils;

public class ClassUtilsTestCase extends TestCase
{

    // we do not want to match these methods when looking for a service method to
    // invoke
    protected String[] ignoreMethods = new String[]{"equals", "getInvocationHandler"};

    public void testIsConcrete() throws Exception
    {
        assertTrue(ClassUtils.isConcrete(Orange.class));
        assertTrue(!ClassUtils.isConcrete(Fruit.class));
        assertTrue(!ClassUtils.isConcrete(AbstractFruit.class));

        try
        {
            ClassUtils.isConcrete(null);
            fail("Class cannot be null, exception should be thrown");
        }
        catch (RuntimeException e)
        {
            // expected
        }
    }

    public void testLoadClass() throws Exception
    {
        Class clazz = ClassUtils.loadClass("java.lang.String", getClass());
        assertNotNull(clazz);

        assertEquals(clazz.getName(), "java.lang.String");

        try
        {
            ClassUtils.loadClass("java.lang.Bing", getClass());
            fail("ClassNotFoundException should be thrown");
        }
        catch (ClassNotFoundException e)
        {
            // expected
        }

    }

    public void testInstanciateClass() throws Exception
    {
        Object object = ClassUtils.instanciateClass("org.mule.tck.testmodels.fruit.Orange", new Object[]{});
        assertNotNull(object);
        assertTrue(object instanceof Orange);

        object = ClassUtils.instanciateClass("org.mule.tck.testmodels.fruit.FruitBowl", new Object[]{
            new Apple(), new Banana()});
        assertNotNull(object);
        assertTrue(object instanceof FruitBowl);

        FruitBowl bowl = (FruitBowl)object;

        assertTrue(bowl.hasApple());
        assertTrue(bowl.hasBanana());

        try
        {
            ClassUtils.instanciateClass("java.lang.Bing", new Object[]{});
            fail("Class does not exist, ClassNotFoundException should have been thrown");
        }
        catch (ClassNotFoundException e)
        {
            // expected
        }

    }

    public void testGetParameterTypes() throws Exception
    {
        FruitBowl bowl = new FruitBowl();

        Class[] classes = ClassUtils.getParameterTypes(bowl, "apple");
        assertNotNull(classes);
        assertEquals(1, classes.length);
        assertEquals(Apple.class, classes[0]);

        classes = ClassUtils.getParameterTypes(bowl, "invalid");
        assertNotNull(classes);
        assertEquals(0, classes.length);
    }

    public void testLoadingResources() throws Exception
    {
        URL resource = ClassUtils.getResource("test-dummy.properties", getClass());
        assertNotNull(resource);

        resource = ClassUtils.getResource("test-dummyX.properties", getClass());
        assertNull(resource);
    }

    public void testLoadingResourceEnumeration() throws Exception
    {
        Enumeration enumeration = ClassUtils.getResources("test-dummy.properties", getClass());
        assertNotNull(enumeration);
        assertTrue(enumeration.hasMoreElements());

        enumeration = ClassUtils.getResources("test-dummyX.properties", getClass());
        assertNotNull(enumeration);
        assertTrue(!enumeration.hasMoreElements());
    }

    public void testGetSatisfiableMethods() throws Exception
    {
        List methods = ClassUtils.getSatisfiableMethods(FruitBowl.class, new Class[]{Apple.class}, true,
            true, ignoreMethods);
        assertNotNull(methods);
        assertEquals(1, methods.size());

        methods = ClassUtils.getSatisfiableMethods(FruitBowl.class, new Class[]{Apple.class}, false, true,
            ignoreMethods);
        assertNotNull(methods);
        assertEquals(0, methods.size());

        // Test object param being unacceptible
        methods = ClassUtils.getSatisfiableMethods(DummyObject.class, new Class[]{WaterMelon.class}, true,
            false, ignoreMethods);
        assertNotNull(methods);
        assertEquals(0, methods.size());

        // Test object param being acceptible
        methods = ClassUtils.getSatisfiableMethods(DummyObject.class, new Class[]{WaterMelon.class}, true,
            true, ignoreMethods);
        assertNotNull(methods);
        assertEquals(2, methods.size());

        // Test object param being acceptible but not void
        methods = ClassUtils.getSatisfiableMethods(DummyObject.class, new Class[]{WaterMelon.class}, false,
            true, ignoreMethods);
        assertNotNull(methods);
        assertEquals(1, methods.size());
        assertEquals("doSomethingElse", ((Method)methods.get(0)).getName());
    }

    private static class DummyObject
    {
        public void doSomething(Object object)
        {
            // do nothing
        }

        public Object doSomethingElse(Object object)
        {
            return object;
        }
    }

}
