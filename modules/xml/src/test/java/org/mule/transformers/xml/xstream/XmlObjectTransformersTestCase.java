/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.transformers.xml.xstream;

import org.mule.api.transformer.Transformer;
import org.mule.module.xml.transformer.ObjectToXml;
import org.mule.module.xml.transformer.XmlToObject;
import org.mule.tck.testmodels.fruit.Apple;
import org.mule.transformers.xml.AbstractXmlTransformerTestCase;

import java.io.ByteArrayInputStream;
import java.util.HashMap;
import java.util.Map;

import org.junit.Test;

import static org.junit.Assert.assertEquals;

public class XmlObjectTransformersTestCase extends AbstractXmlTransformerTestCase
{
    private Apple testObject = null;
    private Map<String, Class<?>> aliases = new HashMap<String, Class<?>>();

    public XmlObjectTransformersTestCase()
    {
        aliases.put("apple", Apple.class);
        testObject = new Apple();
        testObject.wash();
    }

    @Override
    public Transformer getTransformer() throws Exception
    {
        ObjectToXml trans =  createObject(ObjectToXml.class);
        trans.setAliases(aliases);
        return trans;
    }

    @Override
    public Transformer getRoundTripTransformer() throws Exception
    {
        XmlToObject trans = createObject(XmlToObject.class);
        trans.setAliases(aliases);
        return trans;
    }

    @Override
    public Object getTestData()
    {
        return testObject;
    }

    @Override
    public Object getResultData()
    {
        return "<apple>\n" + "  <bitten>false</bitten>\n"
               + "  <washed>true</washed>\n" + "</apple>";
    }

    @Test
    public void testStreaming() throws Exception
    {
        XmlToObject transformer = createObject(XmlToObject.class);
        transformer.setAliases(aliases);

        String input = (String) this.getResultData();
        assertEquals(testObject, transformer.transform(new ByteArrayInputStream(input.getBytes())));
    }
}
