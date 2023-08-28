/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 */
package org.mule.runtime.module.extension.internal.runtime.config;

import static java.util.Collections.unmodifiableMap;
import org.mule.runtime.extension.api.runtime.config.ConfigurationState;

import java.util.Map;

/**
 * Immutable implementation of {@link ConfigurationState}
 *
 * @since 4.0
 */
public class ImmutableConfigurationState implements ConfigurationState {

  private final Map<String, Object> configParameters;
  private final Map<String, Object> connectionParameters;

  /**
   * Creates a new instance
   *
   * @param configParameters     the config parameters
   * @param connectionParameters the connection parameters
   */
  public ImmutableConfigurationState(Map<String, Object> configParameters, Map<String, Object> connectionParameters) {
    this.configParameters = unmodifiableMap(configParameters);
    this.connectionParameters = unmodifiableMap(connectionParameters);
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public Map<String, Object> getConfigParameters() {
    return configParameters;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public Map<String, Object> getConnectionParameters() {
    return connectionParameters;
  }
}
