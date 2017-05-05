/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.core.util.func;

import static org.mule.runtime.core.api.rx.Exceptions.propagateWrappingFatal;

import java.util.function.Function;

@FunctionalInterface
public interface CheckedFunction<T, R> extends Function<T, R> {

  @Override
  default R apply(T t) {
    try {
      return applyChecked(t);
    } catch (Throwable throwable) {
      return handleException(throwable);
    }
  }

  default R handleException(Throwable throwable) {
    throw propagateWrappingFatal(throwable);
  }

  R applyChecked(T t) throws Throwable;
}
