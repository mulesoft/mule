/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.transport.email.transformers;

import org.mule.api.transformer.TransformerException;
import org.mule.api.transport.Connector;
import org.mule.config.i18n.CoreMessages;
import org.mule.transformer.AbstractTransformer;
import org.mule.transformer.types.DataTypeFactory;
import org.mule.transport.email.AbstractMailConnector;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.io.UnsupportedEncodingException;

import javax.mail.MessagingException;
import javax.mail.Session;
import javax.mail.internet.MimeMessage;

public class Rfc822ByteArraytoMimeMessage extends AbstractTransformer
{

    public Rfc822ByteArraytoMimeMessage()
    {
        registerSourceType(DataTypeFactory.BYTE_ARRAY);
        registerSourceType(DataTypeFactory.INPUT_STREAM);
        setReturnDataType(DataTypeFactory.create(MimeMessage.class));
    }

    @Override
    protected Object doTransform(Object src, String encoding) throws TransformerException
    {
        try
        {
            if (src instanceof byte[])
            {
                byte[] bytes = (byte[]) src;
                return new MimeMessage(getSession(), new ByteArrayInputStream(bytes));
            }
            else if (src instanceof InputStream)
            {
                return new MimeMessage(getSession(), (InputStream)src);
            }
            else
            {
                throw new TransformerException(
                    CoreMessages.transformOnObjectUnsupportedTypeOfEndpoint(this.getName(), src.getClass(), endpoint));
            }
        }
        catch (MessagingException e)
        {
            throw new TransformerException(this, e);
        }
    }

    protected Session getSession() throws TransformerException
    {
        if (null == endpoint)
        {
            throw new TransformerException(this,
                    new IllegalStateException("The transformer is no associated with an endpoint."));
        }
        Connector connector = endpoint.getConnector();
        if (!(connector instanceof AbstractMailConnector))
        {
            throw new TransformerException(this,
                    new IllegalStateException("The transformer is not associated with an email endpoint."));
        }
        try
        {
            return ((AbstractMailConnector) connector).getSessionDetails(endpoint).getSession();
        }
        catch (UnsupportedEncodingException e)
        {
            throw new TransformerException(this, e);
        }
    }

}
