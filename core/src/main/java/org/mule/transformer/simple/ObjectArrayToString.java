/*
 * (c) 2003-2014 MuleSoft, Inc. This software is protected under international copyright
 * law. All use of this software is subject to MuleSoft's Master Subscription Agreement
 * (or other master license agreement) separately entered into in writing between you and
 * MuleSoft. If such an agreement is not in place, you may not use the software.
 */
package org.mule.transformer.simple;

import org.mule.api.transformer.DiscoverableTransformer;
import org.mule.api.transformer.TransformerException;
import org.mule.transformer.AbstractTransformer;
import org.mule.transformer.types.DataTypeFactory;
import org.mule.util.StringUtils;

/**
 * <code>ObjectArrayToString</code> transformer is the opposite of
 * StringToObjectArray - it simply converts Object[] to a String in which each
 * element is separated by a configurable delimiter (default is a space).
 */

public class ObjectArrayToString extends AbstractTransformer implements DiscoverableTransformer
{
    /** Give core transformers a slighty higher priority */
    private int priorityWeighting = DiscoverableTransformer.DEFAULT_PRIORITY_WEIGHTING + 1;

    private static final String DEFAULT_DELIMITER = " ";

    private String delimiter = null;

    public ObjectArrayToString()
    {
        registerSourceType(DataTypeFactory.create(Object[].class));
        setReturnDataType(DataTypeFactory.TEXT_STRING);
    }

    @Override
    public Object doTransform(Object src, String encoding) throws TransformerException
    {
        if (src == null)
        {
            return src;
        }

        Object[] in = (Object[]) src;
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

    public String getDelimiter()
    {
        if (delimiter == null)
        {
            return DEFAULT_DELIMITER;
        }
        else
        {
            return delimiter;
        }
    }

    public void setDelimiter(String delimiter)
    {
        this.delimiter = delimiter;
    }

    public int getPriorityWeighting()
    {
        return priorityWeighting;
    }

    public void setPriorityWeighting(int priorityWeighting)
    {
        this.priorityWeighting = priorityWeighting;
    }
}
