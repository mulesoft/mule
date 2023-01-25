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

import org.mule.runtime.api.component.location.ComponentLocation;
import org.mule.runtime.api.component.location.Location;
import org.mule.runtime.ast.api.ArtifactAst;
import org.mule.runtime.ast.api.ComponentAst;
import org.mule.runtime.config.api.LazyComponentInitializer;
import org.mule.runtime.config.api.LazyComponentInitializer.ComponentLocationFilter;

import java.util.Optional;
import java.util.Set;
import java.util.function.Predicate;

/**
 * Represents a component initialization request, grouping together all its parameters and adapting for the different ways they
 * can be expressed.
 */
class ComponentInitializationRequest {

  private final Optional<Location> location;
  private final Predicate<ComponentAst> componentFilter;
  private final boolean isApplyStartPhaseRequested;
  private final boolean isKeepPrevious;

  private Set<String> requestedLocations;

  /**
   * Creates a request from the given parameters.
   *
   * @param location        A single component {@link Location} to initialize.
   * @param applyStartPhase Whether to also apply the start lifecycle phase to the requested components.
   * @param keepPrevious    Whether previously initialized components should be kept unchanged.
   */
  public ComponentInitializationRequest(Location location, boolean applyStartPhase, boolean keepPrevious) {
    this.location = of(location);
    this.componentFilter = buildFilterFromLocation(location);
    this.isApplyStartPhaseRequested = applyStartPhase;
    this.isKeepPrevious = keepPrevious;
  }

  /**
   * Creates a request from the given parameters.
   *
   * @param componentFilter A generic {@link Predicate} to filter {@link ComponentAst}s to initialize.
   * @param applyStartPhase Whether to also apply the start lifecycle phase to the requested components.
   * @param keepPrevious    Whether previously initialized components should be kept unchanged.
   */
  public ComponentInitializationRequest(Predicate<ComponentAst> componentFilter, boolean applyStartPhase, boolean keepPrevious) {
    this.isKeepPrevious = keepPrevious;
    this.location = empty();
    this.componentFilter = componentFilter;
    this.isApplyStartPhaseRequested = applyStartPhase;
  }

  /**
   * Creates a request from the given parameters.
   *
   * @param componentLocationFilter A {@link ComponentLocationFilter} to filter components explicitly based on their
   *                                {@link ComponentLocation}.
   * @param applyStartPhase         Whether to also apply the start lifecycle phase to the requested components.
   * @param keepPrevious            Whether previously initialized components should be kept unchanged.
   */
  public ComponentInitializationRequest(ComponentLocationFilter componentLocationFilter,
                                        boolean applyStartPhase, boolean keepPrevious) {
    this.isKeepPrevious = keepPrevious;
    this.location = empty();
    this.componentFilter = buildFilterFromComponentLocationFilter(componentLocationFilter);
    this.isApplyStartPhaseRequested = applyStartPhase;
  }

  /**
   * @return Returns the requested {@link Location} in case an explicit one was requested.
   */
  public Optional<Location> getLocation() {
    return location;
  }

  /**
   * @return A {@link ComponentAst} filter based on the request parameters.
   */
  public Predicate<ComponentAst> getComponentFilter() {
    return componentFilter;
  }

  /**
   * @return Whether to also apply the start lifecycle phase to the requested components.
   */
  public boolean isApplyStartPhaseRequested() {
    return isApplyStartPhaseRequested;
  }

  /**
   * @return Whether previously initialized components should be kept unchanged.
   */
  public boolean isKeepPreviousRequested() {
    return isKeepPrevious;
  }

  /**
   * Computes the requested locations and remembers them, so we don't need to traverse the artifact's AST again.
   *
   * @param fullArtifactAst The full {@link ArtifactAst} of the artifact.
   * @see #getRequestedLocations()
   */
  public void computeRequestedLocations(ArtifactAst fullArtifactAst) {
    // TODO: think if we can avoid doing it like this. Maybe we can afford doing it in the constructor.
    requestedLocations = location.map(location -> singleton(location.toString()))
        .orElseGet(() -> fullArtifactAst
            .filteredComponents(componentFilter)
            .map(comp -> comp.getLocation().getLocation())
            .collect(toSet()));
  }

  /**
   * @return The locations (belonging to the filtered artifact AST) that match with the request criteria.
   *         <p>
   *         {@link #computeRequestedLocations(ArtifactAst)} needs to be called first.
   */
  public Set<String> getRequestedLocations() {
    checkState(requestedLocations != null, "The requested locations have not been computed yet");
    return requestedLocations;
  }

  private Predicate<ComponentAst> buildFilterFromComponentLocationFilter(
                                                                         ComponentLocationFilter locationFilter) {
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
