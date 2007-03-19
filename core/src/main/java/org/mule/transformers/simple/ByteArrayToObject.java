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

import org.mule.umo.transformer.TransformerException;

import java.io.ObjectStreamConstants;

/**
 * <code>ByteArrayToObject</code> works in the same way as
 * <code>ByteArrayToSerializable</code> but checks if th byte array is a serialised
 * object and if not will return a String created from the bytes is the returnType on
 * the transformer.
 */
public class ByteArrayToObject extends ByteArrayToSerializable
{

    // @Override
    public Object doTransform(Object src, String encoding) throws TransformerException
    {
        byte[] bytes = (byte[])src;

        if (bytes[0] == (byte)((ObjectStreamConstants.STREAM_MAGIC >>> 8) & 0xFF))
        {
            return super.doTransform(src, encoding);
        }
        else
        {
            try
            {
                return new String(bytes, encoding);
            }
            catch (Exception e)
            {
                throw new TransformerException(this, e);
            }
        }
    }

}
