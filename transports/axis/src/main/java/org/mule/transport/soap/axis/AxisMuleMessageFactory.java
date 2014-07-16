/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.transport.soap.axis;

import org.mule.DefaultMuleMessage;
import org.mule.module.cxf.MuleSoapHeaders;
import org.mule.transport.AbstractMuleMessageFactory;
import org.mule.util.StringUtils;

import java.util.Iterator;

import javax.xml.soap.SOAPMessage;

import org.apache.axis.MessageContext;
import org.apache.axis.attachments.AttachmentPart;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

public class AxisMuleMessageFactory extends AbstractMuleMessageFactory
{
    private static Log log = LogFactory.getLog(AxisMuleMessageFactory.class);

    @Override
    protected Class<?>[] getSupportedTransportMessageTypes()
    {
        return new Class[] { Object.class };
    }

    @Override
    protected Object extractPayload(Object transportMessage, String encoding) throws Exception
    {
        return transportMessage;
    }

    @Override
    protected void addProperties(DefaultMuleMessage message, Object transportMessage) throws Exception
    {
        MessageContext ctx = MessageContext.getCurrentContext();
        if (ctx != null)
        {
            MuleSoapHeaders header = new MuleSoapHeaders(
                ctx.getMessage().getSOAPPart().getEnvelope().getHeader());

            if (StringUtils.isNotBlank(header.getReplyTo()))
            {
                message.setReplyTo(header.getReplyTo());
            }
            if (StringUtils.isNotBlank(header.getCorrelationGroup()))
            {
                message.setCorrelationGroupSize(Integer.parseInt(header.getCorrelationGroup()));
            }
            if (StringUtils.isNotBlank(header.getCorrelationSequence()))
            {
                message.setCorrelationSequence(Integer.parseInt(header.getCorrelationSequence()));
            }
            if (StringUtils.isNotBlank(header.getCorrelationId()))
            {
                message.setCorrelationId(header.getCorrelationId());
            }
        }
    }

    @Override
    protected void addAttachments(DefaultMuleMessage message, Object transportMessage) throws Exception
    {
        MessageContext ctx = MessageContext.getCurrentContext();
        if (ctx == null)
        {
            return;
        }
        
        try
        {
            SOAPMessage soapMessage = ctx.getMessage();
            int x = 1;
            for (Iterator<?> i = soapMessage.getAttachments(); i.hasNext(); x++)
            {
                String name = String.valueOf(x);
                AttachmentPart attachmentPart = (AttachmentPart)i.next();
                message.addOutboundAttachment(name, attachmentPart.getActivationDataHandler());
            }
        }
        catch (Exception e)
        {
            // this will not happen
            log.fatal("Failed to read attachments", e);
        }
    }
}
