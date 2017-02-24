/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.runtime.module.deployment.impl.internal.plugin;

import static java.io.File.separator;
import static java.lang.String.format;
import static org.apache.commons.lang.ArrayUtils.toPrimitive;
import static org.apache.commons.lang.StringUtils.isEmpty;
import static org.mule.runtime.api.util.Preconditions.checkArgument;
import static org.mule.runtime.container.internal.ClasspathModuleDiscoverer.EXPORTED_CLASS_PACKAGES_PROPERTY;
import static org.mule.runtime.container.internal.ClasspathModuleDiscoverer.EXPORTED_RESOURCE_PROPERTY;
import static org.mule.runtime.core.config.bootstrap.ArtifactType.PLUGIN;
import static org.mule.runtime.core.util.FileUtils.unzip;
import static org.mule.runtime.core.util.JarUtils.loadFileContentFrom;
import static org.mule.runtime.core.util.PropertiesUtils.loadProperties;
import static org.mule.runtime.deployment.model.api.plugin.ArtifactPluginDescriptor.EXTENSION_BUNDLE_TYPE;
import static org.mule.runtime.deployment.model.api.plugin.ArtifactPluginDescriptor.MULE_ARTIFACT_FOLDER;
import static org.mule.runtime.deployment.model.api.plugin.ArtifactPluginDescriptor.MULE_PLUGIN_CLASSIFIER;
import static org.mule.runtime.deployment.model.api.plugin.ArtifactPluginDescriptor.MULE_PLUGIN_JSON;
import static org.mule.runtime.module.artifact.classloader.ArtifactClassLoaderFilterFactory.parseExportedResource;
import static org.mule.runtime.module.artifact.descriptor.BundleScope.COMPILE;
import org.mule.runtime.api.deployment.meta.MuleArtifactLoaderDescriptor;
import org.mule.runtime.api.deployment.meta.MulePluginModel;
import org.mule.runtime.api.deployment.persistence.MulePluginModelJsonSerializer;
import org.mule.runtime.api.exception.MuleRuntimeException;
import org.mule.runtime.container.api.MuleFoldersUtil;
import org.mule.runtime.core.registry.SpiServiceRegistry;
import org.mule.runtime.core.util.JarUtils;
import org.mule.runtime.deployment.model.api.plugin.ArtifactPluginDescriptor;
import org.mule.runtime.deployment.model.api.plugin.LoaderDescriber;
import org.mule.runtime.module.artifact.descriptor.ArtifactDescriptorCreateException;
import org.mule.runtime.module.artifact.descriptor.ArtifactDescriptorFactory;
import org.mule.runtime.module.artifact.descriptor.BundleDependency;
import org.mule.runtime.module.artifact.descriptor.BundleDescriptor;
import org.mule.runtime.module.artifact.descriptor.BundleDescriptor.Builder;
import org.mule.runtime.module.artifact.descriptor.BundleDescriptorLoader;
import org.mule.runtime.module.artifact.descriptor.ClassLoaderModel;
import org.mule.runtime.module.artifact.descriptor.ClassLoaderModel.ClassLoaderModelBuilder;
import org.mule.runtime.module.artifact.descriptor.ClassLoaderModelLoader;
import org.mule.runtime.module.artifact.descriptor.InvalidDescriptorLoaderException;
import org.mule.runtime.module.deployment.impl.internal.artifact.DescriptorLoaderRepository;
import org.mule.runtime.module.deployment.impl.internal.artifact.LoaderNotFoundException;
import org.mule.runtime.module.deployment.impl.internal.artifact.ServiceRegistryDescriptorLoaderRepository;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Properties;
import java.util.Set;

/**
 * Creates {@link ArtifactPluginDescriptor} instances
 */
public class ArtifactPluginDescriptorFactory implements ArtifactDescriptorFactory<ArtifactPluginDescriptor> {

  public static final String PLUGIN_PROPERTIES = "plugin.properties";
  public static final String PLUGIN_DEPENDENCIES = "plugin.dependencies";
  public static final String PLUGIN_BUNDLE = "plugin.bundle";
  public static final String BUNDLE_DESCRIPTOR_SEPARATOR = ":";

  private final DescriptorLoaderRepository descriptorLoaderRepository;

  /**
   * Creates a default factory
   */
  public ArtifactPluginDescriptorFactory() {
    this(new ServiceRegistryDescriptorLoaderRepository(new SpiServiceRegistry()));
  }

