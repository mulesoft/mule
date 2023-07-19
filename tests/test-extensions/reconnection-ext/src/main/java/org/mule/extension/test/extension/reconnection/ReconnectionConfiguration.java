/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 */
package org.mule.extension.test.extension.reconnection;

import org.mule.runtime.extension.api.annotation.Configuration;
import org.mule.runtime.extension.api.annotation.Operations;
import org.mule.runtime.extension.api.annotation.Sources;
import org.mule.runtime.extension.api.annotation.param.Parameter;
import org.mule.sdk.api.annotation.connectivity.ConnectionProviders;

/**
 * This class represents an extension configuration, values set in this class are commonly used across multiple operations since
 * they represent something core from the extension.
 */
@ConnectionProviders(ReconnectableConnectionProvider.class)
@Operations(ReconnectionOperations.class)
@Sources({ReconectionSource.class})
@org.mule.sdk.api.annotation.Sources({FallibleReconnectableSource.class, NonReconnectableSource.class})
@Configuration(name = "config")
public class ReconnectionConfiguration {

  @Parameter
  private String configId;

  public String getConfigId() {
    return configId;
  }
}
