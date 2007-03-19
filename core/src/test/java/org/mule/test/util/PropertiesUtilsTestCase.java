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

import org.mule.util.MapUtils;
import org.mule.util.PropertiesUtils;

import java.util.HashMap;
import java.util.Map;

import junit.framework.TestCase;

import org.apache.commons.collections.keyvalue.DefaultMapEntry;

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

    public void testMaskedProperties()
    {
        // test nulls
        assertNull(PropertiesUtils.maskedPropertyValue(null));
        assertNull(PropertiesUtils.maskedPropertyValue(new DefaultMapEntry(null, "value")));
        assertNull(PropertiesUtils.maskedPropertyValue(new DefaultMapEntry("key", null)));

        // try non-masked value
        Map.Entry property = new DefaultMapEntry("secretname", "secret");
        assertTrue("secret".equals(PropertiesUtils.maskedPropertyValue(property)));

        // now mask value
        PropertiesUtils.registerMaskedPropertyName("secretname");
        String masked = PropertiesUtils.maskedPropertyValue(property);
        assertFalse("secret".equals(masked));
        assertTrue(masked.startsWith("*"));
    }

}
