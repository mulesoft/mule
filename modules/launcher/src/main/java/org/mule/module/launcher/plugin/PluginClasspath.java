/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.module.launcher.plugin;

import java.io.File;
import java.io.FilenameFilter;
import java.net.MalformedURLException;
import java.net.URL;

import org.apache.commons.io.filefilter.SuffixFileFilter;

/**
 * Represents a physical classpath of the plugin after it was unpacked/deployed.
 */
public class PluginClasspath
{

    private URL runtimeClassesDir;
    private URL[] runtimeLibs = new URL[0];

    protected PluginClasspath()
    {
        // non-public, use a factory method instead
    }

    public static PluginClasspath from(File pluginDir)
    {
        if (!pluginDir.exists())
        {
            throw new IllegalArgumentException("Can't read from the temporary plugin directory: " + pluginDir);
        }
        final PluginClasspath cp = new PluginClasspath();

        try
        {
            cp.setRuntimeClassesDir(new File(pluginDir, "classes").toURI().toURL());
            final File libDir = new File(pluginDir, "lib");
            if (libDir.exists())
            {
                final File[] jars = libDir.listFiles((FilenameFilter) new SuffixFileFilter(".jar"));
                URL[] urls = new URL[jars.length];
                for (int i = 0; i < jars.length; i++)
                {
                    urls[i] = jars[i].toURI().toURL();
                }
                cp.setRuntimeLibs(urls);
            }
        }
        catch (MalformedURLException e)
        {
            throw new IllegalArgumentException("Failed to getDomainClassLoader plugin classpath " + pluginDir);
        }
        return cp;
    }

    public URL[] getRuntimeLibs()
    {
        return runtimeLibs;
    }

    public URL getRuntimeClassesDir()
    {
        return runtimeClassesDir;
    }

    /**
     * @return merged classpath, 'classes' dir coming first
     */
    public URL[] toURLs()
    {
        URL[] merged = new URL[runtimeLibs.length + 1];
        merged[0] = runtimeClassesDir;
        System.arraycopy(runtimeLibs, 0, merged, 1, runtimeLibs.length);

        return merged;
    }

    protected void setRuntimeClassesDir(URL runtimeClassesDir)
    {
        this.runtimeClassesDir = runtimeClassesDir;
    }

    protected void setRuntimeLibs(URL[] runtimeLibs)
    {
        this.runtimeLibs = runtimeLibs;
    }

}
