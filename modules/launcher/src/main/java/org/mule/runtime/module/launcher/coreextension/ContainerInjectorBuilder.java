/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.runtime.module.launcher.coreextension;

import static org.mule.runtime.api.util.Preconditions.checkArgument;
import static org.mule.runtime.core.api.util.StringUtils.isEmpty;

import org.mule.runtime.api.service.Service;
import org.mule.runtime.api.service.ServiceRepository;
import org.mule.runtime.container.api.MuleCoreExtension;
import org.mule.runtime.core.api.Injector;
import org.mule.runtime.core.api.event.EventContextService;
import org.mule.runtime.core.internal.registry.SimpleRegistry;
import org.mule.runtime.core.privileged.registry.RegistrationException;
import org.mule.runtime.module.artifact.api.classloader.ArtifactClassLoaderManager;
import org.mule.runtime.module.deployment.api.DeploymentService;
import org.mule.runtime.module.repository.api.RepositoryService;
import org.mule.runtime.module.tooling.api.ToolingService;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Builds the injector used on the container to inject container's level objects
 *
 * @param <T> class of the implementation builder
 *
 * since 4.1
 */
public class ContainerInjectorBuilder<T extends ContainerInjectorBuilder> {

  private final Map<String, Object> objectsToRegister = new HashMap<>();

  /**
   * @param deploymentService deployment service used on the container
   * @return same builder instance
   */
  public T withDeploymentService(DeploymentService deploymentService) {
    registerObject(DeploymentService.class.getName(), deploymentService);

    return getThis();
  }

  /**
   * @param serviceRepository service repository used on the container
   * @return same builder instance
   */
  public T withServiceRepository(ServiceRepository serviceRepository) {
    if (serviceRepository != null) {

      for (Service service : serviceRepository.getServices()) {
        registerObject(service.getName(), service);
      }

      registerObject(ServiceRepository.class.getName(), serviceRepository);
    }

    return getThis();
  }

  /**
   * @param repositoryService respository service used on the container
   * @return same builder instance
   */
  public T withRepositoryService(RepositoryService repositoryService) {
    registerObject(RepositoryService.class.getName(), repositoryService);

    return getThis();
  }

  /**
   * @param coreExtensions core extensions available on the container
   * @return same builder instance
   */
  public T withCoreExtensions(List<MuleCoreExtension> coreExtensions) {
    registerObject("_coreExtensions", coreExtensions);

    for (MuleCoreExtension muleCoreExtension : coreExtensions) {
      registerObject("_coreExtension_" + muleCoreExtension.getName(), muleCoreExtension);
    }

    return getThis();
  }

  /**
   * @param toolingService tooling service used on the container
   * @return same builder instance
   */
  public T withToolingService(ToolingService toolingService) {
    registerObject(ToolingService.class.getName(), toolingService);

    return getThis();
  }

  /**
   * @param artifactClassLoaderManager tracks all the artifact classloaders created on the container
   * @return same builder instance
   */
  public T withArtifactClassLoaderManager(ArtifactClassLoaderManager artifactClassLoaderManager) {
    registerObject(ArtifactClassLoaderManager.class.getName(), artifactClassLoaderManager);

    return getThis();
  }

  public T withEventContextService(EventContextService eventContextService) {
    registerObject(EventContextService.class.getName(), eventContextService);

    return getThis();
  }

  /**
   * Creates the injector for the container
   *
   * @return an injector witht the provided configuration
   */
  public Injector build() {
    SimpleRegistry injector = new SimpleRegistry(null, null);

    try {
      injector.registerObjects(objectsToRegister);
    } catch (RegistrationException e) {
      throw new RuntimeException(e);
    }

    return injector;
  }

  /**
   * Registers an object to make it available as a candidate to be injected
   *
   * @param key name of the object to be registered
   * @param value object to register
   */
  protected final void registerObject(String key, Object value) {
    if (value == null) {
      return;
    }

    checkArgument(!isEmpty(key), "key cannot be empty");
    objectsToRegister.put(key, value);
  }

  /**
     * @return current instance. Used just to avoid compilation warnings.
     */
  protected T getThis() {
    return (T) this;
  }
}

