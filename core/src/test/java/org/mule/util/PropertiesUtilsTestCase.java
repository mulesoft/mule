/*
 * $Id$
 * --------------------------------------------------------------------------------------
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 *
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.util;

import org.mule.tck.junit4.AbstractMuleTestCase;
import org.mule.tck.size.SmallTest;

import java.text.NumberFormat;
import java.util.HashMap;
import java.util.Map;

import org.apache.commons.collections.keyvalue.DefaultMapEntry;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

@SmallTest
public class PropertiesUtilsTestCase extends AbstractMuleTestCase
{

    @Test
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

    @Test
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

    @Test
    public void testRemoveNamespaces() throws Exception
    {
        Map props = new HashMap();

        props.put("blah.booleanProperty", "true");
        props.put("blah.blah.doubleProperty", NumberFormat.getInstance().format(0.124));
        props.put("blah.blah.Blah.intProperty", "14");
        props.put("longProperty", "999999999");
        props.put("3456.stringProperty", "string");

        props = PropertiesUtils.removeNamespaces(props);

        assertTrue(MapUtils.getBooleanValue(props, "booleanProperty", false));
        assertEquals(0.124, 0, MapUtils.getDoubleValue(props, "doubleProperty", 0));
        assertEquals(14, MapUtils.getIntValue(props, "intProperty", 0));
        assertEquals(999999999, 0, MapUtils.getLongValue(props, "longProperty", 0));
        assertEquals("string", MapUtils.getString(props, "stringProperty", ""));
    }

    @Test
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
