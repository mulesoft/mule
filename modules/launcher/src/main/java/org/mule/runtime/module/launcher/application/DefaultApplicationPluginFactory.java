/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.runtime.module.launcher.application;

import static org.mule.runtime.core.util.Preconditions.checkArgument;
import org.mule.runtime.module.artifact.classloader.ArtifactClassLoader;
import org.mule.runtime.module.launcher.plugin.ApplicationPluginDescriptor;

/**
 * Default implementation for creating an {@link ApplicationPlugin} with the corresponding classloader.
 *
 * @since 4.0
 */
public class DefaultApplicationPluginFactory implements ApplicationPluginFactory
{
    private ApplicationPluginClassLoaderFactory applicationPluginClassLoaderFactory;

    /**
     * Creates an instance
     *
     * @param applicationPluginClassLoaderFactory used to create the {@link ArtifactClassLoader}, cannot be null.
     */
    public DefaultApplicationPluginFactory(ApplicationPluginClassLoaderFactory applicationPluginClassLoaderFactory)
    {
        checkArgument(applicationPluginClassLoaderFactory != null, "Application plugin classloader factory cannot be null");
        this.applicationPluginClassLoaderFactory = applicationPluginClassLoaderFactory;
    }

    @Override
    public ApplicationPlugin create(ApplicationPluginDescriptor descriptor, ArtifactClassLoader parent)
    {
        final ArtifactClassLoader pluginClassLoader = applicationPluginClassLoaderFactory.create(parent, descriptor);

        return new DefaultApplicationPlugin(descriptor, pluginClassLoader);
    }
}