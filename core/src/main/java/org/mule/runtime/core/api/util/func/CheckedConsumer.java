/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.core.api.util.func;

import static org.mule.runtime.core.api.rx.Exceptions.propagateWrappingFatal;

import java.util.function.Consumer;

@FunctionalInterface
public interface CheckedConsumer<T> extends Consumer<T> {

  @Override
  default void accept(T t) {
    try {
      acceptChecked(t);
    } catch (Throwable throwable) {
      throw propagateWrappingFatal(throwable);
    }
  }

  void acceptChecked(T t) throws Throwable;
}
