/*
 * $Id:XFireServiceUsingAxisEndpointTestCase.java 7586 2007-07-19 04:06:50Z aperepel $
 * --------------------------------------------------------------------------------------
 * Copyright (c) MuleSource, Inc.  All rights reserved.  http://www.mulesource.com
 *
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.providers.soap.xfire;

import org.mule.extras.client.MuleClient;
import org.mule.impl.MuleMessage;
import org.mule.tck.FunctionalTestCase;
import org.mule.umo.UMOMessage;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.dom4j.Document;
import org.dom4j.DocumentHelper;
import org.dom4j.Element;

public class XFireServiceUsingAxisEndpointTestCase extends FunctionalTestCase
{

    public void testXFire() throws Exception
    {
        MuleClient client = new MuleClient();
        UMOMessage reply = client.send("vm://xfire.in", new MuleMessage("Testing String"));

        assertNotNull(reply);
        assertNotNull(reply.getPayload());
        assertEquals(reply.getPayloadAsString(), "Received: Testing String");
    }

    public void testRequestWsdl() throws Exception
    {
        MuleClient client = new MuleClient();
        Map props = new HashMap();
        props.put("http.method", "GET");
        UMOMessage reply = client.send("http://localhost:63382/services/XfireService?wsdl",
            "/services/Hello_Xfire?wsdl", props);

        assertNotNull(reply);
        assertNotNull(reply.getPayload());

        Document document = DocumentHelper.parseText(reply.getPayloadAsString());
        List nodes;

        nodes = document.selectNodes("//wsdl:definitions/wsdl:service");
        assertEquals(((Element)nodes.get(0)).attribute("name").getStringValue(), "XfireService");
    }

    protected String getConfigResources()
    {
        return "xfire-using-axis-conf.xml";
    }

}
