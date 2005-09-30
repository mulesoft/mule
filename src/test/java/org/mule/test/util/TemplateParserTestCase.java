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

import java.util.HashMap;
import java.util.Map;

import junit.framework.TestCase;

import org.mule.util.TemplateParser;

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
}
