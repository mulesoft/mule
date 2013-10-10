/*
 * (c) 2003-2014 MuleSoft, Inc. This software is protected under international copyright
 * law. All use of this software is subject to MuleSoft's Master Subscription Agreement
 * (or other master license agreement) separately entered into in writing between you and
 * MuleSoft. If such an agreement is not in place, you may not use the software.
 */
package org.mule.transformers.xml;

import org.mule.api.transformer.Transformer;
import org.mule.module.xml.transformer.XmlToDomDocument;
import org.mule.transformer.types.DataTypeFactory;
import org.mule.util.IOUtils;

import java.io.InputStream;

import org.dom4j.DocumentHelper;
import org.dom4j.io.DOMWriter;

public class DomXmlTransformerEncodingByteArrayTestCase extends DomXmlTransformerEncodingTestCase
{
    private byte[] srcData; // Parsed XML doc
    private String resultData; // String as US-ASCII

    @Override
    protected void doSetUp() throws Exception
    {
        InputStream resourceStream = IOUtils.getResourceAsStream("cdcatalog-utf-8.xml", getClass());
        srcData = IOUtils.toString(resourceStream, "UTF-8").getBytes("UTF-8");
        
        resourceStream = IOUtils.getResourceAsStream("cdcatalog-us-ascii.xml", getClass());
        resultData = IOUtils.toString(resourceStream, "US-ASCII");
    }
    
    @Override
    public Transformer getRoundTripTransformer() throws Exception
    {
        XmlToDomDocument trans = createObject(XmlToDomDocument.class); // encoding is not interesting
        trans.setReturnDataType(DataTypeFactory.create(byte[].class));
        return trans;
    }

    @Override
    public Object getTestData()
    {
        return srcData;
    }

    @Override
    public Object getResultData()
    {
        return resultData;
    }

    @Override
    public boolean compareResults(Object expected, Object result)
    {
        try
        {
            // This is only used during roundtrip test, so it will always be byte[] instances
            if (expected instanceof byte[])
            {
                org.dom4j.Document dom4jDoc = null;
                dom4jDoc = DocumentHelper.parseText(new String((byte[])expected, "UTF-8"));
                expected = new DOMWriter().write(dom4jDoc);
                dom4jDoc = DocumentHelper.parseText(new String((byte[])result, "UTF-8"));
                result = new DOMWriter().write(dom4jDoc);
            }
        }
        catch (Exception ex)
        {
            // ignored.
        }

        return super.compareResults(expected, result);
    }
}
