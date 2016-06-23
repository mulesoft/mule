/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.core.transformer.simple;

import org.mule.runtime.api.metadata.DataType;
import org.mule.runtime.core.RequestContext;
import org.mule.runtime.core.api.transformer.DiscoverableTransformer;
import org.mule.runtime.core.api.transformer.TransformerException;
import org.mule.runtime.core.config.i18n.CoreMessages;
import org.mule.runtime.core.message.OutputHandler;
import org.mule.runtime.core.transformer.AbstractTransformer;
import org.mule.runtime.core.util.IOUtils;
import org.mule.runtime.core.util.StringMessageUtils;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.UnsupportedEncodingException;

/**
 * <code>ObjectToString</code> transformer is useful for debugging. It will return
 * human-readable output for various kinds of objects. Right now, it is just coded to
 * handle Map and Collection objects. Others will be added.
 */
public class ObjectToString extends AbstractTransformer implements DiscoverableTransformer
{
    protected static final int DEFAULT_BUFFER_SIZE = 80;

    /** Give core transformers a slighty higher priority */
    private int priorityWeighting = DiscoverableTransformer.DEFAULT_PRIORITY_WEIGHTING + 1;

    public ObjectToString()
    {
        registerSourceType(DataType.OBJECT);
        registerSourceType(DataType.BYTE_ARRAY);
        registerSourceType(DataType.INPUT_STREAM);
        registerSourceType(DataType.fromType(OutputHandler.class));
        setReturnDataType(DataType.STRING);
    }

    @Override
    public Object doTransform(Object src, String outputEncoding) throws TransformerException
    {
        String output = "";

        if (src instanceof InputStream)
        {
            output = createStringFromInputStream((InputStream) src, outputEncoding);
        }
        else if (src instanceof OutputHandler)
        {
            output = createStringFromOutputHandler((OutputHandler) src, outputEncoding);
        }
        else if (src instanceof byte[])
        {
            output = createStringFromByteArray((byte[]) src, outputEncoding);
        }
        else
        {
            output = StringMessageUtils.toString(src);
        }

        return output;
    }

    protected String createStringFromInputStream(InputStream input, String outputEncoding)
        throws TransformerException
    {
        try
        {
            ByteArrayOutputStream byteOut = new ByteArrayOutputStream();
            IOUtils.copy(input, byteOut);
            return byteOut.toString(outputEncoding);
        }
        catch (IOException e)
        {
            throw new TransformerException(CoreMessages.errorReadingStream(), e);
        }
        finally
        {
            try
            {
                input.close();
            }
            catch (IOException e)
            {
                logger.warn("Could not close stream", e);
            }
        }
    }

    protected String createStringFromOutputHandler(OutputHandler handler, String outputEncoding)
        throws TransformerException
    {
        ByteArrayOutputStream bytes = new ByteArrayOutputStream();
        try
        {
            handler.write(RequestContext.getEvent(), bytes);
            return bytes.toString(outputEncoding);
        }
        catch (IOException e)
        {
            throw new TransformerException(this, e);
        }
    }

    protected String createStringFromByteArray(byte[] bytes, String outputEncoding)
        throws TransformerException
    {
        try
        {
            return new String(bytes, outputEncoding);
        }
        catch (UnsupportedEncodingException e)
        {
            throw new TransformerException(this, e);
        }
    }

    @Override
    public int getPriorityWeighting()
    {
        return priorityWeighting;
    }

    @Override
    public void setPriorityWeighting(int priorityWeighting)
    {
        this.priorityWeighting = priorityWeighting;
    }
}
