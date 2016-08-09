/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.runtime.module.launcher.plugin;

import static org.mule.runtime.core.util.Preconditions.checkArgument;
import static org.mule.runtime.core.util.PropertiesUtils.loadProperties;
import static org.mule.runtime.module.artifact.classloader.ArtifactClassLoaderFilter.EXPORTED_CLASS_PACKAGES_PROPERTY;
import static org.mule.runtime.module.artifact.classloader.ArtifactClassLoaderFilter.EXPORTED_RESOURCE_PROPERTY;
import org.mule.runtime.module.artifact.classloader.ArtifactClassLoaderFilter;
import org.mule.runtime.module.artifact.classloader.ArtifactClassLoaderFilterFactory;
import org.mule.runtime.module.artifact.descriptor.ArtifactDescriptorCreateException;
import org.mule.runtime.module.artifact.descriptor.ArtifactDescriptorFactory;

import java.io.File;
import java.io.FilenameFilter;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.Properties;

import org.apache.commons.io.filefilter.SuffixFileFilter;

public class ArtifactPluginDescriptorFactory implements ArtifactDescriptorFactory<ArtifactPluginDescriptor> {

  public static final String PROPERTY_LOADER_OVERRIDE = "loader.override";
  public static final String PLUGIN_PROPERTIES = "plugin.properties";

  private final ArtifactClassLoaderFilterFactory classLoaderFilterFactory;

  /**
   * Creates a new instance
   * 
   * @param classLoaderFilterFactory creates classloader filters for the created descriptors. Not null.
   */
  public ArtifactPluginDescriptorFactory(ArtifactClassLoaderFilterFactory classLoaderFilterFactory) {
    checkArgument(classLoaderFilterFactory != null, "ClassLoaderFilterFactory cannot be null");

    this.classLoaderFilterFactory = classLoaderFilterFactory;
  }

  @Override
  public ArtifactPluginDescriptor create(File pluginFolder) throws ArtifactDescriptorCreateException {
    final String pluginName = pluginFolder.getName();
    final ArtifactPluginDescriptor descriptor = new ArtifactPluginDescriptor();
    descriptor.setRootFolder(pluginFolder);
    descriptor.setName(pluginName);

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
}
