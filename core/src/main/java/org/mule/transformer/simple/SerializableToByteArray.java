/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.transformer.simple;

import org.mule.api.transformer.DiscoverableTransformer;
import org.mule.api.transformer.TransformerException;
import org.mule.transformer.AbstractTransformer;
import org.mule.transformer.types.DataTypeFactory;

import java.io.Serializable;

/**
 * <code>SerializableToByteArray</code> converts a serializable object or a String
 * to a byte array. If <code>MuleMessage</code> is configured as a source type on this
 * transformer by calling <code>setAcceptMuleMessage(true)</code> then the MuleMessage
 * will be serialised. This is useful for transports such as TCP where the message
 * headers would normally be lost.
 */
public class SerializableToByteArray extends AbstractTransformer implements DiscoverableTransformer
{
    private int priorityWeighting = DiscoverableTransformer.DEFAULT_PRIORITY_WEIGHTING;

    public SerializableToByteArray()
    {
        this.registerSourceType(DataTypeFactory.create(Serializable.class));
        this.setReturnDataType(DataTypeFactory.BYTE_ARRAY);
    }

    public boolean isAcceptMuleMessage()
    {
        return this.isSourceDataTypeSupported(DataTypeFactory.MULE_MESSAGE, true);
    }

    public void setAcceptMuleMessage(boolean value)
    {
        if (value)
        {
            this.registerSourceType(DataTypeFactory.MULE_MESSAGE);
        }
        else
        {
            this.unregisterSourceType(DataTypeFactory.MULE_MESSAGE);
        }
    }

    @Override
    public Object doTransform(Object src, String outputEncoding) throws TransformerException
    {
        /*
         * If the MuleMessage source type has been registered then we can assume that
         * the whole message is to be serialised, not just the payload. This can be
         * useful for protocols such as tcp where the protocol does not support
         * headers and the whole message needs to be serialized.
         */

        try
        {
            return muleContext.getObjectSerializer().serialize(src);
        }
        catch (Exception e)
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
