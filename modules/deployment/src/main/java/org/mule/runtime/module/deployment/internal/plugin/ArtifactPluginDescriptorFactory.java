/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.runtime.module.deployment.internal.plugin;

import static java.lang.String.format;
import static org.apache.commons.lang.StringUtils.isEmpty;
import static org.mule.runtime.core.util.Preconditions.checkArgument;
import static org.mule.runtime.core.util.PropertiesUtils.loadProperties;
import static org.mule.runtime.module.artifact.classloader.DefaultArtifactClassLoaderFilter.EXPORTED_CLASS_PACKAGES_PROPERTY;
import static org.mule.runtime.module.artifact.classloader.DefaultArtifactClassLoaderFilter.EXPORTED_RESOURCE_PROPERTY;

import org.mule.runtime.deployment.model.api.plugin.moved.MalformedPluginException;
import org.mule.runtime.deployment.model.api.plugin.moved.Plugin;
import org.mule.runtime.deployment.model.api.plugin.moved.deployment.DeploymentModel;
import org.mule.runtime.deployment.model.api.plugin.moved.deployment.MalformedDeploymentModelException;
import org.mule.runtime.deployment.model.internal.plugin.moved.deployment.DeploymentModelLoader;
import org.mule.runtime.module.artifact.classloader.ArtifactClassLoaderFilter;
import org.mule.runtime.module.artifact.classloader.ClassLoaderFilterFactory;
import org.mule.runtime.module.artifact.descriptor.ArtifactDescriptorCreateException;
import org.mule.runtime.module.artifact.descriptor.ArtifactDescriptorFactory;
import org.mule.runtime.deployment.model.api.plugin.ArtifactPluginDescriptor;

import java.io.File;
import java.io.FilenameFilter;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.HashSet;
import java.util.Properties;
import java.util.Set;

import org.apache.commons.io.filefilter.SuffixFileFilter;

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
  public ArtifactPluginDescriptor create(File pluginFolder) throws ArtifactDescriptorCreateException {
    //TODO MULE-10785 the PluginDescriptor should be the new ArtifactPluginDescriptor commented out as it forces to be a META-INF/mule-plugin.json file
    //DeploymentModel deploymentModel = getDeploymentModel(pluginFolder);

    final String pluginName = pluginFolder.getName();
    final ArtifactPluginDescriptor descriptor = new ArtifactPluginDescriptor(pluginName);
    descriptor.setRootFolder(pluginFolder);

    final File pluginPropsFile = new File(pluginFolder, PLUGIN_PROPERTIES);
    if (pluginPropsFile.exists()) {
      Properties props;
      try {
        props = loadProperties(pluginPropsFile.toURI().toURL());
      } catch (IOException e) {
        throw new ArtifactDescriptorCreateException("Cannot read plugin.properties file", e);
      }

      String exportedClasses = props.getProperty(EXPORTED_CLASS_PACKAGES_PROPERTY);
      String exportedResources = props.getProperty(EXPORTED_RESOURCE_PROPERTY);

      final ArtifactClassLoaderFilter classLoaderFilter = classLoaderFilterFactory.create(exportedClasses, exportedResources);
      descriptor.setClassLoaderFilter(classLoaderFilter);

      String pluginDependencies = props.getProperty(PLUGIN_DEPENDENCIES);
      if (!isEmpty(pluginDependencies)) {
        descriptor.setPluginDependencies(getPluginDependencies(pluginDependencies));
      }
    }

    try {
      descriptor.setRuntimeClassesDir(new File(pluginFolder, "classes").toURI().toURL());
      final File libDir = new File(pluginFolder, "lib");
      URL[] urls = new URL[0];
      if (libDir.exists()) {
        final File[] jars = libDir.listFiles((FilenameFilter) new SuffixFileFilter(".jar"));
        urls = new URL[jars.length];
        for (int i = 0; i < jars.length; i++) {
          urls[i] = jars[i].toURI().toURL();
        }
      }
      descriptor.setRuntimeLibs(urls);
    } catch (MalformedURLException e) {
      throw new ArtifactDescriptorCreateException("Failed to create plugin descriptor " + pluginFolder);
    }

    return descriptor;
  }

  private DeploymentModel getDeploymentModel(File pluginFolder) {
    Plugin plugin;
    try {
      if (pluginFolder.getName().endsWith("zip")) {
        plugin = Plugin.fromZip(pluginFolder);
      } else {
        plugin = Plugin.fromFolder(pluginFolder);
      }
    } catch (MalformedPluginException e) {
      throw new ArtifactDescriptorCreateException(format("There was an issue loading the plugin descriptor for %s",
                                                         pluginFolder.getName()),
                                                  e);
    }
    DeploymentModel deploymentModel;
    try {
      deploymentModel = DeploymentModelLoader.from(plugin);
    } catch (MalformedDeploymentModelException e) {
      throw new ArtifactDescriptorCreateException(format("There was an issue loading the deployment model for %s",
                                                         plugin.getPluginDescriptor().getName()),
                                                  e);
    }
    return deploymentModel;
  }

  private Set<String> getPluginDependencies(String pluginDependencies) {
    Set<String> plugins = new HashSet<>();
    final String[] split = pluginDependencies.split(",");
    for (String plugin : split) {
      plugins.add(plugin.trim());
    }
    return plugins;
  }
}
