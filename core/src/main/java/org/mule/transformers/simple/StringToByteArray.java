/*
 * $Id$
 * --------------------------------------------------------------------------------------
 * Copyright (c) MuleSource, Inc.  All rights reserved.  http://www.mulesource.com
 *
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.transformers.simple;

import org.mule.config.i18n.MessageFactory;
import org.mule.transformers.AbstractTransformer;
import org.mule.umo.transformer.DiscoverableTransformer;
import org.mule.umo.transformer.TransformerException;

import java.io.UnsupportedEncodingException;

/** <code>StringToByteArray</code> converts a String into a byte array. */

public class StringToByteArray extends AbstractTransformer implements DiscoverableTransformer
{

    /** Give core transformers a slighty higher priority */
    private int priorityWeighting = DiscoverableTransformer.DEFAULT_PRIORITY_WEIGHTING + 1;

    public StringToByteArray()
    {
        registerSourceType(String.class);
        registerSourceType(byte[].class);
        setReturnClass(byte[].class);
    }

    public Object doTransform(Object src, String encoding) throws TransformerException
    {
        if (src instanceof byte[])
        {
            return src;
        }
        else
        {
            try
            {
                return ((String) src).getBytes(encoding);
            }
            catch (UnsupportedEncodingException e)
            {
                throw new TransformerException(MessageFactory
                        .createStaticMessage("Unable to convert String to byte[]."), e);
            }
        }
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
