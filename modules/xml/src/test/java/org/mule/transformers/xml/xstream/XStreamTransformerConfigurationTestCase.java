/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.transformers.xml.xstream;

import org.mule.api.transformer.TransformerException;
import org.mule.module.xml.transformer.XStreamFactory;
import org.mule.module.xml.transformer.XmlToObject;
import org.mule.tck.junit4.AbstractMuleTestCase;

import com.thoughtworks.xstream.XStream;
import com.thoughtworks.xstream.io.xml.DomDriver;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

/**
 * Tests configuration and creation of XStream-based transformers
 */
public class XStreamTransformerConfigurationTestCase extends AbstractMuleTestCase
{
    public static volatile boolean MyDriverDidInitialize;

    protected static class MyDOMDriver extends DomDriver
    {
        public MyDOMDriver()
        {
            super();
            XStreamTransformerConfigurationTestCase.MyDriverDidInitialize = true;
        }
    }

    @Before
    public void doSetUp()
    {
        MyDriverDidInitialize = true;
    }

    @After
    public void doTearDown()
    {
        MyDriverDidInitialize = false;
    }

    @Test
    public void testDefaultDriver() throws Exception
    {
        XmlToObject transformer = new XmlToObject();
        // check for XStream's default
        assertEquals(XStreamFactory.XSTREAM_XPP_DRIVER, transformer.getDriverClass());
    }

    @Test
    public void testCustomDriver() throws Exception
    {
        XmlToObject transformer = new XmlToObject();
        // set custom driver
        transformer.setDriverClass(MyDOMDriver.class.getName());
        XStream xs = transformer.getXStream();

        assertNotNull(xs);
        assertSame(xs, transformer.getXStream());
        assertTrue(MyDriverDidInitialize);
    }

    @Test
    public void testBadDriver() throws Exception
    {
        XmlToObject transformer = new XmlToObject();
        // set nonexisting driver class
        transformer.setDriverClass("DudeWhereIsMyDriver");

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

    @Test
    public void testClassLoader()
    {

        ClassLoader originalClassLoader = Thread.currentThread().getContextClassLoader();

        try
        {
            TestClassLoader classLoader =  new TestClassLoader();   
            Thread.currentThread().setContextClassLoader(classLoader);
            XmlToObject transformer = new XmlToObject();
            transformer.initialise();
            assertEquals(classLoader, transformer.getXStream().getClassLoader());
        }
        catch (Exception e)
        {
            fail(e.getMessage());
        }
        finally
        {
            Thread.currentThread().setContextClassLoader(originalClassLoader);
        }
    }

    private static class TestClassLoader extends ClassLoader
    {
    }

}
