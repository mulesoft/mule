/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.module.tooling.internal;

import org.mule.runtime.core.registry.SpiServiceRegistry;
import org.mule.runtime.module.repository.api.RepositoryService;
import org.mule.runtime.module.tooling.api.ToolingService;
import org.mule.runtime.module.tooling.api.connectivity.ConnectivityTestingServiceBuilder;
import org.mule.runtime.module.tooling.api.artifact.ToolingArtifactBuilderFactory;

/**
 * Default implementation of {@code ToolingService}.
 *
 * @since 4.0
 */
public class DefaultToolingService implements ToolingService
{

    private final ToolingArtifactBuilderFactory artifactBuilderFactory;
    private RepositoryService repositoryService;

    /**
     * @param repositoryService a {@code RepositoryService} which will be used to find extensions required for the service.
     * @param artifactBuilderFactory factory for building a {@link org.mule.runtime.module.tooling.api.artifact.ToolingArtifact} that will be used as context for the tooling services.
     */
    public DefaultToolingService(RepositoryService repositoryService, ToolingArtifactBuilderFactory artifactBuilderFactory)
    {
        this.repositoryService = repositoryService;
        this.artifactBuilderFactory = artifactBuilderFactory;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public ConnectivityTestingServiceBuilder newConnectivityTestingServiceBuilder()
    {
        return new DefaultConnectivityTestingServiceBuilder(repositoryService, artifactBuilderFactory, new SpiServiceRegistry());
    }
}
