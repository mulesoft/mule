/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.module.launcher.coreextension;

import org.mule.MuleCoreExtension;
import org.mule.api.MuleRuntimeException;

import java.util.Collection;
import java.util.LinkedList;
import java.util.List;

/**
 * Resolves dependencies using reflection to inject the required {@link MuleCoreExtension}
 * in the dependant instance.
 */
public class ReflectionMuleCoreExtensionDependencyResolver implements MuleCoreExtensionDependencyResolver
{

    private final MuleCoreExtensionDependencyDiscoverer dependencyDiscoverer;

    public ReflectionMuleCoreExtensionDependencyResolver()
    {
        this(new ReflectionMuleCoreExtensionDependencyDiscoverer());
    }

    public ReflectionMuleCoreExtensionDependencyResolver(MuleCoreExtensionDependencyDiscoverer dependencyDiscoverer)
    {
        this.dependencyDiscoverer = dependencyDiscoverer;
    }

    @Override
    public List<MuleCoreExtension> resolveDependencies(Collection<MuleCoreExtension> coreExtensions)
    {
        List<MuleCoreExtension> unresolvedExtensions = new LinkedList<MuleCoreExtension>(coreExtensions);
        List<MuleCoreExtension> resolvedExtensions = new LinkedList<MuleCoreExtension>();

        boolean continueResolution = true;

        while (continueResolution)
        {
            int initialResolvedCount = resolvedExtensions.size();

            List<MuleCoreExtension> pendingUnresolvedExtensions = new LinkedList<MuleCoreExtension>();

            for (MuleCoreExtension muleCoreExtension : unresolvedExtensions)
            {
                boolean resolvedDependency = isResolvedDependency(resolvedExtensions, muleCoreExtension);

                if (resolvedDependency)
                {
                    resolvedExtensions.add(muleCoreExtension);
                }
                else
                {
                    pendingUnresolvedExtensions.add(muleCoreExtension);
                }
            }

            // Will try to resolve the extensions that are still unresolved
            unresolvedExtensions = pendingUnresolvedExtensions;

            continueResolution = resolvedExtensions.size() > initialResolvedCount;
        }

        if (unresolvedExtensions.size() != 0)
        {
            throw new UnresolveableDependencyException("Unable to resolve core extension dependencies: " + unresolvedExtensions);
        }

        return resolvedExtensions;
    }

    private boolean isResolvedDependency(List<MuleCoreExtension> resolvedExtensions, MuleCoreExtension muleCoreExtension)
    {
        boolean resolvedDependency = false;

        final List<LinkedMuleCoreExtensionDependency> dependencies = dependencyDiscoverer.findDependencies(muleCoreExtension);

        if (dependencies.size() == 0)
        {
            resolvedDependency = true;
        }
        else if (satisfiedDependencies(dependencies, resolvedExtensions))
        {
            injectDependencies(muleCoreExtension, resolvedExtensions, dependencies);
            resolvedDependency = true;
        }

        return resolvedDependency;
    }

    private void injectDependencies(MuleCoreExtension muleCoreExtension, List<MuleCoreExtension> resolvedExtensions, List<LinkedMuleCoreExtensionDependency> dependencies)
    {
        for (LinkedMuleCoreExtensionDependency linkedMuleCoreExtensionDependency : dependencies)
        {
            final MuleCoreExtension dependencyInstance = findDependencyInstance(resolvedExtensions, linkedMuleCoreExtensionDependency.getDependencyClass());

            try
            {
                linkedMuleCoreExtensionDependency.getDependantMethod().invoke(muleCoreExtension, new Object[] {dependencyInstance});
            }
            catch (Exception e)
            {
                throw new MuleRuntimeException(e);
            }
        }
    }

    private MuleCoreExtension findDependencyInstance(List<MuleCoreExtension> resolvedExtensions, Class<? extends MuleCoreExtension> dependencyClass)
    {
        for (MuleCoreExtension coreExtension : resolvedExtensions)
        {
            if (dependencyClass.isAssignableFrom(coreExtension.getClass()))
            {
                return coreExtension;
            }
        }

        throw new IllegalArgumentException("Unable to find an instance for " + dependencyClass);
    }

    private boolean satisfiedDependencies(List<LinkedMuleCoreExtensionDependency> dependencies, List<MuleCoreExtension> resolvedExtensions)
    {
        for (LinkedMuleCoreExtensionDependency dependency : dependencies)
        {
            boolean isResolved = false;

            for (MuleCoreExtension resolved : resolvedExtensions)
            {
                if (dependency.getDependencyClass().isAssignableFrom(resolved.getClass()))
                {
                    isResolved = true;
                }

            }

            if (!isResolved)
            {
                return false;
            }
        }

        return true;
    }
}
