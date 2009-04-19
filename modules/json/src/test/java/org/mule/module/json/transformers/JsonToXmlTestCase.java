/*
 * $Id$
 * --------------------------------------------------------------------------------------
 * Copyright (c) MuleSource, Inc.  All rights reserved.  http://www.mulesource.com
 *
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.module.json.transformers;

import org.mule.api.transformer.Transformer;
import org.mule.transformer.AbstractTransformerTestCase;

import net.sf.json.JSONObject;
import net.sf.json.xml.XMLSerializer;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class JsonToXmlTestCase extends AbstractTransformerTestCase
{
    private static final Logger LOGGER = LoggerFactory.getLogger(JsonToXml.class);

    public void testMoreTransformations() throws Exception
    {
        JsonToXml jsonToXmlString = new JsonToXml();
        jsonToXmlString.initialise();

        String result = null;

        result = (String) jsonToXmlString.doTransform("foo", null);
        assertEquals(true, result.indexOf("string") >= 0);
        assertEquals(true, ((String) jsonToXmlString.transform("1")).indexOf("number") >= 0);
        assertEquals(true,
                ((String) jsonToXmlString.transform(Boolean.valueOf(false))).indexOf("boolean") >= 0);

        result = (String) jsonToXmlString.doTransform("{'name': 'foo', 'passwd': 'bar'}", null);
        LOGGER.info(result);
        assertEquals(true, result.indexOf("<o>") >= 0);
    }

    public void testEncoding() throws Exception
    {
        JsonToXml jsonToXmlString = new JsonToXml();
        jsonToXmlString.initialise();
        String result = null;
        result = (String) jsonToXmlString.doTransform("foo", "utf-8");
        assertTrue(result.indexOf("utf-8") >= 0);
        result = (String) jsonToXmlString.doTransform("foo", "UTF-8");
        assertTrue(result.indexOf("utf-8") < 0);

    }

    public boolean compareResults(Object expected, Object result)
    {
        JSONObject json1 = (JSONObject) new XMLSerializer().read((String) expected);
        JSONObject json2 = (JSONObject) new XMLSerializer().read((String) result);

        return json1.get("name").equals(json2.get("name"));
    }

    public Object getResultData()
    {
        return "<o><name type=\"string\">foo</name><passwd type=\"string\">bar</passwd></o>";
    }

    public Transformer getRoundTripTransformer() throws Exception
    {
        return null;
    }

    public Object getTestData()
    {
        return "{'name': 'foo', 'passwd': 'bar'}";
    }

    public Transformer getTransformer() throws Exception
    {
        return new JsonToXml();
    }
}

