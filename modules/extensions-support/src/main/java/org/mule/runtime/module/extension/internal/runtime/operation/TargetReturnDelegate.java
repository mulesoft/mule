/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.module.extension.internal.runtime.operation;

import static org.mule.runtime.api.meta.TargetType.PAYLOAD;
import org.mule.runtime.api.message.Message;
import org.mule.runtime.api.meta.TargetType;
import org.mule.runtime.api.meta.model.ComponentModel;
import org.mule.runtime.core.api.Event;
import org.mule.runtime.core.api.MuleContext;
import org.mule.runtime.core.api.streaming.CursorProviderFactory;
import org.mule.runtime.extension.api.runtime.operation.Result;
import org.mule.runtime.module.extension.internal.runtime.ExecutionContextAdapter;

/**
 * An implementation of {@link AbstractReturnDelegate} which sets the output message on a variable which key is taken from the
 * {@link #target} field.
 * <p>
 * The target variable will always contain a {@link Message}, even if the operation returned a simple value
 * <p>
 * The original message is left untouched.
 *
 * @since 4.0
 */
final class TargetReturnDelegate extends AbstractReturnDelegate {

  private final String target;
  private final TargetType targetType;

  /**
   * {@inheritDoc}
   *
   * @param target the name of the variable in which the output message will be set
   */
  TargetReturnDelegate(String target,
                       TargetType targetType,
                       ComponentModel componentModel,
                       CursorProviderFactory cursorProviderFactory,
                       MuleContext muleContext) {
    super(componentModel, cursorProviderFactory, muleContext);
    this.target = target;
    this.targetType = targetType;
  }

  @Override
  public Event asReturnValue(Object value, ExecutionContextAdapter operationContext) {
    return Event.builder(operationContext.getEvent())
        .addVariable(target, getTargetValue(value, operationContext))
        .build();
  }

  private Object getTargetValue(Object value, ExecutionContextAdapter operationContext) {
    return targetType == PAYLOAD ? unwrapResultIfNecessary(value) : toMessage(value, operationContext);
  }

  private Object unwrapResultIfNecessary(Object value) {
    if (value instanceof Result) {
      Result result = (Result) value;
      value = result.getOutput();
    }

    return value;
  }
}
