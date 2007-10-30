/*
 * $Id: XFireConnectorHttpFunctionalTestCase.java 5619 2007-03-15 19:14:13Z Lajos $
 * --------------------------------------------------------------------------------------
 * Copyright (c) MuleSource, Inc.  All rights reserved.  http://www.mulesource.com
 *
 * The software in this package is published under the terms of the MuleSource MPL
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.providers.cxf;

import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import org.dom4j.Document;
import org.dom4j.DocumentHelper;
import org.dom4j.Element;
import org.mule.extras.client.MuleClient;
import org.mule.tck.providers.soap.AbstractSoapUrlEndpointFunctionalTestCase;
import org.mule.tck.testmodels.services.Person;
import org.mule.umo.UMOMessage;

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
    public void testSendAndReceiveComplex() throws Throwable
    {
        MuleClient client = new MuleClient();
        UMOMessage result = client.send(getSendReceiveComplexEndpoint1(), new Person("Dino", "Flintstone"),
            null);
        assertNull(result);

        result = client.receive(getSendReceiveComplexEndpoint2(), 0);
        assertNotNull(result);
        assertTrue(result.getPayload() instanceof Person);
        assertEquals("Dino", ((Person) result.getPayload()).getFirstName());
        assertEquals("Flintstone", ((Person) result.getPayload()).getLastName());
    }

    public void testBinding() throws Exception
    {
        String url = "http://localhost:62108/";
        String folder = "mule/";
        String componentName = "mycomponent";
        String fullPath = url + folder + componentName;

        MuleClient client = new MuleClient();
        Map<String, Object> props = new HashMap<String, Object>();
        props.put("http.method", "GET");
        UMOMessage reply = client.send(fullPath + "?wsdl", folder + componentName + "?wsdl", props);

        assertNotNull(reply);
        assertNotNull(reply.getPayload());

        Document document = DocumentHelper.parseText(reply.getPayloadAsString());
        List nodes;

        nodes = document.selectNodes("//wsdl:definitions/wsdl:service");

        Element element = (Element) nodes.get(0);
        assertEquals(componentName, element.attribute("name").getStringValue());

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
