/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.runtime.module.launcher.application;

import org.mule.runtime.module.artifact.classloader.ArtifactClassLoader;
import org.mule.runtime.module.artifact.classloader.MuleArtifactClassLoader;
import org.mule.runtime.module.launcher.plugin.ApplicationPluginDescriptor;

import java.net.URL;

public class DefaultApplicationPluginFactory implements ApplicationPluginFactory
{

    public DefaultApplicationPluginFactory()
    {
    }

    @Override
    public ApplicationPlugin create(ApplicationPluginDescriptor descriptor, ArtifactClassLoader parent)
    {
        final MuleArtifactClassLoader pluginClassLoader = createPluginClassLoader(parent, descriptor);

        return new DefaultApplicationPlugin(descriptor, pluginClassLoader);
    }

    private MuleArtifactClassLoader createPluginClassLoader(ArtifactClassLoader parent, ApplicationPluginDescriptor descriptor)
    {
        URL[] urls = new URL[descriptor.getRuntimeLibs().length + 1];
        urls[0] = descriptor.getRuntimeClassesDir();
        System.arraycopy(descriptor.getRuntimeLibs(), 0, urls, 1, descriptor.getRuntimeLibs().length);

        return new MuleArtifactClassLoader(descriptor.getName(), urls, parent.getClassLoader(), parent.getClassLoaderLookupPolicy());
    }
}