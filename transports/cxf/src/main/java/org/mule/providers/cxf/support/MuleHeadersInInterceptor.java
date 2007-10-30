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

import org.apache.cxf.binding.soap.SoapMessage;
import org.apache.cxf.headers.Header;
import org.apache.cxf.interceptor.Fault;
import org.apache.cxf.message.Message;
import org.apache.cxf.phase.Phase;

import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.w3c.dom.Text;

import javax.xml.namespace.QName;
import java.util.Set;

/**
 * Reads the Mule Soap Header and sets the various header properties on the message.
 */
public class MuleHeadersInInterceptor extends BaseMuleHeaderInterceptor
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
            if (SUPPORTED_HEADERS.contains(child_el.getTagName()))
            {
                message.put(child_el.getTagName(), collectTextFrom(child_el));
            }
        }
    }

    public Set<QName> getUnderstoodHeaders()
    {
        return UNDERSTOOD_HEADERS;
    }

    private String collectTextFrom(Element e)
    {
        NodeList children = e.getChildNodes();
        // Uncomment for 1.5
        // StringBuilder sb = new StringBuilder();
        // Uncomment for 1.4
        StringBuffer sb = new StringBuffer();
        int idx = 0;
        Node n;
        while ((n = children.item(idx++)) != null)
        {
            if (n.getNodeType() == Node.TEXT_NODE)
            {
                sb.append((Text) n);
            }
        }
        return sb.toString();
    }

}
