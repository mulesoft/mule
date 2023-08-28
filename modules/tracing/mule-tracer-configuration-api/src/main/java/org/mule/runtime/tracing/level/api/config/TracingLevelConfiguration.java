/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 */
package org.mule.runtime.tracing.level.api.config;

import java.util.function.Consumer;

/**
 * Allows to configure the desired tracing level
 *
 * @since 4.5.0
 */
public interface TracingLevelConfiguration {

  /**
   * @return the default tracing level, MONITORING, if no other tracing level is specified from a configuration.
   */
  TracingLevel getTracingLevel();

  /**
   * If the specified location and tracing level exist, returns an override of a tracing level corresponding to a location.
   * Otherwise, returns the general tracing level if specified, or the default tracing level.
   *
   * @param location corresponds to the location of a component in a configuration.
   * @return a tracing level.
   */
  TracingLevel getTracingLevelOverride(String location);

  /**
   * Consumer to be invoked when a {@link TracingLevelConfiguration} is changed.
   *
   * @param onConfigurationChangeConsumer to execute when the configuration is changed.
   */
  void onConfigurationChange(Consumer<TracingLevelConfiguration> onConfigurationChangeConsumer);
}
