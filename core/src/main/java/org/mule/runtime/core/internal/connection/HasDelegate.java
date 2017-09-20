/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.core.internal.connection;

import org.mule.runtime.api.connection.ConnectionProvider;

/**
 * Interface to be used in {@link ConnectionProvider} indicating that the marked one has a {@link ConnectionProvider}
 * delegate;
 *
 * @since 4.0
 */
@FunctionalInterface
public interface HasDelegate<C> {

  /**
   * @return The delegate {@link ConnectionProvider} which the marked {@link ConnectionProvider} is backed on.
   */
  ConnectionProvider<C> getDelegate();
}
