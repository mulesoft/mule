/*
 * (c) 2003-2014 MuleSoft, Inc. This software is protected under international copyright
 * law. All use of this software is subject to MuleSoft's Master Subscription Agreement
 * (or other master license agreement) separately entered into in writing between you and
 * MuleSoft. If such an agreement is not in place, you may not use the software.
 */
package org.mule.transport.soap.axis;

import org.mule.DefaultMuleMessage;
import org.mule.api.MuleMessage;
import org.mule.module.client.MuleClient;
import org.mule.tck.junit4.FunctionalTestCase;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.dom4j.Document;
import org.dom4j.DocumentHelper;
import org.dom4j.Element;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

public class AxisServiceUsingCxfEndpointTestCase extends FunctionalTestCase
{

    @Override
    protected String getConfigResources()
    {
        return "axis-using-cxf-config.xml";
    }

    @Test
    public void testAxis() throws Exception
    {
        MuleClient client = new MuleClient(muleContext);
        MuleMessage reply = client.send("vm://axis.in", new DefaultMuleMessage("Test String", muleContext));

        assertNotNull(reply);
        assertNotNull(reply.getPayload());
        assertEquals(reply.getPayloadAsString(), "Received: Test String");
        logger.info(reply.getPayloadAsString());
    }

    @Test
    public void testRequestWsdl() throws Exception
    {
        MuleClient client = new MuleClient(muleContext);
        Map props = new HashMap();
        props.put("http.method", "GET");
        MuleMessage reply = client.send("http://localhost:63381/services/AxisService?WSDL", "", props);

        assertNotNull(reply);
        assertNotNull(reply.getPayload());

        Document document = DocumentHelper.parseText(reply.getPayloadAsString());
        List nodes;

        nodes = document.selectNodes("//wsdl:definitions/wsdl:service");
        assertEquals(((Element)nodes.get(0)).attribute("name").getStringValue(), "AxisService");
    }

}
