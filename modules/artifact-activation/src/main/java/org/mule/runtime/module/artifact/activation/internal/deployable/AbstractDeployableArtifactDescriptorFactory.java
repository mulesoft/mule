/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.module.artifact.activation.internal.deployable;

import static org.mule.runtime.api.i18n.I18nMessageFactory.createStaticMessage;
import static org.mule.runtime.module.artifact.api.descriptor.BundleDescriptor.MULE_PLUGIN_CLASSIFIER;

import static java.lang.String.format;
import static java.util.Collections.emptySet;
import static java.util.Optional.of;

import org.mule.runtime.api.deployment.meta.MuleArtifactLoaderDescriptor;
import org.mule.runtime.api.deployment.meta.MuleDeployableModel;
import org.mule.runtime.api.deployment.meta.MulePluginModel;
import org.mule.runtime.module.artifact.activation.api.ArtifactActivationException;
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
import org.mule.runtime.module.artifact.api.descriptor.ClassLoaderModel;
import org.mule.runtime.module.artifact.api.descriptor.DeployableArtifactDescriptor;
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
    // TODO W-11202204 - validate model dependencies checking for incompatibilities
    this.deployableProjectModel = deployableProjectModel;
    this.deploymentProperties = asProperties(deploymentProperties);
    this.pluginModelResolver = pluginModelResolver;
    this.pluginDescriptorResolver = pluginDescriptorResolver;
    this.pluginDependenciesResolver = new BundlePluginDependenciesResolver();
  }

  private Optional<Properties> asProperties(Map<String, String> deploymentProperties) {
    Properties properties = new Properties();
    properties.putAll(deploymentProperties);
    return of(properties);
  }

  protected Optional<Properties> getDeploymentProperties() {
    return deploymentProperties;
  }

  @Override
  protected ClassLoaderModel getClassLoaderModel(MuleArtifactLoaderDescriptor muleArtifactLoaderDescriptor) {
    return new DeployableClassLoaderConfigurationAssembler<>(deployableProjectModel, muleArtifactLoaderDescriptor)
        .createClassLoaderModel();
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

    Set<String> configs = getArtifactModel().getConfigs();
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
          BundleDescriptor pluginDescriptor = deployableProjectModel.getDependencies()
              .stream()
              .map(BundleDependency::getDescriptor)
              .filter(dependencyDescriptor -> MULE_PLUGIN_CLASSIFIER.equals(dependencyDescriptor.getClassifier().orElse(null)))
              .filter(pluginDependencyDescriptor -> StringUtils
                  .equals(bundleDescriptor.getArtifactId(), pluginDependencyDescriptor.getArtifactId())
                  && StringUtils.equals(bundleDescriptor.getGroupId(), pluginDependencyDescriptor.getGroupId())
                  && StringUtils.equals(bundleDescriptor.getVersion(), pluginDependencyDescriptor.getVersion()))
              .findFirst()
              .orElseThrow(() -> new ArtifactActivationException(createStaticMessage(format("Dependency for plugin '%s' not found",
                                                                                            bundleDescriptor))));

          List<BundleDependency> bundleDependencies = bundlePluginDependency.getTransitiveDependenciesList();
          pluginDescriptors
              .add(pluginDescriptorResolver.resolve(emptySet(), bundleDescriptor)
                  .orElse(createPluginDescriptor(bundlePluginDependency,
                                                 pluginModelResolver.resolve(bundlePluginDependency),
                                                 descriptor,
                                                 bundleDependencies,
                                                 new ArtifactCoordinates(pluginDescriptor.getGroupId(),
                                                                         pluginDescriptor.getArtifactId(),
                                                                         pluginDescriptor.getVersion(),
                                                                         pluginDescriptor.getType(),
                                                                         pluginDescriptor.getClassifier().orElse(null)),
                                                 deployableProjectModel.getDependencies(),
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
   * @param bundleDependency          description of the plugin on a bundle.
   * @param pluginModel               description of the model of the plugin.
   * @param ownerDescriptor           descriptor of the artifact that owns the plugin.
   * @param bundleDependencies        plugin dependencies on a bundle.
   * @param pluginArtifactCoordinates plugin coordinates.
   * @param pluginDependencies        the dependencies on the deployable artifact.
   * @param sharedPluginDependencies  the dependencies on the deployable artifact that are shared to plugins.
   * 
   * @return a descriptor for a plugin.
   */
  private ArtifactPluginDescriptor createPluginDescriptor(BundleDependency bundleDependency,
                                                          MulePluginModel pluginModel,
                                                          DeployableArtifactDescriptor ownerDescriptor,
                                                          List<BundleDependency> bundleDependencies,
                                                          ArtifactCoordinates pluginArtifactCoordinates,
                                                          List<BundleDependency> pluginDependencies,
                                                          Set<BundleDescriptor> sharedPluginDependencies) {
    return new ArtifactPluginDescriptorFactory(bundleDependency,
                                               pluginModel,
                                               ownerDescriptor,
                                               bundleDependencies,
                                               pluginArtifactCoordinates,
                                               pluginDependencies,
                                               sharedPluginDependencies,
                                               ArtifactDescriptorValidatorBuilder.builder())
                                                   .create();
  }
}
