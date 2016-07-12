/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.functional.classloading.isolation.maven;

import com.google.common.collect.ImmutableSet;

import java.util.Collections;
import java.util.Map;
import java.util.Set;

/**
 * Represents a tree graph of maven dependencies for a {@link MavenArtifact}.
 * It has a rootArtifact that is the {@link MavenArtifact} from the one dependencies were collected.
 * A {@link java.util.List} of {@link MavenArtifact} with its dependencies, and a also for each
 * of this dependencies its transitive ones.
 * <p/>
 * The tree graph also has duplicated dependencies, meaning that if rootArtifact has a dependency to A, and B and both have a transitive
 * dependency to C when getting transitives for A and B C will be returned. It is quite different from the maven dependency resolution where
 * dependencies are resolved only once based on a nearest algorithm.
 *
 * @since 4.0
 */
public class DependenciesGraph
{
    private final MavenArtifact rootArtifact;
    private final Set<MavenArtifact> dependencies;
    private final Map<MavenArtifact, Set<MavenArtifact>> transitiveDependencies;

    /**
     * Creates a {@link DependenciesGraph} for the given rootArtifact, dependencies and transitive dependencies
     *
     * @param rootArtifact defines the rootArtifact {@link MavenArtifact} from where the graph was built of
     * @param dependencies {@link java.util.List} of {@link MavenArtifact} that contains the direct dependencies for the rootArtifact
     * @param transitiveDependencies {@link Map} for the transitive dependencies of the rootArtifact, key is a {@link MavenArtifact} dependency and value the {@link Set} of {@link MavenArtifact} dependencies of it
     */
    public DependenciesGraph(MavenArtifact rootArtifact, Set<MavenArtifact> dependencies, Map<MavenArtifact, Set<MavenArtifact>> transitiveDependencies)
    {
        this.rootArtifact = rootArtifact;
        this.dependencies = dependencies;
        this.transitiveDependencies = transitiveDependencies;
    }

    public MavenArtifact getRootArtifact()
    {
        return this.rootArtifact;
    }

    public Set<MavenArtifact> getDependencies()
    {
        return ImmutableSet.<MavenArtifact>builder().addAll(this.dependencies).build();
    }

    /**
     * Gets the transitive dependencies for a {@link MavenArtifact}, rootArtifact cannot be used here.
     *
     * @param dependency the {@link MavenArtifact} to get is transitive dependencies
     * @throws IllegalArgumentException if the rootArtifact was used to get its transitive dependencies
     * @return a {@link Set} of {@link MavenArtifact} with the transitive dependencies
     */
    public Set<MavenArtifact> getTransitiveDependencies(MavenArtifact dependency)
    {
        if (rootArtifact.equals(dependency))
        {
            throw new IllegalArgumentException("RootArtifact cannot be used to get transitive dependencies");
        }
        if (!transitiveDependencies.containsKey(dependency))
        {
            return Collections.emptySet();
        }
        return ImmutableSet.<MavenArtifact>builder().addAll(transitiveDependencies.get(dependency)).build();
    }

}
