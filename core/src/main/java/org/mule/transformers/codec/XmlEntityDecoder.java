/*
 * $Id$
 * --------------------------------------------------------------------------------------
 * Copyright (c) MuleSource, Inc.  All rights reserved.  http://www.mulesource.com
 *
 * The software in this package is published under the terms of the MuleSource MPL
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.transformers.codec;

import org.mule.config.i18n.Message;
import org.mule.config.i18n.Messages;
import org.mule.transformers.AbstractTransformer;
import org.mule.umo.transformer.TransformerException;
import org.mule.util.XMLEntityCodec;

/**
 * Decodes a String or byte[] containing XML entities
 */
public class XmlEntityDecoder extends AbstractTransformer
{
    /**
     * Serial version
     */
    private static final long serialVersionUID = -3198566471610838679L;

    public XmlEntityDecoder()
    {
        registerSourceType(String.class);
        registerSourceType(byte[].class);
        setReturnClass(String.class);
    }

    public Object doTransform(Object src, String encoding) throws TransformerException
    {
        try
        {
            String data;

            if (src instanceof byte[])
            {
                data = new String((byte[])src, encoding);
            }
            else
            {
                data = (String)src;
            }

            return XMLEntityCodec.decodeString(data);
        }
        catch (Exception ex)
        {
            throw new TransformerException(new Message(Messages.TRANSFORM_FAILED_FROM_X_TO_X, src.getClass()
                .getName(), "XML"), this, ex);

        }
    }

}
