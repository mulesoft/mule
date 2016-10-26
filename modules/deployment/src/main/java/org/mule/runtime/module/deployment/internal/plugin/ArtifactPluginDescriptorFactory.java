/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.runtime.module.deployment.internal.plugin;

import static java.lang.String.format;
import static org.mule.runtime.api.util.Preconditions.checkArgument;
import org.mule.runtime.deployment.model.api.plugin.ArtifactPluginDescriptor;
import org.mule.runtime.deployment.model.api.plugin.classloadermodel.ClassloaderModel;
import org.mule.runtime.deployment.model.api.plugin.classloadermodel.MalformedClassloaderModelException;
import org.mule.runtime.deployment.model.api.plugin.dependency.ArtifactDependency;
import org.mule.runtime.deployment.model.api.plugin.loader.MalformedPluginException;
import org.mule.runtime.deployment.model.api.plugin.loader.Plugin;
import org.mule.runtime.deployment.model.internal.plugin.classloadermodel.ClassloaderModelLoader;
import org.mule.runtime.module.artifact.classloader.ClassLoaderFilterFactory;
import org.mule.runtime.module.artifact.descriptor.ArtifactDescriptorCreateException;
import org.mule.runtime.module.artifact.descriptor.ArtifactDescriptorFactory;

import java.io.File;
import java.util.Set;
import java.util.stream.Collectors;

public class ArtifactPluginDescriptorFactory implements ArtifactDescriptorFactory<ArtifactPluginDescriptor> {

  public static final String PLUGIN_PROPERTIES = "plugin.properties";
  public static final String PLUGIN_DEPENDENCIES = "plugin.dependencies";

  private final ClassLoaderFilterFactory classLoaderFilterFactory;

  /**
   * Creates a new instance
   * 
   * @param classLoaderFilterFactory creates classloader filters for the created descriptors. Not null.
   */
  public ArtifactPluginDescriptorFactory(ClassLoaderFilterFactory classLoaderFilterFactory) {
    checkArgument(classLoaderFilterFactory != null, "ClassLoaderFilterFactory cannot be null");

    this.classLoaderFilterFactory = classLoaderFilterFactory;
  }

  @Override
  public ArtifactPluginDescriptor create(File pluginLocation) throws ArtifactDescriptorCreateException {
    Plugin plugin;
    try {
      plugin = Plugin.from(pluginLocation);
    } catch (MalformedPluginException e) {
      throw new ArtifactDescriptorCreateException(format("There was an issue loading the plugin descriptor for %s",
                                                         pluginLocation.getName()),
                                                  e);
    }
    ClassloaderModel classloaderModel;
    try {
      classloaderModel = ClassloaderModelLoader.from(plugin);
    } catch (MalformedClassloaderModelException e) {
      throw new ArtifactDescriptorCreateException(format("There was an issue loading the deployment model for %s",
                                                         plugin.getPluginDescriptor().getName()),
                                                  e);
    }
    final ArtifactPluginDescriptor descriptor = new ArtifactPluginDescriptor(plugin.getPluginDescriptor().getName());
    //TODO MULE-10875 temporary workaround, neither /classes nor /lib/some.jar should be in the current descriptor
    if (classloaderModel.getRuntimeClasses().isPresent()) {
      descriptor.setRuntimeClassesDir(classloaderModel.getRuntimeClasses().get());
    }
    descriptor.setRuntimeLibs(classloaderModel.getRuntimeLibs());
    //TODO MULE-10875 temporary workaround until the factory is removed from the ArtifactPluginDescriptor. the classloader filter must be generated when needed and no beforehand
    descriptor.setClassLoaderFilter(classLoaderFilterFactory.create(String.join(",", classloaderModel.getExportedPackages()),
                                                                    String.join(",", classloaderModel.getExportedResources())));
    descriptor.setPluginDependencies(getPluginDependencies(classloaderModel.getDependencies()));
    return descriptor;
  }

  private Set<String> getPluginDependencies(Set<ArtifactDependency> dependencies) {
    return dependencies.stream()
        .filter(dependency -> "mule-plugin".equals(dependency.getClassifier()))
        .map(dependency -> dependency.getArtifactId()) //TODO MULE-10440 until the plugin.properties identifies all dependencies better, we will work with this
        .collect(Collectors.toSet());
  }
}
