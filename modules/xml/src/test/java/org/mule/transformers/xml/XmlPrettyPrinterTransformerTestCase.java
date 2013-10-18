/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.transformers.xml;

import org.mule.api.transformer.Transformer;
import org.mule.module.xml.transformer.XmlPrettyPrinter;
import org.mule.transformer.AbstractTransformerTestCase;

import org.dom4j.io.OutputFormat;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

public class XmlPrettyPrinterTransformerTestCase extends AbstractTransformerTestCase
{
    // Do not normalize any Strings for this test since we need to test formatting
    @Override
    protected String normalizeString(String rawString)
    {
        return rawString;
    }

    @Override
    public Object getResultData()
    {
        return "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n\n" + "<just>\n" + "  <a>\n"
               + "    <test>test</test>\n" + "  </a>\n" + "</just>\n";
    }

    @Override
    public Transformer getRoundTripTransformer() throws Exception
    {
        // there is no XmlUnprettyPrinter :)
        return null;
    }

    @Override
    public Object getTestData()
    {
        return "<?xml version=\"1.0\" encoding=\"UTF-8\"?><just><a><test>test</test></a></just>";
    }

    @Override
    public Transformer getTransformer() throws Exception
    {
        return createObject(XmlPrettyPrinter.class);
    }

    @Test
    public void testOutputOptions()
    {
        XmlPrettyPrinter t = new XmlPrettyPrinter();
        OutputFormat f = t.getOutputFormat();
        assertEquals(2, f.getIndent().length());
        assertTrue(f.isPadText());

        t.setIndentSize(4);
        t.setPadText(true);
        assertEquals(4, f.getIndent().length());
        assertTrue(f.isPadText());
    }
}
