/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.runtime.module.deployment.internal.plugin;

import static org.apache.commons.lang.StringUtils.isEmpty;
import static org.mule.runtime.core.util.PropertiesUtils.loadProperties;
import static org.mule.runtime.module.artifact.classloader.ArtifactClassLoaderFilterFactory.parseExportedResource;
import static org.mule.runtime.module.artifact.classloader.DefaultArtifactClassLoaderFilter.EXPORTED_CLASS_PACKAGES_PROPERTY;
import static org.mule.runtime.module.artifact.classloader.DefaultArtifactClassLoaderFilter.EXPORTED_RESOURCE_PROPERTY;
import org.mule.runtime.deployment.model.api.plugin.ArtifactPluginDescriptor;
import org.mule.runtime.module.artifact.descriptor.ArtifactDescriptorCreateException;
import org.mule.runtime.module.artifact.descriptor.ArtifactDescriptorFactory;
import org.mule.runtime.module.artifact.descriptor.ClassLoaderModel.ClassLoaderModelBuilder;

import java.io.File;
import java.io.FilenameFilter;
import java.io.IOException;
import java.net.MalformedURLException;
import java.util.HashSet;
import java.util.Properties;
import java.util.Set;

import org.apache.commons.io.filefilter.SuffixFileFilter;

public class ArtifactPluginDescriptorFactory implements ArtifactDescriptorFactory<ArtifactPluginDescriptor> {

  public static final String PLUGIN_PROPERTIES = "plugin.properties";
  public static final String PLUGIN_DEPENDENCIES = "plugin.dependencies";

  @Override
  public ArtifactPluginDescriptor create(File pluginFolder) throws ArtifactDescriptorCreateException {
    final String pluginName = pluginFolder.getName();
    final ArtifactPluginDescriptor descriptor = new ArtifactPluginDescriptor(pluginName);
    descriptor.setRootFolder(pluginFolder);

    ClassLoaderModelBuilder classLoaderModelBuilder = new ClassLoaderModelBuilder();

    final File pluginPropsFile = new File(pluginFolder, PLUGIN_PROPERTIES);
    if (pluginPropsFile.exists()) {
      Properties props;
      try {
        props = loadProperties(pluginPropsFile.toURI().toURL());
      } catch (IOException e) {
        throw new ArtifactDescriptorCreateException("Cannot read plugin.properties file", e);
      }

      String pluginDependencies = props.getProperty(PLUGIN_DEPENDENCIES);
      if (!isEmpty(pluginDependencies)) {
        classLoaderModelBuilder.dependingOn(getPluginDependencies(pluginDependencies));
      }

      classLoaderModelBuilder
          .exportingPackages(parseExportedResource(props.getProperty(EXPORTED_CLASS_PACKAGES_PROPERTY)))
          .exportingResources(parseExportedResource(props.getProperty(EXPORTED_RESOURCE_PROPERTY)));
    }

    try {
      classLoaderModelBuilder.containing(new File(pluginFolder, "classes").toURI().toURL());
      final File libDir = new File(pluginFolder, "lib");
      if (libDir.exists()) {
        final File[] jars = libDir.listFiles((FilenameFilter) new SuffixFileFilter(".jar"));
        for (int i = 0; i < jars.length; i++) {
          classLoaderModelBuilder.containing(jars[i].toURI().toURL());
        }
      }
    } catch (MalformedURLException e) {
      throw new ArtifactDescriptorCreateException("Failed to create plugin descriptor " + pluginFolder);
    }

    descriptor.setClassLoaderModel(classLoaderModelBuilder.build());

    return descriptor;
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
