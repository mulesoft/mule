/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.config.spring.internal;

import static java.util.Collections.unmodifiableList;
import static java.util.Optional.ofNullable;
import static java.util.stream.Collectors.toList;
import org.mule.runtime.api.component.ComponentIdentifier;
import org.mule.runtime.api.component.location.ConfigurationComponentLocator;
import org.mule.runtime.api.component.location.Location;
import org.mule.runtime.api.meta.AnnotatedObject;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.apache.commons.collections.map.HashedMap;

/**
 * Spring implementation of {@link ConfigurationComponentLocator}.
 * 
 * since 4.0
 */
public class SpringConfigurationComponentLocator implements ConfigurationComponentLocator {

  private Map<String, AnnotatedObject> componentsMap = new HashedMap();

  /**
   * Adds a new component to the locator.
   * 
   * @param component the component to be added
   */
  public void addComponent(AnnotatedObject component) {
    this.componentsMap.put(component.getLocation().getLocation(), component);
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public Optional<AnnotatedObject> find(Location location) {
    return ofNullable(componentsMap.get(location.toString()));
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public List<AnnotatedObject> find(ComponentIdentifier componentIdentifier) {
    return componentsMap.values().stream()
        .filter(component -> component.getLocation().getComponentIdentifier().getIdentifier().equals(componentIdentifier))
        .collect(toList());
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public List<AnnotatedObject> findAll() {
    return unmodifiableList(new ArrayList<>(componentsMap.values()));
  }
}
