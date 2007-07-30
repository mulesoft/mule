/*
 * $Id$
 * --------------------------------------------------------------------------------------
 * Copyright (c) MuleSource, Inc.  All rights reserved.  http://www.mulesource.com
 *
 * The software in this package is published under the terms of the MuleSource MPL
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.providers.soap.axis;

import org.mule.impl.ThreadSafeAccess;
import org.mule.providers.AbstractMessageAdapter;
import org.mule.providers.soap.MuleSoapHeaders;
import org.mule.providers.soap.i18n.SoapMessages;
import org.mule.transformers.simple.SerializableToByteArray;
import org.mule.umo.MessagingException;
import org.mule.umo.transformer.UMOTransformer;
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
    private final SOAPMessage message;
    private UMOTransformer trans = new SerializableToByteArray();

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

                this.message = ctx.getMessage();
                int x = 1;
                try
                {
                    for (Iterator i = this.message.getAttachments(); i.hasNext(); x++)
                    {
                        super.addAttachment(String.valueOf(x), ((AttachmentPart)i.next())
                            .getActivationDataHandler());
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
                this.message = null;
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
        payload = template.payload;
        message = template.message;
        trans = template.trans;
    }

    /**
     * Converts the message implementation into a String representation
     * 
     * @param encoding The encoding to use when transforming the message (if
     *            necessary). The parameter is used when converting from a byte array
     * @return String representation of the message payload
     * @throws Exception Implementation may throw an endpoint specific exception
     */
    public String getPayloadAsString(String encoding) throws Exception
    {
        return new String(getPayloadAsBytes(), encoding);
    }

    /**
     * Converts the payload implementation into a String representation
     * 
     * @return String representation of the payload
     * @throws Exception Implemetation may throw an endpoint specific exception
     */
    public byte[] getPayloadAsBytes() throws Exception
    {
        return (byte[])trans.transform(payload);
    }

    /**
     * @return the current payload
     */
    public Object getPayload()
    {
        return payload;
    }

    public SOAPMessage getSoapMessage()
    {
        return message;
    }

    public void addAttachment(String name, DataHandler dataHandler) throws Exception
    {
        if (null != message)
        {
            message.addAttachmentPart(new AttachmentPart(dataHandler));
        }
        super.addAttachment(name, dataHandler);
    }

    public void removeAttachment(String name) throws Exception
    {
        if ("all".equalsIgnoreCase(name))
        {
            message.removeAllAttachments();
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
