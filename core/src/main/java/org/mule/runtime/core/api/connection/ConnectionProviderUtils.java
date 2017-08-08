/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.runtime.core.api.connection;

import org.mule.runtime.api.connection.ConnectionProvider;
import org.mule.runtime.core.internal.connection.ConnectionProviderWrapper;

/**
 * Provides utilities to work with {@link ConnectionProvider}
 *
 * @since 4.0
 */
public class ConnectionProviderUtils {

  private ConnectionProviderUtils() {}

  /**
   * Unwraps a given connection provider if necessary
   *
   * @param connectionProvider connection provider to unwrap
   * @return the wrapped instance when {@code connectionProvider} is a {@link ConnectionProviderWrapper}, the same {@code connectionProvider} otherwise.
   */
  public static ConnectionProvider unwrapProviderWrapper(ConnectionProvider connectionProvider) {
    return connectionProvider instanceof ConnectionProviderWrapper
        ? unwrapProviderWrapper(((ConnectionProviderWrapper) connectionProvider).getDelegate())
        : connectionProvider;
  }
}
