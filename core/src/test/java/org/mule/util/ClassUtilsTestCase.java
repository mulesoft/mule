/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.util;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;
import org.mule.tck.junit4.AbstractMuleTestCase;
import org.mule.tck.size.SmallTest;
import org.mule.tck.testmodels.fruit.AbstractFruit;
import org.mule.tck.testmodels.fruit.Apple;
import org.mule.tck.testmodels.fruit.Banana;
import org.mule.tck.testmodels.fruit.Fruit;
import org.mule.tck.testmodels.fruit.FruitBowl;
import org.mule.tck.testmodels.fruit.Orange;
import org.mule.tck.testmodels.fruit.WaterMelon;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.net.URL;
import java.util.Arrays;
import java.util.Enumeration;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.junit.Ignore;
import org.junit.Test;

@SmallTest
public class ClassUtilsTestCase extends AbstractMuleTestCase
{

    // we do not want to match these methods when looking for a service method to
    // invoke
    protected final Set<String> ignoreMethods = new HashSet<String>(Arrays.asList("equals",
                                                                                  "getInvocationHandler"));

    @Test
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

    @Test
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
    
    @Test
    public void testLoadPrimitiveClass() throws Exception
    {
        assertSame(ClassUtils.loadClass("boolean", getClass()), Boolean.TYPE);
        assertSame(ClassUtils.loadClass("byte", getClass()), Byte.TYPE);
        assertSame(ClassUtils.loadClass("char", getClass()), Character.TYPE);
        assertSame(ClassUtils.loadClass("double", getClass()), Double.TYPE);
        assertSame(ClassUtils.loadClass("float", getClass()), Float.TYPE);
        assertSame(ClassUtils.loadClass("int", getClass()), Integer.TYPE);
        assertSame(ClassUtils.loadClass("long", getClass()), Long.TYPE);
        assertSame(ClassUtils.loadClass("short", getClass()), Short.TYPE);
    }
    
    @Test
    public void testLoadClassOfType() throws Exception
    {

        Class<? extends Exception> clazz = ClassUtils.loadClass("java.lang.IllegalArgumentException", getClass(), Exception.class);
        assertNotNull(clazz);

        assertEquals(clazz.getName(), "java.lang.IllegalArgumentException");

        try
        {
            ClassUtils.loadClass("java.lang.UnsupportedOperationException", getClass(), String.class);            
            fail("IllegalArgumentException should be thrown since class is not of expected type");
        }
        catch (IllegalArgumentException e)
        {
            // expected
        }

    }

    @Test
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

    @Test
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

    @Test
    public void testLoadingResources() throws Exception
    {
        URL resource = ClassUtils.getResource("log4j.properties", getClass());
        assertNotNull(resource);

        resource = ClassUtils.getResource("does-not-exist.properties", getClass());
        assertNull(resource);
    }

    @Test
    public void testLoadingResourceEnumeration() throws Exception
    {
        Enumeration enumeration = ClassUtils.getResources("log4j.properties", getClass());
        assertNotNull(enumeration);
        assertTrue(enumeration.hasMoreElements());

        enumeration = ClassUtils.getResources("does-not-exist.properties", getClass());
        assertNotNull(enumeration);
        assertTrue(!enumeration.hasMoreElements());
    }

    @Test
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

    @Test
    public void testSimpleName()
    {
        simpleNameHelper("String", "foo".getClass());
        simpleNameHelper("int[]", (new int[0]).getClass());
        simpleNameHelper("Object[][]", (new Object[0][0]).getClass());
        simpleNameHelper("null", null);
    }

    @Test
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

    @Test
    public void testHash()
    {
        Object a = new HashBlob(1);
        Object b = new HashBlob(2);
        assertTrue(ClassUtils.hash(new Object[]{a, b, a, b}) == ClassUtils.hash(new Object[]{a, b, a, b}));
        assertFalse(ClassUtils.hash(new Object[]{a, b, a}) == ClassUtils.hash(new Object[]{a, b, a, b}));
        assertFalse(ClassUtils.hash(new Object[]{a, b, a, a}) == ClassUtils.hash(new Object[]{a, b, a, b}));
        assertFalse(ClassUtils.hash(new Object[]{b, a, b, a}) == ClassUtils.hash(new Object[]{a, b, a, b}));
    }

    @Test
    public void testClassTypesWithNullInArray()
    {
        Object[] array = new Object[]{"hello", null, "world"};
        Class<?>[] classTypes = ClassUtils.getClassTypes(array);
        assertEquals(3, classTypes.length);
        assertEquals(String.class, classTypes[0]);
        assertEquals(null, classTypes[1]);
        assertEquals(String.class, classTypes[2]);
    }

    @Test
    public void testCompareWithNull()
    {
        Class[] c1 = new Class[]{String.class, Integer.class};
        Class[] c2 = new Class[]{String.class, null};
        assertFalse(ClassUtils.compare(c1, c2, true));
        assertFalse(ClassUtils.compare(c2, c1, true));
    }

    @Test
    public void testGetFields()
    {
        List<Field> fields = ClassUtils.getDeclaredFields(DummyObject.class, false);
        assertEquals(2, fields.size());
        assertEquals("foo", fields.get(0).getName());
        assertEquals("bar", fields.get(1).getName());
    }

    @Test
    public void testGetFieldsWithInheritanceOnOrphan() {
        List<Field> fields = ClassUtils.getDeclaredFields(DummyObject.class, true);
        assertEquals(2, fields.size());
        assertEquals("foo", fields.get(0).getName());
        assertEquals("bar", fields.get(1).getName());
    }

    @Test
    public void testGetFieldsWithInheritanceOnChild()
    {
        List<Field> fields = ClassUtils.getDeclaredFields(ExtendedDummyObject.class, true);
        assertEquals(3, fields.size());
        assertEquals("extended", fields.get(0).getName());
        assertEquals("foo", fields.get(1).getName());
        assertEquals("bar", fields.get(2).getName());
    }

    @Test
    public void testGetFieldsWithNoResults()
    {
        assertTrue(ClassUtils.getDeclaredFields(Object.class, true).isEmpty());
    }

    @Test
    public void getMethodsAnnotatedWith()
    {
        List<Method> methods = ClassUtils.getMethodsAnnotatedWith(ExtendedDummyObject.class, Ignore.class);
        assertEquals(2, methods.size());
        assertEquals("getExtended", methods.get(0).getName());
        assertEquals("doSomething", methods.get(1).getName());
    }

    @Test
    public void getMethodsAnnotedWithNoResults()
    {
        assertTrue(ClassUtils.getMethodsAnnotatedWith(DummyObject.class, Test.class).isEmpty());
    }


    private void simpleNameHelper(String target, Class clazz)
    {
        assertEquals(target, ClassUtils.getSimpleName(clazz));
    }

    private static class DummyObject
    {

        private String foo;
        private String bar;

        public String getFoo()
        {
            return foo;
        }

        public String getBar()
        {
            return bar;
        }

        @Ignore
        public void doSomething(Object object)
        {
            // do nothing
        }

        public Object doSomethingElse(Object object)
        {
            return object;
        }
    }

    private static class ExtendedDummyObject extends DummyObject
    {

        private String extended;

        @Ignore
        public String getExtended()
        {
            return extended;
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
