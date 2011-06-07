/*
 * $Id$
 * --------------------------------------------------------------------------------------
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 *
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.module.launcher.plugin;

import org.mule.module.launcher.application.Application;

import java.util.HashSet;
import java.util.Set;

public class PluginDescriptor
{
    private Set<String> loaderOverride = new HashSet<String>();
    private Application application;
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

    /**
     * @return owning application ref or null for container-level (system) plugin
     */
    public Application getApplication()
    {
        return application;
    }

    public void setApplication(Application application)
    {
        this.application = application;
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
