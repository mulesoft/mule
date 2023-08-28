/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 */
package org.mule.runtime.core.internal.connection;

import org.mule.runtime.api.connection.ConnectionProvider;

/**
 * Interface to be used in {@link ConnectionProvider} indicating that the marked one has a {@link ConnectionProvider} delegate;
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
