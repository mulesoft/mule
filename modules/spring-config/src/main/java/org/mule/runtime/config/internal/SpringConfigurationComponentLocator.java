/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.config.internal;

import static java.util.Collections.unmodifiableList;
import static java.util.Optional.empty;
import static java.util.Optional.ofNullable;
import static java.util.stream.Collectors.toList;

import org.mule.runtime.api.component.Component;
import org.mule.runtime.api.component.ComponentIdentifier;
import org.mule.runtime.api.component.location.ComponentLocation;
import org.mule.runtime.api.component.location.ConfigurationComponentLocator;
import org.mule.runtime.api.component.location.Location;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.function.Function;

/**
 * Spring implementation of {@link ConfigurationComponentLocator}.
 *
 * since 4.0
 */
public class SpringConfigurationComponentLocator implements ConfigurationComponentLocator {

  private final Function<String, Boolean> isTemplateLocationFunction;
  private final Map<String, Component> componentsMap = new HashMap<>();
  private final Set<ComponentLocation> componentLocations = new HashSet<>();

  public SpringConfigurationComponentLocator(Function<String, Boolean> isTemplateComponentFunction) {
    this.isTemplateLocationFunction = isTemplateComponentFunction;
  }

  /**
   * Adds a new component to the locator.
   *
   * @param component the component to be added
   */
  public void addComponent(Component component) {
    this.componentsMap.put(component.getLocation().getLocation(), component);
  }

  /**
   * Adds a new {@link ComponentLocation} to the locator.
   * <p>
   * This method is used in addition to {@link #addComponent(Component)} when the parser knows a certain location exists but the
   * component in that location is not available (i.e.: is lazy)
   *
   * @param location the component to be added
   */
  public void addComponentLocation(ComponentLocation location) {
    this.componentLocations.add(location);
  }

  /**
   * Removes a component from the locator
   *
   * @param location the location of the component to be removed
   */
  public void removeComponent(Location location) {
    this.componentsMap.remove(location.toString());
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public Optional<Component> find(Location location) {
    if (isTemplateLocationFunction.apply(location.getGlobalName())) {
      return empty();
    }
    return ofNullable(componentsMap.get(location.toString()));
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public List<Component> find(ComponentIdentifier componentIdentifier) {
    return componentsMap.values().stream()
        .filter(component -> component.getLocation().getComponentIdentifier().getIdentifier().equals(componentIdentifier))
        .collect(toList());
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public List<ComponentLocation> findAllLocations() {
    return unmodifiableList(new ArrayList<>(componentLocations));
  }
}
