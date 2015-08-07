/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.module.xml.functional;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import org.mule.DefaultMuleMessage;
import org.mule.api.MuleMessage;
import org.mule.api.client.MuleClient;
import org.mule.tck.junit4.FunctionalTestCase;

import org.junit.Test;
import org.w3c.dom.Node;

public class W3CDocumentXPathNodeEvaluatorTestCase extends FunctionalTestCase
{
    private static final String XML_INPUT =
        "<root>" +
        "  <table>" +
        "    <name>African Coffee Table</name>" +
        "    <width>80</width>" +
        "    <length>120</length>" +
        "  </table>" +
        "</root>";

    @Override
    protected String getConfigFile()
    {
        return "org/mule/module/xml/w3c-dom-xpath-node-config-flow.xml";
    }

    @Test
    public void testW3CDocument() throws Exception
    {
        MuleClient client = muleContext.getClient();

        MuleMessage message = getTestMuleMessage(XML_INPUT);
        MuleMessage response = client.send("vm://test", message);
        assertNotNull(response);
        assertNotNull(response.getPayload());
        assertTrue(response.getPayload() instanceof Node);
    }
}
