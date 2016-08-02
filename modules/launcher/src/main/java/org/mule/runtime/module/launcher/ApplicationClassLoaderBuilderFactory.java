/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.module.launcher;

import org.mule.runtime.module.launcher.application.ArtifactPluginFactory;
import org.mule.runtime.module.launcher.application.MuleApplicationClassLoaderFactory;
import org.mule.runtime.module.launcher.plugin.ArtifactPluginRepository;

/**
 * Factory to create instances of {@code ApplicationClassLoaderBuilder}.
 *
 * @since 4.0
 */
public class ApplicationClassLoaderBuilderFactory
{

    private final MuleApplicationClassLoaderFactory applicationClassLoaderFactory;
    private final ArtifactPluginRepository artifactPluginRepository;
    private final ArtifactPluginFactory artifactPluginFactory;

    /**
     * Creates an {@code ApplicationClassLoaderBuilderFactory} to create {@code ApplicationClassLoaderBuilder} instances.
     *
     * @param applicationClassLoaderFactory factory for the class loader of the artifact resources and classes
     * @param artifactPluginRepository repository for artifact plugins provided by the runtime
     * @param artifactPluginFactory factory for creating plugin instances
     */
    public ApplicationClassLoaderBuilderFactory(MuleApplicationClassLoaderFactory applicationClassLoaderFactory, ArtifactPluginRepository artifactPluginRepository, ArtifactPluginFactory artifactPluginFactory)
    {
        this.applicationClassLoaderFactory = applicationClassLoaderFactory;
        this.artifactPluginRepository = artifactPluginRepository;
        this.artifactPluginFactory = artifactPluginFactory;
    }

    /**
     * Creates a new {@code ApplicationClassLoaderBuilder} instance to create the application artifact class loader.
     *
     * @return a {@code ApplicationClassLoaderBuilder} instance.
     */
    public ApplicationClassLoaderBuilder createArtifactClassLoaderBuilder()
    {
        return new ApplicationClassLoaderBuilder(applicationClassLoaderFactory, artifactPluginRepository, artifactPluginFactory);
    }

}
