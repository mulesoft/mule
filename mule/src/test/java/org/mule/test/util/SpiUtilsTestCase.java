/*
 * $Id$
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

import org.mule.tck.testmodels.fruit.Apple;
import org.mule.tck.testmodels.fruit.Banana;
import org.mule.tck.testmodels.fruit.Fruit;
import org.mule.util.SpiUtils;

import java.io.InputStream;
import java.util.Properties;

import junit.framework.TestCase;

/**
 * <code>SpiHelperTestCase</code> test the methods of the SpiHelper.
 *
 * @author <a href="mailto:ross.mason@symphonysoft.com">Ross Mason</a>
 * @version $Revision$
 */

public class SpiUtilsTestCase extends TestCase
{

    public void testDiscoverDefault() throws Exception
    {
        Class c = SpiUtils.findService(Fruit.class, Banana.class.getName(), getClass());
        assertNotNull(c);
        assertEquals(Banana.class.getName(), c.getName());
    }

    public void testDiscoverNotFound() throws Exception
    {
        Class c = SpiUtils.findService(Fruit.class, getClass());
        assertNull(c);
    }

    public void testDiscoverFromProperty() throws Exception
    {
        System.setProperty(Fruit.class.getName(), Apple.class.getName());
        Class c = SpiUtils.findService(Fruit.class, getClass());
        assertNotNull(c);
        assertEquals(Apple.class.getName(), c.getName());
        Properties p = System.getProperties();
        p.remove(Fruit.class.getName());
        System.setProperties(p);
    }

    public void testDiscoverFromPropertyFile() throws Exception
    {
        final ClassLoader currentClassLoader = Thread.currentThread().getContextClassLoader();
        final InputStream is = currentClassLoader.getResourceAsStream("test-spi.properties");
        assertNotNull("Test resource not found.", is);
        Properties p = new Properties();
        p.load(is);
        assertNotNull(p);
        Class c = SpiUtils.findService(Fruit.class, p, getClass());
        assertNotNull(c);
        assertEquals(Banana.class.getName(), c.getName());
    }

    public void testDiscoverFromResource() throws Exception
    {
        Class c = SpiUtils.findService(Fruit.class, "test-spi.properties", Apple.class.getName(), getClass());
        assertNotNull(c);
        assertEquals(Banana.class.getName(), c.getName());
    }

}
