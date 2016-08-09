/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.module.tooling.api.connectivity;

import org.mule.runtime.api.connection.ConnectionValidationResult;

/**
 * An strategy for doing connectivity testing.
 *
 * Instances of {@code ConnectivityTestingStrategy} will be discovered through SPI.
 *
 * It's responsible to discover a mule component over the one connectivity testing can be done. Only one mule component for
 * connectivity testing must exists for the strategy to work.
 *
 * @since 4.0
 */
public interface ConnectivityTestingStrategy {

  /**
   * Does test connectivity over the provided mule component.
   *
   * @return a {@code ConnectionValidationResult} describing the test connectivity result.
   * @param connectivityTestingObject object over the one connectivity testing must be done
   */
  ConnectionValidationResult testConnectivity(Object connectivityTestingObject);

  /**
   * Determines if this {@code ConnectivityTestingStrategy} must be applied over the provided object.
   *
   * @param connectivityTestingObject object over the one connectivity testing must be done
   * @return true if this strategy can do connectivity testing over the provided component, false otherwise.
   */
  boolean accepts(Object connectivityTestingObject);
}
