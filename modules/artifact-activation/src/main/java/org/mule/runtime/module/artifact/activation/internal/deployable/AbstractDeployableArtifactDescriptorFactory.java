/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.module.artifact.activation.internal.deployable;

import static org.mule.runtime.api.i18n.I18nMessageFactory.createStaticMessage;

import static java.lang.String.format;
import static java.util.Collections.emptySet;
import static java.util.Optional.of;

import org.mule.runtime.api.deployment.meta.MuleDeployableModel;
import org.mule.runtime.api.deployment.meta.MulePluginModel;
import org.mule.runtime.module.artifact.activation.api.ArtifactActivationException;
import org.mule.runtime.module.artifact.activation.api.deployable.DeployableProjectModel;
import org.mule.runtime.module.artifact.activation.api.plugin.PluginDescriptorResolver;
import org.mule.runtime.module.artifact.activation.api.plugin.PluginModelResolver;
import org.mule.runtime.module.artifact.activation.internal.descriptor.AbstractArtifactDescriptorFactory;
import org.mule.runtime.module.artifact.activation.internal.plugin.ArtifactPluginDescriptorFactory;
import org.mule.runtime.module.artifact.api.descriptor.ArtifactDescriptorValidatorBuilder;
import org.mule.runtime.module.artifact.api.descriptor.ArtifactPluginDescriptor;
import org.mule.runtime.module.artifact.api.descriptor.BundleDependency;
import org.mule.runtime.module.artifact.api.descriptor.BundleDescriptor;
import org.mule.runtime.module.artifact.api.descriptor.ClassLoaderModel;
import org.mule.runtime.module.artifact.api.descriptor.DeployableArtifactDescriptor;
import org.mule.tools.api.classloader.model.Artifact;
import org.mule.tools.api.classloader.model.ArtifactCoordinates;

import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Properties;
import java.util.Set;

import com.google.common.collect.ImmutableSet;

