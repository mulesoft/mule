/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 */
package org.mule.runtime.module.service.internal.discoverer;

import org.mule.runtime.api.service.Service;
import org.mule.runtime.api.service.ServiceDefinition;
import org.mule.runtime.api.service.ServiceProvider;
import org.mule.runtime.module.service.api.discoverer.ServiceResolutionError;

import java.util.Collection;
import java.util.List;

/**
 * Utility interface used on the {@link ServiceProvider} resolution process.
 */
public interface ServiceProviderResolutionHelper {

  /**
   * Injects the required service dependencies in the given {@link ServiceProvider}
   *
   * @param serviceProvider  service provider to be injected. Non null.
   * @param resolvedServices services which are available to be injected into the service provider. Non null.
   * @throws ServiceResolutionError in case a given service cannot be injected or there is a missing service dependency.
   */
  void injectInstance(ServiceProvider serviceProvider, Collection<ServiceDefinition> resolvedServices)
      throws ServiceResolutionError;

  /**
   * Lists the service dependencies for a given {@link ServiceProvider}
   * <p/>
   * A service dependency is the class of any field of the service provider class which is annotated with @Inject.
   * <p/>
   * Only service classes are able to be injected, if a non service class is found then the discovery process must fail with
   * {@link IllegalArgumentException}.
   *
   * @param serviceProvider
   * @return a non null list containing the required service classes that must be injected.
   */
  List<Class<? extends Service>> findServiceDependencies(ServiceProvider serviceProvider);
}
