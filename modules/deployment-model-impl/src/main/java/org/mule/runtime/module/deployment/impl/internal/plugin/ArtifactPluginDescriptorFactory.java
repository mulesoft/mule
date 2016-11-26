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
import static org.apache.commons.lang.StringUtils.isNotBlank;
import static org.mule.runtime.api.util.Preconditions.checkArgument;
import static org.mule.runtime.core.util.PropertiesUtils.loadProperties;
import static org.mule.runtime.deployment.model.api.plugin.ArtifactPluginDescriptor.EXTENSION_BUNDLE_TYPE;
import static org.mule.runtime.deployment.model.api.plugin.ArtifactPluginDescriptor.META_INF;
import static org.mule.runtime.deployment.model.api.plugin.ArtifactPluginDescriptor.MULE_PLUGIN_CLASSIFIER;
import static org.mule.runtime.deployment.model.api.plugin.ArtifactPluginDescriptor.MULE_PLUGIN_JSON;
import static org.mule.runtime.deployment.model.api.plugin.ArtifactPluginDescriptor.MULE_PLUGIN_POM;
import static org.mule.runtime.module.artifact.classloader.ArtifactClassLoaderFilterFactory.parseExportedResource;
import static org.mule.runtime.module.artifact.classloader.DefaultArtifactClassLoaderFilter.EXPORTED_CLASS_PACKAGES_PROPERTY;
import static org.mule.runtime.module.artifact.classloader.DefaultArtifactClassLoaderFilter.EXPORTED_RESOURCE_PROPERTY;
import static org.mule.runtime.module.artifact.descriptor.BundleScope.COMPILE;
import org.mule.runtime.api.deployment.meta.MulePluginLoaderDescriptor;
import org.mule.runtime.api.deployment.meta.MulePluginModel;
import org.mule.runtime.api.deployment.persistence.MulePluginModelJsonSerializer;
import org.mule.runtime.deployment.model.api.plugin.ArtifactPluginDescriptor;
import org.mule.runtime.deployment.model.api.plugin.LoaderDescriber;
import org.mule.runtime.module.artifact.descriptor.ArtifactDescriptorCreateException;
import org.mule.runtime.module.artifact.descriptor.ArtifactDescriptorFactory;
import org.mule.runtime.module.artifact.descriptor.BundleDependency;
import org.mule.runtime.module.artifact.descriptor.BundleDescriptor;
import org.mule.runtime.module.artifact.descriptor.BundleDescriptor.Builder;
import org.mule.runtime.module.artifact.descriptor.BundleScope;
import org.mule.runtime.module.artifact.descriptor.ClassLoaderModel;
import org.mule.runtime.module.artifact.descriptor.ClassLoaderModel.ClassLoaderModelBuilder;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileReader;
import java.io.FilenameFilter;
import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Properties;
import java.util.Set;
import java.util.stream.Collectors;

import org.apache.commons.io.IOUtils;
import org.apache.commons.io.filefilter.SuffixFileFilter;
import org.apache.maven.model.Dependency;
import org.apache.maven.model.Model;
import org.apache.maven.model.io.xpp3.MavenXpp3Reader;
import org.codehaus.plexus.util.xml.pull.XmlPullParserException;

public class ArtifactPluginDescriptorFactory implements ArtifactDescriptorFactory<ArtifactPluginDescriptor> {

  public static final String PLUGIN_PROPERTIES = "plugin.properties";
  public static final String PLUGIN_DEPENDENCIES = "plugin.dependencies";
  public static final String BUNDLE_DESCRIPTOR_SEPARATOR = ":";

  public static final String MAVEN_ID_CLASSLOADER_MODEL_DESCRIPTOR = "maven";
  public static final String EXPORTED_PACKAGES = "exportedPackages";
  public static final String EXPORTED_RESOURCES = "exportedResources";

  @Override
  public ArtifactPluginDescriptor create(File pluginFolder) throws ArtifactDescriptorCreateException {
    ArtifactPluginDescriptor artifactPluginDescriptor;
    final File mulePluginJsonFile = new File(pluginFolder, META_INF + separator + MULE_PLUGIN_JSON);
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

    loadUrlsToClassLoaderModelBuilder(pluginFolder, classLoaderModelBuilder);

    descriptor.setClassLoaderModel(classLoaderModelBuilder.build());
    descriptor.setBundleDescriptor(createDefaultPluginBundleDescriptor(descriptor.getName()));
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
      bundleDescriptor = new Builder().setArtifactId(artifactId).setGroupId(groupId).setVersion(version)
          .setType(EXTENSION_BUNDLE_TYPE).setClassifier(MULE_PLUGIN_CLASSIFIER).build();
    } else if (isNameOnlyDefinedBundle(bundleProperties)) {
      // TODO(pablo.kraan): MULE-10966: remove this once extensions and plugins are properly migrated to the new model
      bundleDescriptor = createDefaultPluginBundleDescriptor(bundleProperties[0]);
    } else {
      throw new IllegalArgumentException(format("Cannot create a bundle descriptor from '%s': invalid descriptor format",
                                                bundle));
    }

    return new BundleDependency.Builder().setDescriptor(bundleDescriptor).setScope(COMPILE).build();
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

