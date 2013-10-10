/*
 * (c) 2003-2014 MuleSoft, Inc. This software is protected under international copyright
 * law. All use of this software is subject to MuleSoft's Master Subscription Agreement
 * (or other master license agreement) separately entered into in writing between you and
 * MuleSoft. If such an agreement is not in place, you may not use the software.
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
