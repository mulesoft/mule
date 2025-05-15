/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.module.launcher.coreextension;

import org.mule.runtime.api.lifecycle.Lifecycle;
import org.mule.runtime.api.service.ServiceRepository;
import org.mule.runtime.container.api.ArtifactClassLoaderManagerAware;
import org.mule.runtime.container.api.MuleCoreExtension;
import org.mule.runtime.core.api.event.EventContextService;
import org.mule.runtime.core.internal.lock.ServerLockFactory;
import org.mule.runtime.module.deployment.api.DeploymentServiceAware;
import org.mule.runtime.module.repository.api.RepositoryServiceAware;

import java.util.Map;

/**
 * Manages lifecycle and dependency injection for {@link MuleCoreExtension}
 */
public interface MuleCoreExtensionManagerServer
    extends Lifecycle, DeploymentServiceAware, RepositoryServiceAware, ArtifactClassLoaderManagerAware {

  /**
   * Allows {@link EventContextService} injection.
   *
   * @param eventContextService not null eventContextService implementation.
   */
  void setEventContextService(EventContextService eventContextService);

  /**
   * Allows {@link ServiceRepository} injection.
   *
   * @param serviceRepository not null service repository.
   */
  void setServiceRepository(ServiceRepository serviceRepository);

  /**
   * Allows {@link ServerLockFactory} injection.
   *
   * @param serverLockFactory the container level {@link ServerLockFactory}.
   * @since 4.6.0
   */
  void setServerLockFactory(ServerLockFactory serverLockFactory);

  /**
   * Allows injection of arbitrary objects.
   *
   * @param containerServices the actual implementation of the ContainerServices to use for injection, with the implemented
   *                          interface as key.
   * @since 4.10
   */
  void setContainerServices(Map<Class, Object> containerServices);

}
