/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.runtime.core.api.connection.util;

import org.mule.runtime.api.connection.ConnectionProvider;
import org.mule.runtime.core.internal.connection.ConnectionProviderWrapper;
import org.mule.runtime.core.internal.connection.HasDelegate;

/**
 * Provides utilities to work with {@link ConnectionProvider}
 *
 * @since 4.0
 */
public final class ConnectionProviderUtils {

  private ConnectionProviderUtils() {}

  /**
   * Recursively unwraps a given connection provider if necessary
   *
   * @param connectionProvider connection provider to unwrap
   * @return the wrapped instance when {@code connectionProvider} is a {@link ConnectionProviderWrapper}, the same {@code connectionProvider} otherwise.
   */
  public static ConnectionProvider unwrapProviderWrapper(ConnectionProvider connectionProvider) {
    return unwrapProviderWrapper(connectionProvider, null);
  }

  /**
   * Unwraps a given connection provider if necessary.
   * <p>
   * If {@code stopClass} is not {@code null} and the unwrapped value is an instance of such, recursion is stopped
   * and the value returned, even if such value is actually a {@link ConnectionProviderWrapper}
   *
   * @param connectionProvider connection provider to unwrap
   * @param stopClass          optional stop condition
   * @return the wrapped instance when {@code connectionProvider} is a {@link ConnectionProviderWrapper}, the same {@code connectionProvider} otherwise.
   */
  public static ConnectionProvider unwrapProviderWrapper(ConnectionProvider connectionProvider,
                                                         Class<? extends ConnectionProvider> stopClass) {
    if (connectionProvider instanceof HasDelegate) {
      if (stopClass == null || !stopClass.isInstance(connectionProvider)) {
        return unwrapProviderWrapper(((HasDelegate) connectionProvider).getDelegate(), stopClass);
      }
    }
    return connectionProvider;
  }
}
