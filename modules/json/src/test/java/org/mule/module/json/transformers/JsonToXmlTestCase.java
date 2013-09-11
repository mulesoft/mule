/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.module.json.transformers;

import org.mule.api.transformer.TransformerException;
import org.mule.module.json.transformers.JsonToXml;

import java.io.ByteArrayInputStream;
import java.io.StringReader;

import org.junit.Test;
import static org.junit.Assert.*;

public class JsonToXmlTestCase
{
    @Test
    public void testConversion() throws Exception
    {
        String json = "{\n" +
            "  \"customer\" : {\n" +
            "    \"id\" : 112,\n" +
            "    \"first-name\" : \"Jane\",\n" +
            "    \"last-name\" : \"Doe\",\n" +
            "    \"address\" : {\n" +
            "      \"street\" : \"123 A Street\"\n" +
            "    },\n" +
            "    \"phone-number\" : [ {\n" +
            "      \"@type\" : \"work\",\n" +
            "      \"$\" : \"555-1111\"\n" +
            "    }, {\n" +
            "      \"@type\" : \"cell\",\n" +
            "      \"$\" : \"555-2222\"\n" +
            "    } ]\n" +
            "  }\n" +
            "}";

        String xml = "<?xml version='1.0'?><customer><id>112</id><first-name>Jane</first-name><last-name>Doe</last-name><address><street>123 A Street</street></address><phone-number type=\"work\">555-1111</phone-number><phone-number type=\"cell\">555-2222</phone-number></customer>";

        JsonToXml jToX = new JsonToXml();
        String xmlResponse = (String) jToX.transform(json);
        assertEquals(xml, xmlResponse);

        xmlResponse = (String) jToX.transform(new StringReader(json));
        assertEquals(xml, xmlResponse);

        xmlResponse = (String) jToX.transform(json.getBytes());
        assertEquals(xml, xmlResponse);

        xmlResponse = (String) jToX.transform(new ByteArrayInputStream(json.getBytes()));
        assertEquals(xml, xmlResponse);

        try
        {
            jToX.transform(new Object());
            fail();
        }
        catch (Exception ex)
        {
            assertTrue(ex instanceof TransformerException);
        }
    }
}
