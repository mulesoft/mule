/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.core.internal.connection;

import org.mule.runtime.api.connection.ConnectionProvider;
import org.mule.runtime.api.exception.MuleException;
import org.mule.runtime.core.api.util.func.CheckedRunnable;
import org.mule.runtime.extension.api.runtime.config.ConfigurationInstance;

/**
 * Tests connectivity for the given {@code configurationInstance}.
 * <p>
 * The {@code connectionProvider} is expected to be fully initialised and functional. It is not required for the config wrapped by
 * the {@code configurationInstance} to have been registered through the {@link #bind(Object, ConnectionProvider)} method.
 * However, if it has been, then the test will be performed using the resources allocated by such registration.
 *
 * @since 4.4
 */
public interface ConnectivityTester {

  /**
   * Tests connectivity for the given {@code configurationInstance}.
   * <p>
   * The {@code connectionProvider} is expected to be fully initialised and functional. It is not required for the config wrapped
   * by the {@code configurationInstance} to have been registered through the {@link #bind(Object, ConnectionProvider)} method.
   * However, if it has been, then the test will be performed using the resources allocated by such registration.
   */
  void testConnectivity(ConnectionProvider provider, ConfigurationInstance configurationInstance) throws MuleException;

  /**
   * Runs the provided task as soon as any pending connectivity testing is finished, or immediately if there is not any.
   *
   * @param task
   */
  void withTestConnectivityLock(CheckedRunnable task);

}
