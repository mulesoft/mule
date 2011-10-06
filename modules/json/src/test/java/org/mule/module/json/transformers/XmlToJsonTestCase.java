/*
 * $Id$
 * --------------------------------------------------------------------------------------
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 *
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.module.json.transformers;

import org.junit.Test;
import org.mule.api.transformer.TransformerException;
import org.mule.module.xml.util.XMLUtils;
import org.w3c.dom.Document;

import java.io.ByteArrayInputStream;
import java.io.StringReader;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

public class XmlToJsonTestCase
{
    @Test
    public void testConversion() throws Exception
    {
        String json = "{\n" +
            "  \"customer\" : {\n" +
            "    \"id\" : \"112\",\n" +
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

        String xml = "<?xml version=\"1.0\" ?>\n" +
            "<customer>\n" +
            "\t<id>112</id>\n" +
            "\t<first-name>Jane</first-name>\n" +
            "\t<last-name>Doe</last-name>\n" +
            "\t<address>\n" +
            "\t\t<street>123 A Street</street>\n" +
            "\t</address>\n" +
            "\t<phone-number type=\"work\">555-1111</phone-number>\n" +
            "\t<phone-number type=\"cell\">555-2222</phone-number>\n" +
            "</customer>\n";

        XmlToJson xToJ = new XmlToJson();
        String jsonResponse = (String) xToJ.transform(xml);
        jsonResponse = jsonResponse.replaceAll("\r\n", "\n");
        assertEquals(json, jsonResponse);

        jsonResponse = (String) xToJ.transform(new StringReader(xml));
        jsonResponse = jsonResponse.replaceAll("\r\n", "\n");
        assertEquals(json, jsonResponse);

        jsonResponse = (String) xToJ.transform(xml.getBytes());
        jsonResponse = jsonResponse.replaceAll("\r\n", "\n");
        assertEquals(json, jsonResponse);

        jsonResponse = (String) xToJ.transform(new ByteArrayInputStream(xml.getBytes()));
        jsonResponse = jsonResponse.replaceAll("\r\n", "\n");
        assertEquals(json, jsonResponse);

        Document xmlDoc = XMLUtils.toW3cDocument(xml);
        xmlDoc.setDocumentURI("xxx");
        jsonResponse = (String) xToJ.transform(xmlDoc);
        jsonResponse = jsonResponse.replaceAll("\r\n", "\n");
        assertEquals(json, jsonResponse);


        try
        {
            xToJ.transform(new Object());
            fail();
        }
        catch (Exception ex)
        {
            assertTrue(ex instanceof TransformerException);
        }
    }
}
