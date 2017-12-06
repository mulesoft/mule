/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.module.extension.internal.runtime.operation.resulthandler;

import org.mule.runtime.api.message.Message;
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
   * @param value The value to create the {@link Message.Builder} from
   * @return A Message Builder with the value.
   */
  Message.Builder toMessageBuilder(T value);

  static <T> ReturnHandler<T> nullHandler() {
    return nullHandler.get();
  }

  class NullReturnHandler<T> implements ReturnHandler<T> {

    @Override
    public Message.Builder toMessageBuilder(T value) {
      return Message.builder().payload(TypedValue.of(value));
    }
  }
}
