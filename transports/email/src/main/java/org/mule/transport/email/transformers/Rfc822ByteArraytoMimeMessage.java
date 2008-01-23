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
import org.mule.api.transport.Connector;
import org.mule.transformer.AbstractTransformer;
import org.mule.transport.email.AbstractMailConnector;

import java.io.ByteArrayInputStream;

import javax.mail.MessagingException;
import javax.mail.Session;
import javax.mail.internet.MimeMessage;

public class Rfc822ByteArraytoMimeMessage extends AbstractTransformer
{

    public Rfc822ByteArraytoMimeMessage()
    {
        registerSourceType(byte[].class);
        setReturnClass(MimeMessage.class);
    }

    protected Object doTransform(Object src, String encoding) throws TransformerException
    {
        try
        {
            byte[] bytes = (byte[]) src;
            return new MimeMessage(getSession(), new ByteArrayInputStream(bytes));
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
        return ((AbstractMailConnector) connector).getSessionDetails(endpoint).getSession();
    }

}
