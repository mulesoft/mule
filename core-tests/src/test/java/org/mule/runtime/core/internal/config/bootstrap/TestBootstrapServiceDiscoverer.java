/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 */
package org.mule.runtime.core.internal.config.bootstrap;

import static java.util.Collections.singletonList;

import org.mule.runtime.core.api.config.bootstrap.BootstrapService;
import org.mule.runtime.core.api.config.bootstrap.BootstrapServiceDiscoverer;
import org.mule.runtime.core.api.config.bootstrap.PropertiesBootstrapService;

import java.util.List;
import java.util.Properties;

/**
 * Defines a BootstrapServiceDiscoverer useful for testing purposes
 */
public class TestBootstrapServiceDiscoverer implements BootstrapServiceDiscoverer {

  private final Properties properties;

  /**
   * Creates a new instance that will discover only a {@link BootstrapService}
   *
   * @param properties properties that will be returned by the discovered {@link BootstrapService}
   */
  public TestBootstrapServiceDiscoverer(Properties properties) {
    this.properties = properties;
  }

  @Override
  public List<BootstrapService> discover() {
    return singletonList(new PropertiesBootstrapService(this.getClass().getClassLoader(), properties));
  }
}
