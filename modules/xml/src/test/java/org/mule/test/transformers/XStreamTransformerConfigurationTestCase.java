/*
 * $Id$
 * --------------------------------------------------------------------------------------
 * Copyright (c) MuleSource, Inc.  All rights reserved.  http://www.mulesource.com
 *
 * The software in this package is published under the terms of the MuleSource MPL
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.test.transformers;

import com.thoughtworks.xstream.XStream;
import com.thoughtworks.xstream.io.xml.DomDriver;

import junit.framework.TestCase;

import org.mule.transformers.xml.XmlToObject;
import org.mule.umo.transformer.TransformerException;
import org.mule.util.XStreamFactory;

/**
 * Tests configuration and creation of XStream-based transformers
 */
public class XStreamTransformerConfigurationTestCase extends TestCase
{
    public static boolean MyDriverDidInitialize;

    public void setUp()
    {
        MyDriverDidInitialize = true;
    }

    public void tearDown()
    {
        MyDriverDidInitialize = false;
    }

    public void testDefaultDriver() throws Exception
    {
        XmlToObject transformer = new XmlToObject();
        // check for XStream's default
        assertEquals(XStreamFactory.XSTREAM_XPP_DRIVER, transformer.getDriverClassName());
    }

    public void testCustomDriver() throws Exception
    {
        XmlToObject transformer = new XmlToObject();
        // set custom driver
        transformer.setDriverClassName(MyDOMDriver.class.getName());
        XStream xs = transformer.getXStream();

        assertNotNull(xs);
        assertSame(xs, transformer.getXStream());
        assertTrue(MyDriverDidInitialize);
    }

    public void testBadDriver() throws Exception
    {
        XmlToObject transformer = new XmlToObject();
        // set nonexisting driver class
        transformer.setDriverClassName("DudeWhereIsMyDriver");

        try
        {
            assertNotNull(transformer.getXStream());
            fail();
        }
        catch (TransformerException tex)
        {
            // OK
            assertTrue(tex.getCause() instanceof ClassNotFoundException);
        }
    }

    protected static class MyDOMDriver extends DomDriver
    {
        public MyDOMDriver()
        {
            super();
            XStreamTransformerConfigurationTestCase.MyDriverDidInitialize = true;
        }
    }

}
