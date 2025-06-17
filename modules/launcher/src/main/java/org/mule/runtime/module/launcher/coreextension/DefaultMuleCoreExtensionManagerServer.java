/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.module.launcher.coreextension;

import static org.mule.runtime.core.api.config.bootstrap.ArtifactType.APP;
import static org.mule.runtime.core.api.config.bootstrap.ArtifactType.DOMAIN;
import static org.mule.runtime.module.launcher.privileged.ContainerServiceProvider.loadContainerServiceProviders;

import static org.slf4j.LoggerFactory.getLogger;

import org.mule.runtime.api.exception.MuleException;
import org.mule.runtime.api.lifecycle.InitialisationException;
import org.mule.runtime.api.service.ServiceRepository;
import org.mule.runtime.container.api.ArtifactClassLoaderManagerAware;
import org.mule.runtime.container.api.CoreExtensionsAware;
import org.mule.runtime.container.api.MuleCoreExtension;
import org.mule.runtime.core.api.Injector;
import org.mule.runtime.core.api.config.bootstrap.ArtifactType;
import org.mule.runtime.core.api.event.EventContextService;
import org.mule.runtime.core.internal.lock.ServerLockFactory;
import org.mule.runtime.module.artifact.api.classloader.ArtifactClassLoaderManager;
import org.mule.runtime.module.deployment.api.ArtifactDeploymentListener;
import org.mule.runtime.module.deployment.api.DeploymentListener;
import org.mule.runtime.module.deployment.api.DeploymentService;
import org.mule.runtime.module.deployment.api.DeploymentServiceAware;
import org.mule.runtime.module.deployment.internal.DeploymentListenerAdapter;
import org.mule.runtime.module.repository.api.RepositoryService;
import org.mule.runtime.module.repository.api.RepositoryServiceAware;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import org.slf4j.Logger;

public class DefaultMuleCoreExtensionManagerServer implements MuleCoreExtensionManagerServer {

  private static final Logger LOGGER = getLogger(DefaultMuleCoreExtensionManagerServer.class);

  private final MuleCoreExtensionDiscoverer coreExtensionDiscoverer;
  private final MuleCoreExtensionDependencyResolver coreExtensionDependencyResolver;
  private List<MuleCoreExtension> coreExtensions = new LinkedList<>();
  private DeploymentService deploymentService;
  private RepositoryService repositoryService;
  private List<MuleCoreExtension> orderedCoreExtensions;
  private ArtifactClassLoaderManager artifactClassLoaderManager;
  private ServiceRepository serviceRepository;
  private EventContextService eventContextService;
  private Map<Class, Object> containerServices = new HashMap<>();

  private List<MuleCoreExtension> initializedCoreExtensions = new ArrayList<>();
  private List<MuleCoreExtension> startedCoreExtensions = new ArrayList<>();
  private ServerLockFactory serverLockFactory;

  public DefaultMuleCoreExtensionManagerServer(MuleCoreExtensionDiscoverer coreExtensionDiscoverer,
                                               MuleCoreExtensionDependencyResolver coreExtensionDependencyResolver) {
    this.coreExtensionDiscoverer = coreExtensionDiscoverer;
    this.coreExtensionDependencyResolver = coreExtensionDependencyResolver;
  }

  @Override
  public void dispose() {
    LOGGER.debug("Disposing core extensions");
    for (MuleCoreExtension extension : coreExtensions) {

      if (initializedCoreExtensions.contains(extension)) {
        try {
          extension.dispose();
          LOGGER.debug("Core extension '{}' disposed", extension.getName());
        } catch (Exception ex) {
          LOGGER.atError()
              .setCause(ex)
              .log("Error disposing core extension {}", extension.getName());
        }
      }
    }
    initializedCoreExtensions.clear();
  }

  @Override
  public void initialise() throws InitialisationException {
    try {
      coreExtensions = coreExtensionDiscoverer.discover();

      orderedCoreExtensions = coreExtensionDependencyResolver.resolveDependencies(coreExtensions);

      initializeCoreExtensions();

    } catch (Exception e) {
      throw new InitialisationException(e, this);
    }
  }

  @Override
  public void start() throws MuleException {
    LOGGER.debug("Starting core extensions");
    for (MuleCoreExtension extension : orderedCoreExtensions) {
      extension.start();
      startedCoreExtensions.add(extension);
      LOGGER.debug("Core extension '{}' started", extension.getName());
    }
  }

