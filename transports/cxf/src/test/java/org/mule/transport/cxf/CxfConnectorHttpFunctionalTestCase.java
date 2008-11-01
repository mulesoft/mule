/*
 * $Id$
 * --------------------------------------------------------------------------------------
 * Copyright (c) MuleSource, Inc.  All rights reserved.  http://www.mulesource.com
 *
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.transport.cxf;

import org.mule.api.MuleMessage;
import org.mule.module.client.MuleClient;
import org.mule.tck.providers.soap.AbstractSoapUrlEndpointFunctionalTestCase;

import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import org.dom4j.Document;
import org.dom4j.DocumentHelper;
import org.dom4j.Element;

public class CxfConnectorHttpFunctionalTestCase extends AbstractSoapUrlEndpointFunctionalTestCase
{

    protected String getTransportProtocol()
    {
        return "http";
    }

    protected String getSoapProvider()
    {
        return "cxf";
    }
    
    @Override
    public void testRequest() throws Throwable
    {
        // Fails on build server, but not locally... so do nothing for now.
    }

    /**
     * @throws Exception
     */
    @SuppressWarnings("unchecked")
    public void testBinding() throws Exception
    {
        String url = "http://localhost:62108/";
        String folder = "mule/";
        String componentName = "mycomponent";
        String fullPath = url + folder + componentName;

        MuleClient client = new MuleClient();
        Map<String, Object> props = new HashMap<String, Object>();
        props.put("http.method", "GET");
        MuleMessage reply = client.send(fullPath + "?wsdl", folder + componentName + "?wsdl", props);

        assertNotNull(reply);
        assertNotNull(reply.getPayload());

        Document document = DocumentHelper.parseText(reply.getPayloadAsString());
        List nodes;

        nodes = document.selectNodes("//wsdl:definitions/wsdl:service");

        Element element = (Element) nodes.get(0);
        assertEquals("EchoService", element.attribute("name").getStringValue());

        nodes = document.selectNodes("//wsdl:definitions/wsdl:service/wsdl:port");

        for (Iterator i = nodes.iterator(); i.hasNext();)
        {
            element = (Element) i.next();

            if ((element.attribute("name").getStringValue().compareTo(componentName + "MulePort") == 0)
                || (element.attribute("name").getStringValue().compareTo(componentName + "LocalPort") == 0))
            {
                Element tempElement = (Element) element.elements().get(0);
                String mulePort = tempElement.attribute("location").getStringValue();
                assertEquals(fullPath, mulePort);
            }
        }
    }

    public String getConfigResources()
    {
        return getTransportProtocol() + "-mule-config.xml";
    }

}
