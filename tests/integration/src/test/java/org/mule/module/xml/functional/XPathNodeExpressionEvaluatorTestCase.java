/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.module.xml.functional;

import org.mule.api.MuleMessage;
import org.mule.module.client.MuleClient;
import org.mule.tck.junit4.FunctionalTestCase;

import org.custommonkey.xmlunit.XMLAssert;
import org.junit.Test;

public class XPathNodeExpressionEvaluatorTestCase extends FunctionalTestCase
{

    private static final String SAMPLE_REQUEST =
            "<root>" +
            "<table>" +
            "<name>African Coffee Table</name>" +
            "<width>80</width>" +
            "<length>120</length>" +
            "</table>" +
            "</root>";

    private static final String EXPECTED_RESPONSE =
            "<?xml version=\"1.0\" encoding=\"UTF-8\"?>" +
            "<name>African Coffee Table</name>";

    @Override
    protected String getConfigResources()
    {
        return "org/mule/module/xml/xpath-node-config.xml";
    }

    @Test
    public void testExpressionTransformerUsingXpathNode() throws Exception
    {
        MuleClient client = new MuleClient(muleContext);

        MuleMessage message = client.send("vm://testInput", SAMPLE_REQUEST, null);

        XMLAssert.assertXMLEqual(EXPECTED_RESPONSE, message.getPayloadAsString());
    }
}
