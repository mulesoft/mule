/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.config.internal.context.lazy;

import static org.mule.runtime.ast.api.util.MuleAstUtils.filteredArtifactAst;

import static java.util.Collections.singleton;
import static java.util.Optional.empty;
import static java.util.Optional.of;
import static java.util.stream.Collectors.toSet;

import org.mule.runtime.api.component.location.ComponentLocation;
import org.mule.runtime.api.component.location.Location;
import org.mule.runtime.ast.api.ArtifactAst;
import org.mule.runtime.ast.api.ComponentAst;
import org.mule.runtime.ast.graph.api.ArtifactAstDependencyGraph;
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
 * The result of some methods may depend on the current initialization state.
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

    // postProcessedGraph is used when we get locations and initialize some components
    // while baseGraph is used when we validate Ast during lazy-init.
    private final ArtifactAstDependencyGraph postProcessedGraph;
    private final ComponentInitializationState componentInitializationState;
    private final Predicate<ComponentAst> alwaysEnabledComponentPredicate;
    private final boolean applyStartPhase;
    private final boolean keepPrevious;
    private final ArtifactAstDependencyGraph baseGraph;

    /**
     * Creates a request builder from the given required parameters.
     *
     * @param postProcessedGraph              The {@link ArtifactAstDependencyGraph} to use for extracting the minimal AST from a
     *                                        predicate.
     * @param componentInitializationState    The current {@link ComponentInitializationState}.
     * @param alwaysEnabledComponentPredicate A {@link Predicate} to determine if a component must always be initialized.
     * @param applyStartPhase                 Whether to also apply the start lifecycle phase to the requested components.
     * @param keepPrevious                    Whether previously initialized components should be kept unchanged.
     */
    public Builder(ArtifactAstDependencyGraph postProcessedGraph,
                   ArtifactAstDependencyGraph baseGraph,
                   ComponentInitializationState componentInitializationState,
                   Predicate<ComponentAst> alwaysEnabledComponentPredicate,
                   boolean applyStartPhase,
                   boolean keepPrevious) {
      this.postProcessedGraph = postProcessedGraph;
      this.baseGraph = baseGraph;
      this.componentInitializationState = componentInitializationState;
      this.alwaysEnabledComponentPredicate = alwaysEnabledComponentPredicate;
      this.applyStartPhase = applyStartPhase;
      this.keepPrevious = keepPrevious;
    }

    /**
     * @param location A single component {@link Location} to initialize.
     * @return A {@link ComponentInitializationRequest} for the given {@code location}.
     */
    public ComponentInitializationRequest build(Location location) {
      return new ComponentInitializationRequest(postProcessedGraph,
                                                baseGraph,
                                                componentInitializationState,
                                                alwaysEnabledComponentPredicate,
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
      return new ComponentInitializationRequest(postProcessedGraph,
                                                baseGraph,
                                                componentInitializationState,
                                                alwaysEnabledComponentPredicate,
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

  // postProcessedGraph is used when we get locations and initialize some components
  // while baseGraph is used when we validate Ast during lazy-init.
  private final ArtifactAstDependencyGraph postProcessedGraph;
  private final ArtifactAstDependencyGraph baseGraph;
  private final ComponentInitializationState componentInitializationState;
  private final Predicate<ComponentAst> alwaysEnabledComponentPredicate;
  private final Optional<Location> location;
  private final Predicate<ComponentAst> componentFilter;
  private final boolean isApplyStartPhaseRequested;
  private final boolean isKeepPrevious;

  // Cached results to avoid multiple computations.
  private ArtifactAst postProcessedMinimalArtifactAst;
  // baseMinimalArtifactAst is minimalArtifactAst without macro expansion.
  private ArtifactAst baseMinimalArtifactAst;
  private ArtifactAst artifactAstToInitialize;
  private Set<String> requestedLocations;

  private ComponentInitializationRequest(ArtifactAstDependencyGraph postProcessedGraph,
                                         ArtifactAstDependencyGraph baseGraph,
                                         ComponentInitializationState componentInitializationState,
                                         Predicate<ComponentAst> alwaysEnabledComponentPredicate,
                                         Predicate<ComponentAst> componentFilter,
                                         Optional<Location> location,
                                         boolean applyStartPhase,
                                         boolean keepPrevious) {
    this.postProcessedGraph = postProcessedGraph;
    this.baseGraph = baseGraph;
    this.componentInitializationState = componentInitializationState;
    this.alwaysEnabledComponentPredicate = alwaysEnabledComponentPredicate;
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
      requestedLocations = doGetRequestedLocations(getPostProcessedMinimalArtifactAst());
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
    astValidator.validate(getBaseMinimalArtifactAst());
  }

  private ArtifactAst getBaseMinimalArtifactAst() {
    if (baseMinimalArtifactAst == null) {
      Predicate<ComponentAst> minimalApplicationFilter = getFilterForMinimalArtifactAst();
      return baseGraph.minimalArtifactFor(minimalApplicationFilter);
    }
    return baseMinimalArtifactAst;
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
      artifactAstToInitialize = doGetFilteredAstToInitialize(getPostProcessedMinimalArtifactAst());
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

  private ArtifactAst getPostProcessedMinimalArtifactAst() {
    if (postProcessedMinimalArtifactAst == null) {
      postProcessedMinimalArtifactAst = doGetPostProcessedMinimalArtifactAst();
    }
    return postProcessedMinimalArtifactAst;
  }

  private ArtifactAst doGetPostProcessedMinimalArtifactAst() {
    Predicate<ComponentAst> minimalApplicationFilter = getFilterForMinimalArtifactAst();
    return postProcessedGraph.minimalArtifactFor(minimalApplicationFilter);
  }

  private Predicate<ComponentAst> getFilterForMinimalArtifactAst() {
    // Always enabled components should be included in the minimal graph even if no one depends on them, however, we don't
    // want to include them when keeping previous components because:
    // 1- They will have already been initialized.
    // 2- For some of them we can't detect if they were initialized as we do with other components because they may be unnamed
    // and non-locatable.
    if (isKeepPreviousRequested()) {
      return componentFilter;
    } else {
      return componentFilter.or(alwaysEnabledComponentPredicate);
    }
  }

  private Set<String> doGetRequestedLocations(ArtifactAst artifactAst) {
    return getLocation().map(location -> singleton(location.toString()))
        .orElseGet(() -> artifactAst
            .filteredComponents(componentFilter)
            .map(comp -> comp.getLocation().getLocation())
            .collect(toSet()));
  }

  private ArtifactAst doGetFilteredAstToInitialize(ArtifactAst artifactAst) {
    if (isKeepPreviousRequested()) {
      return filteredArtifactAst(artifactAst, comp -> !componentInitializationState.isComponentAlreadyInitialized(comp));
    } else {
      return artifactAst;
    }
  }
}
