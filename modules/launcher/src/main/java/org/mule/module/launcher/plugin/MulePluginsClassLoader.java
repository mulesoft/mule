/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.module.launcher.plugin;

import org.mule.module.launcher.FineGrainedControlClassLoader;

import java.net.URL;
import java.util.Arrays;
import java.util.Collection;
import java.util.Set;

public class MulePluginsClassLoader extends FineGrainedControlClassLoader
{

    public MulePluginsClassLoader(ClassLoader parent, PluginDescriptor... plugins)
    {
        this(parent, Arrays.asList(plugins));
    }

    public MulePluginsClassLoader(ClassLoader parent, Collection<PluginDescriptor> plugins)
    {
        super(new URL[0], parent);
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
}
