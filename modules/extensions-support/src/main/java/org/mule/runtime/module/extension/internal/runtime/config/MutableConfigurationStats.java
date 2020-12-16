/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.module.extension.internal.runtime.config;

import org.mule.runtime.extension.api.runtime.config.ConfigurationStats;

/**
 * A specialization of {@link ConfigurationStats} which adds behavior to mutate the state of {@code this} instance
 *
 * @since 4.0
 */
public interface MutableConfigurationStats extends ConfigurationStats {

  /**
   * Updates the value of {@link #getLastUsedMillis()}
   *
   * @return the new value
   */
  long updateLastUsed();

  /**
   * Increments the return value of {@link #getInflightOperations()} by one
   *
   * @return the new value
   *
   * @deprecated Use {@link #addActiveComponent()} instead.
   */
  @Deprecated
  int addInflightOperation();

  /**
   * Decrements the return value of {@link #getInflightOperations()} by one
   *
   * @return the new value
   *
   * @deprecated Use {@link #discountActiveComponent()} instead.
   */
  @Deprecated
  int discountInflightOperation();

  /**
   * Increments the return value of {@link #getRunningSources()} by one
   *
   * @return the new value
   * @since 4.1.6 4.2.2 4.3.0
   *
   * @deprecated Use {@link #addActiveComponent()} instead.
   */
  @Deprecated
  int addRunningSource();

  /**
   * Decrements the return value of {@link #getRunningSources()} by one
   *
   * @return the new value
   * @since 4.1.6 4.2.2 4.3.0
   *
   * @deprecated Use {@link #discountActiveComponent()} instead.
   */
  @Deprecated
  int discountRunningSource();

  /**
   * Increments the return value of {@link #getActiveComponents()} by one
   *
   * @return the new value
   * @since 4.2.3 4.3.1 4.4.0
   */
  int addActiveComponent();

  /**
   * Decrements the return value of {@link #getActiveComponents()} by one
   *
   * @return the new value
   * @since 4.2.3 4.3.1 4.4.0
   */
  int discountActiveComponent();
}
