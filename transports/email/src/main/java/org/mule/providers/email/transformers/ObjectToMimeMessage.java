/*
 * $Id$
 * --------------------------------------------------------------------------------------
 * Copyright (c) MuleSource, Inc.  All rights reserved.  http://www.mulesource.com
 *
 * The software in this package is published under the terms of the MuleSource MPL
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.providers.email.transformers;

import org.mule.providers.email.MailMessageAdapter;
import org.mule.transformers.simple.SerializableToByteArray;
import org.mule.umo.UMOEventContext;
import org.mule.umo.transformer.TransformerException;
import org.mule.util.StringUtils;

import java.io.IOException;
import java.io.Serializable;
import java.util.Iterator;
import java.util.Map;

import javax.activation.DataHandler;
import javax.activation.DataSource;
import javax.activation.FileDataSource;
import javax.mail.BodyPart;
import javax.mail.Message;
import javax.mail.MessagingException;
import javax.mail.internet.MimeBodyPart;
import javax.mail.internet.MimeMultipart;

/**
 * Transforms a javax.mail.Message to a UMOMessage, with support for attachments
 */
public class ObjectToMimeMessage extends StringToEmailMessage
{
    /**
     * Serial version
     */
    private static final long serialVersionUID = 7225142214620572674L;

    protected void setContent(Object payload, Message msg, String contentType, UMOEventContext context)
        throws Exception
    {

        if (context.getMessage().getAttachmentNames().size() > 0)
        {
            // The content type must be multipart/mixed
            MimeMultipart multipart = new MimeMultipart("mixed");
            multipart.addBodyPart(getPayloadBodyPart(payload, contentType));
            for (Iterator it = context.getMessage().getAttachmentNames().iterator(); it.hasNext();)
            {
                String name = (String)it.next();
                BodyPart part = getBodyPartForAttachment(context.getMessage().getAttachment(name), name);
                // Check message props for extra headers
                addBodyPartHeaders(part, name, context);
                multipart.addBodyPart(part);
            }
            // the payload must be set to the constructed MimeMultipart message
            payload = multipart;
            // the ContentType of the message to be sent, must be the multipart
            contentType = multipart.getContentType();
            // content type
        }
        // now the message will contain the multipart payload, and the multipart
        // contentType
        super.setContent(payload, msg, contentType, context);
    }

    protected void addBodyPartHeaders(BodyPart part, String name, UMOEventContext context)
    {

        Map headers = (Map)context.getMessage().getProperty(
            name + MailMessageAdapter.ATTACHMENT_HEADERS_PROPERTY_POSTFIX);
        if (null != headers)
        {
            for (Iterator it = headers.keySet().iterator(); it.hasNext();)
            {
                try
                {
                    String key = (String)it.next();
                    part.setHeader(key, (String)headers.get(key));
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

        DataSource source = handler.getDataSource();

        // Only set the file name if the DataSource is a file
        if (source instanceof FileDataSource)
        {
            part.setFileName(StringUtils.defaultString(handler.getName(), name));
        }
        return part;
    }

    protected BodyPart getPayloadBodyPart(Object payload, String contentType)
        throws MessagingException, TransformerException, IOException
    {

        DataHandler handler;
        if (payload instanceof String)
        {
            handler = new DataHandler(new PlainTextDataSource(contentType, payload.toString()));
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

}
