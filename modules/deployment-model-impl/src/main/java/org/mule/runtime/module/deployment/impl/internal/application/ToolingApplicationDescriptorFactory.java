/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.module.deployment.impl.internal.application;

import org.mule.runtime.api.deployment.meta.MuleApplicationModel;
import org.mule.runtime.deployment.model.api.application.ApplicationDescriptor;
import org.mule.runtime.module.artifact.api.descriptor.ArtifactDescriptorValidatorBuilder;
import org.mule.runtime.module.artifact.api.descriptor.DescriptorLoaderRepository;
import org.mule.runtime.module.deployment.impl.internal.plugin.ArtifactPluginDescriptorLoader;

import java.io.File;
import java.util.Optional;
import java.util.Properties;

/**
 * Implementation that allows to create an artifact descriptor for an application from an artifact folder but also
 * from a {@link MuleApplicationModel}.
 * <p/>
 * It also provides a factory method to create the {@link MuleApplicationModel} from an artifact folder.
 *
 *
 * @since 4.1
 */
public class ToolingApplicationDescriptorFactory extends ApplicationDescriptorFactory {

  /**
   * Creates an instance of the factory.
   *
   * @param artifactPluginDescriptorLoader {@link ArtifactPluginDescriptorLoader} to load the descriptor for plugins.
   * @param descriptorLoaderRepository {@link DescriptorLoaderRepository} to get the descriptor loader implementation.
   */
  public ToolingApplicationDescriptorFactory(
                                             ArtifactPluginDescriptorLoader artifactPluginDescriptorLoader,
                                             DescriptorLoaderRepository descriptorLoaderRepository,
                                             ArtifactDescriptorValidatorBuilder artifactDescriptorValidatorBuilder) {
    super(artifactPluginDescriptorLoader, descriptorLoaderRepository, artifactDescriptorValidatorBuilder);
  }

  public ApplicationDescriptor createArtifact(File artifactFolder, Optional<Properties> deploymentProperties,
                                              MuleApplicationModel artifactModel) {
    return super.createArtifact(artifactFolder, deploymentProperties, artifactModel);
  }

  /**
   * Creates a {@link MuleApplicationModel.MuleApplicationModelBuilder} for the application from its artifact folder.
   *
   * @param artifactFolder location of the application root folder.
   * @return a {@link MuleApplicationModel.MuleApplicationModelBuilder}.
   */
  public MuleApplicationModel.MuleApplicationModelBuilder createArtifactModelBuilder(File artifactFolder) {
    MuleApplicationModel muleApplicationModel = super.createArtifactModel(artifactFolder);

    MuleApplicationModel.MuleApplicationModelBuilder builder =
        new MuleApplicationModel.MuleApplicationModelBuilder();
    builder.setRedeploymentEnabled(muleApplicationModel.isRedeploymentEnabled());
    builder.setName(muleApplicationModel.getName());
    builder.setConfigs(muleApplicationModel.getConfigs());
    builder.setMinMuleVersion(muleApplicationModel.getMinMuleVersion());
    builder.setRequiredProduct(muleApplicationModel.getRequiredProduct());
    builder.setSecureProperties(muleApplicationModel.getSecureProperties());
    builder.setLogConfigFile(muleApplicationModel.getLogConfigFile());
    builder.withBundleDescriptorLoader(muleApplicationModel.getBundleDescriptorLoader());
    builder.withClassLoaderModelDescriptorLoader(muleApplicationModel.getClassLoaderModelLoaderDescriptor());

    return builder;
  }

}
