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

/**
 * <code>StringToObjectArray</code> converts a String into an object array. This
 * is useful in certain situations, as when a string needs to be converted into
 * an Object[] in order to be passed to a SOAP service. The input String is parsed
 * into the array based on a configurable delimiter - default is a space.
 */

public class StringToObjectArray extends AbstractTransformer
{
    private String delimiter = null;
    private static final String DEFAULT_DELIMITER = " ";

    public StringToObjectArray()
    {
        registerSourceType(String.class);
        registerSourceType(byte[].class);
        setReturnClass(Object[].class);
    }

    public Object doTransform(Object src, String encoding) throws TransformerException
    {
        String in;

        if (src instanceof byte[])
        {
            in = new String((byte[])src);
        }
        else
        {
            in = (String)src;
        }

        String[] out = StringUtils.splitAndTrim(in, getDelimiter());
        return out;
    }

    /**
     * @return the delimiter
     */
    public String getDelimiter()
    {
        if (delimiter == null)
            return DEFAULT_DELIMITER;
        else
            return delimiter;
    }

    /**
     * @param delimiter the delimiter
     */
    public void setDelimiter(String delimiter)
    {
        this.delimiter = delimiter;
    }
        
}
