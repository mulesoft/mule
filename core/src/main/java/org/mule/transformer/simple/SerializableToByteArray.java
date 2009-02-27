/*
 * $Id$
 * --------------------------------------------------------------------------------------
 * Copyright (c) MuleSource, Inc.  All rights reserved.  http://www.mulesource.com
 *
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.transformer.simple;

import org.mule.api.MuleMessage;
import org.mule.api.transformer.DiscoverableTransformer;
import org.mule.api.transformer.TransformerException;
import org.mule.transformer.AbstractTransformer;

import java.io.Serializable;

import org.apache.commons.lang.SerializationUtils;

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
        this.registerSourceType(Serializable.class);
        this.setReturnClass(byte[].class);
    }

    public boolean isAcceptMuleMessage()
    {
        return this.isSourceTypeSupported(MuleMessage.class, true);
    }

    public void setAcceptMuleMessage(boolean value)
    {
        if (value)
        {
            this.registerSourceType(MuleMessage.class);
        }
        else
        {
            this.unregisterSourceType(MuleMessage.class);
        }
    }

    /**
     * @deprecated use {@link #isAcceptMuleMessage}
     */
    @Deprecated
    public boolean isAcceptUMOMessage()
    {
        return isAcceptMuleMessage();
    }

    /**
     * @deprecated use {@link #setAcceptMuleMessage(boolean)}
     */
    @Deprecated
    public void setAcceptUMOMessage(boolean value)
    {
        setAcceptMuleMessage(value);
    }

    public Object doTransform(Object src, String encoding) throws TransformerException
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
