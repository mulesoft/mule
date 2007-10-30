/*
 * $Id$
 * --------------------------------------------------------------------------------------
 * Copyright (c) MuleSource, Inc.  All rights reserved.  http://www.mulesource.com
 *
 * The software in this package is published under the terms of the MuleSource MPL
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.providers.cxf.support;

import static javax.xml.XMLConstants.XML_NS_URI;
import static org.mule.config.MuleProperties.MULE_CORRELATION_GROUP_SIZE_PROPERTY;
import static org.mule.config.MuleProperties.MULE_CORRELATION_ID_PROPERTY;
import static org.mule.config.MuleProperties.MULE_CORRELATION_SEQUENCE_PROPERTY;
import static org.mule.config.MuleProperties.MULE_EVENT_PROPERTY;
import static org.mule.config.MuleProperties.MULE_REPLY_TO_PROPERTY;
import static org.mule.providers.soap.MuleSoapHeaders.MULE_HEADER;

import javax.xml.namespace.QName;

import org.apache.cxf.binding.soap.SoapHeader;
import org.apache.cxf.binding.soap.SoapMessage;
import org.apache.cxf.helpers.DOMUtils;
import org.apache.cxf.interceptor.Fault;
import org.apache.cxf.message.Message;
import org.apache.cxf.phase.Phase;
import org.mule.providers.soap.MuleSoapHeaders;
import org.mule.umo.UMOEvent;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Text;

/**
 * Writes the Mule Soap Header to the outgoing request.
 */
public class MuleHeadersOutInterceptor extends BaseMuleHeaderInterceptor
{

    public MuleHeadersOutInterceptor()
    {
        super(Phase.PRE_PROTOCOL);
    }

    public void handleMessage(Message m) throws Fault
    {
        if (!(m instanceof SoapMessage))
        {
            return;
        }

        SoapMessage message = (SoapMessage) m;
        UMOEvent event = (UMOEvent) message.get(MULE_EVENT_PROPERTY);

        if (event == null)
        {
            return;
        }

        MuleSoapHeaders muleHeaders = new MuleSoapHeaders(event);

        if (muleHeaders.getCorrelationId() == null && muleHeaders.getReplyTo() == null)
        {
            return;
        }

        Document owner_doc = DOMUtils.createDocument();

        Element mule_header = owner_doc.createElementNS(MULE_NS_URI, QUALIFIED_MULE_HEADER);
        // setup mule: namespace prefix declaration so that we can use it.
        mule_header.setAttributeNS(XML_NS_URI, MULE_XMLNS, MULE_NS_URI);

        if (muleHeaders.getCorrelationId() != null)
        {
            mule_header.appendChild(buildMuleHeader(owner_doc, MULE_CORRELATION_ID_PROPERTY,
                muleHeaders.getCorrelationId()));
            mule_header.appendChild(buildMuleHeader(owner_doc, MULE_CORRELATION_GROUP_SIZE_PROPERTY,
                muleHeaders.getCorrelationGroup()));
            mule_header.appendChild(buildMuleHeader(owner_doc, MULE_CORRELATION_SEQUENCE_PROPERTY,
                muleHeaders.getCorrelationSequence()));
        }
        if (muleHeaders.getReplyTo() != null)
        {
            mule_header.appendChild(buildMuleHeader(owner_doc, MULE_REPLY_TO_PROPERTY,
                muleHeaders.getReplyTo()));
        }

        SoapHeader sh = new SoapHeader(new QName(MULE_NS_URI, MULE_HEADER), mule_header);
        message.getHeaders().add(sh);
    }

    Element buildMuleHeader(Document owner_doc, String localName, String value)
    {
        Element out = owner_doc.createElementNS(MULE_NS_URI, "mule:" + localName);
        if (value != null)
        {
            Text text = owner_doc.createTextNode(value);
            out.appendChild(text);
        }
        return out;
    }

}
