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
 * Provides a value which is independent of any {@link CoreEvent}
 *
 * @since 4.5.0
 */
public interface EventAgnosticValueResolver<T> extends ValueResolver<T> {

  /**
   * Resolves a value without the need of a resolving context.
   *
   * @return a resolved value
   * @throws MuleException if the resolution of the value fails
   */
  T resolve() throws MuleException;

  @Override
  default T resolve(ValueResolvingContext context) throws MuleException {
    return resolve();
  }
}
