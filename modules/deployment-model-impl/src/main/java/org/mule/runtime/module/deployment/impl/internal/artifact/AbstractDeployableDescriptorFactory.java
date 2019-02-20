/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.runtime.module.deployment.impl.internal.artifact;

import static java.lang.String.format;
import static java.util.stream.Collectors.toSet;
import static org.mule.runtime.api.util.Preconditions.checkArgument;
import static org.mule.runtime.container.api.MuleFoldersUtil.getMuleHomeFolder;
import org.mule.runtime.api.deployment.meta.MuleDeployableModel;
import org.mule.runtime.deployment.model.api.DeployableArtifactDescriptor;
import org.mule.runtime.deployment.model.api.plugin.ArtifactPluginDescriptor;
import org.mule.runtime.module.artifact.api.descriptor.AbstractArtifactDescriptorFactory;
import org.mule.runtime.module.artifact.api.descriptor.ArtifactDescriptorValidatorBuilder;
import org.mule.runtime.module.artifact.api.descriptor.BundleDependency;
import org.mule.runtime.module.artifact.api.descriptor.DescriptorLoaderRepository;
import org.mule.runtime.module.deployment.impl.internal.plugin.ArtifactPluginDescriptorLoader;

import com.google.common.collect.ImmutableSet;

import java.io.File;
import java.io.IOException;
import java.nio.file.Path;
import java.util.HashSet;
import java.util.Set;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public abstract class AbstractDeployableDescriptorFactory<M extends MuleDeployableModel, T extends DeployableArtifactDescriptor>
    extends AbstractArtifactDescriptorFactory<M, T> {

  private static final Logger LOGGER = LoggerFactory.getLogger(AbstractDeployableDescriptorFactory.class);

  protected final ArtifactPluginDescriptorLoader artifactPluginDescriptorLoader;

  public AbstractDeployableDescriptorFactory(ArtifactPluginDescriptorLoader artifactPluginDescriptorLoader,
                                             DescriptorLoaderRepository descriptorLoaderRepository,
                                             ArtifactDescriptorValidatorBuilder artifactDescriptorValidatorBuilder) {
    super(descriptorLoaderRepository, artifactDescriptorValidatorBuilder);
    checkArgument(artifactPluginDescriptorLoader != null, "ApplicationPluginDescriptorFactory cannot be null");

    this.artifactPluginDescriptorLoader = artifactPluginDescriptorLoader;
  }

  @Override
  protected void doDescriptorConfig(M artifactModel, T descriptor, File artifactLocation) {
    descriptor.setArtifactLocation(artifactLocation);
    descriptor.setRedeploymentEnabled(artifactModel.isRedeploymentEnabled());
    Set<String> configs = artifactModel.getConfigs();
    if (configs != null && !configs.isEmpty()) {
      descriptor.setConfigResources(configs.stream()
          .collect(toSet()));
    } else {
      descriptor.setConfigResources(ImmutableSet.<String>builder().add(getDefaultConfigurationResource()).build());
    }

    try {
      descriptor.setPlugins(createArtifactPluginDescriptors(descriptor));
    } catch (IOException e) {
      throw new IllegalStateException(e);
    }
    descriptor.setLogConfigFile(getLogConfigFile(artifactModel));
  }

  protected File getLogConfigFile(M artifactModel) {
    File logConfigFile = null;
    if (artifactModel.getLogConfigFile() != null) {
      Path logConfigFilePath = new File(artifactModel.getLogConfigFile()).toPath();
      Path muleHomeFolderPath = getMuleHomeFolder().toPath();
      logConfigFile = muleHomeFolderPath.resolve(logConfigFilePath).toFile();
    }
    return logConfigFile;
  }

  protected abstract String getDefaultConfigurationResource();

  private Set<ArtifactPluginDescriptor> createArtifactPluginDescriptors(T descriptor)
      throws IOException {
    Set<ArtifactPluginDescriptor> pluginDescriptors = new HashSet<>();
    for (BundleDependency bundlePluginDependency : descriptor.getClassLoaderModel().getDependencies()) {
      if (bundlePluginDependency.getDescriptor().isPlugin()) {
        if (bundlePluginDependency.getBundleUri() == null) {
          LOGGER
              .warn(format("Plugin '%s' is declared as 'provided' which means that it will not be added to the artifact's classpath",
                           bundlePluginDependency.getDescriptor()));
        } else {
          File pluginFile = new File(bundlePluginDependency.getBundleUri());
          pluginDescriptors
              .add(artifactPluginDescriptorLoader.load(pluginFile, bundlePluginDependency.getDescriptor(), descriptor));
        }
      }
    }
    return pluginDescriptors;
  }
}
