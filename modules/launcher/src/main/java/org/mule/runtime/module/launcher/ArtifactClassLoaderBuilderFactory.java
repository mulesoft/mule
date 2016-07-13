/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.module.launcher;

import org.mule.runtime.module.artifact.classloader.ArtifactClassLoaderFactory;
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

    private ArtifactClassLoaderFactory artifactClassLoaderFactory;
    private ArtifactPluginRepository applicationPluginRepository;
    private ArtifactPluginFactory artifactPluginFactory;
    private ArtifactPluginDescriptorLoader artifactPluginDescriptorLoader;

    public ArtifactClassLoaderBuilderFactory(ArtifactClassLoaderFactory artifactClassLoaderFactory, ArtifactPluginRepository applicationPluginRepository, ArtifactPluginFactory artifactPluginFactory, ArtifactPluginDescriptorLoader artifactPluginDescriptorLoader)
    {
        this.artifactClassLoaderFactory = artifactClassLoaderFactory;
        this.applicationPluginRepository = applicationPluginRepository;
        this.artifactPluginFactory = artifactPluginFactory;
        this.artifactPluginDescriptorLoader = artifactPluginDescriptorLoader;
    }

    /**
     * @return a new instance of {@code ArtifactClassLoaderBuilder}
     */
    public ArtifactClassLoaderBuilder createArtifactClassLoaderBuilder()
    {
        return new ArtifactClassLoaderBuilder(artifactClassLoaderFactory, applicationPluginRepository, artifactPluginFactory, artifactPluginDescriptorLoader);
    }

}
