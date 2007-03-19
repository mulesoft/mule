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

import org.mule.transformers.AbstractTransformer;
import org.mule.umo.transformer.TransformerException;
import org.mule.util.StringUtils;

import org.apache.commons.lang.ArrayUtils;

/**
 * Converts a Hex String to a Byte array
 */
public class HexStringToByteArray extends AbstractTransformer
{

    public HexStringToByteArray()
    {
        registerSourceType(String.class);
        setReturnClass(byte[].class);
    }

    protected Object doTransform(Object src, String encoding) throws TransformerException
    {
        if (src == null)
        {
            return ArrayUtils.EMPTY_BYTE_ARRAY;
        }

        try
        {
            return StringUtils.hexStringToByteArray((String)src);
        }
        catch (Exception ex)
        {
            throw new TransformerException(this, ex);
        }
    }

}
