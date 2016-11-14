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
import static org.mule.runtime.deployment.model.api.plugin.ArtifactPluginDescriptor.EXTENSION_BUNDLE_TYPE;
import static org.mule.runtime.deployment.model.api.plugin.ArtifactPluginDescriptor.MULE_PLUGIN_CLASSIFIER;
import static org.mule.runtime.module.artifact.classloader.ArtifactClassLoaderFilterFactory.parseExportedResource;
import static org.mule.runtime.module.artifact.classloader.DefaultArtifactClassLoaderFilter.EXPORTED_CLASS_PACKAGES_PROPERTY;
import static org.mule.runtime.module.artifact.classloader.DefaultArtifactClassLoaderFilter.EXPORTED_RESOURCE_PROPERTY;
import static org.mule.runtime.module.artifact.descriptor.BundleScope.COMPILE;
import org.mule.runtime.deployment.model.api.plugin.ArtifactPluginDescriptor;
import org.mule.runtime.module.artifact.descriptor.ArtifactDescriptorCreateException;
import org.mule.runtime.module.artifact.descriptor.ArtifactDescriptorFactory;
import org.mule.runtime.module.artifact.descriptor.BundleDependency;
import org.mule.runtime.module.artifact.descriptor.BundleDescriptor;
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
  public static final String BUNDLE_DESCRIPTOR_SEPARATOR = ":";

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

    descriptor.setBundleDescriptor(createDefaultPluginBundleDescriptor(descriptor.getName()));

    return descriptor;
  }

  private Set<BundleDependency> getPluginDependencies(String pluginDependencies) {
    Set<BundleDependency> plugins = new HashSet<>();
    final String[] split = pluginDependencies.split(",");
    for (String plugin : split) {
      plugins.add(createBundleDescriptor(plugin));
    }
    return plugins;
  }

  private BundleDependency createBundleDescriptor(String bundle) {
    String[] bundleProperties = bundle.trim().split(BUNDLE_DESCRIPTOR_SEPARATOR);

    BundleDescriptor bundleDescriptor;

    if (isFullyDefinedBundle(bundleProperties)) {
      String groupId = bundleProperties[0];
      String artifactId = bundleProperties[1];
      String version = bundleProperties[2];
      bundleDescriptor = new BundleDescriptor.Builder().setArtifactId(artifactId).setGroupId(groupId).setVersion(version).build();
    } else if (isNameOnlyDefinedBundle(bundleProperties)) {
      // TODO(pablo.kraan): MULE-10966: remove this once extensions and plugins are properly migrated to the new model
      bundleDescriptor = createDefaultPluginBundleDescriptor(bundleProperties[0]);
    } else {
      throw new IllegalArgumentException(format("Cannot create a bundle descriptor from '%s': invalid descriptor format",
                                                bundle));
    }

    return new BundleDependency.Builder().setDescriptor(bundleDescriptor).setType(EXTENSION_BUNDLE_TYPE)
        .setClassifier(MULE_PLUGIN_CLASSIFIER).setScope(COMPILE).build();
  }

  private boolean isNameOnlyDefinedBundle(String[] bundleProperties) {
    return bundleProperties.length == 1;
  }

  private boolean isFullyDefinedBundle(String[] bundleProperties) {
    return bundleProperties.length == 3;
  }

  private BundleDescriptor createDefaultPluginBundleDescriptor(String pluginName) {
    return new BundleDescriptor.Builder().setArtifactId(pluginName).setGroupId("test").setVersion("1.0").build();
  }
}
