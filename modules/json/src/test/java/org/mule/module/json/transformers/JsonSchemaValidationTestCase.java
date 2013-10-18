/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.module.json.transformers;

import org.junit.Test;
import org.mule.DefaultMuleMessage;
import org.mule.api.MuleMessage;
import org.mule.tck.junit4.AbstractMuleContextTestCase;
import org.mule.util.IOUtils;
import org.w3c.dom.ls.LSInput;
import org.w3c.dom.ls.LSResourceResolver;
import org.xml.sax.ErrorHandler;
import org.xml.sax.SAXException;
import org.xml.sax.SAXParseException;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.io.Reader;
import java.io.StringReader;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

public class JsonSchemaValidationTestCase extends AbstractMuleContextTestCase
{
    private int numErrors = 0;

    @Test
    public void testConversion() throws Exception
    {
        String json = "{\n" +
            "  \"cust:customer\" : {\n" +
            "    \"@xmlns:cust\" : \"http:customer.com\",\n" +
            "    \"cust:id\" : \"112\",\n" +
            "    \"cust:first-name\" : \"Jane\",\n" +
            "    \"cust:last-name\" : \"Doe\",\n" +
            "    \"cust:address\" : {\n" +
            "      \"cust:street\" : \"123 A Street\"\n" +
            "    },\n" +
            "    \"cust:phone-number\" : [ {\n" +
            "      \"@type\" : \"work\",\n" +
            "      \"$\" : \"555-1111\"\n" +
            "    }, {\n" +
            "      \"@type\" : \"cell\",\n" +
            "      \"$\" : \"555-2222\"\n" +
            "    } ]\n" +
            "  }\n" +
            "}";

        String badJson = "{\n" +
            "  \"cust:customer\" : {\n" +
            "    \"@xmlns:cust\" : \"http:customer.com\",\n" +
            "    \"cust:ID\" : \"112\",\n" +
            "    \"cust:first-name\" : \"Jane\",\n" +
            "    \"cust:last-name\" : \"Doe\",\n" +
            "    \"cust:address\" : {\n" +
            "      \"cust:street\" : \"123 A Street\"\n" +
            "    },\n" +
            "    \"cust:phone-number\" : [ {\n" +
            "      \"@type\" : \"work\",\n" +
            "      \"$\" : \"555-1111\"\n" +
            "    }, {\n" +
            "      \"@type\" : \"cell\",\n" +
            "      \"$\" : \"555-2222\"\n" +
            "    } ]\n" +
            "  }\n" +
            "}";

        String xml = "<?xml version=\"1.0\" ?>\n" +
            "<cust:customer xmlns:cust=\"http:customer.com\">\n" +
            "\t<cust:id>112</cust:id>\n" +
            "\t<cust:first-name>Jane</cust:first-name>\n" +
            "\t<cust:last-name>Doe</cust:last-name>\n" +
            "\t<cust:address>\n" +
            "\t\t<cust:street>123 A Street</cust:street>\n" +
            "\t</cust:address>\n" +
            "\t<cust:phone-number type=\"work\">555-1111</cust:phone-number>\n" +
            "\t<cust:phone-number type=\"cell\">555-2222</cust:phone-number>\n" +
            "</cust:customer>\n";

        XmlToJson xToJ = new XmlToJson();
        String jsonResponse = (String) xToJ.transform(xml);
        jsonResponse = jsonResponse.replaceAll("\r\n", "\n");
        assertEquals(json, jsonResponse);

        JsonSchemaValidationFilter filter = new JsonSchemaValidationFilter();
        filter.setSchemaLocations("customer.xsd");
        filter.setErrorHandler(new ErrorHandler()
        {
            @Override
            public void warning(SAXParseException exception) throws SAXException
            {
            }

            @Override
            public void error(SAXParseException exception) throws SAXException
            {
                numErrors++;
            }

            @Override
            public void fatalError(SAXParseException exception) throws SAXException
            {
                numErrors++;
            }
        });
        filter.setResourceResolver(new Resolver());
        filter.setReturnResult(true);
        filter.setMuleContext(muleContext);
        filter.initialise();

        MuleMessage msg = new DefaultMuleMessage(json, muleContext);
        boolean accepted = filter.accept(msg);
        assertTrue(accepted);
        assertEquals(json, ((String)msg.getPayload()).replaceAll("\r\n", "\n"));

        msg = new DefaultMuleMessage(new StringReader(json), muleContext);
        accepted = filter.accept(msg);
        assertTrue(accepted);
        assertEquals(json, ((String)msg.getPayload()).replaceAll("\r\n", "\n"));

        msg = new DefaultMuleMessage(json.getBytes(), muleContext);
        accepted = filter.accept(msg);
        assertTrue(accepted);
        assertEquals(json, ((String)msg.getPayload()).replaceAll("\r\n", "\n"));

        msg = new DefaultMuleMessage(new ByteArrayInputStream(json.getBytes()), muleContext);
        accepted = filter.accept(msg);
        assertTrue(accepted);
        assertEquals(json, ((String)msg.getPayload()).replaceAll("\r\n", "\n"));

        msg = new DefaultMuleMessage(badJson, muleContext);
        accepted = filter.accept(msg);
        assertTrue(!accepted);
    }

    class Resolver implements LSResourceResolver
    {
        private String schema = IOUtils.toString(getClass().getClassLoader().getResourceAsStream("customer.xsd"));
        @Override
        public LSInput resolveResource(String type, String namespaceURI, String publicId, String systemId, String baseURI)
        {
            return new LSInput()
            {
                @Override
                public Reader getCharacterStream()
                {
                    return new StringReader(schema);
                }

                @Override
                public void setCharacterStream(Reader characterStream)
                {
                }

                @Override
                public InputStream getByteStream()
                {
                    return new ByteArrayInputStream(schema.getBytes());
                }

                @Override
                public void setByteStream(InputStream byteStream)
                {
                }

                @Override
                public String getStringData()
                {
                    return schema;
                }

                @Override
                public void setStringData(String stringData)
                {
                }

                @Override
                public String getSystemId()
                {
                    return "customer.schema";
                }

                @Override
                public void setSystemId(String systemId)
                {
                }

                @Override
                public String getPublicId()
                {
                    return "customer.schema";
                }

                @Override
                public void setPublicId(String publicId)
                {
                }

                @Override
                public String getBaseURI()
                {
                    return "customer.schema";
                }

                @Override
                public void setBaseURI(String baseURI)
                {
                }

                @Override
                public String getEncoding()
                {
                    return "UTF-8";
                }

                @Override
                public void setEncoding(String encoding)
                {
                }

                @Override
                public boolean getCertifiedText()
                {
                    return false;
                }

                @Override
                public void setCertifiedText(boolean certifiedText)
                {
                }
            };
        }
    }
}