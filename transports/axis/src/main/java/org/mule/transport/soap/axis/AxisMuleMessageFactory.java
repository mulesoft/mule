/*
 * (c) 2003-2014 MuleSoft, Inc. This software is protected under international copyright
 * law. All use of this software is subject to MuleSoft's Master Subscription Agreement
 * (or other master license agreement) separately entered into in writing between you and
 * MuleSoft. If such an agreement is not in place, you may not use the software.
 */
package org.mule.transport.soap.axis;

import org.mule.DefaultMuleMessage;
import org.mule.api.MuleContext;
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
    
    public AxisMuleMessageFactory(MuleContext context)
    {
        super(context);
    }

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
