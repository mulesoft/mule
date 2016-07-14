/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.module.artifact.classloader;

import static org.mule.runtime.core.util.Preconditions.checkArgument;

import java.net.URL;
import java.util.Map;

/**
 * Base {@link ArtifactClassLoader} implementation of deployable artifacts.
 *
 * @since 4.0
 */
public class MuleDeployableArtifactClassLoader extends MuleArtifactClassLoader
{
    private final Map<String, ArtifactClassLoader> artifactPluginClassLoaders;

    /**
     * Creates a {@code MuleDeployableArtifactClassLoader} with the provided configuration.
     *
     * @param name artifact name
     * @param urls the URLs from which to load classes and resources
     * @param parent parent class loader in the hierarchy
     * @param lookupPolicy policy for resolving classes and resources
     * @param artifactPluginClassLoaders class loaders for the plugin artifacts contained by this artifact
     */
    public MuleDeployableArtifactClassLoader(String name, URL[] urls, ClassLoader parent, ClassLoaderLookupPolicy lookupPolicy, Map<String, ArtifactClassLoader> artifactPluginClassLoaders)
    {
        super(name, urls, parent, lookupPolicy);
        checkArgument(artifactPluginClassLoaders != null, "artifact plugin class loaders cannot be null");
        this.artifactPluginClassLoaders = artifactPluginClassLoaders;
    }

    /**
     * Provides a map with the plugin name as key and its classloader as value.
     *
     * @return map of plugin class loaders
     */
    public Map<String, ArtifactClassLoader> getArtifactPluginsClassLoaders()
    {
        return artifactPluginClassLoaders;
    }
}