  /**
   * Creates a custom factory
   * 
   * @param descriptorLoaderRepository contains all the {@link ClassLoaderModelLoader} registered on the container. Non null
   */
  public ArtifactPluginDescriptorFactory(DescriptorLoaderRepository descriptorLoaderRepository) {
    checkArgument(descriptorLoaderRepository != null, "descriptorLoaderRepository cannot be null");
    this.descriptorLoaderRepository = descriptorLoaderRepository;
  }

  @Override
  public ArtifactPluginDescriptor create(File pluginJarFile) throws ArtifactDescriptorCreateException {
    try {
      checkArgument(pluginJarFile.getName().endsWith(".jar") || pluginJarFile.getName().endsWith(".zip"),
                    "provided file is not a plugin: " + pluginJarFile.getAbsolutePath());
      Optional<Byte[]> jsonDescriptorContentOptional =
          loadFileContentFrom(pluginJarFile, MULE_ARTIFACT_FOLDER + separator + MULE_PLUGIN_JSON);
      return jsonDescriptorContentOptional
          .map(jsonDescriptorContent -> loadFromJsonDescriptor(pluginJarFile, new String(toPrimitive(jsonDescriptorContent))))
          .orElseGet(() -> {
            Optional<Byte[]> pluginPropertiesFileContentOptional;
            try {
              pluginPropertiesFileContentOptional = loadFileContentFrom(pluginJarFile, PLUGIN_PROPERTIES);
              return loadFromPluginProperties(pluginJarFile, pluginPropertiesFileContentOptional);
            } catch (IOException e) {
              throw new MuleRuntimeException(e);
            }
          });
    } catch (ArtifactDescriptorCreateException e) {
      throw e;
    } catch (IOException e) {
      throw new ArtifactDescriptorCreateException(e);
    }
  }

