/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.runtime.module.deployment.impl.internal.artifact;

import static java.io.File.separator;
import static java.lang.String.format;
import static java.util.stream.Collectors.toList;
import static org.mule.runtime.api.util.Preconditions.checkArgument;
import static org.mule.runtime.core.api.config.bootstrap.ArtifactType.APP;
import static org.mule.runtime.deployment.model.api.plugin.ArtifactPluginDescriptor.MULE_ARTIFACT_FOLDER;
import org.mule.runtime.api.deployment.meta.MuleArtifactLoaderDescriptor;
import org.mule.runtime.api.deployment.meta.MuleDeployableModel;
import org.mule.runtime.api.meta.MuleVersion;
import org.mule.runtime.core.api.config.bootstrap.ArtifactType;
import org.mule.runtime.deployment.model.api.DeployableArtifactDescriptor;
import org.mule.runtime.deployment.model.api.plugin.ArtifactPluginDescriptor;
import org.mule.runtime.module.artifact.descriptor.ArtifactDescriptorCreateException;
import org.mule.runtime.module.artifact.descriptor.ArtifactDescriptorFactory;
import org.mule.runtime.module.artifact.descriptor.BundleDependency;
import org.mule.runtime.module.artifact.descriptor.BundleDescriptor;
import org.mule.runtime.module.artifact.descriptor.BundleDescriptorLoader;
import org.mule.runtime.module.artifact.descriptor.ClassLoaderModel;
import org.mule.runtime.module.artifact.descriptor.ClassLoaderModelLoader;
import org.mule.runtime.module.artifact.descriptor.InvalidDescriptorLoaderException;
import org.mule.runtime.module.deployment.impl.internal.plugin.ArtifactPluginDescriptorLoader;

