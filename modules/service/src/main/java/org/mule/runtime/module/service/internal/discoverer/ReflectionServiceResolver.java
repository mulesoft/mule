/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.runtime.module.service.internal.discoverer;

import static java.util.Comparator.comparing;
import static java.util.stream.Collectors.toList;
import static org.mule.runtime.api.util.Preconditions.checkArgument;
import org.mule.runtime.api.service.Service;
import org.mule.runtime.api.service.ServiceDefinition;
import org.mule.runtime.api.service.ServiceProvider;
import org.mule.runtime.api.util.Pair;
import org.mule.runtime.module.artifact.api.classloader.ArtifactClassLoader;
import org.mule.runtime.module.service.api.discoverer.ServiceResolutionError;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Resolves {@link Service} instances given a set of {@link ServiceProvider} instances.
 * <p/>
 * Resolution process consist if find, for each service provider, all the service dependencies it has. Then, service providers
 * will be instantiated when the required set of dependencies is available.
 * <p/>
 * Resolution of services providers must be done always in the same order for the same set of providers to ensure a consistent
 * startup of the container.
 * <p/>
 * In case of a missing dependency, the resolution will fail and the container should not start.
 */
public class ReflectionServiceResolver implements ServiceResolver {

  private final ServiceProviderResolutionHelper serviceProviderResolutionHelper;

  /**
   * Creates a new instance.
   *
   * @param serviceProviderResolutionHelper utility used to process service providers. Non null.
   */
  public ReflectionServiceResolver(ServiceProviderResolutionHelper serviceProviderResolutionHelper) {
    checkArgument(serviceProviderResolutionHelper != null, "serviceProviderResolutionHelper cannot be null");
    this.serviceProviderResolutionHelper = serviceProviderResolutionHelper;
  }

  @Override
  public List<Pair<ArtifactClassLoader, Service>> resolveServices(List<Pair<ArtifactClassLoader, ServiceProvider>> serviceProviders)
      throws ServiceResolutionError {
    List<DependencyAwareServiceProvider> dependencyAwareServiceProviders =
        createDependencyAwareServiceProviders(serviceProviders);

    Map<Class<? extends Service>, Pair<ArtifactClassLoader, ServiceDefinition>> registeredServices = new LinkedHashMap<>();
    List<DependencyAwareServiceProvider> unresolvedServiceProviders = new LinkedList<>(dependencyAwareServiceProviders);
    List<DependencyAwareServiceProvider> resolvedServiceProviders = new LinkedList<>();

    boolean continueResolution = true;

    while (continueResolution) {
      int initialResolvedCount = resolvedServiceProviders.size();

      List<DependencyAwareServiceProvider> pendingUnresolvedServices = new LinkedList<>();

      for (DependencyAwareServiceProvider dependencyAwareServiceProvider : unresolvedServiceProviders) {
        List<ServiceDefinition> serviceDefinitions =
            registeredServices.values().stream().map(pair -> pair.getSecond()).collect(toList());
        if (isResolvedService(dependencyAwareServiceProvider, serviceDefinitions)) {
          serviceProviderResolutionHelper.injectInstance(dependencyAwareServiceProvider.serviceProvider, serviceDefinitions);
          for (ServiceDefinition serviceDefinition : dependencyAwareServiceProvider.providedServices()) {
            registeredServices.put(serviceDefinition.getServiceClass(),
                                   new Pair<>(dependencyAwareServiceProvider.getArtifactClassLoader(), serviceDefinition));
          }

          resolvedServiceProviders.add(dependencyAwareServiceProvider);
        } else {
          pendingUnresolvedServices.add(dependencyAwareServiceProvider);
        }
      }

      // Will try to resolve the services that are still unresolved
      unresolvedServiceProviders = pendingUnresolvedServices;

      continueResolution = resolvedServiceProviders.size() > initialResolvedCount;
    }

    if (!unresolvedServiceProviders.isEmpty()) {
      final Set<Class<? extends Service>> dependencies = new HashSet<>();
      for (DependencyAwareServiceProvider dependencyAwareServiceProvider : unresolvedServiceProviders) {
        dependencies.addAll(dependencyAwareServiceProvider.getDependencies());
      }

      throw new ServiceResolutionError("Unable to resolve core service dependencies. Missing some of: " + dependencies);
    }

    List<Pair<ArtifactClassLoader, Service>> servicePairs = new ArrayList<>();
    for (Pair<ArtifactClassLoader, ServiceDefinition> pair : registeredServices.values()) {
      servicePairs.add(new Pair<>(pair.getFirst(), pair.getSecond().getService()));
    }
    return servicePairs;
  }

  private List<DependencyAwareServiceProvider> createDependencyAwareServiceProviders(List<Pair<ArtifactClassLoader, ServiceProvider>> serviceProviders) {
    final List<DependencyAwareServiceProvider> result = new ArrayList<>(serviceProviders.size());

    for (Pair<ArtifactClassLoader, ServiceProvider> pair : serviceProviders) {
      result.add(new DependencyAwareServiceProvider(pair.getFirst(), pair.getSecond(),
                                                    serviceProviderResolutionHelper.findServiceDependencies(pair.getSecond())));
    }

    result.sort(comparing(p -> p.getClass().getName()));

    return result;
  }

  private boolean isResolvedService(DependencyAwareServiceProvider dependencyAwareServiceProvider,
                                    Collection<ServiceDefinition> resolvedServices) {
    boolean resolvedCoreExtension = dependencyAwareServiceProvider.dependencies.isEmpty();

    if (!resolvedCoreExtension && satisfiedDependencies(dependencyAwareServiceProvider.dependencies, resolvedServices)) {
      resolvedCoreExtension = true;
    }

    return resolvedCoreExtension;
  }

  private boolean satisfiedDependencies(List<Class<? extends Service>> dependencies,
                                        Collection<ServiceDefinition> resolvedServices) {
    boolean resolvedDependency = true;

    for (Class dependency : dependencies) {
      resolvedDependency = false;
      for (ServiceDefinition registeredService : resolvedServices) {
        if (registeredService.getServiceClass().isAssignableFrom(dependency)) {
          resolvedDependency = true;
        }
      }

      if (!resolvedDependency) {
        break;
      }
    }

    return resolvedDependency;
  }

  private final class DependencyAwareServiceProvider implements ServiceProvider {

    private final ServiceProvider serviceProvider;
    private final List<Class<? extends Service>> dependencies;
    private final ArtifactClassLoader artifactClassLoader;

    DependencyAwareServiceProvider(ArtifactClassLoader artifactClassLoader, ServiceProvider serviceProvider,
                                   List<Class<? extends Service>> dependencies) {
      this.artifactClassLoader = artifactClassLoader;
      this.serviceProvider = serviceProvider;
      this.dependencies = dependencies;
    }

    @Override
    public List<ServiceDefinition> providedServices() {
      return serviceProvider.providedServices();
    }

    List<Class<? extends Service>> getDependencies() {
      return dependencies;
    }

    public ArtifactClassLoader getArtifactClassLoader() {
      return artifactClassLoader;
    }
  }
}
