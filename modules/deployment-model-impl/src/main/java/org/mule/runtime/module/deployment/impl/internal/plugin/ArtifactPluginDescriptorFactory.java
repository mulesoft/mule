/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.runtime.module.deployment.impl.internal.plugin;

import static java.io.File.separator;
import static java.lang.String.format;
import static org.apache.commons.lang.StringUtils.isEmpty;
import static org.mule.runtime.core.util.PropertiesUtils.loadProperties;
import static org.mule.runtime.deployment.model.api.plugin.ArtifactPluginDescriptor.EXTENSION_BUNDLE_TYPE;
import static org.mule.runtime.deployment.model.api.plugin.ArtifactPluginDescriptor.MULE_ARTIFACT_FOLDER;
import static org.mule.runtime.deployment.model.api.plugin.ArtifactPluginDescriptor.MULE_PLUGIN_CLASSIFIER;
import static org.mule.runtime.deployment.model.api.plugin.ArtifactPluginDescriptor.MULE_PLUGIN_JSON;
import static org.mule.runtime.deployment.model.api.plugin.MavenClassLoaderConstants.MAVEN;
import static org.mule.runtime.module.artifact.classloader.ArtifactClassLoaderFilterFactory.parseExportedResource;
import static org.mule.runtime.module.artifact.classloader.DefaultArtifactClassLoaderFilter.EXPORTED_CLASS_PACKAGES_PROPERTY;
import static org.mule.runtime.module.artifact.classloader.DefaultArtifactClassLoaderFilter.EXPORTED_RESOURCE_PROPERTY;
import static org.mule.runtime.module.artifact.descriptor.BundleScope.COMPILE;
import org.apache.commons.io.IOUtils;
import org.apache.commons.io.filefilter.SuffixFileFilter;
import org.mule.runtime.api.deployment.meta.MuleArtifactLoaderDescriptor;
import org.mule.runtime.api.deployment.meta.MulePluginModel;
import org.mule.runtime.api.deployment.persistence.MulePluginModelJsonSerializer;
import org.mule.runtime.deployment.model.api.plugin.ArtifactPluginDescriptor;
import org.mule.runtime.deployment.model.api.plugin.LoaderDescriber;
import org.mule.runtime.module.artifact.descriptor.ArtifactDescriptorCreateException;
import org.mule.runtime.module.artifact.descriptor.ArtifactDescriptorFactory;
import org.mule.runtime.module.artifact.descriptor.BundleDependency;
import org.mule.runtime.module.artifact.descriptor.BundleDescriptor;
import org.mule.runtime.module.artifact.descriptor.BundleDescriptor.Builder;
import org.mule.runtime.module.artifact.descriptor.ClassLoaderModel;
import org.mule.runtime.module.artifact.descriptor.ClassLoaderModel.ClassLoaderModelBuilder;

import java.io.File;
import java.io.FileInputStream;
import java.io.FilenameFilter;
import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.HashSet;
import java.util.Properties;
import java.util.Set;

public class ArtifactPluginDescriptorFactory implements ArtifactDescriptorFactory<ArtifactPluginDescriptor> {

  public static final String PLUGIN_PROPERTIES = "plugin.properties";
  public static final String PLUGIN_DEPENDENCIES = "plugin.dependencies";
  public static final String PLUGIN_BUNDLE = "plugin.bundle";
  public static final String BUNDLE_DESCRIPTOR_SEPARATOR = ":";

  @Override
  public ArtifactPluginDescriptor create(File pluginFolder) throws ArtifactDescriptorCreateException {
    ArtifactPluginDescriptor artifactPluginDescriptor;
    final File mulePluginJsonFile = new File(pluginFolder, MULE_ARTIFACT_FOLDER + separator + MULE_PLUGIN_JSON);
    if (mulePluginJsonFile.exists()) {
      artifactPluginDescriptor = loadFromJsonDescriptor(pluginFolder, mulePluginJsonFile);
    } else {
      // TODO(fernandezlautaro): MULE-11092 once implemented, drop this.
      artifactPluginDescriptor = loadFromPluginProperties(pluginFolder);
    }
    return artifactPluginDescriptor;
  }

  private ArtifactPluginDescriptor loadFromPluginProperties(File pluginFolder) {
    final String pluginName = pluginFolder.getName();
    final ArtifactPluginDescriptor descriptor = new ArtifactPluginDescriptor(pluginName);
    BundleDescriptor defaultPluginBundleDescriptor = null;
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

      String pluginBundle = props.getProperty(PLUGIN_BUNDLE);
      if (!isEmpty(pluginBundle)) {
        defaultPluginBundleDescriptor = createBundleDescriptor(pluginBundle);
      }
    }
    loadUrlsToClassLoaderModelBuilder(pluginFolder, classLoaderModelBuilder);

