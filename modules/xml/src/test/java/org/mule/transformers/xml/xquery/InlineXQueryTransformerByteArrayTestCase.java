/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
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
