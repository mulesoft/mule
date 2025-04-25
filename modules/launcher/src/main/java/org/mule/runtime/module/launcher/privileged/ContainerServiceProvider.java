/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.module.launcher.privileged;

import static java.util.ServiceLoader.load;

import org.mule.runtime.container.api.MuleCoreExtension;
import org.mule.runtime.module.deployment.api.DeploymentService;
import org.mule.runtime.module.deployment.impl.internal.MuleArtifactResourcesRegistry;

import java.util.Collection;
import java.util.ServiceLoader.Provider;

/**
 * Allows Mule modules to contribute services to the container.
 * <p>
 * A service in this context is an object that provides functionality to the runtime and its core extensions.
 * 
 * @since 4.10
 */
public interface ContainerServiceProvider<S> {

  static Collection<ContainerServiceProvider> loadContainerServiceProviders() {
    return load(ContainerServiceProvider.class, ContainerServiceProvider.class.getClassLoader())
        .stream()
        .map(Provider::get)
        .toList();
  }

  /**
   * @return the interface that declares the functionality provided by the provided service.
   */
  Class<S> getServiceInterface();

  /**
   * @param deploymentService
   * @param artifactResourcesRegistry
   * @return the instance of the service implementation.
   */
  S getServiceImplementation(DeploymentService deploymentService, MuleArtifactResourcesRegistry artifactResourcesRegistry);

  /**
   * Injects the required dependencies into {@code forInject}.
   * 
   * @param extension
   * @param forInject
   */
  void inject(MuleCoreExtension extension, S forInject);

}
