/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.module.launcher;

import org.mule.runtime.module.artifact.classloader.DeployableArtifactClassLoaderFactory;
import org.mule.runtime.module.launcher.application.ArtifactPluginFactory;
import org.mule.runtime.module.launcher.plugin.ArtifactPluginDescriptorLoader;
import org.mule.runtime.module.launcher.plugin.ArtifactPluginRepository;

/**
 * Factory for {@code ArtifactClassLoaderBuilder} instances.
 *
 * @since 4.0
 */
public class ArtifactClassLoaderBuilderFactory
{

    private DeployableArtifactClassLoaderFactory artifactClassLoaderFactory;
    private ArtifactPluginRepository applicationPluginRepository;
    private ArtifactPluginFactory artifactPluginFactory;
    private ArtifactPluginDescriptorLoader artifactPluginDescriptorLoader;

    /**
     * Creates an {@code ArtifactClassLoaderBuilderFactory} to create instances of {@code ArtifactClassLoaderBuilder}.
     *
     * @param artifactClassLoaderFactory factory for creating the artifact resources and classes specific class loader
     * @param applicationPluginRepository repository for artifacts plugins that are provided by default by the runtime
     * @param artifactPluginFactory factory for creating an artifact plugin from it's descriptor
     * @param artifactPluginDescriptorLoader factory for loading the artifact plugin descriptor from a file
     */
    public ArtifactClassLoaderBuilderFactory(DeployableArtifactClassLoaderFactory artifactClassLoaderFactory, ArtifactPluginRepository applicationPluginRepository, ArtifactPluginFactory artifactPluginFactory, ArtifactPluginDescriptorLoader artifactPluginDescriptorLoader)
    {
        this.artifactClassLoaderFactory = artifactClassLoaderFactory;
        this.applicationPluginRepository = applicationPluginRepository;
        this.artifactPluginFactory = artifactPluginFactory;
        this.artifactPluginDescriptorLoader = artifactPluginDescriptorLoader;
    }

    /**
     * Create a new instance of a builder to create an artifact class loader.
     *
     * @return a new instance of {@code ArtifactClassLoaderBuilder}
     */
    public ArtifactClassLoaderBuilder createArtifactClassLoaderBuilder()
    {
        return new ArtifactClassLoaderBuilder(artifactClassLoaderFactory, applicationPluginRepository, artifactPluginFactory, artifactPluginDescriptorLoader);
    }

}
