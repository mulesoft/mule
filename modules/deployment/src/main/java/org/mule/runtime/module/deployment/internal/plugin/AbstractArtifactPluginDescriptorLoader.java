/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.module.deployment.internal.plugin;

import static java.lang.String.format;
import static org.apache.commons.lang.StringUtils.isEmpty;
import static org.mule.runtime.core.util.PropertiesUtils.loadProperties;
import static org.mule.runtime.module.artifact.classloader.ArtifactClassLoaderFilterFactory.parseExportedResource;
import static org.mule.runtime.module.artifact.classloader.DefaultArtifactClassLoaderFilter.EXPORTED_CLASS_PACKAGES_PROPERTY;
import static org.mule.runtime.module.artifact.classloader.DefaultArtifactClassLoaderFilter.EXPORTED_RESOURCE_PROPERTY;
import org.mule.runtime.deployment.model.api.plugin.ArtifactPluginDescriptor;
import org.mule.runtime.module.artifact.descriptor.ArtifactDescriptorCreateException;
import org.mule.runtime.module.artifact.descriptor.ClassLoaderModel.ClassLoaderModelBuilder;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Properties;
import java.util.Set;

/**
 * Loads a {@link ArtifactPluginDescriptor} from a file resource.
 *
 * @since 4.0
 */
public abstract class AbstractArtifactPluginDescriptorLoader {

  private static final String PLUGIN_PROPERTIES = "plugin.properties";
  private static final String PLUGIN_DEPENDENCIES = "plugin.dependencies";
  protected static final String CLASSES = "classes";
  protected static final String LIB = "lib";
  protected static final String JAR_EXTENSION = ".jar";

  protected final File pluginLocation;

  /**
   * Stores the reference to a plugin so that it can later constructs an {@link ArtifactPluginDescriptor} through the
   * {@link #load()} method.
   *
   * @param pluginLocation location of a plugin
   */
  public AbstractArtifactPluginDescriptorLoader(File pluginLocation) {
    this.pluginLocation = pluginLocation;
  }

  /**
   * @return plugin's name.
   */
  protected abstract String getName();

  /**
   * @return the {@link URL} targeting the /classes folder.
   * @throws MalformedURLException
   */
  protected abstract URL getClassesUrl() throws MalformedURLException;

  /**
   * @return the collection of {@link URL}s where each entry represents a JAR file within /lib folder.
   */
  protected abstract List<URL> getRuntimeLibs() throws MalformedURLException;

  /**
   * @param resource to look for
   * @return the InputStream of the resource to look for, or {@link Optional#empty()} otherwise.
   */
  protected abstract Optional<InputStream> loadResourceFrom(final String resource);

  /**
   * Just in case there were streams handling, we ensure after reading the plugin structure everything is closed.
   */
  protected void close() {}

  /**
   * Load a {@code ArtifactPluginDescriptor} from a file with the resource of an artifact plugin. Each subclass will hold the
   * responsibility to know how to introspect the current plugin's format (e.g.: ZIP, folder, etc.)
   *
   * @return the plugin {@code ArtifactPluginDescriptor}
   * @throws ArtifactDescriptorCreateException if there was a problem trying to read the artifact plugin.
   */
  public ArtifactPluginDescriptor load() {
    try {
      final ArtifactPluginDescriptor descriptor = new ArtifactPluginDescriptor(getName());

      descriptor.setRootFolder(pluginLocation);
      ClassLoaderModelBuilder classLoaderModelBuilder = new ClassLoaderModelBuilder();

      Optional<InputStream> propertiesInputStream = loadResourceFrom(PLUGIN_PROPERTIES);
      if (propertiesInputStream.isPresent()) {
        loadPropertiesFrom(propertiesInputStream.get(), classLoaderModelBuilder);
      }

      try {
        classLoaderModelBuilder.containing(getClassesUrl());
        getRuntimeLibs().forEach(classLoaderModelBuilder::containing);
      } catch (MalformedURLException e) {
        throw new ArtifactDescriptorCreateException(format("Failed to load URLs for '%s'", getName()), e);
      }
      descriptor.setClassLoaderModel(classLoaderModelBuilder.build());

      return descriptor;
    } finally {
      close();
    }
  }

  private void loadPropertiesFrom(InputStream inputStream, ClassLoaderModelBuilder classLoaderModelBuilder) {
    Properties props;
    try {
      props = loadProperties(inputStream);
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

  private Set<String> getPluginDependencies(String pluginDependencies) {
    Set<String> plugins = new HashSet<>();
    final String[] split = pluginDependencies.split(",");
    for (String plugin : split) {
      plugins.add(plugin.trim());
    }
    return plugins;
  }

}