  @Override
  public void stop() throws MuleException {
    if (orderedCoreExtensions == null) {
      return;
    }

    LOGGER.debug("Stopping core extensions");
    for (int i = orderedCoreExtensions.size() - 1; i >= 0; i--) {
      MuleCoreExtension extension = orderedCoreExtensions.get(i);

      if (startedCoreExtensions.contains(extension)) {
        try {
          extension.stop();
          LOGGER.debug("Core extension '{}' stopped", extension.getName());
        } catch (Exception e) {
          LOGGER.atWarn()
              .setCause(e)
              .log("Error stopping core extension: {}", extension.getName());
        }
      }
    }
    startedCoreExtensions.clear();
  }

  private void initializeCoreExtensions() throws MuleException {
    LOGGER.debug("Initializing core extensions");

    Injector simpleRegistry = createContainerInjector();

    for (MuleCoreExtension extension : orderedCoreExtensions) {

      loadContainerServiceProviders()
          .forEach(containerServiceProvider -> containerServiceProvider.inject(extension,
                                                                               containerServices.get(containerServiceProvider
                                                                                   .getServiceInterface())));


      if (extension instanceof DeploymentServiceAware) {
        ((DeploymentServiceAware) extension).setDeploymentService(deploymentService);
      }

      if (extension instanceof RepositoryServiceAware) {
        ((RepositoryServiceAware) extension).setRepositoryService(repositoryService);
      }

      if (extension instanceof ArtifactDeploymentListener) {
        deploymentService.addDeploymentListener(createDeploymentListenerAdapter((ArtifactDeploymentListener) extension, APP));
        deploymentService
            .addDomainDeploymentListener(createDeploymentListenerAdapter((ArtifactDeploymentListener) extension, DOMAIN));
      }

      if (extension instanceof DeploymentListener) {
        deploymentService.addDeploymentListener((DeploymentListener) extension);
      }

      if (extension instanceof CoreExtensionsAware) {
        ((CoreExtensionsAware) extension).setCoreExtensions(orderedCoreExtensions);
      }

      if (extension instanceof ArtifactClassLoaderManagerAware) {
        ((ArtifactClassLoaderManagerAware) extension).setArtifactClassLoaderManager(artifactClassLoaderManager);
      }

      simpleRegistry.inject(extension);

      extension.initialise();
      initializedCoreExtensions.add(extension);
      LOGGER.debug("Core extension '{}' initialized", extension.getName());
    }
  }

  private Injector createContainerInjector() {
    final var injectorBuilder = new ContainerInjectorBuilder<>()
        .withDeploymentService(deploymentService)
        .withRepositoryService(repositoryService)
        .withServiceRepository(serviceRepository)
        .withCoreExtensions(coreExtensions)
        .withArtifactClassLoaderManager(artifactClassLoaderManager)
        .withEventContextService(eventContextService)
        .withServerLockFactory(serverLockFactory);
    containerServices.forEach((k, v) -> injectorBuilder.registerObject(k.getName(), v));

    return injectorBuilder.build();
  }

  @Override
  public void setDeploymentService(DeploymentService deploymentService) {
    this.deploymentService = deploymentService;
  }

  @Override
  public void setRepositoryService(RepositoryService repositoryService) {
    this.repositoryService = repositoryService;
  }

  @Override
  public void setArtifactClassLoaderManager(ArtifactClassLoaderManager artifactClassLoaderManager) {
    this.artifactClassLoaderManager = artifactClassLoaderManager;
  }

  @Override
  public void setEventContextService(EventContextService eventContextService) {
    this.eventContextService = eventContextService;
  }

  @Override
  public void setServiceRepository(ServiceRepository serviceRepository) {
    this.serviceRepository = serviceRepository;
  }

  @Override
  public void setServerLockFactory(ServerLockFactory serverLockFactory) {
    this.serverLockFactory = serverLockFactory;
  }

  @Override
  public void setContainerServices(Map<Class, Object> containerServices) {
    this.containerServices = containerServices;
  }

  /**
   * Creates a {@link DeploymentListenerAdapter}.
   *
   * @param artifactDeploymentListener the artifactDeploymentListener to be adapted.
   * @param type:                      the artifact type.
   * @return an DeploymentListener.
   */
  DeploymentListener createDeploymentListenerAdapter(ArtifactDeploymentListener artifactDeploymentListener, ArtifactType type) {
    return new DeploymentListenerAdapter(artifactDeploymentListener, type);
  }
}
