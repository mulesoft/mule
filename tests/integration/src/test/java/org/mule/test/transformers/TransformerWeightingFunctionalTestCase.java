/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.test.transformers;

import org.mule.api.MuleMessage;
import org.mule.api.client.LocalMuleClient;
import org.mule.tck.junit4.FunctionalTestCase;

import org.custommonkey.xmlunit.XMLAssert;
import org.junit.Test;

public class TransformerWeightingFunctionalTestCase extends FunctionalTestCase
{
    private static final String XML_REQUEST = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n" +
                                              "<catalog>\n" +
                                              "    <cd>\n" +
                                              "        <title>Empire Burlesque</title>\n" +
                                              "        <artist>Bob Dylan</artist>\n" +
                                              "        <country>USA</country>\n" +
                                              "        <company>Columbia</company>\n" +
                                              "        <price>10.90</price>\n" +
                                              "        <year>1985</year>\n" +
                                              "    </cd>\n" +
                                              "</catalog>";

    @Override
    protected String getConfigFile()
    {
        return "org/mule/test/transformers/transformer-weighting-functional-config.xml";
    }

    @Test
    public void findTwoTransformers() throws Exception
    {
        LocalMuleClient client = muleContext.getClient();

        MuleMessage response = client.send("vm://testInput", XML_REQUEST, null);
        XMLAssert.assertXMLEqual(XML_REQUEST, response.getPayloadAsString());
    }
}
