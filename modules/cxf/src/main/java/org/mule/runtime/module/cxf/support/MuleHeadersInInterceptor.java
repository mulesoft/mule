/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.module.cxf.support;

import static org.mule.runtime.core.api.config.MuleProperties.MULE_CORRELATION_GROUP_SIZE_PROPERTY;
import static org.mule.runtime.core.api.config.MuleProperties.MULE_CORRELATION_ID_PROPERTY;
import static org.mule.runtime.core.api.config.MuleProperties.MULE_CORRELATION_SEQUENCE_PROPERTY;
import static org.mule.runtime.core.api.config.MuleProperties.MULE_REPLY_TO_PROPERTY;

import org.mule.runtime.core.api.MuleEvent;
import org.mule.runtime.core.api.MuleMessage;
import org.mule.runtime.module.cxf.CxfConstants;

import java.util.Set;

import javax.xml.namespace.QName;

import org.apache.cxf.binding.soap.SoapMessage;
import org.apache.cxf.headers.Header;
import org.apache.cxf.interceptor.Fault;
import org.apache.cxf.message.Message;
import org.apache.cxf.phase.Phase;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.w3c.dom.Text;

/**
 * Reads the Mule Soap Header and sets the various header properties on the message.
 */
public class MuleHeadersInInterceptor extends AbstractMuleHeaderInterceptor
{

    public MuleHeadersInInterceptor()
    {
        super(Phase.PRE_PROTOCOL);
    }

    @Override
    public void handleMessage(Message m) throws Fault
    {
        if (!(m instanceof SoapMessage))
        {
            return;
        }

        SoapMessage message = (SoapMessage) m;
        if (!message.hasHeaders())
        {
            return;
        }
        Header mule_header = message.getHeader(MULE_HEADER_Q);
        if (mule_header == null)
        {
            return;
        }
        Object obj = mule_header.getObject();
        if (!(obj instanceof Element))
        {
            // Error? We can't work with it at any rate.
            return;
        }

        Element header_element = (Element) obj;
        NodeList mule_headers = header_element.getChildNodes();
        int idx = 0;
        Node child;
        while ((child = mule_headers.item(idx++)) != null)
        {
            if (child.getNodeType() != Node.ELEMENT_NODE)
            {
                continue;
            }
            Element child_el = (Element) child;
            if (child_el.getNamespaceURI() == null || !child_el.getNamespaceURI().equals(MULE_NS_URI))
            {
                continue;
            }
            
            if (SUPPORTED_HEADERS.contains(child_el.getLocalName()))
            {
                message.put(child_el.getLocalName(), collectTextFrom(child_el));
            }
        }
        

        MuleEvent reqEvent = ((MuleEvent) message.getExchange().get(CxfConstants.MULE_EVENT));

        MuleMessage.Builder builder = MuleMessage.builder(reqEvent.getMessage());

        // Copy correlation headers nto message
        String replyTo = (String) message.get(MULE_REPLY_TO_PROPERTY);
        if (replyTo != null)
        {
            builder.replyTo(replyTo);
        }

        String corId = (String) message.get(MULE_CORRELATION_ID_PROPERTY);
        if (corId != null)
        {
            builder.correlationId(corId);
        }

        String corGroupSize = (String) message.get(MULE_CORRELATION_GROUP_SIZE_PROPERTY);
        if (corGroupSize != null)
        {
            builder.correlationGroupSize(Integer.valueOf(corGroupSize));
        }

        String corSeq = (String) message.get(MULE_CORRELATION_SEQUENCE_PROPERTY);
        if (corSeq != null)
        {
            builder.correlationSequence(Integer.valueOf(corSeq));
        }
        reqEvent.setMessage(builder.build());
    }

    public Set<QName> getUnderstoodHeaders()
    {
        return UNDERSTOOD_HEADERS;
    }

    private String collectTextFrom(Element e)
    {
        NodeList children = e.getChildNodes();
        StringBuilder sb = new StringBuilder();
        int idx = 0;
        Node n;
        while ((n = children.item(idx++)) != null)
        {
            if (n.getNodeType() == Node.TEXT_NODE)
            {
                sb.append(((Text) n).getTextContent());
            }
        }
        return sb.toString();
    }

}
