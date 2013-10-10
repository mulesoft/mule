/*
 * (c) 2003-2014 MuleSoft, Inc. This software is protected under international copyright
 * law. All use of this software is subject to MuleSoft's Master Subscription Agreement
 * (or other master license agreement) separately entered into in writing between you and
 * MuleSoft. If such an agreement is not in place, you may not use the software.
 */
package org.mule.transformer.simple;

import org.mule.RequestContext;
import org.mule.api.transformer.DiscoverableTransformer;
import org.mule.api.transformer.TransformerException;
import org.mule.api.transport.OutputHandler;
import org.mule.config.i18n.CoreMessages;
import org.mule.transformer.AbstractTransformer;
import org.mule.transformer.types.DataTypeFactory;
import org.mule.util.IOUtils;
import org.mule.util.StringMessageUtils;

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
        registerSourceType(DataTypeFactory.OBJECT);
        registerSourceType(DataTypeFactory.BYTE_ARRAY);
        registerSourceType(DataTypeFactory.INPUT_STREAM);
        registerSourceType(DataTypeFactory.create(OutputHandler.class));
        //deliberately set the mime for this transformer to text plain so that other transformers
        //that serialize string types such as XML or JSON will not match this
        setReturnDataType(DataTypeFactory.TEXT_STRING);
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

    public int getPriorityWeighting()
    {
        return priorityWeighting;
    }

    public void setPriorityWeighting(int priorityWeighting)
    {
        this.priorityWeighting = priorityWeighting;
    }
}