import com.google.common.collect.ImmutableList;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public abstract class AbstractDeployableDescriptorFactory<M extends MuleDeployableModel, T extends DeployableArtifactDescriptor>
    implements ArtifactDescriptorFactory<T> {

  protected static final String MULE_CONFIG_FILES_FOLDER = "mule";

  protected final ArtifactPluginDescriptorLoader artifactPluginDescriptorLoader;
  protected final DescriptorLoaderRepository descriptorLoaderRepository;


  public AbstractDeployableDescriptorFactory(ArtifactPluginDescriptorLoader artifactPluginDescriptorLoader,
                                             DescriptorLoaderRepository descriptorLoaderRepository) {
    checkArgument(artifactPluginDescriptorLoader != null, "ApplicationPluginDescriptorFactory cannot be null");

    this.artifactPluginDescriptorLoader = artifactPluginDescriptorLoader;
    this.descriptorLoaderRepository = descriptorLoaderRepository;
  }

  @Override
  public T create(File artifactFolder) throws ArtifactDescriptorCreateException {
    T artifactDescriptor;
    final File artifactJsonFile = new File(artifactFolder, MULE_ARTIFACT_FOLDER + separator + getDescriptorFileName());
    if (!artifactJsonFile.exists()) {
      throw new IllegalStateException("Artifact descriptor does not exists: " + artifactJsonFile);
    }
    artifactDescriptor = loadFromJsonDescriptor(artifactFolder, artifactJsonFile);

    return artifactDescriptor;
  }

  protected abstract String getDescriptorFileName();

  protected T loadFromJsonDescriptor(File artifactFolder, File artifactJsonDescriptor) {
    final M artifactModel = getArtifactJsonDescriber(artifactJsonDescriptor);

    final T descriptor = createArtifactDescriptor(artifactFolder.getName());
    descriptor.setArtifactLocation(artifactFolder);
    descriptor.setRootFolder(artifactFolder);
    descriptor.setBundleDescriptor(getBundleDescriptor(artifactFolder, artifactModel));
    descriptor.setMinMuleVersion(new MuleVersion(artifactModel.getMinMuleVersion()));
    descriptor.setRedeploymentEnabled(artifactModel.isRedeploymentEnabled());
    doDescriptorConfig(artifactModel, descriptor);

    List<String> configs = artifactModel.getConfigs();
    if (configs != null && !configs.isEmpty()) {
      descriptor.setConfigResources(configs.stream().map(configFile -> appendMuleFolder(configFile))
          .collect(toList()));
      List<File> configFiles = descriptor.getConfigResources()
          .stream()
          .map(config -> new File(artifactFolder, config)).collect(toList());
      descriptor.setConfigResourcesFile(configFiles.toArray(new File[configFiles.size()]));
      descriptor.setAbsoluteResourcePaths(configFiles.stream().map(configFile -> configFile.getAbsolutePath()).collect(toList())
          .toArray(new String[configFiles.size()]));
    } else {
      File configFile = new File(artifactFolder, appendMuleFolder(getDefaultConfigurationResource()));
      descriptor.setConfigResourcesFile(new File[] {configFile});
      descriptor.setConfigResources(ImmutableList.<String>builder().add(getDefaultConfigurationResourceLocation()).build());
      descriptor.setAbsoluteResourcePaths(new String[] {configFile.getAbsolutePath()});
    }

    artifactModel.getClassLoaderModelLoaderDescriptor().ifPresent(classLoaderModelLoaderDescriptor -> {
      ClassLoaderModel classLoaderModel = getClassLoaderModel(artifactFolder, classLoaderModelLoaderDescriptor);
      descriptor.setClassLoaderModel(classLoaderModel);

      try {
        descriptor.setPlugins(createArtifactPluginDescriptors(classLoaderModel));
      } catch (IOException e) {
        throw new IllegalStateException(e);
      }
    });
    return descriptor;
  }

  protected abstract void doDescriptorConfig(M artifactModel, T descriptor);

  protected abstract T createArtifactDescriptor(String name);

  protected abstract String getDefaultConfigurationResourceLocation();

  protected abstract String getDefaultConfigurationResource();

  protected static String invalidClassLoaderModelIdError(File pluginFolder,
                                                         MuleArtifactLoaderDescriptor classLoaderModelLoaderDescriptor) {
    return format("The identifier '%s' for a class loader model descriptor is not supported (error found while reading plugin '%s')",
                  classLoaderModelLoaderDescriptor.getId(),
                  pluginFolder.getAbsolutePath());

  }

  protected static String invalidBundleDescriptorLoaderIdError(File pluginFolder,
                                                               MuleArtifactLoaderDescriptor bundleDescriptorLoader) {
    return format("The identifier '%s' for a bundle descriptor loader is not supported (error found while reading plugin '%s')",
                  bundleDescriptorLoader.getId(),
                  pluginFolder.getAbsolutePath());
  }


  protected BundleDescriptor getBundleDescriptor(File appFolder, M muleDomainModel) {
    BundleDescriptorLoader bundleDescriptorLoader;
    try {
      bundleDescriptorLoader =
          descriptorLoaderRepository.get(muleDomainModel.getBundleDescriptorLoader().getId(), APP,
                                         BundleDescriptorLoader.class);
    } catch (LoaderNotFoundException e) {
      throw new ArtifactDescriptorCreateException(invalidBundleDescriptorLoaderIdError(appFolder, muleDomainModel
          .getBundleDescriptorLoader()));
    }

    try {
      return bundleDescriptorLoader.load(appFolder, muleDomainModel.getBundleDescriptorLoader().getAttributes(), APP);
    } catch (InvalidDescriptorLoaderException e) {
      throw new ArtifactDescriptorCreateException(e);
    }
  }

  protected String appendMuleFolder(String configFile) {
    return MULE_CONFIG_FILES_FOLDER + File.separator + configFile;
  }

  protected ClassLoaderModel getClassLoaderModel(File domainFolder,
                                                 MuleArtifactLoaderDescriptor classLoaderModelLoaderDescriptor) {
    ClassLoaderModelLoader classLoaderModelLoader;
    try {
      classLoaderModelLoader =
          descriptorLoaderRepository.get(classLoaderModelLoaderDescriptor.getId(), APP, ClassLoaderModelLoader.class);
    } catch (LoaderNotFoundException e) {
      throw new ArtifactDescriptorCreateException(invalidClassLoaderModelIdError(domainFolder,
                                                                                 classLoaderModelLoaderDescriptor));
    }

    final ClassLoaderModel classLoaderModel;
    try {
      classLoaderModel = classLoaderModelLoader.load(domainFolder, classLoaderModelLoaderDescriptor.getAttributes(),
                                                     getArtifactType());
    } catch (InvalidDescriptorLoaderException e) {
      throw new ArtifactDescriptorCreateException(e);
    }
    return classLoaderModel;
  }

  protected abstract ArtifactType getArtifactType();

  protected Set<ArtifactPluginDescriptor> createArtifactPluginDescriptors(ClassLoaderModel classLoaderModel)
      throws IOException {
    Set<ArtifactPluginDescriptor> pluginDescriptors = new HashSet<>();
    for (BundleDependency bundleDependency : classLoaderModel.getDependencies()) {
      if (bundleDependency.getDescriptor().isPlugin()) {
        File pluginFile = new File(bundleDependency.getBundleUri());
        pluginDescriptors.add(artifactPluginDescriptorLoader.load(pluginFile));
      }
    }
    return pluginDescriptors;
  }

  protected M getArtifactJsonDescriber(File jsonFile) {
    try (InputStream stream = new FileInputStream(jsonFile)) {
      return deserializeArtifactModel(stream);
    } catch (IOException e) {
      throw new IllegalArgumentException(format("Could not read extension describer on artifact '%s'",
                                                jsonFile.getAbsolutePath()),
                                         e);
    }
  }

  protected abstract M deserializeArtifactModel(InputStream stream) throws IOException;
}
