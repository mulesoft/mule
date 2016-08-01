/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.functional.classloading.isolation.maven.dependencies;

import org.mule.functional.classloading.isolation.maven.MavenArtifact;

import java.util.function.Predicate;

/**
 * A definition for the filtering strategy to be used in {@link DependencyResolver}
 *
 * @since 4.0
 */
public final class TransitiveDependenciesFilter
{
    private Predicate<MavenArtifact> predicate = x -> true;
    private boolean traverseWhenNoMatch = false;

    /**
     * Creates a new instance, only visible to package scope so the {@link Configuration} is the only one
     * that should call this constructor.
     *
     * @param predicate
     */
    TransitiveDependenciesFilter(Predicate<MavenArtifact> predicate)
    {
        this.predicate = predicate;
    }

    /**
     * Public constructor, accessible by clients of this API.
     */
    public TransitiveDependenciesFilter()
    {
    }

    /**
     * {@link Predicate} to be used to filter which transitive dependencies should be included.
     *
     * @param predicate
     * @return this
     */
    public TransitiveDependenciesFilter match(Predicate<MavenArtifact> predicate)
    {
        this.predicate = predicate;
        return this;

    }

    /**
     * When a transitive dependency does not match the predicate it is not collected as part of the result, but at the same
     * time its transitive dependencies will not be evaluated. It will stop traversing the tree at this point and continue with
     * others , it should not be included but it will continue with other leafs.
     * <p/>
     * By setting this that behaviour will change and instead of stopping traversing to its transitive dependencies, the resolution
     * process will not include the not matching transitive dependency but it will continue evaluating its transitive dependencies, if
     * any of them match the criteria they will be included and if not it wll continue with its transitive dependencies and so on.
     *
     * @return this
     */
    public TransitiveDependenciesFilter evaluateTransitiveDependenciesWhenPredicateFails()
    {
        this.traverseWhenNoMatch = true;
        return this;
    }

    Predicate<MavenArtifact> getPredicate()
    {
        return predicate;
    }

    boolean isTraverseWhenNoMatch()
    {
        return this.traverseWhenNoMatch;
    }

}
