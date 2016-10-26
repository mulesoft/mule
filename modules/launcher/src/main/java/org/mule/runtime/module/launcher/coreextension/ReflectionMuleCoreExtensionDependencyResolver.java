/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.module.launcher.coreextension;

import org.mule.runtime.container.api.CoreExtensionsAware;
import org.mule.runtime.container.api.MuleCoreExtension;
import org.mule.runtime.api.exception.MuleRuntimeException;
import org.mule.runtime.core.api.registry.IllegalDependencyInjectionException;

import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.LinkedList;
import java.util.List;

/**
 * Resolves dependencies using reflection to inject the required {@link MuleCoreExtension} in the dependant instance.
 */
public class ReflectionMuleCoreExtensionDependencyResolver implements MuleCoreExtensionDependencyResolver {

  private final MuleCoreExtensionDependencyDiscoverer dependencyDiscoverer;

  public ReflectionMuleCoreExtensionDependencyResolver() {
    this(new ReflectionMuleCoreExtensionDependencyDiscoverer());
  }

  public ReflectionMuleCoreExtensionDependencyResolver(MuleCoreExtensionDependencyDiscoverer dependencyDiscoverer) {
    this.dependencyDiscoverer = dependencyDiscoverer;
  }

  @Override
  public List<MuleCoreExtension> resolveDependencies(Collection<MuleCoreExtension> coreExtensions) {
    List<MuleCoreExtension> sortedCoreExtensions = new LinkedList<>(coreExtensions);
    sortCoreExtensionsByName(sortedCoreExtensions);

    List<MuleCoreExtension> coreExtensionAwareExtensions = findCoreExtensionAwareExtensions(sortedCoreExtensions);
    sortedCoreExtensions.removeAll(coreExtensionAwareExtensions);

    List<MuleCoreExtension> resolvedExtensions = resolveCoreExtensionDependenciesOrder(sortedCoreExtensions);
    resolvedExtensions.addAll(coreExtensionAwareExtensions);

    return resolvedExtensions;
  }

  private List<MuleCoreExtension> resolveCoreExtensionDependenciesOrder(List<MuleCoreExtension> sortedCoreExtensions) {
    List<MuleCoreExtension> unresolvedExtensions = new LinkedList<>(sortedCoreExtensions);
    List<MuleCoreExtension> resolvedExtensions = new LinkedList<>();

    boolean continueResolution = true;

    while (continueResolution) {
      int initialResolvedCount = resolvedExtensions.size();

      List<MuleCoreExtension> pendingUnresolvedExtensions = new LinkedList<>();

      for (MuleCoreExtension muleCoreExtension : unresolvedExtensions) {
        if (isResolvedCoreExtension(muleCoreExtension, resolvedExtensions)) {
          resolvedExtensions.add(muleCoreExtension);
        } else {
          pendingUnresolvedExtensions.add(muleCoreExtension);
        }
      }

      // Will try to resolve the extensions that are still unresolved
      unresolvedExtensions = pendingUnresolvedExtensions;

      continueResolution = resolvedExtensions.size() > initialResolvedCount;
    }

    if (unresolvedExtensions.size() != 0) {
      throw new UnresolveableDependencyException("Unable to resolve core extension dependencies: " + unresolvedExtensions);
    }

    return resolvedExtensions;
  }

  private boolean isResolvedCoreExtension(MuleCoreExtension muleCoreExtension, List<MuleCoreExtension> resolvedExtensions) {
    final List<LinkedMuleCoreExtensionDependency> dependencies = dependencyDiscoverer.findDependencies(muleCoreExtension);

    boolean resolvedCoreExtension = dependencies.size() == 0;

    if (!resolvedCoreExtension && satisfiedDependencies(dependencies, resolvedExtensions)) {
      injectDependencies(muleCoreExtension, resolvedExtensions, dependencies);
      resolvedCoreExtension = true;
    }

    return resolvedCoreExtension;
  }

  private List<MuleCoreExtension> findCoreExtensionAwareExtensions(List<MuleCoreExtension> sortedCoreExtensions) {
    List<MuleCoreExtension> coreExtensionAwareExtensions = new LinkedList<>();

    for (MuleCoreExtension muleCoreExtension : sortedCoreExtensions) {
      if (muleCoreExtension instanceof CoreExtensionsAware) {
        final List<LinkedMuleCoreExtensionDependency> dependencies = dependencyDiscoverer.findDependencies(muleCoreExtension);

        if (dependencies.isEmpty()) {

          coreExtensionAwareExtensions.add(muleCoreExtension);
        } else {
          throw new IllegalDependencyInjectionException("A class cannot implement CoreExtensionAware when is also using MuleCoreExtensionDependency");
        }
      }
    }
    return coreExtensionAwareExtensions;
  }

  private void sortCoreExtensionsByName(List<MuleCoreExtension> coreExtensionAwareExtensions1) {
    Collections.sort(coreExtensionAwareExtensions1, new Comparator<MuleCoreExtension>() {

      @Override
      public int compare(MuleCoreExtension coreExtension1, MuleCoreExtension coreExtension2) {
        return coreExtension1.getName().compareTo(coreExtension2.getName());
      }
    });
  }

  private void injectDependencies(MuleCoreExtension muleCoreExtension, List<MuleCoreExtension> resolvedExtensions,
                                  List<LinkedMuleCoreExtensionDependency> dependencies) {
    for (LinkedMuleCoreExtensionDependency linkedMuleCoreExtensionDependency : dependencies) {
      final MuleCoreExtension dependencyInstance =
          findDependencyInstance(resolvedExtensions, linkedMuleCoreExtensionDependency.getDependencyClass());

      try {
        linkedMuleCoreExtensionDependency.getDependantMethod().invoke(muleCoreExtension, new Object[] {dependencyInstance});
      } catch (Exception e) {
        throw new MuleRuntimeException(e);
      }
    }
  }

  private MuleCoreExtension findDependencyInstance(List<MuleCoreExtension> resolvedExtensions,
                                                   Class<? extends MuleCoreExtension> dependencyClass) {
    for (MuleCoreExtension coreExtension : resolvedExtensions) {
      if (dependencyClass.isAssignableFrom(coreExtension.getClass())) {
        return coreExtension;
      }
    }

    throw new IllegalArgumentException("Unable to find an instance for " + dependencyClass);
  }

  private boolean satisfiedDependencies(List<LinkedMuleCoreExtensionDependency> dependencies,
                                        List<MuleCoreExtension> resolvedExtensions) {
    for (LinkedMuleCoreExtensionDependency dependency : dependencies) {
      boolean isResolved = false;

      for (MuleCoreExtension resolved : resolvedExtensions) {
        if (dependency.getDependencyClass().isAssignableFrom(resolved.getClass())) {
          isResolved = true;
        }

      }

      if (!isResolved) {
        return false;
      }
    }

    return true;
  }
}
