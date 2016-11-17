/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.module.tooling.internal;

import org.mule.runtime.core.registry.SpiServiceRegistry;
import org.mule.runtime.deployment.model.api.application.Application;
import org.mule.runtime.module.deployment.impl.internal.application.DefaultApplicationFactory;
import org.mule.runtime.module.deployment.impl.internal.artifact.TemporaryArtifact;
import org.mule.runtime.module.deployment.impl.internal.artifact.TemporaryArtifactBuilderFactory;
import org.mule.runtime.module.repository.api.RepositoryService;
import org.mule.runtime.module.tooling.api.ToolingService;
import org.mule.runtime.module.tooling.api.connectivity.ConnectivityTestingServiceBuilder;

import java.io.File;
import java.io.IOException;

/**
 * Default implementation of {@code ToolingService}.
 *
 * @since 4.0
 */
public class DefaultToolingService implements ToolingService {

  private final TemporaryArtifactBuilderFactory artifactBuilderFactory;
  private final DefaultApplicationFactory applicationFactory;
  private RepositoryService repositoryService;

  /**
   * @param applicationFactory factory for creating the {@link Application}
   * @param repositoryService a {@code RepositoryService} which will be used to find extensions required for the service.
   * @param artifactBuilderFactory factory for building a {@link TemporaryArtifact} that will be used as context for the tooling
   */
  public DefaultToolingService(DefaultApplicationFactory applicationFactory, RepositoryService repositoryService,
                               TemporaryArtifactBuilderFactory artifactBuilderFactory) {
    this.applicationFactory = applicationFactory;
    this.repositoryService = repositoryService;
    this.artifactBuilderFactory = artifactBuilderFactory;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public ConnectivityTestingServiceBuilder newConnectivityTestingServiceBuilder() {
    return new DefaultConnectivityTestingServiceBuilder(repositoryService, artifactBuilderFactory, new SpiServiceRegistry());
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public Application createApplication(File applicationLocation) throws IOException {
    Application application = applicationFactory.createArtifact(applicationLocation);
    application.install();
    application.lazyInit();
    //TODO (MULE-10668) Add support for lazy start when creating Tooling Applications from ToolingService
    //application.start();
    return application;
  }

}
