/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.core.api.util.func;

import static org.mule.runtime.core.api.rx.Exceptions.propagateWrappingFatal;

import java.util.function.Supplier;

/**
 * A supplier which can throw exceptions
 *
 * @param <T> the generic type of the return value
 * @since 4.0
 */
@FunctionalInterface
public interface CheckedSupplier<T> extends Supplier<T> {

  @Override
  default T get() {
    try {
      return getChecked();
    } catch (Throwable throwable) {
      return handleException(throwable);
    }
  }

  default T handleException(Throwable throwable) {
    throw propagateWrappingFatal(throwable);
  }

  T getChecked() throws Throwable;
}
