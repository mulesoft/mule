/*
 * $Id$
 * --------------------------------------------------------------------------------------
 * Copyright (c) MuleSource, Inc.  All rights reserved.  http://www.mulesource.com
 *
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.util;

import org.mule.tck.AbstractMuleTestCase;
import org.mule.tck.testmodels.fruit.AbstractFruit;
import org.mule.tck.testmodels.fruit.Apple;
import org.mule.tck.testmodels.fruit.Banana;
import org.mule.tck.testmodels.fruit.Fruit;
import org.mule.tck.testmodels.fruit.FruitBowl;
import org.mule.tck.testmodels.fruit.Orange;
import org.mule.tck.testmodels.fruit.WaterMelon;

import java.lang.reflect.Method;
import java.net.URL;
import java.util.Arrays;
import java.util.Enumeration;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class ClassUtilsTestCase extends AbstractMuleTestCase
{

    // we do not want to match these methods when looking for a service method to
    // invoke
    protected final Set<String> ignoreMethods = new HashSet<String>(Arrays.asList("equals",
                                                                                  "getInvocationHandler"));

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
        Object object = ClassUtils.instanciateClass("org.mule.tck.testmodels.fruit.Orange");
        assertNotNull(object);
        assertTrue(object instanceof Orange);

        object = ClassUtils.instanciateClass("org.mule.tck.testmodels.fruit.FruitBowl", new Apple(), new Banana());
        assertNotNull(object);
        assertTrue(object instanceof FruitBowl);

        FruitBowl bowl = (FruitBowl) object;

        assertTrue(bowl.hasApple());
        assertTrue(bowl.hasBanana());

        try
        {
            ClassUtils.instanciateClass("java.lang.Bing");
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
        URL resource = ClassUtils.getResource("log4j.properties", getClass());
        assertNotNull(resource);

        resource = ClassUtils.getResource("does-not-exist.properties", getClass());
        assertNull(resource);
    }

    public void testLoadingResourceEnumeration() throws Exception
    {
        Enumeration enumeration = ClassUtils.getResources("log4j.properties", getClass());
        assertNotNull(enumeration);
        assertTrue(enumeration.hasMoreElements());

        enumeration = ClassUtils.getResources("does-not-exist.properties", getClass());
        assertNotNull(enumeration);
        assertTrue(!enumeration.hasMoreElements());
    }

    public void testGetSatisfiableMethods() throws Exception
    {
        List methods = ClassUtils.getSatisfiableMethods(FruitBowl.class, new Class[]{Apple.class}, true,
                true, ignoreMethods);
        assertNotNull(methods);
        assertEquals(2, methods.size());

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
        assertEquals("doSomethingElse", ((Method) methods.get(0)).getName());

        // Test object param being acceptible by interface Type
        methods = ClassUtils.getSatisfiableMethods(FruitBowl.class, new Class[]{WaterMelon[].class}, true,
                true, ignoreMethods);
        assertNotNull(methods);
        assertEquals(1, methods.size());
        assertEquals("setFruit", ((Method) methods.get(0)).getName());
    }

    public void testSimpleName()
    {
        simpleNameHelper("String", "foo".getClass());
        simpleNameHelper("int[]", (new int[0]).getClass());
        simpleNameHelper("Object[][]", (new Object[0][0]).getClass());
        simpleNameHelper("null", null);
    }

    public void testEqual()
    {
        Object a1 = new HashBlob(1);
        Object a2 = new HashBlob(1);
        Object b = new HashBlob(2);
        assertTrue(ClassUtils.equal(a1, a2));
        assertTrue(ClassUtils.equal(b, b));
        assertTrue(ClassUtils.equal(null, null));
        assertFalse(ClassUtils.equal(a1, b));
        assertFalse(ClassUtils.equal(a2, b));
        assertFalse(ClassUtils.equal(null, b));
        assertFalse(ClassUtils.equal(b, a1));
        assertFalse(ClassUtils.equal(b, a2));
        assertFalse(ClassUtils.equal(b, null));
    }

    public void testHash()
    {
        Object a = new HashBlob(1);
        Object b = new HashBlob(2);
        assertTrue(ClassUtils.hash(new Object[]{a, b, a, b}) == ClassUtils.hash(new Object[]{a, b, a, b}));
        assertFalse(ClassUtils.hash(new Object[]{a, b, a}) == ClassUtils.hash(new Object[]{a, b, a, b}));
        assertFalse(ClassUtils.hash(new Object[]{a, b, a, a}) == ClassUtils.hash(new Object[]{a, b, a, b}));
        assertFalse(ClassUtils.hash(new Object[]{b, a, b, a}) == ClassUtils.hash(new Object[]{a, b, a, b}));
    }

    public void testClassTypesWithNullInArray()
    {
        Object[] array = new Object[]{"hello", null, "world"};
        Class[] classTypes = ClassUtils.getClassTypes(array);
        assertEquals(3, classTypes.length);
        assertEquals(String.class, classTypes[0]);
        assertEquals(null, classTypes[1]);
        assertEquals(String.class, classTypes[2]);
    }

    public void testCompareWithNull()
    {
        Class[] c1 = new Class[]{String.class, Integer.class};
        Class[] c2 = new Class[]{String.class, null};
        assertFalse(ClassUtils.compare(c1, c2, true));
        assertFalse(ClassUtils.compare(c2, c1, true));
    }

    private void simpleNameHelper(String target, Class clazz)
    {
        assertEquals(target, ClassUtils.getSimpleName(clazz));
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

    private static class HashBlob
    {

        private int hash;

        public HashBlob(int hash)
        {
            this.hash = hash;
        }

        public int hashCode()
        {
            return hash;
        }

        public boolean equals(Object other)
        {
            if (null == other || !getClass().equals(other.getClass()))
            {
                return false;
            }
            return hash == ((HashBlob) other).hash;
        }

    }

}
