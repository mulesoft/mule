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

import org.mule.transformers.AbstractTransformer;
import org.mule.umo.transformer.TransformerException;

import java.io.ByteArrayOutputStream;

import javax.mail.internet.MimeMessage;

public class MimeMessageToRfc822ByteArray extends AbstractTransformer
{

    public MimeMessageToRfc822ByteArray()
    {
        registerSourceType(MimeMessage.class);
        setReturnClass(byte[].class);
    }

    protected Object doTransform(Object src, String encoding) throws TransformerException
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
