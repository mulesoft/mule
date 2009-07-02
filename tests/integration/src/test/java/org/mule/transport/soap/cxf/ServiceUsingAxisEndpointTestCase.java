/*
 * $Id$
 * --------------------------------------------------------------------------------------
 * Copyright (c) MuleSource, Inc.  All rights reserved.  http://www.mulesource.com
 *
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.transport.soap.cxf;

import org.mule.DefaultMuleMessage;
import org.mule.api.MuleMessage;
import org.mule.module.client.MuleClient;
import org.mule.tck.FunctionalTestCase;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.dom4j.Document;
import org.dom4j.DocumentHelper;
import org.dom4j.Element;

public class ServiceUsingAxisEndpointTestCase extends FunctionalTestCase
{

    public void testCXF() throws Exception
    {
        MuleClient client = new MuleClient();
        MuleMessage reply = client.send("vm://cxf.in", new DefaultMuleMessage("Testing String", muleContext));

        assertNotNull(reply);
        assertNotNull(reply.getPayload());
        assertEquals("Received: Testing String", reply.getPayloadAsString());
    }

    public void testRequestWsdl() throws Exception
    {
        MuleClient client = new MuleClient();
        Map<String, String> props = new HashMap<String, String>();
        props.put("http.method", "GET");
        MuleMessage reply = client.send("http://localhost:63382/services/CxfService?wsdl",
            "/services/CxfService?wsdl", props);

        assertNotNull(reply);
        assertNotNull(reply.getPayload());

        Document document = DocumentHelper.parseText(reply.getPayloadAsString());
        
        List nodes = document.selectNodes("//wsdl:definitions/wsdl:service");
        assertEquals("CxfService", ((Element) nodes.get(0)).attribute("name").getStringValue());
    }

    protected String getConfigResources()
    {
        return "using-axis-conf.xml";
    }

}
