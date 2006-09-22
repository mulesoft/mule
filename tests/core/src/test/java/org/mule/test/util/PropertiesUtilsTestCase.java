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

import org.apache.commons.lang.SystemUtils;
import org.mule.util.MapUtils;
import org.mule.util.PropertiesUtils;

import java.util.HashMap;
import java.util.Map;

import junit.framework.TestCase;

/**
 * @author <a href="mailto:ross.mason@symphonysoft.com">Ross Mason</a>
 * @version $Revision$
 */
public class PropertiesUtilsTestCase extends TestCase
{

    public void testRemoveNameSpacePrefix()
    {
        String temp = "this.is.a.namespace";
        String result = PropertiesUtils.removeNamespacePrefix(temp);
        assertEquals("namespace", result);

        temp = "this.namespace";
        result = PropertiesUtils.removeNamespacePrefix(temp);
        assertEquals("namespace", result);

        temp = "namespace";
        result = PropertiesUtils.removeNamespacePrefix(temp);
        assertEquals("namespace", result);

        temp = "this_is-a-namespace";
        result = PropertiesUtils.removeNamespacePrefix(temp);
        assertEquals("this_is-a-namespace", result);
    }

    public void testRemoveXMLNameSpacePrefix()
    {
        String temp = "j:namespace";
        String result = PropertiesUtils.removeXmlNamespacePrefix(temp);
        assertEquals("namespace", result);

        temp = "this-namespace";
        result = PropertiesUtils.removeNamespacePrefix(temp);
        assertEquals("this-namespace", result);

        temp = "namespace";
        result = PropertiesUtils.removeNamespacePrefix(temp);
        assertEquals("namespace", result);
    }

    public void testRemoveNamespaces() throws Exception
    {
        Map props = new HashMap();

        props.put("blah.booleanProperty", "true");
        props.put("blah.blah.doubleProperty", "0.1243");
        props.put("blah.blah.Blah.intProperty", "14");
        props.put("longProperty", "999999999");
        props.put("3456.stringProperty", "string");

        props = PropertiesUtils.removeNamespaces(props);

        assertTrue(MapUtils.getBooleanValue(props, "booleanProperty", false));
        assertEquals(0.1243, 0, MapUtils.getDoubleValue(props, "doubleProperty", 0));
        assertEquals(14, MapUtils.getIntValue(props, "intProperty", 0));
        assertEquals(999999999, 0, MapUtils.getLongValue(props, "longProperty", 0));
        assertEquals("string", MapUtils.getString(props, "stringProperty", ""));
    }

    public void testMapNull() throws Exception
    {
        Map props = null;
        assertEquals("{}", PropertiesUtils.propertiesToString(props, false));
        assertEquals("{}", PropertiesUtils.propertiesToString(props, true));
    }

    public void testMapEmpty() throws Exception
    {
        Map props = new HashMap();
        assertEquals("{}", PropertiesUtils.propertiesToString(props, false));
        assertEquals("{}", PropertiesUtils.propertiesToString(props, true));
    }

    public void testMapSingleElement() throws Exception
    {
        Map props = MapUtils.mapWithKeysAndValues(HashMap.class, new Object[]{"foo"},
                new Object[]{"bar"});

        assertEquals("{foo=bar}", PropertiesUtils.propertiesToString(props, false));
        assertEquals("{" + SystemUtils.LINE_SEPARATOR + "foo=bar" + SystemUtils.LINE_SEPARATOR + "}",
                PropertiesUtils.propertiesToString(props, true));
    }

    public void testMapTwoElements() throws Exception
    {
        Map props = MapUtils.mapWithKeysAndValues(HashMap.class, new Object[]{"foo","foozle"},
                new Object[]{"bar","doozle"});

        assertEquals("{foo=bar, foozle=doozle}", PropertiesUtils.propertiesToString(props, false));

        assertEquals("{" + SystemUtils.LINE_SEPARATOR + "foo=bar" + SystemUtils.LINE_SEPARATOR
                + "foozle=doozle" + SystemUtils.LINE_SEPARATOR + "}", PropertiesUtils
                .propertiesToString(props, true));
    }

}
