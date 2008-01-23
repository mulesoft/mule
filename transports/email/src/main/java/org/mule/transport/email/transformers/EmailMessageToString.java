/*
 * $Id$
 * --------------------------------------------------------------------------------------
 * Copyright (c) MuleSource, Inc.  All rights reserved.  http://www.mulesource.com
 *
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.transport.email.transformers;

import org.mule.api.transformer.TransformerException;
import org.mule.transformer.AbstractTransformer;

import javax.mail.Message;
import javax.mail.internet.MimeMultipart;

/**
 * <code>EmailMessageToString</code> extracts a java mail Message contents and
 * returns a string.
 */
public class EmailMessageToString extends AbstractTransformer
{

    public EmailMessageToString()
    {
        registerSourceType(Message.class);
        setReturnClass(String.class);
    }

    /*
     * (non-Javadoc)
     * 
     * @see org.mule.transformer.AbstractTransformer#doTransform(java.lang.Object)
     */
    public Object doTransform(Object src, String encoding) throws TransformerException
    {
        Message msg = (Message) src;
        try
        {
            /*
             * Other information about the message such as cc addresses, attachments
             * are handled by the mail message adapter. If Transformers need access
             * to these properties they should extends from
             * AbstractEventAwareTransformer and extract these properties from the
             * passed MuleEventContext
             */

            // For this impl we just pass back the email content
            Object result = msg.getContent();
            if (result instanceof String)
            {
                return result;
            }
            else
            {
                // very simplisitic, only gets first part
                MimeMultipart part = (MimeMultipart)result;
                String transMsg = (String) part.getBodyPart(0).getContent();
                return transMsg;
            }
        }
        catch (Exception e)
        {
            throw new TransformerException(this, e);
        }
    }
}
