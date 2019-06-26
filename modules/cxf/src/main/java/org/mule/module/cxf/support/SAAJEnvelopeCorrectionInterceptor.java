/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.module.cxf.support;

import static org.apache.cxf.phase.Phase.PRE_PROTOCOL;
import org.mule.module.cxf.CxfConstants;

import javax.xml.soap.SOAPMessage;
import javax.xml.stream.XMLStreamReader;
import javax.xml.transform.dom.DOMSource;

import org.apache.cxf.binding.soap.SoapMessage;
import org.apache.cxf.binding.soap.interceptor.AbstractSoapInterceptor;
import org.apache.cxf.interceptor.Fault;
import org.apache.cxf.phase.Phase;
import org.apache.cxf.staxutils.StaxUtils;

/**
 * {@link org.apache.cxf.interceptor.Interceptor} that corrects the {@link XMLStreamReader} contained in the CXF
 * Message, in case the proxy is configured with an envelope-type-payload, and {@link
 * org.apache.cxf.binding.soap.saaj.SAAJInInterceptor} is present.
 */
public class SAAJEnvelopeCorrectionInterceptor extends AbstractSoapInterceptor
{

    public SAAJEnvelopeCorrectionInterceptor()
    {
        super(PRE_PROTOCOL);
    }

    public SAAJEnvelopeCorrectionInterceptor(String phase)
    {
        super(phase);
    }

    @Override
    public void handleMessage(SoapMessage soapMessage) throws Fault
    {
        // If SAAJInInterceptor is configured in the CXF Proxy with envelope payload, the XMLStreamReader Content will be overwritten
        // by one containing the whole SOAPPart, instead of the SAAJ one, which contains from the body inwards.
        SOAPMessage saajSoapMessage = soapMessage.getContent(SOAPMessage.class);
        if (soapMessage != null)
        {
            XMLStreamReader xmlStreamReader = StaxUtils.createXMLStreamReader(new DOMSource(saajSoapMessage.getSOAPPart()));
            soapMessage.setContent(XMLStreamReader.class, xmlStreamReader);
        }

    }
}
