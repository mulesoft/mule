/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.transport.email.transformers;

import org.mule.api.MuleMessage;
import org.mule.api.transformer.TransformerException;
import org.mule.transformer.simple.SerializableToByteArray;
import org.mule.transformer.types.DataTypeFactory;
import org.mule.transport.email.AbstractMailConnector;
import org.mule.util.StringUtils;

import java.io.IOException;
import java.io.Serializable;
import java.util.Map;

import javax.activation.DataHandler;
import javax.mail.BodyPart;
import javax.mail.Message;
import javax.mail.MessagingException;
import javax.mail.internet.MimeBodyPart;
import javax.mail.internet.MimeMultipart;
import javax.mail.util.ByteArrayDataSource;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

/**
 * Transforms a {@link javax.mail.Message} to a {@link MuleMessage}, with support for attachments
 */
public class ObjectToMimeMessage extends StringToEmailMessage
{
    private Log logger = LogFactory.getLog(getClass());
    private boolean useInboundAttachments = true;
    private boolean useOutboundAttachments = true;

    public ObjectToMimeMessage()
    {
        this.registerSourceType(DataTypeFactory.create(Message.class));
    }

    @Override
    public Object transformMessage(MuleMessage message, String outputEncoding) throws TransformerException
    {
        if (message.getPayload() instanceof Message)
        {
            return message.getPayload();
        }
        else
        {
            return super.transformMessage(message, outputEncoding);
        }
    }

    @Override
    protected void setContent(Object payload, Message msg, String contentType, MuleMessage message)
        throws Exception
    {
        boolean transformInboundAttachments = useInboundAttachments && message.getInboundAttachmentNames().size() > 0;
        boolean transformOutboundAttachments = useOutboundAttachments && message.getOutboundAttachmentNames().size() > 0;
        if (transformInboundAttachments || transformOutboundAttachments)
        {
            // The content type must be multipart/mixed
            MimeMultipart multipart = new MimeMultipart("mixed");
            multipart.addBodyPart(getPayloadBodyPart(message.getPayload(), contentType));
            if (transformInboundAttachments)
            {
                for (String name : message.getInboundAttachmentNames())
                {
                    // Let outbound attachments override inbound ones
                    if (!transformOutboundAttachments || message.getOutboundAttachment(name) == null)
                    {
                        BodyPart part = getBodyPartForAttachment(message.getInboundAttachment(name), name);
                        // Check message props for extra headers
                        addBodyPartHeaders(part, name, message);
                        multipart.addBodyPart(part);
                    }
                }
            }
            if (transformOutboundAttachments)
            {
                for (String name : message.getOutboundAttachmentNames())
                {
                    BodyPart part = getBodyPartForAttachment(message.getOutboundAttachment(name), name);
                    // Check message props for extra headers
                    addBodyPartHeaders(part, name, message);
                    multipart.addBodyPart(part);
                }
            }
            // the payload must be set to the constructed MimeMultipart message
            payload = multipart;
            // the ContentType of the message to be sent, must be the multipart content type
            contentType = multipart.getContentType();
        }
        
        // now the message will contain the multipart payload, and the multipart
        // contentType
        super.setContent(payload, msg, contentType, message);
    }

    protected void addBodyPartHeaders(BodyPart part, String name, MuleMessage message)
    {
        Map<String, String> headers = message.getOutboundProperty(
            name + AbstractMailConnector.ATTACHMENT_HEADERS_PROPERTY_POSTFIX);

        if (null != headers)
        {
            for (String key : headers.keySet())
            {
                try
                {
                    part.setHeader(key, headers.get(key));
                }
                catch (MessagingException me)
                {
                    logger.error("Failed to set bodypart header", me);
                }
            }
        }
    }

    protected BodyPart getBodyPartForAttachment(DataHandler handler, String name) throws MessagingException
    {
        BodyPart part = new MimeBodyPart();
        part.setDataHandler(handler);
        part.setDescription(name);

        part.setFileName(StringUtils.defaultString(handler.getName(), name));
        return part;
    }

    protected BodyPart getPayloadBodyPart(Object payload, String contentType)
        throws MessagingException, TransformerException, IOException
    {
        DataHandler handler;
        if (payload instanceof String)
        {
            handler = new DataHandler(new ByteArrayDataSource((String) payload, contentType));
        }
        else if (payload instanceof byte[])
        {
            handler = new DataHandler(new ByteArrayDataSource((byte[])payload, contentType));
        }
        else if (payload instanceof Serializable)
        {
            handler = new DataHandler(new ByteArrayDataSource(
                (byte[])new SerializableToByteArray().transform(payload), contentType));
        }
        else
        {
            throw new IllegalArgumentException();
        }
        BodyPart part = new MimeBodyPart();
        part.setDataHandler(handler);
        part.setDescription("Payload");
        return part;
    }

    /**
     * Set whether inbound attachments should be transformed into MIME parts
     */
    public void setUseInboundAttachments(boolean useInboundAttachments)
    {
        this.useInboundAttachments = useInboundAttachments;
    }

    /**
     * Set whether outbound attachments should be transformed into MIME parts
     */
    public void setUseOutboundAttachments(boolean useOutboundAttachments)
    {
        this.useOutboundAttachments = useOutboundAttachments;
    }
}
