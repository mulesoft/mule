/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.runtime.core.internal.config.bootstrap;

import org.mule.runtime.core.api.config.bootstrap.BootstrapService;
import org.mule.runtime.core.api.config.bootstrap.BootstrapServiceDiscoverer;
import org.mule.runtime.core.api.config.bootstrap.PropertiesBootstrapService;

import java.util.Collections;
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
    return Collections.singletonList(new PropertiesBootstrapService(this.getClass().getClassLoader(), properties));
  }
}
