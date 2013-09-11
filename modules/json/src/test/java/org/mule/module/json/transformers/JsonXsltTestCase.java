/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.module.json.transformers;

import org.mule.api.transformer.TransformerException;
import org.mule.tck.junit4.AbstractMuleContextTestCase;

import java.io.ByteArrayInputStream;
import java.io.StringReader;

import org.junit.Test;
import org.skyscreamer.jsonassert.JSONAssert;

public class JsonXsltTestCase extends AbstractMuleContextTestCase
{
    private static final String JSON_INPUT =
        "{\n" +
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

    private static final String EXPECTED_JSON =
        "{" +
        "    \"customer\" : {" +
        "        \"id\" : \"112\"," +
        "        \"first-name\" : \"Bill\"," +
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

    private static final String XSLT =
        "<xsl:stylesheet version=\"1.0\" xmlns:xsl=\"http://www.w3.org/1999/XSL/Transform\">\n" +
        "  <xsl:template match=\"first-name\">\n" +
        "    <first-name>Bill</first-name>\n" +
        "  </xsl:template>\n" +
        "  <xsl:template match=\"@*|node()\">\n" +
        "    <xsl:copy>\n" +
        "      <xsl:apply-templates select=\"@*|node()\"/>\n" +
        "    </xsl:copy>\n" +
        "  </xsl:template>\n" +
        "</xsl:stylesheet>";

    private JsonXsltTransformer transformer;

    @Override
    protected void doSetUp() throws Exception
    {
        super.doSetUp();

        transformer = new JsonXsltTransformer();
        transformer.setMuleContext(muleContext);
        transformer.setXslt(XSLT);
    }

    @Test
    public void stringInputShouldBeTransformedToValidJson() throws Exception
    {
        String jsonResponse = (String) transformer.transform(JSON_INPUT);
        JSONAssert.assertEquals(EXPECTED_JSON, jsonResponse, false);
    }

    @Test
    public void readerInputShouldBeTransformedToValidJson() throws Exception
    {
        String jsonResponse = (String) transformer.transform(new StringReader(JSON_INPUT));
        JSONAssert.assertEquals(EXPECTED_JSON, jsonResponse, false);
    }

    @Test
    public void byteArrayInputShouldBeTransformedToValidJson() throws Exception
    {
        String jsonResponse = (String) transformer.transform(JSON_INPUT.getBytes());
        JSONAssert.assertEquals(EXPECTED_JSON, jsonResponse, false);
    }

    @Test
    public void inputStreamInputShouldBeTransformedToValidJson() throws Exception
    {
        String jsonResponse = (String) transformer.transform(new ByteArrayInputStream(JSON_INPUT.getBytes()));
        JSONAssert.assertEquals(EXPECTED_JSON, jsonResponse, false);
    }

    @Test(expected = TransformerException.class)
    public void invalidInputShouldThrow() throws Exception
    {
        transformer.transform(new Object());
    }
}
