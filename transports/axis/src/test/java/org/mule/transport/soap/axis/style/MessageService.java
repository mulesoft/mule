/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
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
