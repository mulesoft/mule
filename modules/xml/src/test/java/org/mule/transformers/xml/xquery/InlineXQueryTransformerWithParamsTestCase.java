/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.transformers.xml.xquery;

import org.mule.DefaultMuleMessage;
import org.mule.api.transformer.Transformer;
import org.mule.module.xml.transformer.XQueryTransformer;
import org.mule.transformer.AbstractTransformerTestCase;
import org.mule.transformer.types.DataTypeFactory;
import org.mule.util.IOUtils;

import java.util.HashMap;
import java.util.Map;

import org.custommonkey.xmlunit.XMLUnit;
import org.w3c.dom.Document;

public class InlineXQueryTransformerWithParamsTestCase extends AbstractTransformerTestCase
{
    private String srcData;
    private String resultData;

    @Override
    protected void doSetUp() throws Exception
    {
        XMLUnit.setIgnoreWhitespace(true);
        XMLUnit.setIgnoreComments(true);
        srcData = IOUtils.getResourceAsString("cd-catalog.xml", getClass());
        resultData = IOUtils.getResourceAsString("cd-catalog-result-with-params.xml", getClass());
    }

    @Override
    public Transformer getTransformer() throws Exception
    {
        XQueryTransformer transformer = new XQueryTransformer();
        transformer.setXquery(
                "declare variable $document external;\n" +
                "declare variable $title external;\n" +
                "declare variable $rating external;\n" +
                " <cd-listings title='{$title}' rating='{$rating}'>\n" +
                "{\n" +
                "    for $cd in $document/catalog/cd\n" +
                "    return <cd-title>{data($cd/title)}</cd-title>\n" +
                "} \n</cd-listings>");
        transformer.setReturnDataType(DataTypeFactory.STRING);

        Map<String, Object> params = new HashMap<String, Object>();
        params.put("title", "#[header:ListTitle]");
        params.put("rating", "#[header:ListRating]");
        transformer.setContextProperties(params);

        transformer.setMuleContext(muleContext);
        transformer.initialise();
        return transformer;
    }


    @Override
    public Transformer getRoundTripTransformer() throws Exception
    {
        return null;
    }

    @Override
    public void testRoundtripTransform() throws Exception
    {
        // disable this test
    }

    @Override
    public Object getTestData()
    {
        Map<String, Object> props = new HashMap<String, Object>(2);
        props.put("ListTitle", "MyList");
        props.put("ListRating", new Integer(6));
        return new DefaultMuleMessage(srcData, props, muleContext);
    }

    @Override
    public Object getResultData()
    {
        return resultData;
    }

    @Override
    public boolean compareResults(Object expected, Object result)
    {
        if (expected instanceof Document && result instanceof Document)
        {
            return XMLUnit.compareXML((Document)expected, (Document)result).similar();
        }
        else if (expected instanceof String && result instanceof String)
        {
            try
            {
                String expectedString = this.normalizeString((String)expected);
                String resultString = this.normalizeString((String)result);
                return XMLUnit.compareXML(expectedString, resultString).similar();
            }
            catch (Exception ex)
            {
                return false;
            }
        }

        // all other comparisons are passed up
        return super.compareResults(expected, result);
    }
}
