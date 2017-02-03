/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.module.extension.internal.runtime.resolver;

import org.mule.runtime.api.exception.MuleException;
import org.mule.runtime.core.api.Event;

import java.util.Optional;

/**
 * Provides a value which is dependant on a {@link Event}
 *
 * @since 3.7.0
 */
public interface ValueResolver<T> {

  /**
   * Resolves a value from the given {@code event}
   *
   * @param event a {@link Event}
   * @return a resolved value
   * @throws MuleException if the resolution of the value fails
   */
  T resolve(Event event) throws MuleException;

  /**
   * returns {@code false} if subsequent invocations to {@link #resolve(Event)} will return the same value. Notice that if it
   * returns {@code true}, then it might return different values per invocation but that's not guaranteed.
   *
   * @return whether this resolve is dynamic
   */
  boolean isDynamic();

  /**
   * @return the {@link ResolverSet} that will be used to resolve the values from a given event if there is any.
   */
  default Optional<ResolverSet> getResolverSet() {
    return Optional.empty();
  }

}
