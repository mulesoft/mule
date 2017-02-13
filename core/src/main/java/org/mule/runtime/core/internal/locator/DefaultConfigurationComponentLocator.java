/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.core.internal.locator;

import static java.util.Optional.empty;
import static java.util.Optional.of;
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
    for (ComponentLocation componentLocation : componentMap.keySet()) {
      if (componentLocation.getLocation().equals(location.toString())) {
        return of(componentMap.get(componentLocation));
      }
    }
    return empty();
  }

  public void setComponentMap(Map<ComponentLocation, Object> componentMap) {
    this.componentMap = componentMap;
  }
}
