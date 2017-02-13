/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.core.internal.locator;

import org.mule.runtime.api.component.location.ComponentLocation;
import org.mule.runtime.core.api.locator.ConfigurationComponentLocator;
import org.mule.runtime.core.api.locator.Location;

import java.util.Map;
import java.util.Optional;

import org.apache.commons.collections.map.HashedMap;

/**
 * Default implementation of {@link ConfigurationComponentLocator}.
 * 
 * An instance of {@link DefaultConfigurationComponentLocator} is created with all the components and it's location injected via
 * the {@code {@link #setComponentMap(Map)}} method.
 *
 * @since 4.0
 */
public class DefaultConfigurationComponentLocator implements ConfigurationComponentLocator {

  private Map<ComponentLocation, Object> componentMap = new HashedMap();

  public Optional<Object> find(Location location) {
    return componentMap.entrySet().stream()
        .filter(entry -> entry.getKey().getLocation().equals(location.toString()))
        .map(entry -> entry.getValue())
        .findAny();
  }

  public void setComponentMap(Map<ComponentLocation, Object> componentMap) {
    this.componentMap = componentMap;
  }
}
