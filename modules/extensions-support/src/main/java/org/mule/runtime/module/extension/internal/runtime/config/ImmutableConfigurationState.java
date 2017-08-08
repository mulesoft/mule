/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.module.extension.internal.runtime.config;

import static java.util.Collections.unmodifiableMap;
import org.mule.runtime.extension.api.runtime.config.ConfigurationState;

import java.util.Map;

public class ImmutableConfigurationState implements ConfigurationState {

  private final Map<String, Object> configParameters;
  private final Map<String, Object> connectionParameters;

  public ImmutableConfigurationState(Map<String, Object> configParameters, Map<String, Object> connectionParameters) {
    this.configParameters = unmodifiableMap(configParameters);
    this.connectionParameters = unmodifiableMap(connectionParameters);
  }

  @Override
  public Map<String, Object> getConfigParameters() {
    return configParameters;
  }

  @Override
  public Map<String, Object> getConnectionParameters() {
    return connectionParameters;
  }
}
