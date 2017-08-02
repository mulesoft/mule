/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
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
