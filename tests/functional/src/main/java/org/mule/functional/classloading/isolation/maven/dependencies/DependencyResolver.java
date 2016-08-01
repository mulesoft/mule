/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.functional.classloading.isolation.maven.dependencies;

import org.mule.functional.classloading.isolation.maven.MavenArtifact;

import java.util.HashSet;
import java.util.Set;
import java.util.function.Predicate;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Responsible of filtering, traversing and selectDependencies a list of URLs with different conditions and patterns in order to
 * build class loaders by filtering an initial and complete classpath urls and using a maven dependency graph represented
 * by their dependencies transitions in a {@link java.util.Map}.
 *
 * @since 4.0
 */
public class DependencyResolver
{

    protected final transient Logger logger = LoggerFactory.getLogger(this.getClass());

    private Configuration configuration;

    /**
     * Creates a dependency resolver with the given configuration that defines the resolution strategy.
     *
     * @param configuration
     */
    public DependencyResolver(Configuration configuration)
    {
        this.configuration = configuration;
    }

    /**
     * Resolves the dependencies by applying the strategy configured in the {@link Configuration}
     *
     * @return a non-null {@link Set} of {@link MavenArtifact} representing the resolved dependencies
     */
    public Set<MavenArtifact> resolveDependencies()
    {
        MavenArtifact rootMavenArtifact = configuration.getDependencyGraph().getRootArtifact();
        Set<MavenArtifact> dependencies = configuration.getDependencyGraph().getDependencies()
                .stream().filter(key -> configuration.getDependenciesFilter().getPredicate().test(key)).collect(Collectors.toSet());

        Set<MavenArtifact> resolvedDependencies = new HashSet<>();
        if (!configuration.getDependenciesFilter().isOnlyCollectTransitiveDependencies())
        {
            resolvedDependencies.addAll(dependencies);
        }

        dependencies.stream().map(artifact -> collectTransitiveDependencies(artifact, configuration.getTransitiveDependencyFilter().getPredicate(),
                                                                            configuration.getTransitiveDependencyFilter().isTraverseWhenNoMatch()))
                .forEach(resolvedDependencies::addAll);

        Predicate<MavenArtifact> includeRootArtifactPredicate = configuration.getIncludeRootArtifactPredicate();
        if ((includeRootArtifactPredicate != null && includeRootArtifactPredicate.test(rootMavenArtifact)) ||
            configuration.isRootArtifactIncluded())
        {
            resolvedDependencies.add(rootMavenArtifact);
        }

        return resolvedDependencies;
    }

    /**
     * Builds a list of {@link MavenArtifact} representing the transitive dependencies of the provided dependency {@link MavenArtifact}.
     *
     * @param dependency a {@link MavenArtifact} for which we want to know its dependencies.
     * @param predicate a filter to be applied for each transitive dependency, if the filter passes the dependency is added and recursively collected its dependencies using the same filter.
     * @param traverseWhenNoMatch traverse to transitive dependencies no matter if the current one doesn't match
     * @return recursively gets the dependencies for the given artifact.
     */
    private Set<MavenArtifact> collectTransitiveDependencies(final MavenArtifact dependency, final Predicate<MavenArtifact> predicate, final boolean traverseWhenNoMatch)
    {
        Set<MavenArtifact> transitiveDependencies = new HashSet<>();

        configuration.getDependencyGraph().getTransitiveDependencies(dependency).stream().forEach(transitiveDependency -> {
            if (predicate.test(transitiveDependency))
            {
                transitiveDependencies.add(transitiveDependency);
                transitiveDependencies.addAll(collectTransitiveDependencies(transitiveDependency, predicate, traverseWhenNoMatch));
            }
            else
            {
                // Just the case for getting all their dependencies from an excluded dependencies (case of org.mule:core for instance, we also need their transitive dependencies)
                if (traverseWhenNoMatch)
                {
                    transitiveDependencies.addAll(collectTransitiveDependencies(transitiveDependency, predicate, traverseWhenNoMatch));
                }
            }
        });
        return transitiveDependencies;
    }

}