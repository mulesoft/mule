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
 * <code>ObjectArrayToString</code> transformer is the opposite of 
 * StringToObjectArray - it simply converts Object[] to a String in which each
 * element is separated by a configurable delimiter (default is a space).
 */

public class ObjectArrayToString extends AbstractTransformer
{
    private String delimiter = null;
    private static final String DEFAULT_DELIMITER = " ";

    public ObjectArrayToString()
    {
        registerSourceType(Object[].class);
        setReturnClass(String.class);
    }

    public Object doTransform(Object src, String encoding) throws TransformerException
    {
        if (src == null) return src;

        Object[] in = (Object[])src;
        String out = StringUtils.join(in, getDelimiter());

        /*
        for (int i = 0; i < in.length; i++)
        {
            if (in[i] != null)
            {
                if (i > 0) out += getDelimiter();
                out += in[i].toString();
            }
        }
        */

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
     * @param sets the delimiter
     */
    public void setDelimiter(String delimiter)
    {
        this.delimiter = delimiter;
    }
        
}
