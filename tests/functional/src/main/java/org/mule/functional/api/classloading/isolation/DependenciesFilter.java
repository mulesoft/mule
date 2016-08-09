/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.functional.api.classloading.isolation;

import java.util.function.Predicate;

/**
 * Filter definition for selecting dependencies when resolving them from {@link DependencyResolver}
 *
 * @since 4.0
 */
public final class DependenciesFilter {

  private Predicate<MavenArtifact> predicate = x -> true;
  private boolean onlyCollectTransitiveDependencies = false;

  /**
   * {@link Predicate} to be used to filter which dependencies should be included and used to traverse the tree in order to
   * collect their transitive dependencies.
   *
   * @param predicate
   * @return this
   */
  public DependenciesFilter match(Predicate<MavenArtifact> predicate) {
    this.predicate = predicate;
    return this;
  }

  /**
   * Sets the filter strategy to not include as part of the dependencies list resolved those dependencies that do match with the
   * filter defined here, but these dependencies should be used still to traverse the tree in order to collect their transitive
   * dependencies A frequently use case for this is, I would like to get all the transitive dependencies of one of my dependencies
   * but I don't want the dependency as part of the result.
   *
   * @return
   */
  public DependenciesFilter onlyCollectTransitiveDependencies() {
    this.onlyCollectTransitiveDependencies = true;
    return this;
  }

  Predicate<MavenArtifact> getPredicate() {
    return predicate;
  }

  boolean isOnlyCollectTransitiveDependencies() {
    return onlyCollectTransitiveDependencies;
  }

}
