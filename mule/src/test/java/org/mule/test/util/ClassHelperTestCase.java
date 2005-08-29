/* 
 * $Header$
 * $Revision$
 * $Date$
 * ------------------------------------------------------------------------------------------------------
 * 
 * Copyright (c) SymphonySoft Limited. All rights reserved.
 * http://www.symphonysoft.com
 * 
 * The software in this package is published under the terms of the BSD
 * style license a copy of which has been included with this distribution in
 * the LICENSE.txt file. 
 *
 */

package org.mule.test.util;

import junit.framework.TestCase;
import org.mule.tck.testmodels.fruit.AbstractFruit;
import org.mule.tck.testmodels.fruit.Apple;
import org.mule.tck.testmodels.fruit.Banana;
import org.mule.tck.testmodels.fruit.Fruit;
import org.mule.tck.testmodels.fruit.FruitBowl;
import org.mule.tck.testmodels.fruit.Orange;
import org.mule.tck.testmodels.fruit.WaterMelon;
import org.mule.util.ClassHelper;

import java.io.InputStream;
import java.net.URL;
import java.util.Enumeration;
import java.util.List;

/**
 * @author <a href="mailto:ross.mason@symphonysoft.com">Ross Mason</a>
 * @version $Revision$
 */
public class ClassHelperTestCase extends TestCase
{
    public void testIsConcrete() throws Exception
    {
        assertTrue(ClassHelper.isConcrete(Orange.class));
        assertTrue(!ClassHelper.isConcrete(Fruit.class));
        assertTrue(!ClassHelper.isConcrete(AbstractFruit.class));

        try {
            ClassHelper.isConcrete(null);
            fail("Class cannot be null, exception should be thrown");
        } catch (RuntimeException e) {
            // expected
        }
    }

    public void testLoadClass() throws Exception
    {
        Class clazz = ClassHelper.loadClass("java.lang.String", getClass());
        assertNotNull(clazz);

        assertEquals(clazz.getName(), "java.lang.String");

        try {
            clazz = ClassHelper.loadClass("java.lang.Bing", getClass());
            fail("Class not found exception should be found");
        } catch (ClassNotFoundException e) {
            // expected
        }

    }

    public void testInstanciateClass() throws Exception
    {
        Object object = ClassHelper.instanciateClass("org.mule.tck.testmodels.fruit.Orange", new Object[] {});
        assertNotNull(object);
        assertTrue(object instanceof Orange);

        object = ClassHelper.instanciateClass("org.mule.tck.testmodels.fruit.FruitBowl", new Object[] { new Apple(),
                new Banana() });
        assertNotNull(object);
        assertTrue(object instanceof FruitBowl);

        FruitBowl bowl = (FruitBowl) object;

        assertTrue(bowl.hasApple());
        assertTrue(bowl.hasBanana());

        try {
            object = ClassHelper.instanciateClass("java.lang.Bing", new Object[] {});
            fail("Class does not exist, exception should have been thrown");
        } catch (Exception e) {
            // expected
        }

    }

    public void testGetParameterTypes() throws Exception
    {
        FruitBowl bowl = new FruitBowl();

        Class[] classes = ClassHelper.getParameterTypes(bowl, "apple");
        assertNotNull(classes);
        assertEquals(1, classes.length);
        assertEquals(Apple.class, classes[0]);

        classes = ClassHelper.getParameterTypes(bowl, "invalid");
        assertNotNull(classes);
        assertEquals(0, classes.length);
    }

    public void testLoadingResources() throws Exception
    {
        URL resource = ClassHelper.getResource("test-dummy.properties", getClass());
        assertNotNull(resource);

        resource = ClassHelper.getResource("test-dummyX.properties", getClass());
        assertNull(resource);
    }

    public void testLoadingResourcesAsStream() throws Exception
    {
        InputStream is = ClassHelper.getResourceAsStream("test-dummy.properties", getClass());
        assertNotNull(is);

        is = ClassHelper.getResourceAsStream("test-dummyX.properties", getClass());
        assertNull(is);
    }

    public void testLoadingResourceEnumeration() throws Exception
    {

        Enumeration enumeration = ClassHelper.getResources("test-dummy.properties", getClass());
        assertNotNull(enumeration);
        assertTrue(enumeration.hasMoreElements());

        enumeration = ClassHelper.getResources("test-dummyX.properties", getClass());
        assertNotNull(enumeration);
        assertTrue(!enumeration.hasMoreElements());
    }

    public void testGetSatisfiableMethods() throws Exception
    {
        List methods = ClassHelper.getSatisfiableMethods(FruitBowl.class, new Class[]{Apple.class}, true, true);
        assertNotNull(methods);
        assertEquals(1, methods.size());

        methods = ClassHelper.getSatisfiableMethods(FruitBowl.class, new Class[]{Apple.class}, false, true);
        assertNotNull(methods);
        assertEquals(0, methods.size());

        methods = ClassHelper.getSatisfiableMethods(FruitBowl.class, new Class[]{WaterMelon.class}, true, true);
        assertNotNull(methods);
        assertEquals(0, methods.size());
    }
}