    descriptor.setClassLoaderModel(loadClassLoaderModel(pluginFolder, descriptor, mulePluginModel));

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

  private ClassLoaderModel loadClassLoaderModel(File pluginFolder, ArtifactPluginDescriptor descriptor,
                                                MulePluginModel mulePluginModel) {
    ClassLoaderModelBuilder classLoaderModelBuilder = new ClassLoaderModelBuilder();

    loadUrlsToClassLoaderModelBuilder(pluginFolder, classLoaderModelBuilder);

    mulePluginModel.getClassLoaderModelLoaderDescriptor().ifPresent(classLoaderModel -> {
      loadClassLoaderModelBuilderFromDescriptor(pluginFolder, descriptor, classLoaderModelBuilder, classLoaderModel);
    });

    return classLoaderModelBuilder.build();
  }

  private void loadClassLoaderModelBuilderFromDescriptor(File pluginFolder, ArtifactPluginDescriptor descriptor,
                                                         ClassLoaderModelBuilder classLoaderModelBuilder,
                                                         MulePluginLoaderDescriptor classLoaderModel) {
    // TODO(fernandezlautaro): MULE-11094 once implemented, the loaders for ClassLoaderModels should be treated dynamically
    if (!classLoaderModel.getId().equals(MAVEN_ID_CLASSLOADER_MODEL_DESCRIPTOR)) {
      throw new ArtifactDescriptorCreateException(format("The identifier '%s' for a class loader model descriptor is not supported (error found while reading plugin '%s')",
                                                         classLoaderModel.getId(), descriptor.getName()));
    }
    final File mulePluginPom = new File(pluginFolder, META_INF + separator + MULE_PLUGIN_POM);
    if (!mulePluginPom.exists()) {
      throw new ArtifactDescriptorCreateException(format("The identifier '%s' requires the file '%s' (error found while reading plugin '%s')",
                                                         MAVEN_ID_CLASSLOADER_MODEL_DESCRIPTOR, mulePluginPom.getName(),
                                                         descriptor.getName()));
    }

    classLoaderModelBuilder
        .exportingPackages(new HashSet<>(getAttribute(classLoaderModel, EXPORTED_PACKAGES)))
        .exportingResources(new HashSet<>(getAttribute(classLoaderModel, EXPORTED_RESOURCES)));


    Model model = getPomModel(pluginFolder, mulePluginPom);
    final Set<BundleDependency> plugins = model.getDependencies().stream()
        .filter(dependency -> isMulePlugin(dependency))
        .map(dependency -> {
          final Builder bundleDescriptorBuilder = new Builder()
              .setArtifactId(dependency.getArtifactId())
              .setGroupId(dependency.getGroupId())
              .setVersion(dependency.getVersion())
              .setType(dependency.getType());
          if (isNotBlank(dependency.getClassifier())) {
            bundleDescriptorBuilder.setClassifier(dependency.getClassifier());
          }

          return new BundleDependency.Builder()
              .setDescriptor(bundleDescriptorBuilder.build())
              .setScope(BundleScope.valueOf(dependency.getScope().toUpperCase()))
              .build();
        })
        .collect(Collectors.toSet());
    classLoaderModelBuilder.dependingOn(plugins);

    final BundleDescriptor bundleDescriptor = new Builder()
        .setArtifactId(model.getArtifactId())
        .setGroupId(model.getGroupId())
        .setVersion(model.getVersion())
        .setType(EXTENSION_BUNDLE_TYPE)
        .setClassifier(MULE_PLUGIN_CLASSIFIER)
        .build();
    descriptor.setBundleDescriptor(bundleDescriptor);
  }

  /**
   * Dependency validator to keep those that are Mule plugins.
   * TODO(fernandezlautaro): MULE-11095 We will keep only Mule plugins dependencies or org.mule.runtime.deployment.model.internal.plugin.BundlePluginDependenciesResolver.getArtifactsWithDependencies() will fail looking them up.
   *
   * @param dependency to validate
   * @return true if the {@link Dependency#getClassifier()} is {@link ArtifactPluginDescriptor#MULE_PLUGIN_CLASSIFIER}, false otherwise
   */
  private boolean isMulePlugin(Dependency dependency) {
    return MULE_PLUGIN_CLASSIFIER.equals(dependency.getClassifier());
  }

  private Model getPomModel(File pluginFolder, File mulePluginPom) {
    MavenXpp3Reader reader = new MavenXpp3Reader();
    Model model;
    try {
      model = reader.read(new FileReader(mulePluginPom));
    } catch (IOException | XmlPullParserException e) {
      throw new ArtifactDescriptorCreateException(format("There was an issue reading '%s' for the plugin '%s'",
                                                         mulePluginPom.getName(), pluginFolder.getAbsolutePath()),
                                                  e);
    }
    return model;
  }

  private List<String> getAttribute(MulePluginLoaderDescriptor classLoaderModel, String attribute) {
    final Object attributeObject = classLoaderModel.getAttributes().getOrDefault(attribute, new ArrayList<String>());
    checkArgument(attributeObject instanceof List, format("The '%s' attribute must be of '%s', found '%s'", attribute,
                                                          List.class.getName(), attributeObject.getClass().getName()));
    return (List<String>) attributeObject;
  }

}
