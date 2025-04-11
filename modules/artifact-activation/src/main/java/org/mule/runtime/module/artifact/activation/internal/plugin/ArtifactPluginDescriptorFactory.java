/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.module.artifact.activation.internal.plugin;

import org.mule.runtime.api.deployment.meta.MuleArtifactLoaderDescriptor;
import org.mule.runtime.api.deployment.meta.MulePluginModel;
import org.mule.runtime.module.artifact.activation.internal.descriptor.AbstractArtifactDescriptorFactory;
import org.mule.runtime.module.artifact.api.descriptor.ArtifactDescriptorValidatorBuilder;
import org.mule.runtime.module.artifact.api.descriptor.ArtifactPluginDescriptor;
import org.mule.runtime.module.artifact.api.descriptor.BundleDependency;
import org.mule.runtime.module.artifact.api.descriptor.BundleDescriptor;
import org.mule.runtime.module.artifact.api.descriptor.ClassLoaderConfiguration;
import org.mule.runtime.module.artifact.api.descriptor.DeployableArtifactDescriptor;
import org.mule.runtime.module.artifact.api.plugin.LoaderDescriber;

import java.io.File;
import java.util.List;
import java.util.Set;

/**
 * Creates an artifact descriptor for a plugin.
 */
public class ArtifactPluginDescriptorFactory
    extends AbstractArtifactDescriptorFactory<MulePluginModel, ArtifactPluginDescriptor> {

  private final MulePluginModel pluginModel;
  private final BundleDependency bundleDependency;
  private final DeployableArtifactDescriptor ownerDescriptor;
  private final List<BundleDependency> bundleDependencies;
  private final Set<BundleDescriptor> sharedProjectDependencies;

  public ArtifactPluginDescriptorFactory(BundleDependency bundleDependency,
                                         MulePluginModel pluginModel,
                                         DeployableArtifactDescriptor ownerDescriptor,
                                         List<BundleDependency> bundleDependencies,
                                         Set<BundleDescriptor> sharedProjectDependencies,
                                         ArtifactDescriptorValidatorBuilder artifactDescriptorValidatorBuilder) {
    super(new File(bundleDependency.getBundleUri()), artifactDescriptorValidatorBuilder);

    this.pluginModel = pluginModel;
    this.bundleDependency = bundleDependency;
    this.ownerDescriptor = ownerDescriptor;
    this.bundleDependencies = bundleDependencies;
    this.sharedProjectDependencies = sharedProjectDependencies;
  }

  @Override
  protected MulePluginModel createArtifactModel() {
    return pluginModel;
  }

  @Override
  protected void doDescriptorConfig(ArtifactPluginDescriptor descriptor) {
    getArtifactModel().getExtensionModelLoaderDescriptor().ifPresent(extensionModelDescriptor -> {
      final LoaderDescriber loaderDescriber = new LoaderDescriber(extensionModelDescriptor.getId());
      loaderDescriber.addAttributes(extensionModelDescriptor.getAttributes());
      descriptor.setExtensionModelDescriptorProperty(loaderDescriber);
    });

    getArtifactModel().getLicense().ifPresent(descriptor::setLicenseModel);
  }

  @Override
  protected ClassLoaderConfiguration getClassLoaderConfiguration(MuleArtifactLoaderDescriptor muleArtifactLoaderDescriptor) {
    return new PluginClassLoaderConfigurationAssembler(bundleDependency,
                                                       sharedProjectDependencies,
                                                       getArtifactLocation(),
                                                       muleArtifactLoaderDescriptor,
                                                       bundleDependencies,
                                                       ownerDescriptor)
        .createClassLoaderConfiguration();
  }

  @Override
  protected BundleDescriptor getBundleDescriptor() {
    return bundleDependency.getDescriptor();
  }

  @Override
  protected ArtifactPluginDescriptor doCreateArtifactDescriptor() {
    return new ArtifactPluginDescriptor(getArtifactModel().getName());
  }
}
