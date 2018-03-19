/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.module.launcher.coreextension;

import static org.mule.runtime.core.api.config.bootstrap.ArtifactType.APP;
import static org.mule.runtime.core.api.config.bootstrap.ArtifactType.DOMAIN;
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
import org.mule.runtime.module.artifact.api.classloader.ArtifactClassLoaderManager;
import org.mule.runtime.module.deployment.api.ArtifactDeploymentListener;
import org.mule.runtime.module.deployment.api.DeploymentListener;
import org.mule.runtime.module.deployment.api.DeploymentService;
import org.mule.runtime.module.deployment.api.DeploymentServiceAware;
import org.mule.runtime.module.deployment.internal.DeploymentListenerAdapter;
import org.mule.runtime.module.repository.api.RepositoryService;
import org.mule.runtime.module.repository.api.RepositoryServiceAware;
import org.mule.runtime.module.tooling.api.ToolingService;
import org.mule.runtime.module.tooling.api.ToolingServiceAware;

import org.slf4j.Logger;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;

public class DefaultMuleCoreExtensionManagerServer implements MuleCoreExtensionManagerServer {

  private static final Logger LOGGER = getLogger(DefaultMuleCoreExtensionManagerServer.class);

  private final MuleCoreExtensionDiscoverer coreExtensionDiscoverer;
  private final MuleCoreExtensionDependencyResolver coreExtensionDependencyResolver;
  private List<MuleCoreExtension> coreExtensions = new LinkedList<>();
  private DeploymentService deploymentService;
  private RepositoryService repositoryService;
  private ToolingService toolingService;
  private List<MuleCoreExtension> orderedCoreExtensions;
  private ArtifactClassLoaderManager artifactClassLoaderManager;
  private ServiceRepository serviceRepository;
  private EventContextService eventContextService;

  private List<MuleCoreExtension> initializedCoreExtensions = new ArrayList<>();
  private List<MuleCoreExtension> startedCoreExtensions = new ArrayList<>();

  public DefaultMuleCoreExtensionManagerServer(MuleCoreExtensionDiscoverer coreExtensionDiscoverer,
                                               MuleCoreExtensionDependencyResolver coreExtensionDependencyResolver) {
    this.coreExtensionDiscoverer = coreExtensionDiscoverer;
    this.coreExtensionDependencyResolver = coreExtensionDependencyResolver;
  }

  @Override
  public void dispose() {
    LOGGER.info("Disposing core extensions");
    for (MuleCoreExtension extension : coreExtensions) {

      if (initializedCoreExtensions.contains(extension)) {
        try {
          extension.dispose();
          LOGGER.info("Core extension '{}' disposed", extension.toString());
        } catch (Exception ex) {
          LOGGER.error("Error disposing core extension " + extension.getName(), ex);
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
    LOGGER.info("Starting core extensions");
    for (MuleCoreExtension extension : orderedCoreExtensions) {
      extension.start();
      startedCoreExtensions.add(extension);
      LOGGER.info("Core extension '{}' started", extension.toString());
    }
  }

  @Override
  public void stop() throws MuleException {
    if (orderedCoreExtensions == null) {
      return;
    }

    LOGGER.info("Stopping core extensions");
    for (int i = orderedCoreExtensions.size() - 1; i >= 0; i--) {
      MuleCoreExtension extension = orderedCoreExtensions.get(i);

      if (startedCoreExtensions.contains(extension)) {
        try {
          extension.stop();
          LOGGER.info("Core extension '{}' stopped", extension.toString());
        } catch (Throwable e) {
          LOGGER.warn("Error stopping core extension: " + extension.getName(), e);
        }
      }
    }
    startedCoreExtensions.clear();
  }

  private void initializeCoreExtensions() throws MuleException {
    LOGGER.info("Initializing core extensions");

    Injector simpleRegistry = createContainerInjector();

    for (MuleCoreExtension extension : orderedCoreExtensions) {
      if (extension instanceof DeploymentServiceAware) {
        ((DeploymentServiceAware) extension).setDeploymentService(deploymentService);
      }

      if (extension instanceof RepositoryServiceAware) {
        ((RepositoryServiceAware) extension).setRepositoryService(repositoryService);
      }

      if (extension instanceof ToolingServiceAware) {
        ((ToolingServiceAware) extension).setToolingService(toolingService);
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
      LOGGER.info("Core extension '{}' initialized", extension.toString());
    }
  }

  private Injector createContainerInjector() {
    return new ContainerInjectorBuilder()
        .withDeploymentService(deploymentService)
        .withRepositoryService(repositoryService)
        .withServiceRepository(serviceRepository)
        .withCoreExtensions(coreExtensions)
        .withArtifactClassLoaderManager(artifactClassLoaderManager)
        .withEventContextService(eventContextService)
        .withToolingService(toolingService)
        .build();
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
  public void setToolingService(ToolingService toolingService) {
    this.toolingService = toolingService;
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

  /**
   * Creates a {@link DeploymentListenerAdapter}.
   *
   * @param artifactDeploymentListener the artifactDeploymentListener to be adapted.
   * @param type: the artifact type.
   * @return an DeploymentListener.
   */
  DeploymentListener createDeploymentListenerAdapter(ArtifactDeploymentListener artifactDeploymentListener, ArtifactType type) {
    return new DeploymentListenerAdapter(artifactDeploymentListener, type);
  }
}
