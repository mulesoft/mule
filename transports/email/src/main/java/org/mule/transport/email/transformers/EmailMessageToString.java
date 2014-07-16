/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.transport.email.transformers;

import org.mule.api.transformer.TransformerException;
import org.mule.transformer.AbstractDiscoverableTransformer;
import org.mule.transformer.types.DataTypeFactory;

import javax.mail.BodyPart;
import javax.mail.Message;
import javax.mail.internet.MimeMultipart;

/**
 * <code>EmailMessageToString</code> extracts the text body of java mail Message and
 * returns a string. If there is no text body then an empty string is returned.
 */
public class EmailMessageToString extends AbstractDiscoverableTransformer
{

    public EmailMessageToString()
    {
        registerSourceType(DataTypeFactory.create(Message.class));
        setReturnDataType(DataTypeFactory.STRING);
    }

    @Override
    public Object doTransform(Object src, String outputEncoding) throws TransformerException
    {
        Message msg = (Message) src;
        try
        {
            /*
             * Other information about the message such as cc addresses, attachments
             * are handled by the mail mule message factory.
             */

            // For this impl we just pass back the email content
            Object result = msg.getContent();
            if (result instanceof String)
            {
                return result;
            }
            else if (result instanceof MimeMultipart)
            {
                // very simplistic, only gets first part
                BodyPart firstBodyPart = ((MimeMultipart) result).getBodyPart(0);
                if (firstBodyPart != null && firstBodyPart.getContentType().startsWith("text/"))
                {
                    Object content = firstBodyPart.getContent();
                    if (content instanceof String)
                    {
                        return content;
                    }
                }
            }
            // No text content found either in message or in first body part of
            // MultiPart content
            return "";
        }
        catch (Exception e)
        {
            throw new TransformerException(this, e);
        }
    }

}
