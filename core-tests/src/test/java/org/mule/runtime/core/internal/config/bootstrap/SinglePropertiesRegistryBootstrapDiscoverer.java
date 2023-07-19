/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 */
package org.mule.runtime.core.internal.config.bootstrap;

import org.mule.runtime.core.api.config.bootstrap.RegistryBootstrapDiscoverer;

import java.util.Arrays;
import java.util.List;
import java.util.Properties;

/**
 * Allows to configure a SimpleRegistryBootstrap using a single Properties object.
 */
public class SinglePropertiesRegistryBootstrapDiscoverer implements RegistryBootstrapDiscoverer {

  private final Properties properties;

  public SinglePropertiesRegistryBootstrapDiscoverer(Properties properties) {
    this.properties = properties;
  }

  @Override
  public List<Properties> discover() {
    return Arrays.asList(properties);
  }
}
