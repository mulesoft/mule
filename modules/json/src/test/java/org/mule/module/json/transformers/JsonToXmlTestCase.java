/*
 * (c) 2003-2014 MuleSoft, Inc. This software is protected under international copyright
 * law. All use of this software is subject to MuleSoft's Master Subscription Agreement
 * (or other master license agreement) separately entered into in writing between you and
 * MuleSoft. If such an agreement is not in place, you may not use the software.
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
