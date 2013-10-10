/*
 * (c) 2003-2014 MuleSoft, Inc. This software is protected under international copyright
 * law. All use of this software is subject to MuleSoft's Master Subscription Agreement
 * (or other master license agreement) separately entered into in writing between you and
 * MuleSoft. If such an agreement is not in place, you may not use the software.
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
    protected String getConfigResources()
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
