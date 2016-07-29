/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.module.tooling.api.connectivity;

/**
 * An strategy for doing connectivity testing.
 *
 * Instances of {@code ConnectivityTestingStrategy} will be discovered through SPI.
 *
 * It's responsible to discover a mule component over the one connectivity testing can be done. Only
 * one mule component for connectivity testing must exists for the strategy to work.
 *
 * @since 4.0
 */
public interface ConnectivityTestingStrategy
{

    /**
     * Determines if this {@code ConnectivityTestingStrategy} must be applied over the mule configuration.
     *
     * @return true if this strategy can do connectivity testing over the components in the configuration, false otherwise.
     * throws a {@link MultipleConnectivityTestingObjectsFoundException} when the strategy founds in the context more than one connectivity testing object.
     */
    boolean connectionTestingObjectIsPresent();

    /**
     * Does test connectivity over the mule configuration.
     *
     * @return a {@code ConnectionResult} describing the test connectivity result.
     */
    ConnectionResult testConnectivity();

}
