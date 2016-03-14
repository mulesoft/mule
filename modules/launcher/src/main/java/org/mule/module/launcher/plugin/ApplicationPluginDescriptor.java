/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.module.launcher.plugin;

import org.mule.module.artifact.descriptor.ArtifactDescriptor;

import java.net.URL;

public class ApplicationPluginDescriptor extends ArtifactDescriptor
{

    private URL runtimeClassesDir;
    private URL[] runtimeLibs = new URL[0];

    public URL getRuntimeClassesDir()
    {
        return runtimeClassesDir;
    }

    public void setRuntimeClassesDir(URL runtimeClassesDir)
    {
        this.runtimeClassesDir = runtimeClassesDir;
    }

    public URL[] getRuntimeLibs()
    {
        return runtimeLibs;
    }

    public void setRuntimeLibs(URL[] runtimeLibs)
    {
        this.runtimeLibs = runtimeLibs;
    }
}
