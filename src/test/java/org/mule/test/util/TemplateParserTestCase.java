/*
 * $Header$
 * $Revision$
 * $Date$
 * ------------------------------------------------------------------------------------------------------
 *
 * Copyright (c) Cubis Limited. All rights reserved.
 * http://www.cubis.co.uk
 *
 * The software in this package is published under the terms of the BSD
 * style license a copy of which has been included with this distribution in
 * the LICENSE.txt file.
 */
package org.mule.test.util;

import junit.framework.TestCase;
import org.mule.util.TemplateParser;

import java.util.HashMap;
import java.util.Map;

/**
 * @author <a href="mailto:ross.mason@cubis.co.uk">Ross Mason</a>
 * @version $Revision$
 */
public class TemplateParserTestCase extends TestCase
{
    public void testStringParser() {
        Map props = new HashMap();
        props.put("fromAddress", "ross.mason@cubis.co.uk");
        String string = "smtp://[fromAddress]";

        String result = TemplateParser.parseString(props, string);
        assertEquals("smtp://ross.mason@cubis.co.uk", result);
        string = "smtp://[toAddress]";
        result = TemplateParser.parseString(props, string);
        assertEquals("smtp://", result);

    }
}
