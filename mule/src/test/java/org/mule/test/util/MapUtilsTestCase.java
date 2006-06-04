/*
 * $Id$
 * ------------------------------------------------------------------------------------------------------
 *
 * Copyright (c) SymphonySoft Limited. All rights reserved.
 * http://www.symphonysoft.com
 *
 * The software in this package is published under the terms of the BSD
 * style license a copy of which has been included with this distribution in
 * the LICENSE.txt file.
 */

package org.mule.test.util;

import org.apache.commons.collections.map.CaseInsensitiveMap;
import org.mule.util.MapUtils;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import junit.framework.TestCase;

/**
 * @author Holger Hoffstaette
 */

public class MapUtilsTestCase extends TestCase
{

    public void testMapCreationNullClass()
    {
        try {
            MapUtils.mapWithKeysAndValues(null, (String[])null, (String[])null);
            fail();
        }
        catch (IllegalArgumentException ex) {
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

}
