/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.module.cxf.support;

import org.mule.api.MuleEvent;
import org.mule.api.MuleMessage;
import org.mule.api.config.MuleProperties;
import org.mule.module.cxf.CxfConstants;

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
        

        MuleMessage reqMsg = ((MuleEvent) message.getExchange().get(CxfConstants.MULE_EVENT)).getMessage();
        
        // Copy correlation headers nto message
        String replyTo = (String) message.get(MuleProperties.MULE_REPLY_TO_PROPERTY);
        if (replyTo != null)
        {
            reqMsg.setReplyTo(replyTo);
        }
        
        String corId = (String) message.get(MuleProperties.MULE_CORRELATION_ID_PROPERTY);
        if (corId != null)
        {
            reqMsg.setCorrelationId(corId);
        }

        String corGroupSize = (String) message.get(MuleProperties.MULE_CORRELATION_GROUP_SIZE_PROPERTY);
        if (corGroupSize != null)
        {
            reqMsg.setCorrelationGroupSize(Integer.valueOf(corGroupSize));
        }

        String corSeq = (String) message.get(MuleProperties.MULE_CORRELATION_SEQUENCE_PROPERTY);
        if (corSeq != null)
        {
            reqMsg.setCorrelationSequence(Integer.valueOf(corSeq));
        }
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
