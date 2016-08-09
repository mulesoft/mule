/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.functional.api.classloading.isolation;

import static org.apache.commons.lang.StringUtils.isNotEmpty;

import com.google.common.collect.ImmutableSet;

import java.util.Collections;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Represents a tree graph of maven dependencies for a {@link MavenArtifact}. It has a rootArtifact that is the
 * {@link MavenArtifact} from the one dependencies were collected. A {@link java.util.List} of {@link MavenArtifact} with its
 * dependencies, and a also for each of this dependencies its transitive ones.
 * <p/>
 * The tree graph also has duplicated dependencies, meaning that if rootArtifact has a dependency to A, and B and both have a
 * transitive dependency to C when getting transitives for A and B C will be returned. It is quite different from the maven
 * dependency resolution where dependencies are resolved only once based on a nearest algorithm.
 *
 * @since 4.0
 */
public class DependenciesGraph {

  private final MavenArtifact rootArtifact;
  private final Set<MavenArtifact> dependencies;
  private final Map<MavenArtifact, Set<MavenArtifact>> transitiveDependencies;

  /**
   * Creates a {@link DependenciesGraph} for the given rootArtifact, dependencies and transitive dependencies
   *
   * @param rootArtifact defines the rootArtifact {@link MavenArtifact} from where the graph was built of
   * @param dependencies {@link java.util.List} of {@link MavenArtifact} that contains the direct dependencies for the
   *        rootArtifact
   * @param transitiveDependencies {@link Map} for the transitive dependencies of the rootArtifact, key is a {@link MavenArtifact}
   *        dependency and value the {@link Set} of {@link MavenArtifact} dependencies of it
   */
  public DependenciesGraph(MavenArtifact rootArtifact, Set<MavenArtifact> dependencies,
                           Map<MavenArtifact, Set<MavenArtifact>> transitiveDependencies) {
    this.rootArtifact = rootArtifact;
    this.dependencies = dependencies;
    this.transitiveDependencies = transitiveDependencies;
  }

  public MavenArtifact getRootArtifact() {
    return this.rootArtifact;
  }

  public Set<MavenArtifact> getDependencies() {
    return ImmutableSet.<MavenArtifact>builder().addAll(this.dependencies).build();
  }

  /**
   * Gets the transitive dependencies for a {@link MavenArtifact}, rootArtifact cannot be used here.
   * <p/>
   * The dependencies graph could have transitive dependencies from provided->compile scope when the rootArtifact defines a
   * dependency {@code x->y} as provided therefore {@code y->z} would be also provided but at the same time it has y as compile so
   * there will be a link from {@code x:provided->y:provided} and {@code y:compile->z:compile}. So we have to look for these
   * scenarios when looking for y dependencies we cannot rely on y's scope, we just need to look for its
   * groupId/artifactId/version (if it has one).
   * <p/>
   * The identity of a {@link MavenArtifact} cannot be used here, we have to compare it with a different logic.
   *
   * @param dependency the {@link MavenArtifact} to get is transitive dependencies
   * @throws IllegalArgumentException if the rootArtifact was used to get its transitive dependencies
   * @return a {@link Set} of {@link MavenArtifact} with the transitive dependencies
   */
  public Set<MavenArtifact> getTransitiveDependencies(MavenArtifact dependency) {
    if (rootArtifact.equals(dependency)) {
      throw new IllegalArgumentException("RootArtifact cannot be used to get transitive dependencies");
    }
    Set<MavenArtifact> transitiveDependencyKeys = transitiveDependencies.keySet().stream()
        .filter(mavenArtifact -> areSameArtifactIgnoringScope(mavenArtifact, dependency)).collect(Collectors.toSet());
    if (transitiveDependencyKeys.size() == 0) {
      return Collections.emptySet();
    }

    ImmutableSet.Builder<MavenArtifact> builder = ImmutableSet.<MavenArtifact>builder();
    transitiveDependencyKeys.forEach(key -> builder.addAll(transitiveDependencies.get(key)));
    return builder.build();
  }

  /**
   * Compares two {@link MavenArtifact} if they reference to the same maven artifact, without taking into account the
   * {@code scope} due to that defines how it could be used but not the identity.
   *
   * @param artifact {@link MavenArtifact}
   * @param anotherArtifact {@link MavenArtifact}
   * @return true if the reference to the same {@MavenArtifact} by groupdId, artifactId and version (optional)
   */
  private static boolean areSameArtifactIgnoringScope(MavenArtifact artifact, MavenArtifact anotherArtifact) {
    return artifact.getGroupId().equals(anotherArtifact.getGroupId())
        && artifact.getArtifactId().equals(anotherArtifact.getArtifactId())
        && (isNotEmpty(artifact.getVersion()) ? artifact.getVersion().equals(anotherArtifact.getVersion()) : true);
  }

}
