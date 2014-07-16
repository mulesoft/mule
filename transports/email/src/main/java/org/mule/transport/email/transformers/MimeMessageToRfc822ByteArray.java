/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.transport.email.transformers;

import org.mule.api.transformer.TransformerException;
import org.mule.transformer.AbstractTransformer;
import org.mule.transformer.types.DataTypeFactory;

import java.io.ByteArrayOutputStream;

import javax.mail.internet.MimeMessage;

public class MimeMessageToRfc822ByteArray extends AbstractTransformer
{
    public MimeMessageToRfc822ByteArray()
    {
        registerSourceType(DataTypeFactory.create(MimeMessage.class));
        setReturnDataType(DataTypeFactory.BYTE_ARRAY);
    }

    @Override
    protected Object doTransform(Object src, String outputEncoding) throws TransformerException
    {
        try
        {
            MimeMessage mime = (MimeMessage) src;
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            mime.writeTo(baos);
            return baos.toByteArray();
        }
        catch (Exception e)
        {
            throw new TransformerException(this, e);
        }
    }
}
