/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.module.launcher;

import org.mule.runtime.module.launcher.application.ArtifactPluginFactory;
import org.mule.runtime.module.launcher.plugin.ArtifactPluginRepository;

/**
 * Factory for {@code ArtifactClassLoaderBuilder} instances.
 *
 * @since 4.0
 */
public class TemporaryArtifactClassLoaderBuilderFactory
{

    private ArtifactPluginRepository applicationPluginRepository;
    private ArtifactPluginFactory artifactPluginFactory;

    /**
     * Creates an {@code ArtifactClassLoaderBuilderFactory} to create instances of {@code ArtifactClassLoaderBuilder}.
     *
     * @param applicationPluginRepository repository for artifacts plugins that are provided by default by the runtime
     * @param artifactPluginFactory factory for creating an artifact plugin from it's descriptor
     */
    public TemporaryArtifactClassLoaderBuilderFactory(ArtifactPluginRepository applicationPluginRepository, ArtifactPluginFactory artifactPluginFactory)
    {
        this.applicationPluginRepository = applicationPluginRepository;
        this.artifactPluginFactory = artifactPluginFactory;
    }

    /**
     * Create a new instance of a builder to create an artifact class loader.
     *
     * @return a new instance of {@code ArtifactClassLoaderBuilder}
     */
    public TemporaryArtifactClassLoaderBuilder createArtifactClassLoaderBuilder()
    {
        return new TemporaryArtifactClassLoaderBuilder(applicationPluginRepository, artifactPluginFactory);
    }

}
