/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.module.launcher.plugin;

import org.mule.module.launcher.FineGrainedControlClassLoader;
import org.mule.module.launcher.artifact.ArtifactClassLoader;

import java.net.URL;
import java.util.Arrays;
import java.util.Collection;
import java.util.Set;

public class MulePluginsClassLoader extends FineGrainedControlClassLoader implements ArtifactClassLoader
{

    private final ArtifactClassLoader parent;

    public MulePluginsClassLoader(ArtifactClassLoader parent, PluginDescriptor... plugins)
    {
        this(parent, Arrays.asList(plugins));
    }

    public MulePluginsClassLoader(ArtifactClassLoader parent, Collection<PluginDescriptor> plugins)
    {
        super(new URL[0], parent.getClassLoader());
        this.parent = parent;
        for (PluginDescriptor plugin : plugins)
        {
            final URL[] pluginUrls = plugin.getClasspath().toURLs();
            for (URL pluginUrl : pluginUrls)
            {
                addURL(pluginUrl);
            }

            final Set<String> override = plugin.getLoaderOverride();
            processOverrides(override);
        }
    }

    @Override
    public String getArtifactName()
    {
        return parent.getArtifactName();
    }

    @Override
    public ClassLoader getClassLoader()
    {
        return parent.getClassLoader();
    }

    @Override
    public void dispose()
    {
        parent.dispose();
    }

    @Override
    public URL locateResource(String name)
    {
        return parent.locateResource(name);
    }
}
