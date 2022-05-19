/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.module.artifact.activation.internal.deployable;

import static java.lang.String.format;
import static java.util.Collections.emptySet;
import static java.util.Optional.of;
import static org.mule.runtime.api.i18n.I18nMessageFactory.createStaticMessage;

import org.mule.runtime.api.deployment.meta.MuleDeployableModel;
import org.mule.runtime.module.artifact.activation.api.ArtifactActivationException;
import org.mule.runtime.module.artifact.activation.api.descriptor.ArtifactDescriptorFactory;
import org.mule.runtime.module.artifact.activation.api.deployable.DeployableProjectModel;
import org.mule.runtime.module.artifact.activation.api.plugin.PluginDescriptorResolver;
import org.mule.runtime.module.artifact.activation.api.plugin.PluginModelResolver;
import org.mule.runtime.module.artifact.activation.internal.descriptor.AbstractArtifactDescriptorFactory;
import org.mule.runtime.module.artifact.api.descriptor.ArtifactDescriptorValidatorBuilder;
import org.mule.runtime.module.artifact.api.descriptor.ArtifactPluginDescriptor;
import org.mule.runtime.module.artifact.api.descriptor.BundleDependency;
import org.mule.runtime.module.artifact.api.descriptor.BundleDescriptor;
import org.mule.runtime.module.artifact.api.descriptor.ClassLoaderModel;
import org.mule.runtime.module.artifact.api.descriptor.DeployableArtifactDescriptor;
import org.mule.tools.api.classloader.model.Artifact;
import org.mule.tools.api.classloader.model.ArtifactCoordinates;

import com.google.common.collect.ImmutableSet;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Properties;
import java.util.Set;

/**
 * Base class to create descriptors of artifacts that are deployable.
 *
 * @param <M> type of the artifact model that owns the descriptor.
 * @param <T> type of descriptor being created.
 */
public abstract class AbstractDeployableArtifactDescriptorFactory<M extends MuleDeployableModel, T extends DeployableArtifactDescriptor>
    extends AbstractArtifactDescriptorFactory<M, T> {

  private static final Logger LOGGER = LoggerFactory.getLogger(AbstractDeployableArtifactDescriptorFactory.class);

  protected final DeployableProjectModel<M> deployableProjectModel;
  protected final Optional<Properties> deploymentProperties;
  private final PluginModelResolver pluginModelResolver;
  private final PluginDescriptorResolver pluginDescriptorResolver;
  private final ArtifactDescriptorFactory artifactDescriptorFactory;

  public AbstractDeployableArtifactDescriptorFactory(DeployableProjectModel<M> deployableProjectModel,
                                                     Map<String, String> deploymentProperties,
                                                     PluginModelResolver pluginModelResolver,
                                                     PluginDescriptorResolver pluginDescriptorResolver,
                                                     ArtifactDescriptorValidatorBuilder artifactDescriptorValidatorBuilder,
                                                     ArtifactDescriptorFactory artifactDescriptorFactory) {
    super(deployableProjectModel.getProjectFolder(), deployableProjectModel.getMuleDeployableModel(),
          artifactDescriptorValidatorBuilder);
    this.deployableProjectModel = deployableProjectModel;
    this.deploymentProperties = getProperties(deploymentProperties);
    this.pluginModelResolver = pluginModelResolver;
    this.pluginDescriptorResolver = pluginDescriptorResolver;
    this.artifactDescriptorFactory = artifactDescriptorFactory;
  }

  private Optional<Properties> getProperties(Map<String, String> deploymentProperties) {
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

    // TODO: add log config file to descriptor, which currently relies in the mule home folder
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
              .add(pluginDescriptorResolver.resolve(emptySet(), bundleDescriptor).orElse(artifactDescriptorFactory
                  .createPluginDescriptor(bundlePluginDependency,
                                          pluginModelResolver.resolve(bundlePluginDependency), descriptor, bundleDependencies,
                                          pluginDependencies.getKey(), pluginDependencies.getValue(),
                                          deployableProjectModel.getPluginsExportedPackages().get(bundleDescriptor),
                                          deployableProjectModel.getPluginsExportedResources().get(bundleDescriptor))));
        }
      }
    }
    return pluginDescriptors;
  }

  protected abstract String getDefaultConfigurationResource();

}
