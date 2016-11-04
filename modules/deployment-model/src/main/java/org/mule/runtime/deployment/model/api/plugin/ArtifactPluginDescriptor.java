/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.deployment.model.api.plugin;

import org.mule.runtime.deployment.model.api.DeployableArtifactDescriptor;
import org.mule.runtime.module.artifact.classloader.ArtifactClassLoaderFilter;
import org.mule.runtime.module.artifact.classloader.DefaultArtifactClassLoaderFilter;

import java.net.URL;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class ArtifactPluginDescriptor extends DeployableArtifactDescriptor {

  public static final String PLUGIN_PROPERTIES = "plugin.properties";

  private ArtifactClassLoaderFilter classLoaderFilter = DefaultArtifactClassLoaderFilter.NULL_CLASSLOADER_FILTER;
  private URL runtimeClassesDir;
  private Set<String> pluginDependencies = new HashSet<>();
  private List<ArtifactPluginDescriptor> artifactPluginDescriptors = new ArrayList<>();

  /**
   * Creates a new artifact plugin descriptor
   *
   * @param name artifact plugin name. Non empty.
   */
  public ArtifactPluginDescriptor(String name) {
    super(name);
  }

  public ArtifactClassLoaderFilter getClassLoaderFilter() {
    return classLoaderFilter;
  }

  public void setClassLoaderFilter(ArtifactClassLoaderFilter classLoaderFilter) {
    this.classLoaderFilter = classLoaderFilter;
  }

  public URL getRuntimeClassesDir() {
    return runtimeClassesDir;
  }

  public void setRuntimeClassesDir(URL runtimeClassesDir) {
    this.runtimeClassesDir = runtimeClassesDir;
  }

  public Set<String> getPluginDependencies() {
    return pluginDependencies;
  }

  public void setPluginDependencies(Set<String> pluginDependencies) {
    this.pluginDependencies = pluginDependencies;
  }

  public List<ArtifactPluginDescriptor> getArtifactPluginDescriptors() {
    return artifactPluginDescriptors;
  }

  public void setArtifactPluginDescriptors(List<ArtifactPluginDescriptor> pluginDependencyDescriptors) {
    this.artifactPluginDescriptors = pluginDependencyDescriptors;
  }
}
