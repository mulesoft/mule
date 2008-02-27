/*
 * $Id$
 * --------------------------------------------------------------------------------------
 * Copyright (c) MuleSource, Inc.  All rights reserved.  http://www.mulesource.com
 *
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.module.xml.config;

import org.mule.tck.FunctionalTestCase;
import org.mule.transformers.xml.XmlPrettyPrinter;

public class XmlPrettyPrinterConfigurationTestCase extends FunctionalTestCase
{

    protected String getConfigResources()
    {
        return "org/mule/module/xml/xml-prettyprinter-config.xml";
    }

    public void testPrettyPrinter()
    {
        XmlPrettyPrinter pp = (XmlPrettyPrinter) muleContext.getRegistry().lookupTransformer(
            "MyXMLPrettyPrinter");

        assertNotNull(pp);
        assertEquals("ISO-8859-15", pp.getEncoding());
        assertEquals(true, pp.isExpandEmptyElements());
        assertEquals(true, pp.getIndentEnabled());
        assertEquals("   ", pp.getIndentString());
        assertEquals("\\n\\n", pp.getLineSeparator());
        assertEquals(1, pp.getNewLineAfterNTags());
        assertFalse(pp.isNewlines());
        assertFalse(pp.isOmitEncoding());
        assertFalse(pp.isPadText());
        assertFalse(pp.isTrimText());
        assertFalse(pp.isSuppressDeclaration());
        assertTrue(pp.isXHTML());
    }

}
