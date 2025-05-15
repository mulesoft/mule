/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.module.artifact.activation.internal.deployable;

import static org.mule.runtime.container.api.MuleFoldersUtil.getMuleHomeFolder;

import static java.lang.String.format;
import static java.util.Collections.emptySet;
import static java.util.Collections.singleton;
import static java.util.Optional.empty;
import static java.util.Optional.of;

import org.mule.runtime.api.deployment.meta.MuleArtifactLoaderDescriptor;
import org.mule.runtime.api.deployment.meta.MuleDeployableModel;
import org.mule.runtime.api.deployment.meta.MulePluginModel;
import org.mule.runtime.module.artifact.activation.api.deployable.DeployableProjectModel;
import org.mule.runtime.module.artifact.activation.api.plugin.PluginDescriptorResolver;
import org.mule.runtime.module.artifact.activation.api.plugin.PluginModelResolver;
import org.mule.runtime.module.artifact.activation.internal.descriptor.AbstractArtifactDescriptorFactory;
import org.mule.runtime.module.artifact.activation.internal.plugin.ArtifactPluginDescriptorFactory;
import org.mule.runtime.module.artifact.activation.internal.plugin.BundlePluginDependenciesResolver;
import org.mule.runtime.module.artifact.api.descriptor.ArtifactDescriptorValidatorBuilder;
import org.mule.runtime.module.artifact.api.descriptor.ArtifactPluginDescriptor;
import org.mule.runtime.module.artifact.api.descriptor.BundleDependency;
import org.mule.runtime.module.artifact.api.descriptor.BundleDescriptor;
import org.mule.runtime.module.artifact.api.descriptor.ClassLoaderConfiguration;
import org.mule.runtime.module.artifact.api.descriptor.DeployableArtifactDescriptor;

import java.io.File;
import java.nio.file.Path;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Properties;
import java.util.Set;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Base class to create descriptors of artifacts that are deployable.
 *
 * @param <M> type of the artifact model that owns the descriptor.
 * @param <T> type of descriptor being created.
 */
