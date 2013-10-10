/*
 * (c) 2003-2014 MuleSoft, Inc. This software is protected under international copyright
 * law. All use of this software is subject to MuleSoft's Master Subscription Agreement
 * (or other master license agreement) separately entered into in writing between you and
 * MuleSoft. If such an agreement is not in place, you may not use the software.
 */
package org.mule.transformers.xml.xquery;

import org.mule.util.IOUtils;

import java.io.InputStream;

import org.custommonkey.xmlunit.XMLUnit;

public class InlineXQueryTransformerByteArrayTestCase extends InlineXQueryTransformerTestCase
{
    private byte[] srcData;
    private String resultData;

    @Override
    protected void doSetUp() throws Exception
    {
        XMLUnit.setIgnoreWhitespace(true);
        srcData = IOUtils.toByteArray(IOUtils.getResourceAsStream("cdcatalog-utf-8.xml", getClass()));
        
        InputStream resourceStream = IOUtils.getResourceAsStream("cdcatalog-result-utf-8.xml", getClass());
        resultData = new String(IOUtils.toByteArray(resourceStream), "UTF-8");
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
}
