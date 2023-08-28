/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 */
package org.mule.runtime.core.internal.connection;

/**
 * Provides a way of creating {@link ConnectivityTester}s for a connection.
 *
 * @since 4.4
 */
public interface ConnectivityTesterFactory {

  /**
   * Creates a new {@link ConnectivityTester} for the connection with the given {@code connectionName}.
   *
   * @param connectionName the name of the connection under test.
   * @return a new {@link ConnectivityTester}
   */
  ConnectivityTester create(String connectionName);

}
