/*
 * $Id$
 * --------------------------------------------------------------------------------------
 * Copyright (c) MuleSource, Inc.  All rights reserved.  http://www.mulesource.com
 *
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.transport.soap.axis;

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

public class AxisServiceUsingXFireEndpointTestCase extends FunctionalTestCase
{

    public AxisServiceUsingXFireEndpointTestCase()
    {
        super();
        this.setDisposeManagerPerSuite(true);
    }

    public void testAxis() throws Exception
    {
        MuleClient client = new MuleClient();
        MuleMessage reply = client.send("vm://axis.in", new DefaultMuleMessage("Test String"));

        assertNotNull(reply);
        assertNotNull(reply.getPayload());
        assertEquals(reply.getPayloadAsString(), "Received: Test String");
        logger.info(reply.getPayloadAsString());
    }

    public void testRequestWsdl() throws Exception
    {
        MuleClient client = new MuleClient();
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

    protected String getConfigResources()
    {
        return "axis-using-xfire-config.xml";
    }

}
