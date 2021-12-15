/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.core.internal.registry;

import org.mule.runtime.core.api.Injector;

/**
 * Contract for a decorator which is wrapping an instance on which values are to be injected, either through dependency injection,
 * parameter resolution, etc.
 *
 * Because the decorator class can hide the injection targets during introspection, implementing this interface will allow the
 * {@link Injector} (or whichever component is performing the introspection) to obtain the actual instance in which values are to
 * be injected. Said component must recursively invoke the {@link #getDelegate()} method until the actual target is found.
 *
 * @param <T> the delegate generic type
 * @since 4.5.0
 */
public interface InjectionTargetDecorator<T> {

  /**
   * @return the decorated instance
   */
  T getDelegate();
}
