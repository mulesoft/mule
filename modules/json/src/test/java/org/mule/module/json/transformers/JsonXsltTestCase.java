/*
 * (c) 2003-2014 MuleSoft, Inc. This software is protected under international copyright
 * law. All use of this software is subject to MuleSoft's Master Subscription Agreement
 * (or other master license agreement) separately entered into in writing between you and
 * MuleSoft. If such an agreement is not in place, you may not use the software.
 */
package org.mule.module.json.transformers;

import org.junit.Test;
import org.mule.api.transformer.TransformerException;
import org.mule.tck.junit4.AbstractMuleContextTestCase;

import java.io.ByteArrayInputStream;
import java.io.StringReader;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

public class JsonXsltTestCase extends AbstractMuleContextTestCase
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

        String jsonOut = "{\n" +
            "  \"customer\" : {\n" +
            "    \"id\" : \"112\",\n" +
            "    \"first-name\" : \"Bill\",\n" +
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

        String xsltString="<xsl:stylesheet version=\"1.0\" xmlns:xsl=\"http://www.w3.org/1999/XSL/Transform\">\n" +
            "  <xsl:template match=\"first-name\">\n" +
            "    <first-name>Bill</first-name>\n" +
            "  </xsl:template>\n" +
            "  <xsl:template match=\"@*|node()\">\n" +
            "    <xsl:copy>\n" +
            "      <xsl:apply-templates select=\"@*|node()\"/>\n" +
            "    </xsl:copy>\n" +
            "  </xsl:template>\n" +
            "</xsl:stylesheet>";

        JsonXsltTransformer jToJ = new JsonXsltTransformer();
        jToJ.setMuleContext(muleContext);
        jToJ.setXslt(xsltString);

        String jsonResponse = (String) jToJ.transform(json);
        jsonResponse = jsonResponse.replaceAll("\r\n", "\n");
        assertEquals(jsonOut, jsonResponse);

        jsonResponse = (String) jToJ.transform(new StringReader(json));
        jsonResponse = jsonResponse.replaceAll("\r\n", "\n");
        assertEquals(jsonOut, jsonResponse);

        jsonResponse = (String) jToJ.transform(json.getBytes());
        jsonResponse = jsonResponse.replaceAll("\r\n", "\n");
        assertEquals(jsonOut, jsonResponse);

        jsonResponse = (String) jToJ.transform(new ByteArrayInputStream(json.getBytes()));
        jsonResponse = jsonResponse.replaceAll("\r\n", "\n");
        assertEquals(jsonOut, jsonResponse);

        try
        {
            jToJ.transform(new Object());
            fail();
        }
        catch (Exception ex)
        {
            assertTrue(ex instanceof TransformerException);
        }
    }
}
