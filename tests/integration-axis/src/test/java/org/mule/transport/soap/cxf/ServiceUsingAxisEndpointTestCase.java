/*
 * (c) 2003-2014 MuleSoft, Inc. This software is protected under international copyright
 * law. All use of this software is subject to MuleSoft's Master Subscription Agreement
 * (or other master license agreement) separately entered into in writing between you and
 * MuleSoft. If such an agreement is not in place, you may not use the software.
 */
package org.mule.transport.soap.cxf;

import org.mule.DefaultMuleMessage;
import org.mule.api.MuleMessage;
import org.mule.module.client.MuleClient;
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

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

public class ServiceUsingAxisEndpointTestCase extends FunctionalTestCase
{

    @Rule
    public DynamicPort dynamicPort = new DynamicPort("port1");
    
    @Test
    public void testCXF() throws Exception
    {
        MuleClient client = new MuleClient(muleContext);
        MuleMessage reply = client.send("vm://cxf.in", new DefaultMuleMessage("Testing String", muleContext));

        assertNotNull(reply);
        assertNotNull(reply.getPayload());
        assertEquals("Received: Testing String", reply.getPayloadAsString());
    }

    @Test
    public void testRequestWsdl() throws Exception
    {
        MuleClient client = new MuleClient(muleContext);
        Map<String, String> props = new HashMap<String, String>();
        props.put("http.method", "GET");
        MuleMessage reply = client.send("http://localhost:" + dynamicPort.getNumber() + "/services/CxfService?wsdl",
            "/services/CxfService?wsdl", props);

        assertNotNull(reply);
        assertNotNull(reply.getPayload());

        Document document = DocumentHelper.parseText(reply.getPayloadAsString());
        
        List<?> nodes = document.selectNodes("//wsdl:definitions/wsdl:service");
        assertEquals("CxfService", ((Element) nodes.get(0)).attribute("name").getStringValue());
    }

    @Override
    protected String getConfigResources()
    {
        return "using-axis-conf.xml";
    }

}
