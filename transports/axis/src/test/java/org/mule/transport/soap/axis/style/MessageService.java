/*
 * (c) 2003-2014 MuleSoft, Inc. This software is protected under international copyright
 * law. All use of this software is subject to MuleSoft's Master Subscription Agreement
 * (or other master license agreement) separately entered into in writing between you and
 * MuleSoft. If such an agreement is not in place, you may not use the software.
 */
package org.mule.transport.soap.axis.style;

import javax.xml.soap.SOAPEnvelope;

import org.w3c.dom.Document;
import org.w3c.dom.Element;

/**
 * Simple message-style service sample.
 */

public interface MessageService
{
    /**
     * Service methods, echo back any XML received.
     */

    public org.apache.axis.message.SOAPBodyElement[] soapBodyElement(org.apache.axis.message.SOAPBodyElement[] bodyElements);

    public Document document(Document body);

    public Element[] elementArray(Element[] elems);

    public void soapRequestResponse(SOAPEnvelope req, SOAPEnvelope resp) throws javax.xml.soap.SOAPException;

}
