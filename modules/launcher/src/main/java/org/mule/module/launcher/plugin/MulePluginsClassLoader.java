/*
 * (c) 2003-2014 MuleSoft, Inc. This software is protected under international copyright
 * law. All use of this software is subject to MuleSoft's Master Subscription Agreement
 * (or other master license agreement) separately entered into in writing between you and
 * MuleSoft. If such an agreement is not in place, you may not use the software.
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
