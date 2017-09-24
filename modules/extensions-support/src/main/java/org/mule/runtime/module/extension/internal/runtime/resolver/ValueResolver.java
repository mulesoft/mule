/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.module.extension.internal.runtime.resolver;

import org.mule.runtime.api.exception.MuleException;
import org.mule.runtime.core.api.event.CoreEvent;

/**
 * Provides a value which is dependant on a {@link CoreEvent}
 *
 * @since 3.7.0
 */
public interface ValueResolver<T> {

  /**
   * Resolves a value from the given {@code event}
   *
   * @param context the {@link ValueResolvingContext context} for the current resolution attempt
   * @return a resolved value
   * @throws MuleException if the resolution of the value fails
   */
  T resolve(ValueResolvingContext context) throws MuleException;

  /**
   * Returns {@code false} if subsequent invocations to {@link #resolve(ValueResolvingContext)} will return the same value.
   * Notice that if it returns {@code true}, then it might return different values per invocation but that's not guaranteed.
   *
   * @return whether the resolved value changes based or the resolution context or not
   */
  boolean isDynamic();
}
