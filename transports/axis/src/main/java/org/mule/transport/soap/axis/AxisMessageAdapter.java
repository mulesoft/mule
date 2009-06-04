/*
 * $Id$
 * --------------------------------------------------------------------------------------
 * Copyright (c) MuleSource, Inc.  All rights reserved.  http://www.mulesource.com
 *
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.transport.soap.axis;

import org.mule.api.MessagingException;
import org.mule.api.ThreadSafeAccess;
import org.mule.api.transformer.Transformer;
import org.mule.transformer.simple.SerializableToByteArray;
import org.mule.transport.AbstractMessageAdapter;
import org.mule.transport.soap.MuleSoapHeaders;
import org.mule.transport.soap.i18n.SoapMessages;
import org.mule.util.StringUtils;

import java.util.Iterator;

import javax.activation.DataHandler;
import javax.xml.soap.SOAPException;
import javax.xml.soap.SOAPMessage;

import org.apache.axis.MessageContext;
import org.apache.axis.attachments.AttachmentPart;

/**
 * <code>AxisMessageAdapter</code> wraps a soap message. The payload of the adapter
 * is the raw message received from the transport, but you also have access to the
 * SOAPMessage object by using <code>adapter.getSOAPMessage()</code>
 */
public class AxisMessageAdapter extends AbstractMessageAdapter
{
    /**
     * Serial version
     */
    private static final long serialVersionUID = -923205879581370143L;

    private final Object payload;
    private final SOAPMessage soapMessage;
    private Transformer trans = new SerializableToByteArray();

    public AxisMessageAdapter(Object message) throws MessagingException
    {
        this.payload = message;
        try
        {
            MessageContext ctx = MessageContext.getCurrentContext();

            if (ctx != null)
            {
                MuleSoapHeaders header = new MuleSoapHeaders(ctx.getMessage().getSOAPPart().getEnvelope()
                    .getHeader());

                if (StringUtils.isNotBlank(header.getReplyTo()))
                {
                    setReplyTo(header.getReplyTo());
                }

                if (StringUtils.isNotBlank(header.getCorrelationGroup()))
                {
                    setCorrelationGroupSize(Integer.parseInt(header.getCorrelationGroup()));
                }
                if (StringUtils.isNotBlank(header.getCorrelationSequence()))
                {
                    setCorrelationSequence(Integer.parseInt(header.getCorrelationSequence()));
                }
                if (StringUtils.isNotBlank(header.getCorrelationId()))
                {
                    setCorrelationId(header.getCorrelationId());
                }

                this.soapMessage = ctx.getMessage();
                int x = 1;
                try
                {
                    for (Iterator i = this.soapMessage.getAttachments(); i.hasNext(); x++)
                    {
                        String name = String.valueOf(x);
                        AttachmentPart attachmentPart = (AttachmentPart) i.next();
                        super.addAttachment(name, attachmentPart.getActivationDataHandler());
                    }
                }
                catch (Exception e)
                {
                    // this will not happen
                    logger.fatal("Failed to read attachments", e);
                }
            }
            else
            {
                this.soapMessage = null;
            }
        }
        catch (SOAPException e)
        {
            throw new MessagingException(SoapMessages.failedToProcessSoapHeaders(), message, e);
        }
    }

    public AxisMessageAdapter(AxisMessageAdapter template)
    {
        super(template);
        soapMessage = template.soapMessage;
        payload = template.payload;
        trans = template.trans;
    }

    /** @return the current message */
    public Object getPayload()
    {
        return payload;
    }

    public SOAPMessage getSoapMessage()
    {
        return soapMessage;
    }

    public void addAttachment(String name, DataHandler dataHandler) throws Exception
    {
        if (null != soapMessage)
        {
            soapMessage.addAttachmentPart(new AttachmentPart(dataHandler));
        }
        super.addAttachment(name, dataHandler);
    }

    public void removeAttachment(String name) throws Exception
    {
        if ("all".equalsIgnoreCase(name))
        {
            soapMessage.removeAllAttachments();
            attachments.clear();
        }
        else
        {
            throw new SOAPException(SoapMessages.cannotRemoveSingleAttachment().toString());
        }
    }

    public ThreadSafeAccess newThreadCopy()
    {
        return new AxisMessageAdapter(this);
    }

}
