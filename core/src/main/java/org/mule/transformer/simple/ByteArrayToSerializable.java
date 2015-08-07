/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.transformer.simple;

import org.mule.api.serialization.ObjectSerializer;
import org.mule.api.transformer.DiscoverableTransformer;
import org.mule.api.transformer.TransformerException;
import org.mule.config.i18n.CoreMessages;
import org.mule.transformer.AbstractTransformer;
import org.mule.transformer.types.DataTypeFactory;

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
        ObjectSerializer serializer = muleContext.getObjectSerializer();
        try
        {
            final Object result;
            if (src instanceof byte[])
            {
                result = serializer.deserialize((byte[]) src);
            }
            else
            {
                result = serializer.deserialize((InputStream) src);
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
