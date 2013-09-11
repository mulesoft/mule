/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.module.xml.functional;

import org.mule.api.MuleMessage;
import org.mule.api.client.MuleClient;
import org.mule.tck.AbstractServiceAndFlowTestCase;

import java.util.Arrays;
import java.util.Collection;

import org.custommonkey.xmlunit.XMLAssert;
import org.junit.Test;
import org.junit.runners.Parameterized.Parameters;

public class XPathNodeExpressionEvaluatorTestCase extends AbstractServiceAndFlowTestCase
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

    @Parameters
    public static Collection<Object[]> parameters()
    {
        return Arrays.asList(new Object[][]{
            {ConfigVariant.SERVICE, "org/mule/module/xml/xpath-node-config-service.xml"},
            {ConfigVariant.FLOW, "org/mule/module/xml/xpath-node-config-flow.xml"}
        });
    }

    public XPathNodeExpressionEvaluatorTestCase(ConfigVariant variant, String configResources)
    {
        super(variant, configResources);
    }

    @Test
    public void testExpressionTransformerUsingXpathNode() throws Exception
    {
        MuleClient client = muleContext.getClient();

        MuleMessage message = client.send("vm://testInput", SAMPLE_REQUEST, null);
        XMLAssert.assertXMLEqual(EXPECTED_RESPONSE, message.getPayloadAsString());
    }
}
