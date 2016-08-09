/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.module.launcher.plugin;

import org.mule.runtime.module.artifact.classloader.ArtifactClassLoaderFilter;
import org.mule.runtime.module.launcher.descriptor.DeployableArtifactDescriptor;

import java.net.URL;

public class ArtifactPluginDescriptor extends DeployableArtifactDescriptor {

  public static final String PLUGIN_PROPERTIES = "plugin.properties";

  private URL runtimeClassesDir;
  private URL[] runtimeLibs = new URL[0];
  private ArtifactClassLoaderFilter classLoaderFilter = ArtifactClassLoaderFilter.NULL_CLASSLOADER_FILTER;

  public URL getRuntimeClassesDir() {
    return runtimeClassesDir;
  }

  public void setRuntimeClassesDir(URL runtimeClassesDir) {
    this.runtimeClassesDir = runtimeClassesDir;
  }

  public URL[] getRuntimeLibs() {
    return runtimeLibs;
  }

  public void setRuntimeLibs(URL[] runtimeLibs) {
    this.runtimeLibs = runtimeLibs;
  }

  public ArtifactClassLoaderFilter getClassLoaderFilter() {
    return classLoaderFilter;
  }

  public void setClassLoaderFilter(ArtifactClassLoaderFilter classLoaderFilter) {
    this.classLoaderFilter = classLoaderFilter;
  }
}
