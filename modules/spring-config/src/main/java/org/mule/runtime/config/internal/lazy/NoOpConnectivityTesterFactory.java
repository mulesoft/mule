/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 */
package org.mule.runtime.config.internal.lazy;

import org.mule.runtime.api.connection.ConnectionProvider;
import org.mule.runtime.api.exception.MuleException;
import org.mule.runtime.core.api.util.func.CheckedRunnable;
import org.mule.runtime.core.internal.connection.ConnectivityTester;
import org.mule.runtime.core.internal.connection.ConnectivityTesterFactory;
import org.mule.runtime.extension.api.runtime.config.ConfigurationInstance;

/**
 * Implementation of {@link ConnectivityTesterFactory} that doesn't do any connectivity testing.
 *
 * @since 4.4
 */
public class NoOpConnectivityTesterFactory implements ConnectivityTesterFactory {

  @Override
  public ConnectivityTester create(String name) {
    return new ConnectivityTester() {

      @Override
      public void testConnectivity(ConnectionProvider provider, ConfigurationInstance configurationInstance)
          throws MuleException {
        // Nothing to do
      }

      @Override
      public void withTestConnectivityLock(CheckedRunnable task) {
        task.run();
      }

    };
  }

}
