/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.config.internal.context.lazy;

import static org.mule.runtime.api.util.Preconditions.checkState;

import static java.util.Collections.singleton;
import static java.util.Optional.empty;
import static java.util.Optional.of;
import static java.util.stream.Collectors.toSet;

import org.mule.runtime.api.component.location.Location;
import org.mule.runtime.ast.api.ArtifactAst;
import org.mule.runtime.ast.api.ComponentAst;
import org.mule.runtime.config.api.LazyComponentInitializer;

import java.util.Optional;
import java.util.Set;
import java.util.function.Predicate;

class ComponentInitializationRequest {

  private final Optional<Location> location;
  private final Predicate<ComponentAst> componentFilter;
  private final boolean isApplyStartPhaseRequested;
  private final boolean isKeepPrevious;

  private Set<String> requestedLocations;

  public ComponentInitializationRequest(Location location, boolean applyStartPhase, boolean keepPrevious) {
    this.location = of(location);
    this.componentFilter = buildFilterFromLocation(location);
    this.isApplyStartPhaseRequested = applyStartPhase;
    this.isKeepPrevious = keepPrevious;
  }

  public ComponentInitializationRequest(Predicate<ComponentAst> componentFilter, boolean applyStartPhase, boolean keepPrevious) {
    this.isKeepPrevious = keepPrevious;
    this.location = empty();
    this.componentFilter = componentFilter;
    this.isApplyStartPhaseRequested = applyStartPhase;
  }

  public ComponentInitializationRequest(LazyComponentInitializer.ComponentLocationFilter componentLocationFilter,
                                        boolean applyStartPhase, boolean keepPrevious) {
    this.isKeepPrevious = keepPrevious;
    this.location = empty();
    this.componentFilter = buildFilterFromComponentLocationFilter(componentLocationFilter);
    this.isApplyStartPhaseRequested = applyStartPhase;
  }

  public Optional<Location> getLocation() {
    return location;
  }

  public Predicate<ComponentAst> getComponentFilter() {
    return componentFilter;
  }

  public boolean isApplyStartPhaseRequested() {
    return isApplyStartPhaseRequested;
  }

  public boolean isKeepPreviousRequested() {
    return isKeepPrevious;
  }

  public void computeRequestedLocations(ArtifactAst fullApplicationModel) {
    // TODO: think if we can avoid doing it like this. Maybe we can afford doing it in the constructor.
    requestedLocations = location.map(location -> singleton(location.toString()))
        .orElseGet(() -> fullApplicationModel
            .filteredComponents(componentFilter)
            .map(comp -> comp.getLocation().getLocation())
            .collect(toSet()));
  }

  public Set<String> getRequestedLocations() {
    checkState(requestedLocations != null, "The requested locations have not been computed yet");
    return requestedLocations;
  }

  private Predicate<ComponentAst> buildFilterFromComponentLocationFilter(
                                                                         LazyComponentInitializer.ComponentLocationFilter locationFilter) {
    return componentModel -> {
      if (componentModel.getLocation() != null) {
        return locationFilter.accept(componentModel.getLocation());
      }
      return false;
    };
  }

  private Predicate<ComponentAst> buildFilterFromLocation(Location location) {
    return comp -> comp.getLocation() != null
        && comp.getLocation().getLocation().equals(location.toString());
  }
}
