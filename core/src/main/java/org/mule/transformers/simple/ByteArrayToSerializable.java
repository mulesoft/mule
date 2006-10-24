/*
 * $Id$
 * --------------------------------------------------------------------------------------
 * Copyright (c) MuleSource, Inc.  All rights reserved.  http://www.mulesource.com
 *
 * The software in this package is published under the terms of the MuleSource MPL
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.transformers.simple;

import org.apache.commons.lang.SerializationUtils;
import org.mule.config.i18n.Message;
import org.mule.config.i18n.Messages;
import org.mule.transformers.AbstractTransformer;
import org.mule.umo.transformer.TransformerException;

import java.io.InputStream;

/**
 * <code>ByteArrayToSerializable</code> converts a serialized object to its object
 * representation
 */
public class ByteArrayToSerializable extends AbstractTransformer
{
    /**
     * Serial version
     */
    private static final long serialVersionUID = 5305641786345530597L;

    public ByteArrayToSerializable()
    {
        registerSourceType(byte[].class);
        registerSourceType(InputStream.class);
    }

    public Object doTransform(Object src, String encoding) throws TransformerException
    {
        try
        {
            if (src instanceof byte[])
            {
                return SerializationUtils.deserialize((byte[])src);
            }
            else
            {
                return SerializationUtils.deserialize((InputStream)src);

            }
        }
        catch (Exception e)
        {
            throw new TransformerException(new Message(Messages.TRANSFORM_FAILED_FROM_X_TO_X, "byte[]",
                "Object"), this, e);
        }
    }

}
