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

import java.io.InputStream;
import java.util.Properties;

import junit.framework.TestCase;

import org.mule.tck.testmodels.fruit.Apple;
import org.mule.tck.testmodels.fruit.Banana;
import org.mule.tck.testmodels.fruit.Fruit;
import org.mule.util.IOUtils;
import org.mule.util.SpiUtils;

/**
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
        InputStream is = IOUtils.getResourceAsStream("test-spi.properties", getClass());
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
