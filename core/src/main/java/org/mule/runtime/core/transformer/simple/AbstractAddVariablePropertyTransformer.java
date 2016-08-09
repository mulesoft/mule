/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.core.transformer.simple;

import org.mule.runtime.api.metadata.DataType;
import org.mule.runtime.core.api.MuleEvent;
import org.mule.runtime.core.api.lifecycle.InitialisationException;
import org.mule.runtime.core.api.transformer.TransformerException;
import org.mule.runtime.core.metadata.TypedValue;
import org.mule.runtime.core.transformer.AbstractMessageTransformer;
import org.mule.runtime.core.util.AttributeEvaluator;
import org.mule.runtime.core.util.StringUtils;

import java.nio.charset.Charset;
import java.text.MessageFormat;

public abstract class AbstractAddVariablePropertyTransformer<T> extends AbstractMessageTransformer {

  private AttributeEvaluator identifierEvaluator;
  private AttributeEvaluator valueEvaluator;

  public AbstractAddVariablePropertyTransformer() {
    registerSourceType(DataType.OBJECT);
    setReturnDataType(DataType.OBJECT);
  }

  @Override
  public void initialise() throws InitialisationException {
    super.initialise();
    identifierEvaluator.initialize(muleContext.getExpressionManager());
    valueEvaluator.initialize(muleContext.getExpressionManager());
  }

  @Override
  public Object transformMessage(MuleEvent event, Charset outputEncoding) throws TransformerException {
    Object keyValue = identifierEvaluator.resolveValue(event);
    String key = (keyValue == null ? null : keyValue.toString());
    if (key == null) {
      logger.error("Setting Null variable keys is not supported, this entry is being ignored");
    } else {
      TypedValue<T> typedValue = valueEvaluator.resolveTypedValue(event);
      if (typedValue.getValue() == null) {
        removeProperty(event, key);

        if (logger.isDebugEnabled()) {
          logger.debug(MessageFormat.format(
                                            "Variable with key \"{0}\", not found on message using \"{1}\". Since the value was marked optional, nothing was set on the message for this variable",
                                            key, valueEvaluator.getRawValue()));
        }
      } else {
        addProperty(event, key, typedValue.getValue(), DataType.builder().type(typedValue.getValue().getClass())
            .mediaType(getReturnDataType().getMediaType()).charset(resolveEncoding(typedValue)).build());
      }
    }
    return event.getMessage();
  }

  /**
   * Adds the property with its value and dataType to a property or variables scope.
   *
   * @param event event to which property is to be added
   * @param propertyName name of the property or variable to add
   * @param value value of the property or variable to add
   * @param dataType data type of the property or variable to add
   */
  protected abstract void addProperty(MuleEvent event, String propertyName, T value, DataType dataType);

  /**
   * Removes the property from a property or variables scope.
   *
   * @param event event to which property is to be removed
   * @param propertyName name of the property or variable to remove
   */
  protected abstract void removeProperty(MuleEvent event, String propertyName);

  @Override
  public Object clone() throws CloneNotSupportedException {
    AbstractAddVariablePropertyTransformer clone = (AbstractAddVariablePropertyTransformer) super.clone();
    clone.setIdentifier(this.identifierEvaluator.getRawValue());
    clone.setValue(this.valueEvaluator.getRawValue());
    return clone;
  }

  public void setIdentifier(String identifier) {
    if (StringUtils.isBlank(identifier)) {
      throw new IllegalArgumentException("Key cannot be blank");
    }
    this.identifierEvaluator = new AttributeEvaluator(identifier);
  }

  public void setValue(String value) {
    if (value == null) {
      throw new IllegalArgumentException("Value must not be null");
    }
    this.valueEvaluator = new AttributeEvaluator(value);
  }

}
