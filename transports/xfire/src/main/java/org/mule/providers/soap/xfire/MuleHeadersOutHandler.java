/*
 * $Id$
 * --------------------------------------------------------------------------------------
 * Copyright (c) MuleSource, Inc.  All rights reserved.  http://www.mulesource.com
 *
 * The software in this package is published under the terms of the MuleSource MPL
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.providers.soap.xfire;

import org.codehaus.xfire.MessageContext;
import org.codehaus.xfire.handler.AbstractHandler;
import org.jdom.Element;
import org.jdom.Namespace;
import org.mule.config.MuleProperties;
import org.mule.providers.soap.MuleSoapHeaders;
import org.mule.umo.UMOEvent;

/**
 * Writes the Mule Soap Header to the outgoing request.
 */
public class MuleHeadersOutHandler extends AbstractHandler
{
    /**
     * Invoke a handler. If a fault occurs it will be handled via the
     * <code>handleFault</code> method.
     * 
     * @param context The message context.
     */
    public void invoke(MessageContext context) throws Exception
    {
        UMOEvent event = (UMOEvent) context.getProperty(MuleProperties.MULE_EVENT_PROPERTY);

        if (event == null && context.getClient() != null)
        {
            event = (UMOEvent) context.getClient().getProperty(MuleProperties.MULE_EVENT_PROPERTY);
        }

        if (event != null)
        {
            MuleSoapHeaders muleHeaders = new MuleSoapHeaders(event);
            Element header = context.getOutMessage().getHeader();

            if (header == null)
            {
                header = new Element("Header", context.getOutMessage().getSoapVersion().getPrefix(),
                    context.getOutMessage().getSoapVersion().getNamespace());
            }

            // we can also add some extra properties like
            // Enconding Property, Session Property

            Element muleHeader = null;
            Namespace ns = Namespace.getNamespace(MuleSoapHeaders.MULE_NAMESPACE,
                MuleSoapHeaders.MULE_10_ACTOR);
            if (muleHeaders.getCorrelationId() != null || muleHeaders.getReplyTo() != null)
            {
                muleHeader = new Element(MuleSoapHeaders.MULE_HEADER, ns);
            }
            else
            {
                return;
            }

            Element e = null;
            if (muleHeaders.getCorrelationId() != null)
            {

                e = new Element(MuleProperties.MULE_CORRELATION_ID_PROPERTY, ns);
                e.setText(muleHeaders.getCorrelationId());
                muleHeader.addContent(e);

                e = new Element(MuleProperties.MULE_CORRELATION_GROUP_SIZE_PROPERTY, ns);
                e.setText(muleHeaders.getCorrelationGroup());
                muleHeader.addContent(e);

                e = new Element(MuleProperties.MULE_CORRELATION_SEQUENCE_PROPERTY, ns);
                e.setText(muleHeaders.getCorrelationSequence());
                muleHeader.addContent(e);

            }
            if (muleHeaders.getReplyTo() != null)
            {

                e = new Element(MuleProperties.MULE_REPLY_TO_PROPERTY, ns);
                e.setText(muleHeaders.getReplyTo());
                muleHeader.addContent(e);
            }
            header.addContent(muleHeader);

            context.getOutMessage().setHeader(header);
        }
    }
}
