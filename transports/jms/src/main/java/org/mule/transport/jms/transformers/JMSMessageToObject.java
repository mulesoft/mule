/*
 * (c) 2003-2014 MuleSoft, Inc. This software is protected under international copyright
 * law. All use of this software is subject to MuleSoft's Master Subscription Agreement
 * (or other master license agreement) separately entered into in writing between you and
 * MuleSoft. If such an agreement is not in place, you may not use the software.
 */
package org.mule.transport.jms.transformers;

import org.mule.api.MuleMessage;
import org.mule.api.transformer.TransformerException;
import org.mule.transformer.types.DataTypeFactory;
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

    @Override
    protected void declareInputOutputClasses()
    {
        registerSourceType(DataTypeFactory.create(Message.class));
        registerSourceType(DataTypeFactory.create(TextMessage.class));
        registerSourceType(DataTypeFactory.create(ObjectMessage.class));
        registerSourceType(DataTypeFactory.create(BytesMessage.class));
        registerSourceType(DataTypeFactory.create(MapMessage.class));
        registerSourceType(DataTypeFactory.create(StreamMessage.class));
    }

    @Override
    public Object transformMessage(MuleMessage message, String outputEncoding) throws TransformerException
    {
        try
        {
            if (logger.isDebugEnabled())
            {
                logger.debug("Source object is " + ClassUtils.getSimpleName(message.getPayload().getClass()));
            }

            Object result = transformFromMessage((Message) message.getPayload(), outputEncoding);

            // We need to handle String / byte[] explicitly since this transformer does not define
            // a single return type
            if (returnType.getType().equals(byte[].class) && result instanceof String)
            {
                result = result.toString().getBytes(outputEncoding);
            }
            else if (returnType.getType().equals(String.class) && result instanceof byte[])
            {
                result = new String((byte[]) result, outputEncoding);
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
