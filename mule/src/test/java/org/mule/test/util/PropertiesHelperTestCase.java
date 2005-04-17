/* 
 * $Header$
 * $Revision$
 * $Date$
 * ------------------------------------------------------------------------------------------------------
 * 
 * Copyright (c) SymphonySoft Limited. All rights reserved.
 * http://www.symphonysoft.com
 * 
 * The software in this package is published under the terms of the BSD
 * style license a copy of which has been included with this distribution in
 * the LICENSE.txt file. 
 *
 */

package org.mule.test.util;

import junit.framework.TestCase;
import org.mule.util.PropertiesHelper;

import java.util.HashMap;
import java.util.Map;

/**
 * @author <a href="mailto:ross.mason@symphonysoft.com">Ross Mason</a>
 * @version $Revision$
 */
public class PropertiesHelperTestCase extends TestCase
{
    public void testPropertyHelpers() throws Exception
    {
        HashMap props = new HashMap();

        props.put("booleanProperty", "true");
        props.put("doubleProperty", "0.1243");
        props.put("intProperty", "14");
        props.put("longProperty", "999999999");
        props.put("stringProperty", "string");

        assertTrue(PropertiesHelper.getBooleanProperty(props, "booleanProperty", false));
        assertEquals(0.1243, 0, PropertiesHelper.getDoubleProperty(props, "doubleProperty", 0));
        assertEquals(14, PropertiesHelper.getIntProperty(props, "intProperty", 0));
        assertEquals(999999999, 0, PropertiesHelper.getLongProperty(props, "longProperty", 0));
        assertEquals("string", PropertiesHelper.getStringProperty(props, "stringProperty", ""));

        assertTrue(!PropertiesHelper.getBooleanProperty(props, "booleanPropertyX", false));
        assertEquals(1, 0, PropertiesHelper.getDoubleProperty(props, "doublePropertyX", 1));
        assertEquals(1, PropertiesHelper.getIntProperty(props, "intPropertyX", 1));
        assertEquals(1, 0, PropertiesHelper.getLongProperty(props, "longPropertyX", 1));
        assertEquals("", PropertiesHelper.getStringProperty(props, "stringPropertyX", ""));

    }

    public void testRemoveNameSpacePrefix()
    {
        String temp = "this.is.a.namespace";
        String result = PropertiesHelper.removeNamespacePrefix(temp);
        assertEquals("namespace", result);

        temp = "this.namespace";
        result = PropertiesHelper.removeNamespacePrefix(temp);
        assertEquals("namespace", result);

        temp = "namespace";
        result = PropertiesHelper.removeNamespacePrefix(temp);
        assertEquals("namespace", result);

        temp = "this_is-a-namespace";
        result = PropertiesHelper.removeNamespacePrefix(temp);
        assertEquals("this_is-a-namespace", result);
    }

    public void testRemoveXMLNameSpacePrefix()
    {
        String temp = "j:namespace";
        String result = PropertiesHelper.removeXmlNamespacePrefix(temp);
        assertEquals("namespace", result);

        temp = "this-namespace";
        result = PropertiesHelper.removeNamespacePrefix(temp);
        assertEquals("this-namespace", result);

        temp = "namespace";
        result = PropertiesHelper.removeNamespacePrefix(temp);
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

        props = PropertiesHelper.removeNamspaces(props);

        assertTrue(PropertiesHelper.getBooleanProperty(props, "booleanProperty", false));
        assertEquals(0.1243, 0, PropertiesHelper.getDoubleProperty(props, "doubleProperty", 0));
        assertEquals(14, PropertiesHelper.getIntProperty(props, "intProperty", 0));
        assertEquals(999999999, 0, PropertiesHelper.getLongProperty(props, "longProperty", 0));
        assertEquals("string", PropertiesHelper.getStringProperty(props, "stringProperty", ""));
    }

    public void testReverseProperties() throws Exception
    {
        HashMap props = new HashMap();

        props.put("name1", "value1");
        props.put("name2", "value2");

        Map newProps = PropertiesHelper.reverseProperties(props);
        assertEquals("name1", newProps.get("value1"));
        assertEquals("name2", newProps.get("value2"));
    }
}

