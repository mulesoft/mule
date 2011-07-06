/*
 * $Id$
 * --------------------------------------------------------------------------------------
 * Copyright (c) MuleSource, Inc.  All rights reserved.  http://www.mulesource.com
 *
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.module.xml.functional;

import java.util.Arrays;
import java.util.Collection;

import org.junit.Test;
import org.junit.runners.Parameterized.Parameters;
import org.mule.DefaultMuleMessage;
import org.mule.api.MuleMessage;
import org.mule.api.client.MuleClient;
import org.mule.tck.AbstractServiceAndFlowTestCase;
import org.w3c.dom.Node;

public class W3CDocumentXPathNodeEvaluatorTestCase extends AbstractServiceAndFlowTestCase
{
    private static final String XML_INPUT =
        "<root>" +
        "  <table>" +
        "    <name>African Coffee Table</name>" +
        "    <width>80</width>" +
        "    <length>120</length>" +
        "  </table>" +
        "</root>";
    
    public W3CDocumentXPathNodeEvaluatorTestCase(ConfigVariant variant, String configResources)
    {
        super(variant, configResources);
    }

    @Parameters
    public static Collection<Object[]> parameters()
    {
        return Arrays.asList(new Object[][]{
            {ConfigVariant.SERVICE, "org/mule/module/xml/w3c-dom-xpath-node-config-service.xml"},
            {ConfigVariant.FLOW, "org/mule/module/xml/w3c-dom-xpath-node-config-flow.xml"}
        });
    }

   
    @Test
    public void testW3CDocument() throws Exception
    {
        MuleClient client = muleContext.getClient();

        MuleMessage message = new DefaultMuleMessage(XML_INPUT, muleContext);
        MuleMessage response = client.send("vm://test", message);
        assertNotNull(response);
        assertNotNull(response.getPayload());
        assertTrue(response.getPayload() instanceof Node);
    }
}
