/*
 * Copyright 2002-2004 The Apache Software Foundation.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or =mplied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.mule.test.integration.providers.soap.axis.style;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.w3c.dom.Document;
import org.w3c.dom.Element;

import javax.xml.soap.Name;
import javax.xml.soap.SOAPBody;
import javax.xml.soap.SOAPElement;
import javax.xml.soap.SOAPEnvelope;
import javax.xml.soap.SOAPException;

/**
 * Simple message-style service sample.
 */
public class DefaultMessageService implements MessageService {

    private static transient Log logger = LogFactory.getLog(DefaultMessageService.class);

    /**
     * Service method, which simply echoes back any XML it receives.
     *
     * @param bodyElements an array of DOM Elements, one for each SOAP body =lement
     * @return an array of DOM Elements to be sent in the response body
     */
    public org.apache.axis.message.SOAPBodyElement [] soapBodyElement(org.apache.axis.message.SOAPBodyElement [] bodyElements) {
        //Echo back
        logger.debug("bodyElementTest Called");
        return bodyElements;
    }
    
    public Document document(Document body) {
        //Echo back
        logger.debug("documentTest Called");
        body.setNodeValue("TEST RESPONSE");
        return body;
    }

    
    public Element[] elementArray(Element [] elems) {
        //Echo back
        logger.debug("echoElements Called");
        return elems;
    }

    public void soapRequestResponse(SOAPEnvelope req, SOAPEnvelope resp) throws SOAPException {
        //Echo back
        logger.debug("envelopeTest Called");
       SOAPBody body = resp.getBody();
        Name ns0 =  resp.createName("TestNS0", "ns0", "http://example.com");
        Name ns1 =  resp.createName("TestNS1", "ns1", "http://example.com");
        SOAPElement bodyElmnt = body.addBodyElement(ns0);
        SOAPElement el = bodyElmnt.addChildElement(ns1);
        el.addTextNode("TEST RESPONSE");
    }
}