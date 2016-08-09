/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.compatibility.transport.jms.transformers;

import org.mule.runtime.api.metadata.DataType;
import org.mule.runtime.core.api.MuleEvent;
import org.mule.runtime.core.api.MuleMessage;
import org.mule.runtime.core.api.transformer.TransformerException;
import org.mule.runtime.core.util.ClassUtils;

import java.nio.charset.Charset;

import javax.jms.BytesMessage;
import javax.jms.MapMessage;
import javax.jms.Message;
import javax.jms.ObjectMessage;
import javax.jms.StreamMessage;
import javax.jms.TextMessage;

/**
 * <code>JMSMessageToObject</code> Will convert a <code>javax.jms.Message</code> or sub-type into an object by extracting the
 * message payload. Users of this transformer can set different return types on the transform to control the way it behaves.
 * <ul>
 * <li>javax.jms.TextMessage - java.lang.String</li>
 * <li>javax.jms.ObjectMessage - java.lang.Object</li>
 * <li>javax.jms.BytesMessage - Byte[]. Note that the transformer will check if the payload is compressed and automatically
 * uncompress the message.</li>
 * <li>javax.jms.MapMessage - java.util.Map</li>
 * <li>javax.jms.StreamMessage - java.util.Vector of objects from the Stream Message.</li>
 * </ul>
 */
public class JMSMessageToObject extends AbstractJmsTransformer {

  public JMSMessageToObject() {
    super();
  }

  @Override
  protected void declareInputOutputClasses() {
    registerSourceType(DataType.fromType(Message.class));
    registerSourceType(DataType.fromType(TextMessage.class));
    registerSourceType(DataType.fromType(ObjectMessage.class));
    registerSourceType(DataType.fromType(BytesMessage.class));
    registerSourceType(DataType.fromType(MapMessage.class));
    registerSourceType(DataType.fromType(StreamMessage.class));
  }

  @Override
  public Object transformMessage(MuleEvent event, Charset outputEncoding) throws TransformerException {
    final MuleMessage message = event.getMessage();
    try {
      if (logger.isDebugEnabled()) {
        logger.debug("Source object is " + ClassUtils.getSimpleName(message.getDataType().getType()));
      }

      Object result = transformFromMessage((Message) message.getPayload(), outputEncoding);

      // We need to handle String / byte[] explicitly since this transformer does not define
      // a single return type
      if (byte[].class.isAssignableFrom(getReturnDataType().getType()) && result instanceof String) {
        result = result.toString().getBytes(outputEncoding);
      } else if (String.class.isAssignableFrom(getReturnDataType().getType()) && result instanceof byte[]) {
        result = new String((byte[]) result, outputEncoding);
      }


      if (logger.isDebugEnabled()) {
        logger.debug("Resulting object is " + ClassUtils.getSimpleName(result.getClass()));
      }

      return result;
    } catch (Exception e) {
      throw new TransformerException(this, e);
    }
  }
}
