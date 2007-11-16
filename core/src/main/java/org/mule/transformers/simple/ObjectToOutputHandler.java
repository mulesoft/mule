/*
 * $Id: ByteArrayToString.java 8077 2007-08-27 20:15:25Z aperepel $
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
import org.mule.umo.UMOEvent;
import org.mule.umo.provider.OutputHandler;
import org.mule.umo.transformer.DiscoverableTransformer;
import org.mule.umo.transformer.TransformerException;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.Serializable;

import org.apache.commons.lang.SerializationUtils;

/** <code>ObjectToOutputHandler</code> converts a byte array into a String. */
public class ObjectToOutputHandler extends AbstractTransformer implements DiscoverableTransformer
{

    /** Give core transformers a slighty higher priority */
    private int priorityWeighting = DiscoverableTransformer.DEFAULT_PRIORITY_WEIGHTING + 1;

    public ObjectToOutputHandler()
    {
        registerSourceType(byte[].class);
        registerSourceType(String.class);
        registerSourceType(InputStream.class);
        setReturnClass(OutputHandler.class);
    }

    public Object doTransform(final Object src, final String encoding) throws TransformerException
    {
        if (src instanceof String)
        {
            return new OutputHandler()
            {
                public void write(UMOEvent event, OutputStream out) throws IOException
                {
                    out.write(((String) src).getBytes(encoding));
                }
            };
        }
        else if (src instanceof byte[])
        {
            return new OutputHandler()
            {
                public void write(UMOEvent event, OutputStream out) throws IOException
                {
                    out.write((byte[]) src);
                }
            };
        }
        else if (src instanceof Serializable)
        {
            return new OutputHandler()
            {
                public void write(UMOEvent event, OutputStream out) throws IOException
                {
                    SerializationUtils.serialize((Serializable) src, out);
                }
            };
        }
        else
        {
            throw new TransformerException(MessageFactory
                    .createStaticMessage("Unable to convert " + src.getClass() + " to OutputHandler."));
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
