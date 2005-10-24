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
 */
package org.mule.test.util;

import junit.framework.TestCase;
import org.mule.util.TemplateParser;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * @author <a href="mailto:ross.mason@symphonysoft.com">Ross Mason</a>
 * @version $Revision$
 */
public class TemplateParserTestCase extends TestCase
{
    public void testStringParserSquareBraces()
    {
        TemplateParser tp = TemplateParser.createSquareBracesStyleParser();
        Map props = new HashMap();
        props.put("fromAddress", "ross.mason@symphonysoft.com");
        String string = "smtp://[fromAddress]";

        String result = tp.parse(props, string);
        assertEquals("smtp://ross.mason@symphonysoft.com", result);
        string = "smtp://[toAddress]";
        result = tp.parse(props, string);
        assertEquals("smtp://", result);

    }

    public void testStringParserAntBraces()
    {
        TemplateParser tp = TemplateParser.createAntStyleParser();
        Map props = new HashMap();
        props.put("prop1", "value1");
        props.put("prop2", "value2");
        String string = "Some String with ${prop1} and ${prop2} in it";

        String result = tp.parse(props, string);
        assertEquals("Some String with value1 and value2 in it", result);
        string = "${prop1}${prop1}${prop2}";
        result = tp.parse(props, string);
        assertEquals("value1value1value2", result);

    }

    public void testListParserAntBraces()
    {
        TemplateParser tp = TemplateParser.createAntStyleParser();
        Map props = new HashMap();
        props.put("prop1", "value1");
        props.put("prop2", "value2");
        List list = new ArrayList();
        list.add("Some String with ${prop1} and ${prop2} in it");
        list.add("Some String with ${prop1} in it");

        List result = tp.parse(props, list);
        assertEquals("Some String with value1 and value2 in it", result.get(0));
        assertEquals("Some String with value1 in it", result.get(1));

        result = tp.parse(props, (List)null);
        assertNotNull(result);
        assertEquals(0, result.size());
    }

    public void testMapParserAntBraces()
    {
        TemplateParser tp = TemplateParser.createAntStyleParser();
        Map props = new HashMap();
        props.put("prop1", "value1");
        props.put("prop2", "value2");
        Map map = new HashMap();
        map.put("value1", "Some String with ${prop1} and ${prop2} in it");
        map.put("value2", "Some String with ${prop1} in it");

        Map result = tp.parse(props, map);
        assertEquals("Some String with value1 and value2 in it", result.get("value1"));
        assertEquals("Some String with value1 in it", result.get("value2"));

        result = tp.parse(props, (Map)null);
        assertNotNull(result);
        assertEquals(0, result.size());
    }

    public void testStringParserAntBracesWithSimilarNames()
    {
        TemplateParser tp = TemplateParser.createAntStyleParser();
        Map props = new HashMap();
        props.put("prop1", "value1");
        props.put("prop1-2", "value2");
        String string = "Some String with ${prop1} and ${prop1-2} in it";

        String result = tp.parse(props, string);
        assertEquals("Some String with value1 and value2 in it", result);
        string = "A${prop1-2}B${prop1}C${prop2}";
        result = tp.parse(props, string);
        assertEquals("Avalue2Bvalue1C", result);

    }
}
