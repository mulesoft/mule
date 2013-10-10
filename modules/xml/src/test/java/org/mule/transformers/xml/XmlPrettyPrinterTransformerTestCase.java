/*
 * (c) 2003-2014 MuleSoft, Inc. This software is protected under international copyright
 * law. All use of this software is subject to MuleSoft's Master Subscription Agreement
 * (or other master license agreement) separately entered into in writing between you and
 * MuleSoft. If such an agreement is not in place, you may not use the software.
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
