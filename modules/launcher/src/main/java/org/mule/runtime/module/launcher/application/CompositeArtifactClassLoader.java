/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.runtime.module.launcher.application;

import static java.util.stream.Collectors.toCollection;

import org.mule.runtime.module.artifact.Artifact;
import org.mule.runtime.module.artifact.classloader.ArtifactClassLoader;
import org.mule.runtime.module.artifact.classloader.ClassLoaderLookupPolicy;
import org.mule.runtime.module.artifact.classloader.CompositeClassLoader;
import org.mule.runtime.module.artifact.classloader.DisposableClassLoader;
import org.mule.runtime.module.artifact.classloader.EnumerationAdapter;
import org.mule.runtime.module.artifact.classloader.ShutdownListener;
import org.mule.runtime.module.launcher.MuleApplicationClassLoader;

import java.io.IOException;
import java.net.URL;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

/**
 * Composite classloader to use on {@link Artifact}
 */
public class CompositeArtifactClassLoader extends CompositeClassLoader implements ArtifactClassLoader
{

    protected static final Log logger = LogFactory.getLog(CompositeApplicationClassLoader.class);
    private final String artifactName;
    private final List<ArtifactClassLoader> artifactClassLoaders;

    /**
     * Creates a new instance
     *  @param artifactName         name of the artifact owning the created instance.
     * @param parent               parent class loader used to delegate the lookup process. Can be null.
     * @param artifactClassLoaders artifact class loaders to compose. Non empty.
     * @param lookupPolicy         policy used to guide the lookup process. Non null
     */
    public CompositeArtifactClassLoader(String artifactName, ClassLoader parent, List<ArtifactClassLoader> artifactClassLoaders, ClassLoaderLookupPolicy lookupPolicy)
    {
        super(parent, getClassLoaders(artifactClassLoaders), lookupPolicy);
        this.artifactName = artifactName;
        this.artifactClassLoaders = artifactClassLoaders;
    }

    private static List<ClassLoader> getClassLoaders(List<ArtifactClassLoader> artifactClassLoaders)
    {
        return artifactClassLoaders.stream().map(ArtifactClassLoader::getClassLoader).collect(toCollection(LinkedList::new));
    }

    @Override
    public String getArtifactName()
    {
        return this.artifactName;
    }

    @Override
    public URL findResource(String name)
    {
        for (ArtifactClassLoader artifactClassLoader : artifactClassLoaders)
        {
            final URL resource = artifactClassLoader.findResource(name);

            if (resource != null)
            {
                if (logger.isDebugEnabled())
                {
                    logger.debug(String.format("Resource '%s' loaded from classLoader '%s", name, artifactClassLoader));
                }

                return resource;
            }
        }

        return null;
    }

    @Override
    public Enumeration<URL> findResources(String name) throws IOException
    {
        final Map<String, URL> resources = new HashMap<>();

        for (ArtifactClassLoader artifactClassLoader : artifactClassLoaders)
        {
            Enumeration<URL> partialResources = artifactClassLoader.findResources(name);

            while (partialResources.hasMoreElements())
            {
                URL url = partialResources.nextElement();
                if (resources.get(url.toString()) == null)
                {
                    resources.put(url.toString(), url);
                }
            }
        }

        return new EnumerationAdapter<>(resources.values());
    }

    @Override
    public URL findLocalResource(String resourceName)
    {
        for (ArtifactClassLoader artifactClassLoader : artifactClassLoaders)
        {
            URL resource = artifactClassLoader.findLocalResource(resourceName);

            if (resource != null)
            {
                return resource;
            }
        }
        return null;
    }

    @Override
    public ClassLoader getClassLoader()
    {
        return this;
    }

    @Override
    public void dispose()
    {
        for (ClassLoader classLoader : classLoaders)
        {
            if (classLoader instanceof DisposableClassLoader)
            {
                ((DisposableClassLoader) classLoader).dispose();
            }
        }
    }

    @Override
    public void addShutdownListener(ShutdownListener listener)
    {
        for (ClassLoader classLoader : classLoaders)
        {
            if (classLoader instanceof MuleApplicationClassLoader)
            {
                ((MuleApplicationClassLoader) classLoader).addShutdownListener(listener);
                return;
            }
        }
    }

    @Override
    public String toString()
    {
        return getClass().getSimpleName() + classLoaders.toString();
    }
}
