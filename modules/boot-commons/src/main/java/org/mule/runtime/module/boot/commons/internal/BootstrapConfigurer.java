/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.module.boot.commons.internal;

import org.mule.runtime.module.boot.api.MuleContainer;

/**
 * Allows for executing bootstrapping code before the creation of the {@link MuleContainer}.
 * <p>
 * The configuration does not need to be completed inside {@link #configure()}, it can proceed without blocking. In that case, the
 * creation of the {@link MuleContainer} may take place, and part of its startup too. It is guaranteed that the
 * {@link MuleContainer} will not show its splash screen until all configurations are completed.
 * <p>
 * The {@link #await()} method must block until the configuration is completed. By default, it returns immediately, assuming the
 * configuration was synchronous.
 *
 * @since 4.5
 */
public interface BootstrapConfigurer {

  /**
   * Performs some configuration task as part of the bootstrapping before the {@link MuleContainer} is created.
   * <p>
   * The configuration may continue in a non-blocking fashion. See: {@link #await()}.
   *
   * @return Whether the bootstrapping process should proceed. This is used to communicate that the configuration completed
   *         normally, but, based on some parameterization, it is expected to stop the bootstrapping.
   * @throws BootstrapConfigurationException If there was any anomaly during the configuration and the bootstrapping process must
   *                                         stop.
   */
  boolean configure() throws BootstrapConfigurationException;

  /**
   * Blocks until all the configuration work started by {@link #configure()} is completed.
   *
   * @throws BootstrapConfigurationException If the bootstrapping process must stop.
   */
  default void await() throws BootstrapConfigurationException {}

  /**
   * Allows for disposal of resources associated with the configurer.
   */
  default void dispose() {}
}
