/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.runtime.core.internal.processor.simple;

import static org.mule.runtime.api.metadata.DataType.OBJECT;
import org.mule.runtime.api.exception.MuleException;
import org.mule.runtime.api.lifecycle.InitialisationException;
import org.mule.runtime.api.message.Message;
import org.mule.runtime.api.metadata.DataType;
import org.mule.runtime.api.metadata.DataTypeParamsBuilder;
import org.mule.runtime.api.metadata.TypedValue;
import org.mule.runtime.core.api.event.CoreEvent;
import org.mule.runtime.core.privileged.util.AttributeEvaluator;
import org.mule.runtime.core.privileged.processor.simple.SimpleMessageProcessor;

/**
 * Modifies the payload of a {@link Message} according to the provided value.
 */
public class SetPayloadMessageProcessor extends SimpleMessageProcessor {

  private static final TypedValue NULL_TYPED_VALUE = new TypedValue<>(null, OBJECT);
  private DataType dataType;
  private String value;
  private AttributeEvaluator valueEvaluator;

  @Override
  public CoreEvent process(CoreEvent event) throws MuleException {
    final Message.Builder builder = Message.builder(event.getMessage());
    final CoreEvent.Builder eventBuilder = CoreEvent.builder(event);

    if (dataType == null) {
      final TypedValue typedValue = resolveTypedValue(event);
      builder.value(typedValue.getValue()).mediaType(typedValue.getDataType().getMediaType());
    } else {
      Object value = resolveValue(event);
      final DataTypeParamsBuilder dataTypeBuilder =
          DataType.builder(dataType).type(value == null ? Object.class : value.getClass());
      builder.value(value).mediaType(dataTypeBuilder.build().getMediaType());
    }

    return eventBuilder.message(builder.build()).build();
  }

  private Object resolveValue(CoreEvent event) {
    Object value;
    if (valueEvaluator.getRawValue() == null) {
      value = null;
    } else {
      value = valueEvaluator.resolveValue(event);
    }
    return value;
  }

  private TypedValue resolveTypedValue(CoreEvent event) {
    if (valueEvaluator.getRawValue() == null) {
      return NULL_TYPED_VALUE;
    } else {
      return valueEvaluator.resolveTypedValue(event);
    }
  }

  public void setMimeType(String mimeType) {
    setDataType(DataType.builder(dataType == null ? OBJECT : dataType).mediaType(mimeType).build());
  }

  public void setEncoding(String encoding) {
    setDataType(DataType.builder(dataType == null ? OBJECT : dataType).charset(encoding).build());
  }

  public void setDataType(DataType dataType) {
    if (dataType.getMediaType().getCharset().isPresent()) {
      this.dataType = dataType;
    } else {
      this.dataType = DataType.builder(dataType).build();
    }
  }

  public void setValue(String value) {
    this.value = value;
  }

  @Override
  public void initialise() throws InitialisationException {
    valueEvaluator = new AttributeEvaluator(value, dataType);
    valueEvaluator.initialize(muleContext.getExpressionManager());
  }
}
