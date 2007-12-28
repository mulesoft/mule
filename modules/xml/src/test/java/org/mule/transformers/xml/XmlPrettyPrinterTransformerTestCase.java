/*
 * $Id$
 * --------------------------------------------------------------------------------------
 * Copyright (c) MuleSource, Inc.  All rights reserved.  http://www.mulesource.com
 *
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.transformers.xml;

import org.mule.tck.AbstractTransformerTestCase;
import org.mule.umo.transformer.UMOTransformer;

import org.dom4j.io.OutputFormat;

public class XmlPrettyPrinterTransformerTestCase extends AbstractTransformerTestCase
{

    // Do not normalize any Strings for this test since we need to test formatting
    protected String normalizeString(String rawString)
    {
        return rawString;
    }

    public Object getResultData()
    {
        return "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n\n" + "<just>\n" + "  <a>\n"
               + "    <test>test</test>\n" + "  </a>\n" + "</just>\n";
    }

    public UMOTransformer getRoundTripTransformer() throws Exception
    {
        // there is no XmlUnprettyPrinter :)
        return null;
    }

    public Object getTestData()
    {
        return "<?xml version=\"1.0\" encoding=\"UTF-8\"?><just><a><test>test</test></a></just>";
    }

    public UMOTransformer getTransformer() throws Exception
    {
        return new XmlPrettyPrinterTransformer();
    }

    public void testOutputOptions()
    {
        XmlPrettyPrinterTransformer t = new XmlPrettyPrinterTransformer();
        OutputFormat f = t.getOutputFormat();
        assertEquals(2, f.getIndent().length());
        assertFalse(f.isPadText());

        t.setIndentSize(4);
        t.setPadText(true);
        assertEquals(4, f.getIndent().length());
        assertTrue(f.isPadText());
    }

}