    descriptor.setClassLoaderModel(classLoaderModelBuilder.build());
    if (defaultPluginBundleDescriptor == null) {
      defaultPluginBundleDescriptor = createDefaultPluginBundleDescriptor(descriptor.getName());
    }
    descriptor.setBundleDescriptor(defaultPluginBundleDescriptor);
    return descriptor;
  }

  /**
   * Given a {@code pluginFolder} as a starting point, it will add a /classes and every JAR file {@link URL}s into the
   * {@link ClassLoaderModelBuilder}.
   *
   * @param pluginFolder plugin's location
   * @param classLoaderModelBuilder builder of the {@link ClassLoaderModel}
   */
  private void loadUrlsToClassLoaderModelBuilder(File pluginFolder, ClassLoaderModelBuilder classLoaderModelBuilder) {
    try {
      classLoaderModelBuilder.containing(new File(pluginFolder, "classes").toURI().toURL());
      final File libDir = new File(pluginFolder, "lib");
      if (libDir.exists()) {
        final File[] jars = libDir.listFiles((FilenameFilter) new SuffixFileFilter(".jar"));
        for (File jar : jars) {
          classLoaderModelBuilder.containing(jar.toURI().toURL());
        }
      }
    } catch (MalformedURLException e) {
      throw new ArtifactDescriptorCreateException("Failed to create plugin descriptor " + pluginFolder);
    }
  }

  private Set<BundleDependency> getPluginDependencies(String pluginDependencies) {
    Set<BundleDependency> plugins = new HashSet<>();
    final String[] split = pluginDependencies.split(",");
    for (String plugin : split) {
      plugins.add(createBundleDependency(plugin));
    }
    return plugins;
  }

  private BundleDependency createBundleDependency(String bundle) {
    BundleDescriptor bundleDescriptor = createBundleDescriptor(bundle);
    return new BundleDependency.Builder().setDescriptor(bundleDescriptor).setScope(COMPILE).build();
  }

  private BundleDescriptor createBundleDescriptor(String bundle) {
    String[] bundleProperties = bundle.trim().split(BUNDLE_DESCRIPTOR_SEPARATOR);

    BundleDescriptor bundleDescriptor;

    if (isFullyDefinedBundle(bundleProperties)) {
      String groupId = bundleProperties[0];
      String artifactId = bundleProperties[1];
      String version = bundleProperties[2];
      bundleDescriptor = new Builder().setArtifactId(artifactId).setGroupId(groupId).setVersion(version)
          .setType(EXTENSION_BUNDLE_TYPE).setClassifier(MULE_PLUGIN_CLASSIFIER).build();
    } else if (isNameOnlyDefinedBundle(bundleProperties)) {
      // TODO(pablo.kraan): MULE-10966: remove this once extensions and plugins are properly migrated to the new model
      bundleDescriptor = createDefaultPluginBundleDescriptor(bundleProperties[0]);
    } else {
      throw new IllegalArgumentException(format("Cannot create a bundle descriptor from '%s': invalid descriptor format",
                                                bundle));
    }
    return bundleDescriptor;
  }

  private boolean isNameOnlyDefinedBundle(String[] bundleProperties) {
    return bundleProperties.length == 1;
  }

  private boolean isFullyDefinedBundle(String[] bundleProperties) {
    return bundleProperties.length == 3;
  }

  private BundleDescriptor createDefaultPluginBundleDescriptor(String pluginName) {
    return new Builder().setArtifactId(pluginName).setGroupId("test").setVersion("1.0")
        .setClassifier(MULE_PLUGIN_CLASSIFIER).setType(EXTENSION_BUNDLE_TYPE).build();
  }

  private ArtifactPluginDescriptor loadFromJsonDescriptor(File pluginFolder, File mulePluginJsonFile) {
    final MulePluginModel mulePluginModel = getMulePluginJsonDescriber(mulePluginJsonFile);

    final ArtifactPluginDescriptor descriptor = new ArtifactPluginDescriptor(mulePluginModel.getName());
    descriptor.setRootFolder(pluginFolder);

    mulePluginModel.getClassLoaderModelLoaderDescriptor().ifPresent(classLoaderModelLoaderDescriptor -> {
      // TODO(fernandezlautaro): MULE-11094 once implemented, the loaders for ClassLoaderModels should be treated dynamically
      if (!classLoaderModelLoaderDescriptor.getId().equals(MAVEN)) {
        throw new ArtifactDescriptorCreateException(format("The identifier '%s' for a class loader model descriptor is not supported (error found while reading plugin '%s')",
                                                           classLoaderModelLoaderDescriptor.getId(),
                                                           pluginFolder.getAbsolutePath()));
      }
      final MavenClassLoaderModelLoader mavenClassLoaderModelLoader = new MavenClassLoaderModelLoader();
      final ClassLoaderModel classLoaderModel =
          mavenClassLoaderModelLoader.loadClassLoaderModel(pluginFolder, classLoaderModelLoaderDescriptor.getAttributes());
      descriptor.setClassLoaderModel(classLoaderModel);
    });

    MuleArtifactLoaderDescriptor bundleDescriptorLoader = mulePluginModel.getBundleDescriptorLoader();
    if (!bundleDescriptorLoader.getId().equals(MAVEN)) {
      throw new ArtifactDescriptorCreateException(format("The identifier '%s' for a bundle descriptor loader is not supported (error found while reading plugin '%s')",
                                                         bundleDescriptorLoader.getId(),
                                                         pluginFolder.getAbsolutePath()));
    }
    descriptor.setBundleDescriptor(new MavenBundleDescriptorLoader().loadBundleDescriptor(pluginFolder));

    mulePluginModel.getExtensionModelLoaderDescriptor().ifPresent(extensionModelDescriptor -> {
      final LoaderDescriber loaderDescriber = new LoaderDescriber(extensionModelDescriptor.getId());
      loaderDescriber.addAttributes(extensionModelDescriptor.getAttributes());
      descriptor.setExtensionModelDescriptorProperty(loaderDescriber);
    });

    return descriptor;
  }

  private MulePluginModel getMulePluginJsonDescriber(File jsonFile) {
    try (InputStream stream = new FileInputStream(jsonFile)) {
      return new MulePluginModelJsonSerializer().deserialize(IOUtils.toString(stream));
    } catch (IOException e) {
      throw new IllegalArgumentException(format("Could not read extension describer on plugin '%s'", jsonFile.getAbsolutePath()),
                                         e);
    }
  }
}
