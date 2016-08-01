/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.functional.classloading.isolation.maven;

/**
 * Resolves maven dependencies for the artifact being tested.
 *
 * @since 4.0
 */
public interface MavenDependenciesResolver
{

    /**
     * Creates a dependency graph where with all the transitive dependencies, including duplicates. By duplicates it means that it
     * is not the dependency tree resolved by maven where it takes the approach of resolving the duplicates dependencies by nearest algorithm.
     * This case the whole graph of dependencies and duplicates, two artifacts depend on the same third artifact, those dependencies would
     * appear in graph too.
     *
     * @throws IllegalStateException if the dependencies are empty
     * @return it generates the dependencies for the maven artifact where the resolver is being called.
     * It returns a {@link DependenciesGraph} that holds the rootArtifact, dependencies and transitive dependencies for each dependency.
     * The rootArtifact represents the current maven artifact that the test belongs to.
     */
    DependenciesGraph buildDependencies();
}
