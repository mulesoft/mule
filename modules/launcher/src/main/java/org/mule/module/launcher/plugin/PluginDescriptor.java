/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.module.launcher.plugin;

import org.mule.module.artifact.descriptor.ArtifactDescriptor;
import org.mule.module.launcher.descriptor.ApplicationDescriptor;

public class PluginDescriptor extends ArtifactDescriptor
{
    private PluginClasspath classpath = new PluginClasspath();
    private ApplicationDescriptor appDescriptor;

    public ApplicationDescriptor getAppDescriptor()
    {
        return appDescriptor;
    }

    public void setAppDescriptor(ApplicationDescriptor appDescriptor)
    {
        this.appDescriptor = appDescriptor;
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
