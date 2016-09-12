/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.compatibility.core.transport;

import org.mule.compatibility.core.api.transport.MessageTypeNotSupportedException;
import org.mule.compatibility.core.api.transport.MuleMessageFactory;
import org.mule.compatibility.core.message.CompatibilityMessage;
import org.mule.compatibility.core.message.MuleCompatibilityMessageBuilder;
import org.mule.runtime.api.metadata.DataType;
import org.mule.runtime.api.metadata.DataTypeParamsBuilder;
import org.mule.runtime.core.api.MuleContext;
import org.mule.runtime.core.api.InternalMessage;
import org.mule.runtime.core.util.StringUtils;

import java.nio.charset.Charset;

public abstract class AbstractMuleMessageFactory implements MuleMessageFactory {

  protected MuleContext muleContext;

  /**
   * Required by subclasses to instantiate factory through reflection.
   */
  public AbstractMuleMessageFactory() {}

  @Override
  public CompatibilityMessage create(Object transportMessage, InternalMessage previousMessage, Charset encoding)
      throws Exception {
    return doCreate(transportMessage, previousMessage, encoding);
  }

  @Override
  public CompatibilityMessage create(Object transportMessage, Charset encoding) throws Exception {
    return doCreate(transportMessage, null, encoding);
  }

  private CompatibilityMessage doCreate(Object transportMessage, InternalMessage previousMessage, Charset encoding)
      throws Exception {
    if (transportMessage == null) {
      return (CompatibilityMessage) new MuleCompatibilityMessageBuilder().nullPayload().build();
    }

    if (!isTransportMessageTypeSupported(transportMessage)) {
      throw new MessageTypeNotSupportedException(transportMessage, getClass());
    }

    Object payload = extractPayload(transportMessage, encoding);
    DataTypeParamsBuilder dataTypeBuilder =
        DataType.builder().type(payload == null ? Object.class : payload.getClass()).charset(encoding);
    String mimeType = getMimeType(transportMessage);
    if (StringUtils.isNotEmpty(mimeType)) {
      dataTypeBuilder = dataTypeBuilder.mediaType(mimeType);
    }
    final DataType dataType = dataTypeBuilder.build();
    MuleCompatibilityMessageBuilder messageBuilder;
    if (previousMessage != null) {
      messageBuilder = (MuleCompatibilityMessageBuilder) new MuleCompatibilityMessageBuilder(previousMessage).payload(payload);
    } else if (payload instanceof InternalMessage) {
      messageBuilder = new MuleCompatibilityMessageBuilder((InternalMessage) payload);
    } else if (payload == null) {
      messageBuilder = (MuleCompatibilityMessageBuilder) new MuleCompatibilityMessageBuilder().nullPayload();
    } else {
      messageBuilder = (MuleCompatibilityMessageBuilder) new MuleCompatibilityMessageBuilder().payload(payload);
    }

    messageBuilder.mediaType(dataType.getMediaType());
    addProperties(messageBuilder, transportMessage);
    addAttachments(messageBuilder, transportMessage);
    return messageBuilder.build();
  }

  protected String getMimeType(Object transportMessage) {
    return null;
  }

  protected abstract Class<?>[] getSupportedTransportMessageTypes();

  protected abstract Object extractPayload(Object transportMessage, Charset encoding) throws Exception;

  protected void addProperties(MuleCompatibilityMessageBuilder messageBuilder, Object transportMessage) throws Exception {
    // Template method
  }

  protected void addAttachments(InternalMessage.Builder messageBuilder, Object transportMessage) throws Exception {
    // Template method
  }

  private boolean isTransportMessageTypeSupported(Object transportMessage) {
    Class<?> transportMessageType = transportMessage.getClass();
    boolean match = false;
    for (Class<?> type : getSupportedTransportMessageTypes()) {
      if (type.isAssignableFrom(transportMessageType)) {
        match = true;
        break;
      }
    }
    return match;
  }
}
