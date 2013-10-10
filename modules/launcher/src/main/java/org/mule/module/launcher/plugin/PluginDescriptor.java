/*
 * (c) 2003-2014 MuleSoft, Inc. This software is protected under international copyright
 * law. All use of this software is subject to MuleSoft's Master Subscription Agreement
 * (or other master license agreement) separately entered into in writing between you and
 * MuleSoft. If such an agreement is not in place, you may not use the software.
 */
package org.mule.module.launcher.plugin;

import org.mule.module.launcher.descriptor.ApplicationDescriptor;

import java.util.HashSet;
import java.util.Set;

public class PluginDescriptor
{
    private Set<String> loaderOverride = new HashSet<String>();
    private ApplicationDescriptor appDescriptor;
    private String name;
    private PluginClasspath classpath = new PluginClasspath();

    public Set<String> getLoaderOverride()
    {
        return loaderOverride;
    }

    public void setLoaderOverride(Set<String> loaderOverride)
    {
        this.loaderOverride = loaderOverride;
    }

    public ApplicationDescriptor getAppDescriptor()
    {
        return appDescriptor;
    }

    public void setAppDescriptor(ApplicationDescriptor appDescriptor)
    {
        this.appDescriptor = appDescriptor;
    }

    public String getName()
    {
        return name;
    }

    public void setName(String name)
    {
        this.name = name;
    }

    public PluginClasspath getClasspath()
    {
        return classpath;
    }

    public void setClasspath(PluginClasspath classpath)
    {
        this.classpath = classpath;
    }
}
