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
import org.mule.util.SerializationUtils;

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
            return SerializationUtils.serialize((Serializable) src);
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
