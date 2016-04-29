/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.transport.jms.transformers;

import org.mule.runtime.core.api.MuleEvent;
import org.mule.runtime.core.api.MuleMessage;
import org.mule.runtime.core.api.transformer.TransformerException;
import org.mule.runtime.core.transformer.types.DataTypeFactory;
import org.mule.runtime.core.util.ClassUtils;

import javax.jms.Message;

/**
 * <code>ObjectToJMSMessage</code> will convert any object to a
 * <code>javax.jms.Message</code> or sub-type into an object. One of the 5 types of
 * JMS message will be created based on the type of Object passed in.
 * <ul>
 * <li>java.lang.String - javax.jms.TextMessage</li>
 * <li>byte[] - javax.jms.BytesMessage</li>
 * <li>java.util.Map - javax.jms.MapMessage</li>
 * <li>java.io.InputStream - javax.jms.StreamMessage</li>
 * <li>java.lang.Object - javax.jms.ObjectMessage</li>
 * </ul>
 * Note that if compression is turned on then a <code>javax.jms.BytesMessage</code>
 * is sent.
 */
public class ObjectToJMSMessage extends AbstractJmsTransformer
{

    public ObjectToJMSMessage()
    {
        super();
    }

    @Override
    protected void declareInputOutputClasses()
    {
        setReturnDataType(DataTypeFactory.create(Message.class));
    }

    @Override
    public Object transformMessage(MuleEvent event, String outputEncoding) throws TransformerException
    {
        final MuleMessage message = event.getMessage();
        try
        {
            if (logger.isDebugEnabled())
            {
                logger.debug("Source object is " + ClassUtils.getSimpleName(message.getPayload().getClass()));
            }

            Object result = transformToMessage(message);

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
