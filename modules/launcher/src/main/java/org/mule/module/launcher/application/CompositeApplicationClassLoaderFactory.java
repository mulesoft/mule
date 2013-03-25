/*
 * $Id$
 * --------------------------------------------------------------------------------------
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 *
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.module.launcher.application;

import org.mule.module.launcher.PluginClassLoaderManager;
import org.mule.module.launcher.descriptor.ApplicationDescriptor;

import java.util.LinkedList;
import java.util.List;

/**
 * Composes a {@link CompositeApplicationClassLoader} using a {@link ApplicationClassLoaderFactory}
 * to create the classloader for a Mule application and the plugin
 * classloaders available in the {@link PluginClassLoaderManager}
 */
public class CompositeApplicationClassLoaderFactory implements ApplicationClassLoaderFactory
{

    private final PluginClassLoaderManager pluginClassLoaderManager;
    private final ApplicationClassLoaderFactory applicationClassLoaderFactory;

    public CompositeApplicationClassLoaderFactory(ApplicationClassLoaderFactory applicationClassLoaderFactory, PluginClassLoaderManager pluginClassLoaderManager)
    {
        this.applicationClassLoaderFactory = applicationClassLoaderFactory;
        this.pluginClassLoaderManager = pluginClassLoaderManager;
    }

    @Override
    public ClassLoader create(ApplicationDescriptor descriptor)
    {
        List<ClassLoader> pluginClassLoaders = pluginClassLoaderManager.getPluginClassLoaders();

        ClassLoader appClassLoader = applicationClassLoaderFactory.create(descriptor);

        if (!pluginClassLoaders.isEmpty())
        {
            List<ClassLoader> classLoaders = new LinkedList<ClassLoader>();
            classLoaders.add(appClassLoader);
            classLoaders.addAll(pluginClassLoaders);

            appClassLoader = new CompositeApplicationClassLoader(descriptor.getAppName(), classLoaders);
        }

        return appClassLoader;
    }
}
