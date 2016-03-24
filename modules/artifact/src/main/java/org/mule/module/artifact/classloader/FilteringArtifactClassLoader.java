/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.module.artifact.classloader;

import java.io.IOException;
import java.net.URL;
import java.util.Enumeration;
import java.util.LinkedList;
import java.util.List;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

/**
 * Defines a {@link ClassLoader} that filter which classes and resources can
 * be resolved based on a {@link ClassLoaderFilter}
 */
public class FilteringArtifactClassLoader extends ClassLoader
{

    protected static final Log logger = LogFactory.getLog(FilteringArtifactClassLoader.class);

    private String artifactName;
    private final ClassLoader pluginClassLoader;
    private final ClassLoaderFilter filter;

    public FilteringArtifactClassLoader(String artifactName, ClassLoader pluginClassLoader, ClassLoaderFilter filter)
    {
        this.artifactName = artifactName;
        this.pluginClassLoader = pluginClassLoader;
        this.filter = filter;
    }

    @Override
    public Class<?> loadClass(String name) throws ClassNotFoundException
    {
        if (filter.exportsClass(name))
        {
            return pluginClassLoader.loadClass(name);
        }
        else
        {
            throw new ClassNotFoundException();
        }
    }

    @Override
    public URL getResource(String name)
    {
        if (filter.exportsResource(name))
        {
            return pluginClassLoader.getResource(name);
        }
        else
        {
            return null;
        }
    }

    @Override
    public Enumeration<URL> getResources(String name) throws IOException
    {
        List<URL> filteredResources = new LinkedList<>();

        if (filter.exportsResource(name))
        {
            Enumeration<URL> resources = pluginClassLoader.getResources(name);

            while (resources.hasMoreElements())
            {
                filteredResources.add(resources.nextElement());
            }
        }

        return new EnumerationAdapter<>(filteredResources);
    }

    @Override
    public String toString()
    {
        return String.format("%s[%s]@%s", getClass().getName(),
                             artifactName,
                             Integer.toHexString(System.identityHashCode(this)));
    }
}
