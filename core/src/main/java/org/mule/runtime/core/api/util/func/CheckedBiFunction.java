/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.core.api.util.func;

import static org.mule.runtime.core.api.rx.Exceptions.propagateWrappingFatal;

import java.util.function.BiFunction;

@FunctionalInterface
public interface CheckedBiFunction<T, U, R> extends BiFunction<T, U, R> {

  @Override
  default R apply(T t, U u) {
    try {
      return applyChecked(t, u);
    } catch (Throwable throwable) {
      throw propagateWrappingFatal(throwable);
    }
  }

  R applyChecked(T t, U u) throws Throwable;
}
