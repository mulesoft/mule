/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.config.internal.lazy;

import org.mule.runtime.api.connection.ConnectionProvider;
import org.mule.runtime.api.exception.MuleException;
import org.mule.runtime.core.api.util.func.CheckedRunnable;
import org.mule.runtime.core.internal.connection.ConnectivityTester;
import org.mule.runtime.core.internal.connection.ConnectivityTesterFactory;
import org.mule.runtime.extension.api.runtime.config.ConfigurationInstance;

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
