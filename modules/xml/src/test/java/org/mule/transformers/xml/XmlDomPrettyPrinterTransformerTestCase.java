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

import java.io.ByteArrayInputStream;

import javax.xml.parsers.DocumentBuilderFactory;

import static org.junit.Assert.fail;

public class XmlDomPrettyPrinterTransformerTestCase extends AbstractTransformerTestCase
{
    private static final String rawData ="<?xml version=\"1.0\" encoding=\"UTF-8\"?><just><a><test>test</test></a></just>";

    // Do not normalize any Strings for this test since we need to test formatting
    protected String normalizeString(String rawString)
    {
        return rawData;
    }

    public Object getResultData()
    {
        return "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n\n" + "<just>\n" + "  <a>\n"
               + "    <test>test</test>\n" + "  </a>\n" + "</just>\n";
    }

    public Transformer getRoundTripTransformer() throws Exception
    {
        // there is no XmlUnprettyPrinter :)
        return null;
    }

    public Object getTestData()
    {
        try
        {
            return DocumentBuilderFactory.newInstance().newDocumentBuilder().parse(new ByteArrayInputStream(rawData.getBytes()));
        }
        catch (Exception e)
        {
            fail(e.getMessage());
            return null;
        }

    }

    public Transformer getTransformer() throws Exception
    {
        return createObject(XmlPrettyPrinter.class);
    }
}