public abstract class AbstractDeployableArtifactDescriptorFactory<M extends MuleDeployableModel, T extends DeployableArtifactDescriptor>
    extends AbstractArtifactDescriptorFactory<M, T> {

  private static final Logger LOGGER = LoggerFactory.getLogger(AbstractDeployableArtifactDescriptorFactory.class);

  private final DeployableProjectModel deployableProjectModel;
  private final Optional<Properties> deploymentProperties;
  private final PluginModelResolver pluginModelResolver;
  private final PluginDescriptorResolver pluginDescriptorResolver;
  private final BundlePluginDependenciesResolver pluginDependenciesResolver;

  public AbstractDeployableArtifactDescriptorFactory(DeployableProjectModel deployableProjectModel,
                                                     Map<String, String> deploymentProperties,
                                                     PluginModelResolver pluginModelResolver,
                                                     PluginDescriptorResolver pluginDescriptorResolver,
                                                     ArtifactDescriptorValidatorBuilder artifactDescriptorValidatorBuilder) {
    super(deployableProjectModel.getProjectFolder(),
          artifactDescriptorValidatorBuilder);
    this.deployableProjectModel = deployableProjectModel;
    this.deploymentProperties = asProperties(deploymentProperties);
    this.pluginModelResolver = pluginModelResolver;
    this.pluginDescriptorResolver = pluginDescriptorResolver;
    this.pluginDependenciesResolver = new BundlePluginDependenciesResolver();
  }

  private Optional<Properties> asProperties(Map<String, String> deploymentProperties) {
    Properties properties = new Properties();
    properties.putAll(deploymentProperties);
    return properties.isEmpty() ? empty() : of(properties);
  }

  protected Optional<Properties> getDeploymentProperties() {
    return deploymentProperties;
  }

  @Override
  protected ClassLoaderConfiguration getClassLoaderConfiguration(MuleArtifactLoaderDescriptor muleArtifactLoaderDescriptor) {
    return new DeployableClassLoaderConfigurationAssembler(deployableProjectModel, muleArtifactLoaderDescriptor)
        .createClassLoaderConfiguration();
  }

  @Override
  protected BundleDescriptor getBundleDescriptor() {
    return deployableProjectModel.getDescriptor();
  }

  protected M getDeployableModel() {
    return (M) deployableProjectModel.getDeployableModel();
  }

  protected BundlePluginDependenciesResolver getPluginDependenciesResolver() {
    return pluginDependenciesResolver;
  }

  @Override
  protected void doDescriptorConfig(T descriptor) {
    descriptor.setArtifactLocation(getArtifactLocation());
    descriptor.setRedeploymentEnabled(getArtifactModel().isRedeploymentEnabled());

    if (getArtifactLocation().isDirectory()) {
      descriptor.setRootFolder(getArtifactLocation());
    }

    Set<String> configs = getArtifactModel().getConfigs();
    if (configs != null && !configs.isEmpty()) {
      descriptor.setConfigResources(new HashSet<>(configs));
    } else {
      descriptor.setConfigResources(singleton(getDefaultConfigurationResource()));
    }

    descriptor.setPlugins(createArtifactPluginDescriptors(descriptor));
    descriptor.setLogConfigFile(getLogConfigFile(getArtifactModel()));
    descriptor.setSupportedJavaVersions(getArtifactModel().getSupportedJavaVersions());
  }

  private File getLogConfigFile(M artifactModel) {
    File logConfigFile = null;
    if (artifactModel.getLogConfigFile() != null) {
      Path logConfigFilePath = new File(artifactModel.getLogConfigFile()).toPath();
      Path muleHomeFolderPath = getMuleHomeFolder().toPath();
      logConfigFile = muleHomeFolderPath.resolve(logConfigFilePath).toFile();
    }
    return logConfigFile;
  }

  private Set<ArtifactPluginDescriptor> createArtifactPluginDescriptors(DeployableArtifactDescriptor descriptor) {
    Set<ArtifactPluginDescriptor> pluginDescriptors = new HashSet<>();
    for (BundleDependency bundlePluginDependency : descriptor.getClassLoaderConfiguration().getDependencies()) {
      BundleDescriptor bundleDescriptor = bundlePluginDependency.getDescriptor();
      if (bundleDescriptor.isPlugin()) {
        if (bundlePluginDependency.getBundleUri() == null) {
          LOGGER
              .warn(format("Plugin '%s' is declared as 'provided' which means that it will not be added to the artifact's classpath",
                           bundleDescriptor));
        } else {
          List<BundleDependency> bundleDependencies = bundlePluginDependency.getTransitiveDependenciesList();
          pluginDescriptors
              .add(pluginDescriptorResolver.resolve(emptySet(), bundleDescriptor)
                  .orElse(createPluginDescriptor(bundlePluginDependency,
                                                 pluginModelResolver.resolve(bundlePluginDependency),
                                                 descriptor,
                                                 bundleDependencies,
                                                 deployableProjectModel.getSharedLibraries())));
        }
      }
    }
    return pluginDescriptors;
  }

  protected abstract String getDefaultConfigurationResource();

  /**
   * Creates a descriptor for a plugin.
   *
   * @param bundleDependency         description of the plugin on a bundle.
   * @param pluginModel              description of the model of the plugin.
   * @param ownerDescriptor          descriptor of the artifact that owns the plugin.
   * @param pluginBundleDependencies plugin dependencies on a bundle.
   * @param sharedPluginDependencies the dependencies on the deployable artifact that are shared to plugins.
   *
   * @return a descriptor for a plugin.
   */
  private ArtifactPluginDescriptor createPluginDescriptor(BundleDependency bundleDependency,
                                                          MulePluginModel pluginModel,
                                                          DeployableArtifactDescriptor ownerDescriptor,
                                                          List<BundleDependency> pluginBundleDependencies,
                                                          Set<BundleDescriptor> sharedPluginDependencies) {
    return new ArtifactPluginDescriptorFactory(bundleDependency,
                                               pluginModel,
                                               ownerDescriptor,
                                               pluginBundleDependencies,
                                               sharedPluginDependencies,
                                               ArtifactDescriptorValidatorBuilder.builder())
                                                   .create();
  }
}