  private ArtifactPluginDescriptor loadFromPluginProperties(File pluginJarFile,
                                                            Optional<Byte[]> pluginPropertiesContentOptional) {
    final ArtifactPluginDescriptor descriptor = new ArtifactPluginDescriptor(pluginJarFile.getName());

    BundleDescriptor defaultPluginBundleDescriptor = null;

    ClassLoaderModelBuilder classLoaderModelBuilder = new ClassLoaderModelBuilder();

    Properties props = null;
    if (pluginPropertiesContentOptional.isPresent()) {
      try {
        props = loadProperties(new ByteArrayInputStream(toPrimitive(pluginPropertiesContentOptional.get())));
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

    // TODO MULE-11849 Convert data weave into a service or server plugin - only DW is being bundled with dependencies
    if (props != null && props.get("module.name").equals("weave")) {
      try {
        File weavePluginFolder = new File(MuleFoldersUtil.getContainerAppPluginsFolder(), "weave-plugin");
        unzip(pluginJarFile, weavePluginFolder);
        File libsFolder = new File(weavePluginFolder, "lib");
        String[] libs = libsFolder.list((dir, name) -> name.endsWith(".jar"));
        for (String lib : libs) {
          classLoaderModelBuilder.containing(new File(libsFolder, lib).toURI().toURL());
        }
      } catch (Exception e) {
        throw new ArtifactDescriptorCreateException("Failed to create plugin descriptor " + pluginJarFile);
      }
    } else {
      loadUrlsToClassLoaderModelBuilder(pluginJarFile, classLoaderModelBuilder);
    }

    descriptor.setClassLoaderModel(classLoaderModelBuilder.build());
    if (defaultPluginBundleDescriptor == null) {
      defaultPluginBundleDescriptor = createDefaultPluginBundleDescriptor(descriptor.getName());
    }
    descriptor.setBundleDescriptor(defaultPluginBundleDescriptor);
    return descriptor;
  }

  /**
   * Given a {@code pluginFolder} as a starting point, it will add a the root folder of it and every JAR file {@link URL}s into
   * the {@link ClassLoaderModelBuilder}.
   *
   * @param pluginJarFile plugin's location
   * @param classLoaderModelBuilder builder of the {@link ClassLoaderModel}
   */
  private void loadUrlsToClassLoaderModelBuilder(File pluginJarFile, ClassLoaderModelBuilder classLoaderModelBuilder) {
    try {
      classLoaderModelBuilder.containing(pluginJarFile.toURI().toURL());
      List<URL> urls = JarUtils.getUrlsWithinJar(pluginJarFile, "lib");
      for (URL url : urls) {
        classLoaderModelBuilder.containing(url);
      }
    } catch (Exception e) {
      throw new ArtifactDescriptorCreateException("Failed to create plugin descriptor " + pluginJarFile);
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

  private ArtifactPluginDescriptor loadFromJsonDescriptor(File pluginJarFile, String jsonDescriptorContent) {
    final MulePluginModel mulePluginModel = getMulePluginJsonDescriber(jsonDescriptorContent);

    final ArtifactPluginDescriptor descriptor = new ArtifactPluginDescriptor(mulePluginModel.getName());
    // TODO this needs to go away
    // descriptor.setRootFolder(pluginFolder);

    mulePluginModel.getClassLoaderModelLoaderDescriptor().ifPresent(classLoaderModelLoaderDescriptor -> {
      descriptor.setClassLoaderModel(getClassLoaderModel(pluginJarFile, classLoaderModelLoaderDescriptor));
    });

    descriptor.setBundleDescriptor(getBundleDescriptor(pluginJarFile, mulePluginModel));

    mulePluginModel.getExtensionModelLoaderDescriptor().ifPresent(extensionModelDescriptor -> {
      final LoaderDescriber loaderDescriber = new LoaderDescriber(extensionModelDescriptor.getId());
      loaderDescriber.addAttributes(extensionModelDescriptor.getAttributes());
      descriptor.setExtensionModelDescriptorProperty(loaderDescriber);
    });

    return descriptor;
  }

  private BundleDescriptor getBundleDescriptor(File pluginFolder, MulePluginModel mulePluginModel) {
    BundleDescriptorLoader bundleDescriptorLoader;
    try {
      bundleDescriptorLoader =
          descriptorLoaderRepository.get(mulePluginModel.getBundleDescriptorLoader().getId(), PLUGIN,
                                         BundleDescriptorLoader.class);
    } catch (LoaderNotFoundException e) {
      throw new ArtifactDescriptorCreateException(invalidBundleDescriptorLoaderIdError(pluginFolder, mulePluginModel
          .getBundleDescriptorLoader()));
    }

    try {
      return bundleDescriptorLoader.load(pluginFolder, mulePluginModel.getBundleDescriptorLoader().getAttributes());
    } catch (InvalidDescriptorLoaderException e) {
      throw new ArtifactDescriptorCreateException(e);
    }
  }

  private ClassLoaderModel getClassLoaderModel(File pluginJarFile,
                                               MuleArtifactLoaderDescriptor classLoaderModelLoaderDescriptor) {
    ClassLoaderModelLoader classLoaderModelLoader;
    try {
      classLoaderModelLoader =
          descriptorLoaderRepository.get(classLoaderModelLoaderDescriptor.getId(), PLUGIN, ClassLoaderModelLoader.class);
    } catch (LoaderNotFoundException e) {
      throw new ArtifactDescriptorCreateException(invalidClassLoaderModelIdError(pluginJarFile,
                                                                                 classLoaderModelLoaderDescriptor));
    }

    final ClassLoaderModel classLoaderModel;
    try {
      classLoaderModel = classLoaderModelLoader.load(pluginJarFile, classLoaderModelLoaderDescriptor.getAttributes());
    } catch (InvalidDescriptorLoaderException e) {
      throw new ArtifactDescriptorCreateException(e);
    }
    return classLoaderModel;
  }

  protected static String invalidBundleDescriptorLoaderIdError(File pluginFolder,
                                                               MuleArtifactLoaderDescriptor bundleDescriptorLoader) {
    return format("The identifier '%s' for a bundle descriptor loader is not supported (error found while reading plugin '%s')",
                  bundleDescriptorLoader.getId(),
                  pluginFolder.getAbsolutePath());
  }

  protected static String invalidClassLoaderModelIdError(File pluginFolder,
                                                         MuleArtifactLoaderDescriptor classLoaderModelLoaderDescriptor) {
    return format("The identifier '%s' for a class loader model descriptor is not supported (error found while reading plugin '%s')",
                  classLoaderModelLoaderDescriptor.getId(),
                  pluginFolder.getAbsolutePath());
  }

  private MulePluginModel getMulePluginJsonDescriber(String jsonDescriptorContent) {
    return new MulePluginModelJsonSerializer().deserialize(jsonDescriptorContent);
  }
}
