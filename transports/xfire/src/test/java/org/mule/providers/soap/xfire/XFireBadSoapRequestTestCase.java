/*
 * $Id$
 * --------------------------------------------------------------------------------------
 * Copyright (c) MuleSource, Inc.  All rights reserved.  http://www.mulesource.com
 *
 * The software in this package is published under the terms of the MuleSource MPL
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.providers.soap.xfire;

import org.mule.extras.client.MuleClient;
import org.mule.impl.MuleMessage;
import org.mule.tck.FunctionalTestCase;
import org.mule.umo.UMOMessage;

import java.util.List;

import org.dom4j.Document;
import org.dom4j.DocumentHelper;
import org.dom4j.Element;

public class XFireBadSoapRequestTestCase extends FunctionalTestCase
{

    public XFireBadSoapRequestTestCase()
    {
        super();
        this.setDisposeManagerPerSuite(true);
    }

    protected String getConfigResources()
    {
        return "xfire-soap-request-conf.xml";
    }

    public void testSoapDocumentError() throws Exception
    {
        MuleClient client = new MuleClient();

        String soapRequest = "<soap:Envelope xmlns:soap=\"http://schemas.xmlsoap.org/soap/envelope/\" xmlns:xsd=\"http://www.w3.org/2001/XMLSchema\" xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\">"
                             + "<soap:Body>"
                             + "<ssss xmlns=\"http://www.muleumo.org\">"
                             + "<request xmlns=\"http://www.muleumo.org\">Bad Request</request>"
                             + "</ssss>"
                             + "</soap:Body>" + "</soap:Envelope>";

        UMOMessage reply = client.send("http://localhost:33381/services/TestComponent", new MuleMessage(
            soapRequest));

        assertNotNull(reply);
        assertNotNull(reply.getPayload());

        Document document = DocumentHelper.parseText(reply.getPayloadAsString());
        List fault = document.selectNodes("//soap:Envelope/soap:Body/soap:Fault/faultcode");

        assertEquals(1, fault.size());
        Element faultCodeElement = (Element)fault.get(0);

        assertEquals("soap:Client", faultCodeElement.getStringValue());

        fault = document.selectNodes("//soap:Envelope/soap:Body/soap:Fault/faultstring");
        assertEquals(1, fault.size());
        Element faultStringElement = (Element)fault.get(0);

        assertEquals("Invalid operation: {http://www.muleumo.org}ssss", faultStringElement.getStringValue());
    }

}
