/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.runtime.module.artifact.classloader;

import static java.util.Collections.emptyList;
import static org.mule.runtime.core.util.Preconditions.checkArgument;

import java.io.IOException;
import java.net.URL;
import java.util.Enumeration;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

/**
 * Defines a {@link ClassLoader} that filter which classes and resources can
 * be resolved based on a {@link ClassLoaderFilter}
 */
public class FilteringArtifactClassLoader extends ClassLoader implements ArtifactClassLoader
{

    protected static final Log logger = LogFactory.getLog(FilteringArtifactClassLoader.class);

    private final ArtifactClassLoader artifactClassLoader;
    private final ClassLoaderFilter filter;

    /**
     * Creates a new filtering classLoader
     *
     * @param artifactClassLoader artifact classLoader to filter. Non null
     * @param filter filters access to classes and resources from the artifact classLoader. Non null
     */
    public FilteringArtifactClassLoader(ArtifactClassLoader artifactClassLoader, ClassLoaderFilter filter)
    {
        checkArgument(artifactClassLoader != null, "ArtifactClassLoader cannot be null");
        checkArgument(filter!= null, "Filter cannot be null");

        this.artifactClassLoader = artifactClassLoader;
        this.filter = filter;
    }

    @Override
    public Class<?> loadClass(String name) throws ClassNotFoundException
    {
        if (filter.exportsClass(name))
        {
            return artifactClassLoader.getClassLoader().loadClass(name);
        }
        else
        {
            throw new ClassNotFoundException(name);
        }
    }

    @Override
    public URL getResource(String name)
    {
        if (filter.exportsResource(name))
        {
            return getResourceFromDelegate(artifactClassLoader, name);
        }
        else
        {
            return null;
        }
    }

    protected URL getResourceFromDelegate(ArtifactClassLoader artifactClassLoader, String name)
    {
        return artifactClassLoader.findResource(name);
    }

    @Override
    public Enumeration<URL> getResources(String name) throws IOException
    {
        if (filter.exportsResource(name))
        {
            return getResourcesFromDelegate(artifactClassLoader, name);
        }
        else
        {
            return new EnumerationAdapter<>(emptyList());
        }
    }

    protected Enumeration<URL> getResourcesFromDelegate(ArtifactClassLoader artifactClassLoader, String name) throws IOException
    {
        return artifactClassLoader.findResources(name);
    }

    @Override
    public URL findResource(String name)
    {
        return artifactClassLoader.findResource(name);
    }

    @Override
    public Enumeration<URL> findResources(String name) throws IOException
    {
        return artifactClassLoader.findResources(name);
    }

    @Override
    public String toString()
    {
        return String.format("%s[%s]@%s", getClass().getName(),
                             artifactClassLoader.getArtifactName(),
                             Integer.toHexString(System.identityHashCode(this)));
    }

    @Override
    public String getArtifactName()
    {
        return artifactClassLoader.getArtifactName();
    }

    @Override
    public ClassLoader getClassLoader()
    {
        return this;
    }

    @Override
    public void addShutdownListener(ShutdownListener listener)
    {
        artifactClassLoader.addShutdownListener(listener);
    }

    @Override
    public ClassLoaderLookupPolicy getClassLoaderLookupPolicy()
    {
        return artifactClassLoader.getClassLoaderLookupPolicy();
    }

    @Override
    public void dispose()
    {
        // Nothing to do here as this is just wrapper for another classLoader
    }

    @Override
    public URL findLocalResource(String resourceName)
    {
        return artifactClassLoader.findLocalResource(resourceName);
    }
}
