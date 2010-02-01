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

import java.util.Map;

/**
 * TODO
 */
public class CaseInsensitiveHashMapTestCase extends AbstractMuleTestCase
{
    public void testMap() throws Exception
    {
        Map map = new CaseInsensitiveHashMap();
        map.put("FOO", "BAR");
        map.put("DOO", new Integer(3));

        assertEquals("BAR", map.get("FOO"));
        assertEquals("BAR", map.get("foo"));
        assertEquals("BAR", map.get("Foo"));

        assertEquals(new Integer(3), map.get("DOO"));
        assertEquals(new Integer(3), map.get("doo"));
        assertEquals(new Integer(3), map.get("Doo"));

        assertEquals(2, map.size());
        for (Object o : map.keySet())
        {
            assertTrue(o.equals("FOO") || o.equals("DOO"));
        }

    }
}
