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
