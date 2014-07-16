/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.transport.soap.axis.style;

import javax.xml.soap.Name;
import javax.xml.soap.SOAPBody;
import javax.xml.soap.SOAPElement;
import javax.xml.soap.SOAPEnvelope;
import javax.xml.soap.SOAPException;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.w3c.dom.Document;
import org.w3c.dom.Element;

/**
 * Simple message-style service sample.
 */
public class DefaultMessageService implements MessageService
{

    private static Log logger = LogFactory.getLog(DefaultMessageService.class);

    /**
     * Service method, which simply echoes back any XML it receives.
     * 
     * @param bodyElements an array of DOM Elements, one for each SOAP body =lement
     * @return an array of DOM Elements to be sent in the response body
     */
    public org.apache.axis.message.SOAPBodyElement[] soapBodyElement(org.apache.axis.message.SOAPBodyElement[] bodyElements)
    {
        // Echo back
        logger.debug("bodyElementTest Called");
        return bodyElements;
    }

    public Document document(Document body)
    {
        // Echo back
        logger.debug("documentTest Called");
        body.setNodeValue("TEST RESPONSE");
        return body;
    }

    public Element[] elementArray(Element[] elems)
    {
        // Echo back
        logger.debug("echoElements Called");
        return elems;
    }

    public void soapRequestResponse(SOAPEnvelope req, SOAPEnvelope resp) throws SOAPException
    {
        // Echo back
        logger.debug("envelopeTest Called");
        SOAPBody body = resp.getBody();
        Name ns0 = resp.createName("TestNS0", "ns0", "http://example.com");
        Name ns1 = resp.createName("TestNS1", "ns1", "http://example.com");
        SOAPElement bodyElmnt = body.addBodyElement(ns0);
        SOAPElement el = bodyElmnt.addChildElement(ns1);
        el.addTextNode("TEST RESPONSE");
    }
}
