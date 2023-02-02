/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.config.internal.context.lazy;

import static java.util.Collections.singleton;
import static java.util.Optional.empty;
import static java.util.Optional.of;
import static java.util.stream.Collectors.toSet;

import org.mule.runtime.api.component.location.ComponentLocation;
import org.mule.runtime.api.component.location.Location;
import org.mule.runtime.ast.api.ArtifactAst;
import org.mule.runtime.ast.api.ComponentAst;
import org.mule.runtime.config.api.LazyComponentInitializer.ComponentLocationFilter;
import org.mule.runtime.core.api.config.ConfigurationException;

import java.util.Optional;
import java.util.Set;
import java.util.function.Consumer;
import java.util.function.Predicate;

/**
 * Represents a component initialization request, grouping together all its parameters and adapting for the different ways they
 * can be expressed.
 * <p>
 * This implementation is not thread safe.
 */
class ComponentInitializationRequest {

  /**
   * A callback for performing validations over the minimal artifact AST.
   * <p>
   * Unlike a regular {@link Consumer}, this one can throw a {@link ConfigurationException}.
   */
  @FunctionalInterface
  public interface ArtifactAstValidator {

    void validate(ArtifactAst artifactAst) throws ConfigurationException;
  }


  /**
   * A builder to make it easy to build the request from the different ways it can be expressed.
   */
  public static class Builder {

    private final ComponentInitializationArtifactAstGenerator componentInitializationAstGenerator;
    private final boolean applyStartPhase;
    private final boolean keepPrevious;

    /**
     * Creates a request builder from the given required parameters.
     *
     * @param componentInitializationAstGenerator A {@link ComponentInitializationArtifactAstGenerator}.
     * @param applyStartPhase                     Whether to also apply the start lifecycle phase to the requested components.
     * @param keepPrevious                        Whether previously initialized components should be kept unchanged.
     */
    public Builder(ComponentInitializationArtifactAstGenerator componentInitializationAstGenerator,
                   boolean applyStartPhase,
                   boolean keepPrevious) {
      this.componentInitializationAstGenerator = componentInitializationAstGenerator;
      this.applyStartPhase = applyStartPhase;
      this.keepPrevious = keepPrevious;
    }

    /**
     * @param location A single component {@link Location} to initialize.
     * @return A {@link ComponentInitializationRequest} for the given {@code location}.
     */
    public ComponentInitializationRequest build(Location location) {
      return new ComponentInitializationRequest(componentInitializationAstGenerator,
                                                buildFilterFromLocation(location),
                                                of(location),
                                                applyStartPhase,
                                                keepPrevious);
    }

    /**
     * @param componentFilter A generic {@link Predicate} to filter {@link ComponentAst}s to initialize.
     * @return A {@link ComponentInitializationRequest} for the given {@code componentFilter}.
     */
    public ComponentInitializationRequest build(Predicate<ComponentAst> componentFilter) {
      return new ComponentInitializationRequest(componentInitializationAstGenerator,
                                                componentFilter,
                                                empty(),
                                                applyStartPhase,
                                                keepPrevious);
    }

    /**
     * @param componentLocationFilter A {@link ComponentLocationFilter} to filter components explicitly based on their
     *                                {@link ComponentLocation}.
     * @return A {@link ComponentInitializationRequest} for the given {@code componentLocationFilter}.
     */
    public ComponentInitializationRequest build(ComponentLocationFilter componentLocationFilter) {
      return build(buildFilterFromComponentLocationFilter(componentLocationFilter));
    }
  }

  private final ComponentInitializationArtifactAstGenerator componentInitializationAstGenerator;
  private final Optional<Location> location;
  private final Predicate<ComponentAst> componentFilter;
  private final boolean isApplyStartPhaseRequested;
  private final boolean isKeepPrevious;

  // Cached results to avoid multiple computations.
  private ArtifactAst minimalArtifactAst;
  private ArtifactAst artifactAstToInitialize;
  private Set<String> requestedLocations;

  private ComponentInitializationRequest(ComponentInitializationArtifactAstGenerator componentInitializationAstGenerator,
                                         Predicate<ComponentAst> componentFilter,
                                         Optional<Location> location,
                                         boolean applyStartPhase,
                                         boolean keepPrevious) {
    this.componentInitializationAstGenerator = componentInitializationAstGenerator;
    this.location = location;
    this.componentFilter = componentFilter;
    this.isApplyStartPhaseRequested = applyStartPhase;
    this.isKeepPrevious = keepPrevious;
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
   * @return The locations (belonging to the minimal artifact AST) that match with the request criteria. Already initialized
   *         locations are also included.
   */
  public Set<String> getRequestedLocations() {
    if (requestedLocations == null) {
      requestedLocations = doGetRequestedLocations(getMinimalAst());
    }
    return requestedLocations;
  }

  /**
   * Runs the given {@link ArtifactAstValidator} over the minimal {@link ArtifactAst} corresponding to this request.
   *
   * @param astValidator A validator to call with the minimal artifact AST. Already initialized components are still included at
   *                     this stage.
   * @throws ConfigurationException If the validation fails.
   */
  public void validateRequestedAst(ArtifactAstValidator astValidator) throws ConfigurationException {
    astValidator.validate(getMinimalAst());
  }

  /**
   * Returns the minimal artifact AST that satisfies the given {@code componentInitializationRequest} and taking into account the
   * current {@code componentInitializationState}.
   * <p>
   * A minimal artifact AST in this context is the one that includes the requested components and all of their dependencies but
   * excluding any that may be already initialized.
   *
   * @return The minimal (and potentially also filtered) artifact AST.
   */
  public ArtifactAst getFilteredAstToInitialize() {
    if (artifactAstToInitialize == null) {
      artifactAstToInitialize = doGetFilteredAstToInitialize(getMinimalAst());
    }
    return artifactAstToInitialize;
  }

  private static Predicate<ComponentAst> buildFilterFromComponentLocationFilter(ComponentLocationFilter locationFilter) {
    return componentModel -> {
      if (componentModel.getLocation() != null) {
        return locationFilter.accept(componentModel.getLocation());
      }
      return false;
    };
  }

  private static Predicate<ComponentAst> buildFilterFromLocation(Location location) {
    return comp -> comp.getLocation() != null
        && comp.getLocation().getLocation().equals(location.toString());
  }

  private ArtifactAst getMinimalAst() {
    if (minimalArtifactAst == null) {
      minimalArtifactAst = componentInitializationAstGenerator.getMinimalArtifactAstForRequest(this);
    }
    return minimalArtifactAst;
  }

  private Set<String> doGetRequestedLocations(ArtifactAst artifactAst) {
    return getLocation().map(location -> singleton(location.toString()))
        .orElseGet(() -> artifactAst
            .filteredComponents(getComponentFilter())
            .map(comp -> comp.getLocation().getLocation())
            .collect(toSet()));
  }

  private ArtifactAst doGetFilteredAstToInitialize(ArtifactAst artifactAst) {
    if (isKeepPreviousRequested()) {
      return componentInitializationAstGenerator.getArtifactAstExcludingAlreadyInitialized(artifactAst);
    } else {
      return artifactAst;
    }
  }
}
