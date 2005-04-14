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
import org.mule.tck.testmodels.fruit.*;
import org.mule.util.ClassHelper;

/**
 * <code>ReflectionHelperTestCase</code> TODO (document class)
 *
 * @author <a href="mailto:ross.mason@symphonysoft.com">Ross Mason</a>
 * @version $Revision$
 */
public class ClassHelperTestCase extends TestCase
{

    /**
     * 
     */
    public ClassHelperTestCase()
    {
        super();
    }

    public void testIsConcrete() throws Exception
    {
        assertTrue(ClassHelper.isConcrete(Orange.class));
        assertTrue(!ClassHelper.isConcrete(Fruit.class));
        assertTrue(!ClassHelper.isConcrete(AbstractFruit.class));

        try
        {
            ClassHelper.isConcrete(null);
            fail("Class cannot be null, exception should be thrown");
        }
        catch (RuntimeException e)
        {
            // expected
        }
    }

    public void testLoadClass() throws Exception
    {
        Class clazz = ClassHelper.loadClass("java.lang.String", getClass());
        assertNotNull(clazz);

        assertEquals(clazz.getName(), "java.lang.String");

        try
        {
            clazz = ClassHelper.loadClass("java.lang.Bing", getClass());
            fail("Class not found exception should be found");
        }
        catch (ClassNotFoundException e)
        {
            // expected
        }

    }

    public void testInstanciateClass() throws Exception
    {
        Object object = ClassHelper.instanciateClass("org.mule.tck.testmodels.fruit.Orange", new Object[]{});
        assertNotNull(object);
        assertTrue(object instanceof Orange);

        object = ClassHelper.instanciateClass("org.mule.tck.testmodels.fruit.FruitBowl", new Object[]{new Apple(), new Banana()});
        assertNotNull(object);
        assertTrue(object instanceof FruitBowl);

        FruitBowl bowl = (FruitBowl) object;

        assertTrue(bowl.hasApple());
        assertTrue(bowl.hasBanana());

        try
        {
            object = ClassHelper.instanciateClass("java.lang.Bing", new Object[]{});
            fail("Class does not exist, exception should have been thrown");
        }
        catch (Exception e)
        {
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
}
