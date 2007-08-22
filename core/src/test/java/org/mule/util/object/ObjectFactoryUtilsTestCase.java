/*
 * $Id$
 * --------------------------------------------------------------------------------------
 * Copyright (c) MuleSource, Inc.  All rights reserved.  http://www.mulesource.com
 *
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.util.object;

import org.mule.tck.AbstractMuleTestCase;

public class ObjectFactoryUtilsTestCase extends AbstractMuleTestCase
{

    public void testCreateIfNecessaryObject() throws Exception
    {
        String hello = "hello";
        assertEquals(hello, ObjectFactoryUtils.createIfNecessary(hello, String.class));
        SubClass subClass = new SubClass();
        assertEquals(subClass, ObjectFactoryUtils.createIfNecessary(subClass, Interface.class));
    }

    public void testCreateIfNecessaryBadObject() throws Exception
    {
        String hello = "hello";
        try
        {
            ObjectFactoryUtils.createIfNecessary(hello, Double.class);
            fail("no exception");
        }
        catch (IllegalArgumentException e)
        {
            // expected
        }
    }

    public void testCreateIfNecessaryFactory() throws Exception
    {
        ObjectFactory factory = new SimpleObjectFactory(SubClass.class);
        Object result = ObjectFactoryUtils.createIfNecessary(factory, Interface.class);
        assertTrue(result instanceof Interface);
    }

    public void testCreateIfNecessaryBadFactory() throws Exception
    {
        try
        {
            ObjectFactoryUtils.createIfNecessary(new SimpleObjectFactory(SubClass.class), Double.class);
            fail("no exception");
        }
        catch (IllegalArgumentException e)
        {
            // expected
        }
    }

    private static interface Interface
    {
        // empty
    }

    public static class SubClass implements Interface
    {
        // empty
    }

}
