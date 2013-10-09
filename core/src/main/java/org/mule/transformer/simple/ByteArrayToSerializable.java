/*
 * (c) 2003-2014 MuleSoft, Inc. This software is protected under international copyright
 * law. All use of this software is subject to MuleSoft's Master Subscription Agreement
 * (or other master license agreement) separately entered into in writing between you and
 * MuleSoft. If such an agreement is not in place, you may not use the software.
 */
package org.mule.transformer.simple;

import org.mule.api.transformer.DiscoverableTransformer;
import org.mule.api.transformer.TransformerException;
import org.mule.config.i18n.CoreMessages;
import org.mule.transformer.AbstractTransformer;
import org.mule.transformer.types.DataTypeFactory;
import org.mule.util.SerializationUtils;

import java.io.InputStream;

/**
 * <code>ByteArrayToSerializable</code> converts a serialized object to its object
 * representation
 */
public class ByteArrayToSerializable extends AbstractTransformer implements DiscoverableTransformer
{

    /**
     * Give core transformers a slighty higher priority
     */
    private int priorityWeighting = DiscoverableTransformer.DEFAULT_PRIORITY_WEIGHTING + 1;

    public ByteArrayToSerializable()
    {
        registerSourceType(DataTypeFactory.BYTE_ARRAY);
        registerSourceType(DataTypeFactory.INPUT_STREAM);
    }

    @Override
    public Object doTransform(Object src, String encoding) throws TransformerException
    {
        try
        {
            final Object result;
            if (src instanceof byte[])
            {
                result = SerializationUtils.deserialize((byte[]) src, muleContext);
            }
            else
            {
                result = SerializationUtils.deserialize((InputStream) src, muleContext);
            }
            return result;
        }
        catch (Exception e)
        {
            throw new TransformerException(
                    CoreMessages.transformFailed("byte[]", "Object"), this, e);
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
