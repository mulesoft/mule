/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.module.launcher.application;

import org.mule.runtime.module.artifact.classloader.ArtifactClassLoader;
import org.mule.runtime.module.artifact.classloader.ArtifactClassLoaderFactory;
import org.mule.runtime.module.launcher.ServerPluginClassLoaderManager;
import org.mule.runtime.module.launcher.descriptor.ApplicationDescriptor;

import java.util.LinkedList;
import java.util.List;

/**
 * Composes a {@link CompositeApplicationClassLoader} using a {@link ArtifactClassLoaderFactory}
 * to getDomainClassLoader the classloader for a Mule application and the plugin
 * classloaders available in the {@link ServerPluginClassLoaderManager}
 */
public class CompositeApplicationClassLoaderFactory implements ArtifactClassLoaderFactory<ApplicationDescriptor>
{

    private final ServerPluginClassLoaderManager serverPluginClassLoaderManager;
    private final ArtifactClassLoaderFactory applicationClassLoaderFactory;

    public CompositeApplicationClassLoaderFactory(ArtifactClassLoaderFactory applicationClassLoaderFactory, ServerPluginClassLoaderManager serverPluginClassLoaderManager)
    {
        this.applicationClassLoaderFactory = applicationClassLoaderFactory;
        this.serverPluginClassLoaderManager = serverPluginClassLoaderManager;
    }

    @Override
    public ArtifactClassLoader create(ArtifactClassLoader parent, ApplicationDescriptor descriptor)
    {
        ArtifactClassLoader appClassLoader = applicationClassLoaderFactory.create(parent, descriptor);

        List<ArtifactClassLoader> pluginClassLoaders = serverPluginClassLoaderManager.getPluginClassLoaders();

        if (!pluginClassLoaders.isEmpty())
        {
            List<ArtifactClassLoader> classLoaders = new LinkedList<>();
            classLoaders.add(appClassLoader);
            classLoaders.addAll(pluginClassLoaders);

            appClassLoader = new CompositeApplicationClassLoader(descriptor.getName(), appClassLoader.getClassLoader().getParent(), classLoaders, appClassLoader.getClassLoaderLookupPolicy());
        }

        return appClassLoader;
    }
}
