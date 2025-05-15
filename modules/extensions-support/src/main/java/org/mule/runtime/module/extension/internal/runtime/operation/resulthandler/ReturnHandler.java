/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.module.extension.internal.runtime.operation.resulthandler;

import org.mule.runtime.api.message.Message;
import org.mule.runtime.api.metadata.DataType;
import org.mule.runtime.api.metadata.TypedValue;
import org.mule.runtime.api.util.LazyValue;

/**
 * Handler for operation return values.
 *
 * @param <T>
 * @since 4.1
 */
public interface ReturnHandler<T> {

  LazyValue<ReturnHandler> nullHandler = new LazyValue<>(NullReturnHandler::new);

  /**
   * Creates a {@link Message.Builder from a given value}
   *
   * @param value The value to create the {@link Message.Builder} from
   * @return A Message Builder with the value.
   */
  Message.Builder toMessageBuilder(T value);

  /**
   * @return The current {@link DataType} of the handler
   */
  DataType getDataType();

  /**
   * @return A boolean indicating if the current handler supports the given value
   */
  boolean handles(Object value);

  static <T> ReturnHandler<T> nullHandler() {
    return nullHandler.get();
  }

  class NullReturnHandler<T> implements ReturnHandler<T> {

    @Override
    public Message.Builder toMessageBuilder(T value) {
      return Message.builder().payload(TypedValue.of(value));
    }

    @Override
    public DataType getDataType() {
      return DataType.OBJECT;
    }

    @Override
    public boolean handles(Object value) {
      return false;
    }
  }
}
