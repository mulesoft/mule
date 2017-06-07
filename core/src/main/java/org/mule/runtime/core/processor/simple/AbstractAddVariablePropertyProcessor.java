/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.core.processor.simple;

import static java.text.MessageFormat.format;
import static java.util.Objects.requireNonNull;
import static org.mule.runtime.api.metadata.DataType.STRING;
import static org.mule.runtime.core.api.util.SystemUtils.getDefaultEncoding;

import org.mule.runtime.api.exception.MuleException;
import org.mule.runtime.api.lifecycle.InitialisationException;
import org.mule.runtime.api.message.Message;
import org.mule.runtime.api.metadata.DataType;
import org.mule.runtime.api.metadata.TypedValue;
import org.mule.runtime.core.api.Event;
import org.mule.runtime.core.api.Event.Builder;
import org.mule.runtime.core.api.util.AttributeEvaluator;
import org.mule.runtime.core.api.util.StringUtils;

import java.nio.charset.Charset;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public abstract class AbstractAddVariablePropertyProcessor<T> extends SimpleMessageProcessor {

  private static final Logger logger = LoggerFactory.getLogger(AbstractAddVariablePropertyProcessor.class);

  private AttributeEvaluator identifierEvaluator;
  private String value;
  private AttributeEvaluator valueEvaluator;
  private DataType returnType = DataType.OBJECT;

  @Override
  public void initialise() throws InitialisationException {
    identifierEvaluator.initialize(muleContext.getExpressionManager());
    valueEvaluator = new AttributeEvaluator(value, getReturnDataType());
    valueEvaluator.initialize(muleContext.getExpressionManager());
  }

  @Override
  public Event process(Event event) throws MuleException {
    String key = identifierEvaluator.resolveValue(event);
    if (key == null) {
      logger.error("Setting Null variable keys is not supported, this entry is being ignored");
      return event;
    } else {
      final Builder builder = Event.builder(event);
      TypedValue<T> typedValue = valueEvaluator.resolveTypedValue(event);
      event = builder.build();
      if (typedValue.getValue() == null) {
        if (logger.isDebugEnabled()) {
          logger.debug(format(
                              "Variable with key '{0}', not found on message using '{1}'. Since the value was marked optional, nothing was set on the message for this variable",
                              key, valueEvaluator.getRawValue()));
        }
        return removeProperty(event, key);
      } else {
        return addProperty(event, key, typedValue.getValue(), DataType.builder().type(typedValue.getDataType().getType())
            .mediaType(getReturnDataType().getMediaType()).charset(resolveEncoding(typedValue)).build());
      }
    }
  }

  protected Charset resolveEncoding(Object src) {
    return getReturnDataType().getMediaType().getCharset().orElse(getEncoding(src));
  }

  private Charset getEncoding(Object src) {
    if (src instanceof Message) {
      return ((Message) src).getPayload().getDataType().getMediaType().getCharset()
          .orElse(getDefaultEncoding(muleContext));
    } else {
      return getDefaultEncoding(muleContext);
    }
  }

  /**
   * Adds the property with its value and dataType to a property or variables scope.
   *
   * @param event event to which property is to be added
   * @param propertyName name of the property or variable to add
   * @param value value of the property or variable to add
   * @param dataType data type of the property or variable to add
   */
  protected abstract Event addProperty(Event event, String propertyName, T value, DataType dataType);

  /**
   * Removes the property from a property or variables scope.
   *
   * @param event event to which property is to be removed
   * @param propertyName name of the property or variable to remove
   */
  protected abstract Event removeProperty(Event event, String propertyName);

  public void setIdentifier(String identifier) {
    if (StringUtils.isBlank(identifier)) {
      throw new IllegalArgumentException("Key cannot be blank");
    }
    this.identifierEvaluator = new AttributeEvaluator(identifier, STRING);
  }

  public void setValue(String value) {
    requireNonNull(value);
    this.value = value;
  }

  public void setReturnDataType(DataType type) {
    this.returnType = type;
  }

  public DataType getReturnDataType() {
    return returnType;
  }
}
