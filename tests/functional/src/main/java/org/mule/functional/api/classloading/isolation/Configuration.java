/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.functional.api.classloading.isolation;

import java.util.function.Predicate;

/**
 * Defines the settings the resolution strategy for {@link DependencyResolver}
 *
 * @since 4.0
 */
public final class Configuration {

  private DependenciesGraph dependencyGraph;
  private boolean rootArtifactIncluded = false;
  private DependenciesFilter dependenciesFilter = new DependenciesFilter();
  private TransitiveDependenciesFilter transitiveDependencyFilter = new TransitiveDependenciesFilter(x -> false);
  private Predicate<MavenArtifact> rootArtifactPredicate = null;

  /**
   * Sets the {@link DependenciesGraph} used for resolving and collecting dependencies.
   *
   * @param dependencyGraph
   * @return this
   */
  public Configuration setMavenDependencyGraph(DependenciesGraph dependencyGraph) {
    this.dependencyGraph = dependencyGraph;
    return this;
  }

  /**
   * It sets the strategy to also include the root artifact in the result of the dependencies resolved. By default it is not
   * included due to the most common usage is to get dependencies instead of the whole set of root artifact plus dependencies.
   *
   * @return this
   */
  public Configuration includeRootArtifact() {
    this.rootArtifactIncluded = true;
    return this;
  }

  /**
   * A conditional way to define if the root artifact should be included or not in results. A {@link Predicate} can be passed that
   * will be evaluated with the root artifact during the resolution of the dependencies.
   *
   * @param rootArtifactPredicate
   * @return this
   */
  public Configuration includeRootArtifact(Predicate<MavenArtifact> rootArtifactPredicate) {
    this.rootArtifactPredicate = rootArtifactPredicate;
    return this;
  }

  /**
   * Sets a {@link DependenciesFilter} that defines the strategy for selecting the dependencies. If this is not defined, by
   * default, the resolver will take all the dependencies of the root artifact without their transitive dependencies.
   *
   * @param dependenciesFilter
   * @return this
   */
  public Configuration selectDependencies(DependenciesFilter dependenciesFilter) {
    this.dependenciesFilter = dependenciesFilter;
    return this;
  }

  /**
   * Sets a {@link TransitiveDependenciesFilter} that defines the strategy for selecting the transitive dependencies for the
   * dependencies of the root artifact that matched the criteria defined. By default (if this is not set) no transitive
   * dependencies will be collected during the dependencies resolution.
   *
   * @param transitiveDependenciesFilter
   * @return this
   */
  public Configuration collectTransitiveDependencies(TransitiveDependenciesFilter transitiveDependenciesFilter) {
    this.transitiveDependencyFilter = transitiveDependenciesFilter;
    return this;
  }

  DependenciesGraph getDependencyGraph() {
    return dependencyGraph;
  }

  DependenciesFilter getDependenciesFilter() {
    return dependenciesFilter;
  }

  TransitiveDependenciesFilter getTransitiveDependencyFilter() {
    return transitiveDependencyFilter;
  }

  boolean isRootArtifactIncluded() {
    return this.rootArtifactIncluded;
  }

  Predicate<MavenArtifact> getIncludeRootArtifactPredicate() {
    return rootArtifactPredicate;
  }
}
