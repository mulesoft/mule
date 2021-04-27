/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.core.internal.connection;

/**
 * Provides a way of creating {@link ConnectivityTester}s for a connection.
 *
 * @since 4.4
 */
public interface ConnectivityTesterFactory {

  /**
   * Created a new {@link ConnectivityTester} for the connection with the given {@code connectionName}.
   *
   * @param connectionName the name of the connection under test.
   * @return a new {@link ConnectivityTester}
   */
  ConnectivityTester create(String connectionName);

}
