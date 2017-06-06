/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.core.api.util.func;

import static org.mule.runtime.core.api.rx.Exceptions.propagateWrappingFatal;

import java.util.function.BiPredicate;

@FunctionalInterface
public interface CheckedBiPredicate<T, U> extends BiPredicate<T, U> {

  @Override
  default boolean test(T t, U u) {
    try {
      return testChecked(t, u);
    } catch (Throwable throwable) {
      throw propagateWrappingFatal(throwable);
    }
  }

  boolean testChecked(T t, U u) throws Throwable;
}