import org.apache.commons.lang3.StringUtils;
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

  private final DeployableProjectModel<M> deployableProjectModel;
  protected final Optional<Properties> deploymentProperties;
  private final PluginModelResolver pluginModelResolver;
  private final PluginDescriptorResolver pluginDescriptorResolver;

  public AbstractDeployableArtifactDescriptorFactory(DeployableProjectModel<M> deployableProjectModel,
                                                     Map<String, String> deploymentProperties,
                                                     PluginModelResolver pluginModelResolver,
                                                     PluginDescriptorResolver pluginDescriptorResolver,
                                                     ArtifactDescriptorValidatorBuilder artifactDescriptorValidatorBuilder) {
    super(deployableProjectModel.getProjectFolder(), deployableProjectModel.getMuleDeployableModel(),
          artifactDescriptorValidatorBuilder);
    // TODO W-11202204 - validate model dependencies checking for incompatibilities
    this.deployableProjectModel = deployableProjectModel;
    this.deploymentProperties = asProperties(deploymentProperties);
    this.pluginModelResolver = pluginModelResolver;
    this.pluginDescriptorResolver = pluginDescriptorResolver;
  }

  private Optional<Properties> asProperties(Map<String, String> deploymentProperties) {
    Properties properties = new Properties();
    properties.putAll(deploymentProperties);
    return of(properties);
  }

  @Override
  protected ClassLoaderModel getClassLoaderModel() {
    return new DeployableClassLoaderConfigurationAssembler<>(deployableProjectModel)
        .createClassLoaderModel();
  }

  @Override
  protected BundleDescriptor getBundleDescriptor() {
    return deployableProjectModel.getBundleDescriptor();
  }

  @Override
  protected void doDescriptorConfig(T descriptor) {
    descriptor.setArtifactLocation(artifactLocation);
    descriptor.setRedeploymentEnabled(artifactModel.isRedeploymentEnabled());

    Set<String> configs = artifactModel.getConfigs();
    if (configs != null && !configs.isEmpty()) {
      descriptor.setConfigResources(new HashSet<>(configs));
    } else {
      descriptor.setConfigResources(ImmutableSet.<String>builder().add(getDefaultConfigurationResource()).build());
    }

    descriptor.setPlugins(createArtifactPluginDescriptors(descriptor));

    // TODO W-11202321 - add log config file to descriptor, which currently relies in the mule home folder
  }

  private Set<ArtifactPluginDescriptor> createArtifactPluginDescriptors(DeployableArtifactDescriptor descriptor) {
    Set<ArtifactPluginDescriptor> pluginDescriptors = new HashSet<>();
    for (BundleDependency bundlePluginDependency : descriptor.getClassLoaderModel().getDependencies()) {
      BundleDescriptor bundleDescriptor = bundlePluginDependency.getDescriptor();
      if (bundleDescriptor.isPlugin()) {
        if (bundlePluginDependency.getBundleUri() == null) {
          LOGGER
              .warn(format("Plugin '%s' is declared as 'provided' which means that it will not be added to the artifact's classpath",
                           bundleDescriptor));
        } else {
          Map.Entry<ArtifactCoordinates, List<Artifact>> pluginDependencies =
              deployableProjectModel.getPluginsDependencies().entrySet().stream()
                  .filter(pluginDependenciesEntry -> StringUtils
                      .equals(bundleDescriptor.getArtifactId(), pluginDependenciesEntry.getKey().getArtifactId())
                      && StringUtils.equals(bundleDescriptor.getGroupId(), pluginDependenciesEntry.getKey().getGroupId())
                      && StringUtils.equals(bundleDescriptor.getVersion(), pluginDependenciesEntry.getKey().getVersion()))
                  .findFirst()
                  .orElseThrow(() -> new ArtifactActivationException(createStaticMessage(format("Class loader model for plugin '%s' not found",
                                                                                                bundleDescriptor))));

          List<BundleDependency> bundleDependencies = deployableProjectModel.getPluginsBundleDependencies().get(bundleDescriptor);
          pluginDescriptors
              .add(pluginDescriptorResolver.resolve(emptySet(), bundleDescriptor)
                  .orElse(createPluginDescriptor(bundlePluginDependency,
                                                 pluginModelResolver.resolve(bundlePluginDependency), descriptor,
                                                 bundleDependencies,
                                                 pluginDependencies.getKey(), pluginDependencies.getValue(),
                                                 deployableProjectModel.getPluginsExportedPackages().get(bundleDescriptor),
                                                 deployableProjectModel.getPluginsExportedResources().get(bundleDescriptor))));
        }
      }
    }
    return pluginDescriptors;
  }

  protected abstract String getDefaultConfigurationResource();

  /**
   * Creates a descriptor for a plugin.
   *
   * @param bundleDependency          description of the plugin on a bundle.
   * @param pluginModel               description of the model of the plugin.
   * @param ownerDescriptor           descriptor of the artifact that owns the plugin.
   * @param bundleDependencies        plugin dependencies on a bundle.
   * @param pluginArtifactCoordinates plugin coordinates.
   * @param pluginDependencies        resolved plugin dependencies as artifacts.
   * @param pluginExportedPackages    {@link List list} of the packages the plugin exports.
   * @param pluginExportedResources   {@link List list} of the resources the plugin exports.
   * @return a descriptor for a plugin.
   */
  private ArtifactPluginDescriptor createPluginDescriptor(BundleDependency bundleDependency,
                                                          MulePluginModel pluginModel,
                                                          DeployableArtifactDescriptor ownerDescriptor,
                                                          List<BundleDependency> bundleDependencies,
                                                          ArtifactCoordinates pluginArtifactCoordinates,
                                                          List<Artifact> pluginDependencies,
                                                          List<String> pluginExportedPackages,
                                                          List<String> pluginExportedResources) {
    return new ArtifactPluginDescriptorFactory(bundleDependency, pluginModel, ownerDescriptor,
                                               bundleDependencies, pluginArtifactCoordinates, pluginDependencies,
                                               pluginExportedPackages, pluginExportedResources,
                                               ArtifactDescriptorValidatorBuilder.builder()).createArtifactDescriptor();
  }

}
