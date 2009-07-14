/*
 * $Id$
 * --------------------------------------------------------------------------------------
 * Copyright (c) MuleSource, Inc.  All rights reserved.  http://www.mulesource.com
 *
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.transport.jms.transformers;

import org.mule.api.MuleMessage;
import org.mule.api.transformer.TransformerException;
import org.mule.util.ClassUtils;

import javax.jms.BytesMessage;
import javax.jms.MapMessage;
import javax.jms.Message;
import javax.jms.ObjectMessage;
import javax.jms.StreamMessage;
import javax.jms.TextMessage;

/**
 * <code>JMSMessageToObject</code> Will convert a <code>javax.jms.Message</code>
 * or sub-type into an object by extracting the message payload. Users of this
 * transformer can set different return types on the transform to control the way it
 * behaves.
 * <ul>
 * <li>javax.jms.TextMessage - java.lang.String</li>
 * <li>javax.jms.ObjectMessage - java.lang.Object</li>
 * <li>javax.jms.BytesMessage - Byte[]. Note that the transformer will check if the
 * payload is compressed and automatically uncompress the message.</li>
 * <li>javax.jms.MapMessage - java.util.Map</li>
 * <li>javax.jms.StreamMessage - java.util.Vector of objects from the Stream
 * Message.</li>
 * </ul>
 */

public class JMSMessageToObject extends AbstractJmsTransformer
{

    public JMSMessageToObject()
    {
        super();
    }

    protected void declareInputOutputClasses()
    {
        registerSourceType(Message.class);
        registerSourceType(TextMessage.class);
        registerSourceType(ObjectMessage.class);
        registerSourceType(BytesMessage.class);
        registerSourceType(MapMessage.class);
        registerSourceType(StreamMessage.class);
    }
    
    public Object transform(MuleMessage message, String outputEncoding) throws TransformerException
    {
        try
        {
            if (logger.isDebugEnabled())
            {
                logger.debug("Source object is " + ClassUtils.getSimpleName(message.getPayload().getClass()));
            }

            Object result = transformFromMessage((Message) message.getPayload(), outputEncoding);

            //We need to handle String / byte[] explicitly since this transformer does not nefine a single return type
            //TODO I don't think we should allow a null return class
            if (returnClass != null)
            {
                if (returnClass.equals(byte[].class) && result instanceof String)
                {
                    result = result.toString().getBytes(outputEncoding);
                }
                else if (returnClass.equals(String.class) && result instanceof byte[])
                {
                    result = new String((byte[]) result, outputEncoding);
                }
            }

            if (logger.isDebugEnabled())
            {
                logger.debug("Resulting object is " + ClassUtils.getSimpleName(result.getClass()));
            }

            return result;
        }
        catch (Exception e)
        {
            throw new TransformerException(this, e);
        }
    }

}
