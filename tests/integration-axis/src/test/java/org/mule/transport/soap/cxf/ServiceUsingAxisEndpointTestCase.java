/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.transport.soap.cxf;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

import org.mule.DefaultMuleMessage;
import org.mule.api.MuleMessage;
import org.mule.api.client.MuleClient;
import org.mule.tck.junit4.FunctionalTestCase;
import org.mule.tck.junit4.rule.DynamicPort;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.dom4j.Document;
import org.dom4j.DocumentHelper;
import org.dom4j.Element;
import org.junit.Rule;
import org.junit.Test;

public class ServiceUsingAxisEndpointTestCase extends FunctionalTestCase
{
    @Rule
    public DynamicPort dynamicPort = new DynamicPort("port1");

    @Test
    public void testCXF() throws Exception
    {
        MuleClient client = muleContext.getClient();
        MuleMessage reply = client.send("vm://cxf.in", new DefaultMuleMessage("Testing String", muleContext));

        assertNotNull(reply);
        assertNotNull(reply.getPayload());
        assertEquals("Received: Testing String", reply.getPayloadAsString());
    }

    @Test
    public void testRequestWsdl() throws Exception
    {
        MuleClient client = muleContext.getClient();
        MuleMessage reply = client.send("http://localhost:" + dynamicPort.getNumber() + "/services/CxfService?wsdl",
           new DefaultMuleMessage("/services/CxfService?wsdl", muleContext));

        assertNotNull(reply);
        assertNotNull(reply.getPayload());

        Document document = DocumentHelper.parseText(reply.getPayloadAsString());

        List<?> nodes = document.selectNodes("//wsdl:definitions/wsdl:service");
        assertEquals("CxfService", ((Element) nodes.get(0)).attribute("name").getStringValue());
    }

    @Override
    protected String getConfigFile()
    {
        return "using-axis-conf.xml";
    }
}
