/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.runtime.module.deployment.impl.internal.artifact;

import static java.util.stream.Collectors.toList;
import static org.mule.runtime.api.util.Preconditions.checkArgument;
import org.mule.runtime.api.deployment.meta.MuleDeployableModel;
import org.mule.runtime.deployment.model.api.DeployableArtifactDescriptor;
import org.mule.runtime.deployment.model.api.plugin.ArtifactPluginDescriptor;
import org.mule.runtime.module.artifact.api.descriptor.BundleDependency;
import org.mule.runtime.module.artifact.api.descriptor.ClassLoaderModel;
import org.mule.runtime.module.artifact.api.descriptor.AbstractArtifactDescriptorFactory;
import org.mule.runtime.module.artifact.api.descriptor.DescriptorLoaderRepository;
import org.mule.runtime.module.deployment.impl.internal.plugin.ArtifactPluginDescriptorLoader;

import com.google.common.collect.ImmutableList;

import java.io.File;
import java.io.IOException;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public abstract class AbstractDeployableDescriptorFactory<M extends MuleDeployableModel, T extends DeployableArtifactDescriptor>
    extends AbstractArtifactDescriptorFactory<M, T> {

  protected static final String MULE_CONFIG_FILES_FOLDER = "mule";

  protected final ArtifactPluginDescriptorLoader artifactPluginDescriptorLoader;

  public AbstractDeployableDescriptorFactory(ArtifactPluginDescriptorLoader artifactPluginDescriptorLoader,
                                             DescriptorLoaderRepository descriptorLoaderRepository) {
    super(descriptorLoaderRepository);
    checkArgument(artifactPluginDescriptorLoader != null, "ApplicationPluginDescriptorFactory cannot be null");

    this.artifactPluginDescriptorLoader = artifactPluginDescriptorLoader;
  }

  @Override
  protected void doDescriptorConfig(M artifactModel, T descriptor, File artifactLocation) {
    descriptor.setArtifactLocation(artifactLocation);
    descriptor.setRedeploymentEnabled(artifactModel.isRedeploymentEnabled());
    List<String> configs = artifactModel.getConfigs();
    if (configs != null && !configs.isEmpty()) {
      descriptor.setConfigResources(configs.stream().map(configFile -> appendMuleFolder(configFile))
          .collect(toList()));
      List<File> configFiles = descriptor.getConfigResources()
          .stream()
          .map(config -> new File(artifactLocation, config)).collect(toList());
      descriptor.setConfigResourcesFile(configFiles.toArray(new File[configFiles.size()]));
      descriptor.setAbsoluteResourcePaths(configFiles.stream().map(configFile -> configFile.getAbsolutePath()).collect(toList())
          .toArray(new String[configFiles.size()]));
    } else {
      File configFile = new File(artifactLocation, appendMuleFolder(getDefaultConfigurationResource()));
      descriptor.setConfigResourcesFile(new File[] {configFile});
      descriptor.setConfigResources(ImmutableList.<String>builder().add(getDefaultConfigurationResourceLocation()).build());
      descriptor.setAbsoluteResourcePaths(new String[] {configFile.getAbsolutePath()});
    }

    try {
      descriptor.setPlugins(createArtifactPluginDescriptors(descriptor.getClassLoaderModel()));
    } catch (IOException e) {
      throw new IllegalStateException(e);
    }
  }

  protected abstract String getDefaultConfigurationResourceLocation();

  protected abstract String getDefaultConfigurationResource();

  private String appendMuleFolder(String configFile) {
    return MULE_CONFIG_FILES_FOLDER + File.separator + configFile;
  }

  private Set<ArtifactPluginDescriptor> createArtifactPluginDescriptors(ClassLoaderModel classLoaderModel)
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
}
