/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.core.api.config.bootstrap;

import static java.util.Collections.emptyList;
import static java.util.stream.Collectors.toList;

import org.mule.runtime.core.internal.config.bootstrap.ClassLoaderRegistryBootstrapDiscoverer;

import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Discovers {@link BootstrapService} instances corresponding to bootstrap.properties files
 */
public final class PropertiesBootstrapServiceDiscoverer implements BootstrapServiceDiscoverer {

  private final Logger logger = LoggerFactory.getLogger(getClass());
  private final ClassLoader classLoader;
  private final RegistryBootstrapDiscoverer registryBootstrapDiscoverer;

  /**
   * Creates a new instance
   *
   * @param classLoader classLoader used to search for bootstrap.properties files
   */
  public PropertiesBootstrapServiceDiscoverer(ClassLoader classLoader) {
    this(classLoader, new ClassLoaderRegistryBootstrapDiscoverer(classLoader));
  }

  public PropertiesBootstrapServiceDiscoverer(ClassLoader classLoader, RegistryBootstrapDiscoverer registryBootstrapDiscoverer) {
    this.classLoader = classLoader;
    this.registryBootstrapDiscoverer = registryBootstrapDiscoverer;
  }

  @Override
  public List<BootstrapService> discover() {
    try {
      return registryBootstrapDiscoverer.discover().stream()
          .map(discoveredProperty -> new PropertiesBootstrapService(classLoader, discoveredProperty))
          .collect(toList());
    } catch (BootstrapException e) {
      logger.error("Unable to discover bootstrap properties", e);
      return emptyList();
    }
  }
}
