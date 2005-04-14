/*
 * $Header$
 * $Revision$
 * $Date$
 * ------------------------------------------------------------------------------------------------------
 *
 * Copyright (c) SymphonySoft Limited. All rights reserved. http://www.symphonysoft.com
 *
 * The software in this package is published under the terms of the BSD style
 * license a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 *
 */

package org.mule.test.util;

import junit.framework.TestCase;
import org.mule.tck.testmodels.fruit.Apple;
import org.mule.tck.testmodels.fruit.Banana;
import org.mule.tck.testmodels.fruit.Fruit;
import org.mule.util.SpiHelper;

import java.io.FileInputStream;
import java.util.Properties;


/**
 * <code>SpiHelperTestCase</code> test the methods of the SpiHelper.
 *
 * @author <a href="mailto:ross.mason@symphonysoft.com">Ross Mason</a>
 * @version $Revision$
 */


public class SpiHelperTestCase extends TestCase
{

    public void testDiscoverDefault() throws Exception
    {
        Class c = SpiHelper.findService(Fruit.class, Banana.class.getName(), getClass());
        assertNotNull(c);
        assertEquals(Banana.class.getName(), c.getName());
    }

    public void testDiscoverNotFound() throws Exception
    {
        Class c = SpiHelper.findService(Fruit.class, getClass());
        assertNull(c);
    }

    public void testDiscoverFromProperty() throws Exception
    {
        System.setProperty(Fruit.class.getName(), Apple.class.getName());
        Class c = SpiHelper.findService(Fruit.class, getClass());
        assertNotNull(c);
        assertEquals(Apple.class.getName(), c.getName());
        Properties p = System.getProperties();
        p.remove(Fruit.class.getName());
        System.setProperties(p);
    }

    public void testDiscoverFromPropertyFile() throws Exception
    {
        Properties p = new Properties();
        p.load(new FileInputStream("src/test/conf/test-spi.properties"));
        assertNotNull(p);
        Class c = SpiHelper.findService(Fruit.class, p, getClass());
        assertNotNull(c);
        assertEquals(Banana.class.getName(), c.getName());
    }

    public void testDiscoverFromResource() throws Exception
    {
        Class c = SpiHelper.findService(Fruit.class, "test-spi.properties", Apple.class.getName(), getClass());
        assertNotNull(c);
        assertEquals(Banana.class.getName(), c.getName());
    }

}