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

import org.mule.DefaultMuleMessage;
import org.mule.api.MuleMessage;
import org.mule.module.client.MuleClient;
import org.mule.tck.FunctionalTestCase;
import org.mule.transport.http.HttpConstants;

import java.util.List;

import org.dom4j.Document;
import org.dom4j.DocumentHelper;
import org.dom4j.Element;

public class CxfBadSoapRequestTestCase extends FunctionalTestCase
{

    protected String getConfigResources()
    {
        return "soap-request-conf.xml";
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

        MuleMessage reply = client.send("http://localhost:63381/services/TestComponent", new DefaultMuleMessage(
            soapRequest));

        assertNotNull(reply);
        assertNotNull(reply.getPayload());

        String ct = reply.getStringProperty(HttpConstants.HEADER_CONTENT_TYPE, "");
        assertEquals("text/xml", ct);
        
        Document document = DocumentHelper.parseText(reply.getPayloadAsString());
        List fault = document.selectNodes("//soap:Envelope/soap:Body/soap:Fault/faultcode");

        assertEquals(1, fault.size());
        Element faultCodeElement = (Element) fault.get(0);

        assertEquals("soap:Client", faultCodeElement.getStringValue());

        fault = document.selectNodes("//soap:Envelope/soap:Body/soap:Fault/faultstring");
        assertEquals(1, fault.size());
        Element faultStringElement = (Element) fault.get(0);
        assertEquals("Message part {http://www.muleumo.org}ssss was not recognized.  (Does it exist in service WSDL?)",
            faultStringElement.getStringValue());
    }

}
