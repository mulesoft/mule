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

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.commons.collections.map.CaseInsensitiveMap;

public class MapUtilsTestCase extends AbstractMuleTestCase
{

    public void testMapCreationNullClass()
    {
        try
        {
            MapUtils.mapWithKeysAndValues(null, (String[])null, (String[])null);
            fail();
        }
        catch (IllegalArgumentException ex)
        {
            // expected
        }
    }

    public void testMapCreationWithoutElements()
    {
        Map m = MapUtils.mapWithKeysAndValues(HashMap.class, (List)null, (List)null);
        assertTrue(m.isEmpty());
    }

    public void testCaseInsensitiveMapCreation()
    {
        List strings = Arrays.asList(new String[]{"foo"});

        Map m = MapUtils.mapWithKeysAndValues(CaseInsensitiveMap.class, strings.iterator(), strings
            .iterator());

        assertEquals("foo", m.get("foo"));
        assertEquals("foo", m.get("Foo"));
        assertEquals("foo", m.get("FOO"));
    }

    public void testToStringNull() throws Exception
    {
        Map props = null;
        assertEquals("{}", MapUtils.toString(props, false));
        assertEquals("{}", MapUtils.toString(props, true));
    }

    public void testToStringEmpty() throws Exception
    {
        Map props = new HashMap();
        assertEquals("{}", MapUtils.toString(props, false));
        assertEquals("{}", MapUtils.toString(props, true));
    }

    public void testToStringSingleElement() throws Exception
    {
        Map props = MapUtils.mapWithKeysAndValues(HashMap.class, new Object[]{"foo"}, new Object[]{"bar"});

        assertEquals("{foo=bar}", MapUtils.toString(props, false));
        assertEquals("{" + SystemUtils.LINE_SEPARATOR + "foo=bar" + SystemUtils.LINE_SEPARATOR + "}",
            MapUtils.toString(props, true));
    }

    public void testToStringMultipleElements() throws Exception
    {
        Map props = MapUtils.mapWithKeysAndValues(HashMap.class, new Object[]{"foo", "foozle"}, new Object[]{
            "bar", "doozle"});

        String result = MapUtils.toString(props, false);
        assertTrue(result.indexOf("foo=bar") != -1);
        assertTrue(result.indexOf("foozle=doozle") != -1);

        result = MapUtils.toString(props, true);
        assertTrue(result.startsWith("{" + SystemUtils.LINE_SEPARATOR));
        assertTrue(result.indexOf("foo=bar") != -1);
        assertTrue(result.indexOf("foozle=doozle") != -1);
        assertTrue(result.endsWith(SystemUtils.LINE_SEPARATOR + "}"));
    }

}
