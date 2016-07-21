/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.module.launcher;

import org.mule.runtime.module.artifact.classloader.ArtifactClassLoader;
import org.mule.runtime.module.artifact.classloader.ArtifactClassLoaderFactory;
import org.mule.runtime.module.artifact.classloader.DeployableArtifactClassLoaderFactory;
import org.mule.runtime.module.launcher.application.ArtifactPluginFactory;
import org.mule.runtime.module.launcher.plugin.ArtifactPluginDescriptorLoader;
import org.mule.runtime.module.launcher.plugin.ArtifactPluginRepository;

/**
 * {@link ArtifactClassLoader} builder for class loaders used by mule artifacts such as domains or applications.
 *
 * Allows to construct a classloader when using a set of artifact plugins and takes into account default plugins
 * provided by the runtime and the shared libraries configured for the plugins.
 *
 * @since 4.0
 */
public class ArtifactClassLoaderBuilder extends AbstractArtifactClassLoaderBuilder<ArtifactClassLoaderBuilder>
{

    private ArtifactClassLoader parentClassLoader;

    /**
     * {@inheritDoc}
     */
    public ArtifactClassLoaderBuilder(DeployableArtifactClassLoaderFactory artifactClassLoaderFactory, ArtifactPluginRepository artifactPluginRepository, ArtifactPluginFactory artifactPluginFactory, ArtifactPluginDescriptorLoader artifactPluginDescriptorLoader)
    {
        super(artifactClassLoaderFactory, artifactPluginRepository, artifactPluginFactory, artifactPluginDescriptorLoader);
    }

    /**
     * @param parentClassLoader parent class loader for the artifact class loader.
     * @return the builder
     */
    public ArtifactClassLoaderBuilder setParentClassLoader(ArtifactClassLoader parentClassLoader)
    {
        this.parentClassLoader = parentClassLoader;
        return this;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    ArtifactClassLoader getParentClassLoader()
    {
        return parentClassLoader;
    }
}
