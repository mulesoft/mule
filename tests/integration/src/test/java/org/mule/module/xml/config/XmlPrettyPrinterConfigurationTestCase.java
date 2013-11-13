/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.module.xml.config;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import org.mule.module.xml.transformer.XmlPrettyPrinter;
import org.mule.tck.junit4.FunctionalTestCase;

import org.junit.Test;

public class XmlPrettyPrinterConfigurationTestCase extends FunctionalTestCase
{
    public XmlPrettyPrinterConfigurationTestCase()
    {
        setStartContext(false);
    }

    @Override
    protected String getConfigFile()
    {
        return "org/mule/module/xml/xml-prettyprinter-config.xml";
    }

    @Test
    public void testPrettyPrinter()
    {
        XmlPrettyPrinter pp = (XmlPrettyPrinter) muleContext.getRegistry().lookupTransformer("MyXMLPrettyPrinter");

        assertNotNull(pp);
        assertEquals("ISO-8859-15", pp.getEncoding());
        assertEquals(true, pp.isExpandEmptyElements());
        assertEquals(true, pp.getIndentEnabled());
        assertEquals("   ", pp.getIndentString());
        assertEquals("\\n\\n", pp.getLineSeparator());
        assertEquals(1, pp.getNewLineAfterNTags());
        assertFalse(pp.isNewlines());
        assertFalse(pp.isNewLineAfterDeclaration());
        assertFalse(pp.isOmitEncoding());
        assertFalse(pp.isPadText());
        assertFalse(pp.isTrimText());
        assertFalse(pp.isSuppressDeclaration());
        assertTrue(pp.isXHTML());
    }
}
