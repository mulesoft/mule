/*
 * (c) 2003-2014 MuleSoft, Inc. This software is protected under international copyright
 * law. All use of this software is subject to MuleSoft's Master Subscription Agreement
 * (or other master license agreement) separately entered into in writing between you and
 * MuleSoft. If such an agreement is not in place, you may not use the software.
 */
package org.mule.module.xml.config;

import org.mule.module.xml.transformer.XmlPrettyPrinter;
import org.mule.tck.junit4.FunctionalTestCase;

import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

public class XmlPrettyPrinterConfigurationTestCase extends FunctionalTestCase
{

    public XmlPrettyPrinterConfigurationTestCase()
    {
        setStartContext(false);
    }

    @Override
    protected String getConfigResources()
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
