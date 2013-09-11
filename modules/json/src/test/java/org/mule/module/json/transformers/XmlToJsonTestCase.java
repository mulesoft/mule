/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.module.json.transformers;

import org.mule.api.transformer.TransformerException;
import org.mule.module.xml.util.XMLUtils;
import org.mule.tck.junit4.AbstractMuleTestCase;

import java.io.ByteArrayInputStream;
import java.io.StringReader;

import org.junit.Test;
import org.skyscreamer.jsonassert.JSONAssert;
import org.w3c.dom.Document;

public class XmlToJsonTestCase extends AbstractMuleTestCase
{
    private static final String EXPECTED_JSON =
        "{" +
        "    \"customer\" : {" +
        "        \"id\" : \"112\"," +
        "        \"first-name\" : \"Jane\"," +
        "        \"last-name\" : \"Doe\"," +
        "        \"address\" : {" +
        "            \"street\" : \"123 A Street\"" +
        "        }," +
        "        \"phone-number\" : [ {" +
        "            \"@type\" : \"work\"," +
        "            \"$\" : \"555-1111\"" +
        "        }, {" +
        "            \"@type\" : \"cell\"," +
        "            \"$\" : \"555-2222\"" +
        "        } ]" +
        "    }" +
        "}";

    private static final String EXPECTED_JSON_WITH_NAMESPACE =
        "{" +
        "    \"cust:customer\" : {" +
        "        \"@xmlns:cust\" : \"http:customer.com\"," +
        "        \"cust:id\" : \"112\"," +
        "        \"cust:first-name\" : \"Jane\"," +
        "        \"cust:last-name\" : \"Doe\"," +
        "        \"cust:address\" : {" +
        "           \"cust:street\" : \"123 A Street\"" +
        "        }," +
        "        \"cust:phone-number\" : [ {" +
        "            \"@type\" : \"work\"," +
        "            \"$\" : \"555-1111\"" +
        "        }, {" +
        "            \"@type\" : \"cell\"," +
        "            \"$\" : \"555-2222\"" +
        "        } ]" +
        "    }" +
        "}";

    private static final String XML =
        "<?xml version=\"1.0\" ?>" +
        "<customer>" +
        "    <id>112</id>" +
        "    <first-name>Jane</first-name>" +
        "    <last-name>Doe</last-name>" +
        "    <address>" +
        "        <street>123 A Street</street>" +
        "    </address>" +
        "    <phone-number type=\"work\">555-1111</phone-number>" +
        "    <phone-number type=\"cell\">555-2222</phone-number>" +
        "</customer>";

    private static final String XML_WITH_NAMESPACE =
        "<?xml version=\"1.0\" ?>" +
        "<cust:customer xmlns:cust=\"http:customer.com\">" +
        "    <cust:id>112</cust:id>" +
        "    <cust:first-name>Jane</cust:first-name>" +
        "    <cust:last-name>Doe</cust:last-name>" +
        "    <cust:address>" +
        "        <cust:street>123 A Street</cust:street>" +
        "    </cust:address>\n" +
        "    <cust:phone-number type=\"work\">555-1111</cust:phone-number>" +
        "    <cust:phone-number type=\"cell\">555-2222</cust:phone-number>" +
        "</cust:customer>";

    @Test
    public void stringInputShouldBeTransformedToJson() throws Exception
    {
        XmlToJson transformer = new XmlToJson();
        String jsonResponse = (String) transformer.transform(XML);
        JSONAssert.assertEquals(EXPECTED_JSON, jsonResponse, false);
    }

    @Test
    public void readerInputShouldBeTransformedToJson() throws Exception
    {
        StringReader reader = new StringReader(XML);

        XmlToJson transformer = new XmlToJson();
        String jsonResponse = (String) transformer.transform(reader);
        JSONAssert.assertEquals(EXPECTED_JSON, jsonResponse, false);
    }

    @Test
    public void byteArrayInputShouldBeTransformedToJson() throws Exception
    {
        byte[] bytes = XML.getBytes();

        XmlToJson transformer = new XmlToJson();
        String jsonResponse = (String) transformer.transform(bytes);
        JSONAssert.assertEquals(EXPECTED_JSON, jsonResponse, false);
    }

    @Test
    public void inputStreamInputShouldBeTransformedToJson() throws Exception
    {
        ByteArrayInputStream input = new ByteArrayInputStream(XML.getBytes());

        XmlToJson transformer = new XmlToJson();
        String jsonResponse = (String) transformer.transform(input);
        JSONAssert.assertEquals(EXPECTED_JSON, jsonResponse, false);
    }

    @Test
    public void documentInputShouldBeTransformedToJson() throws Exception
    {
        Document document = XMLUtils.toW3cDocument(XML);
        document.setDocumentURI("xxx");

        XmlToJson transformer = new XmlToJson();
        String jsonResponse = (String) transformer.transform(document);
        JSONAssert.assertEquals(EXPECTED_JSON, jsonResponse, false);
    }

    @Test(expected = TransformerException.class)
    public void badInputShouldThrow() throws Exception
    {
        XmlToJson transformer = new XmlToJson();
        transformer.transform(new Object());
    }

    @Test
    public void xmlWithNamespaceShouldBeTransformedToJson() throws Exception
    {
        XmlToJson transformer = new XmlToJson();
        String jsonResponse = (String) transformer.transform(XML_WITH_NAMESPACE);
        JSONAssert.assertEquals(EXPECTED_JSON_WITH_NAMESPACE, jsonResponse, false);
    }
}
