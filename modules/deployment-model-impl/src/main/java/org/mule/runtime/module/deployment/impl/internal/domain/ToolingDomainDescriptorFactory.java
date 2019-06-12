/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.module.deployment.impl.internal.domain;

import static org.mule.runtime.deployment.model.internal.artifact.ArtifactUtils.createBundleDescriptorFromName;
import org.mule.runtime.api.deployment.meta.MuleDomainModel;
import org.mule.runtime.module.artifact.api.descriptor.ArtifactDescriptorValidatorBuilder;
import org.mule.runtime.module.artifact.api.descriptor.BundleDescriptor;
import org.mule.runtime.module.artifact.api.descriptor.DescriptorLoaderRepository;
import org.mule.runtime.module.deployment.impl.internal.plugin.ArtifactPluginDescriptorLoader;

import java.io.File;
import java.util.Optional;
import java.util.Properties;

public class ToolingDomainDescriptorFactory extends DomainDescriptorFactory {

  public ToolingDomainDescriptorFactory(ArtifactPluginDescriptorLoader artifactPluginDescriptorLoader,
                                        DescriptorLoaderRepository descriptorLoaderRepository,
                                        ArtifactDescriptorValidatorBuilder artifactDescriptorValidatorBuilder) {
    super(artifactPluginDescriptorLoader, descriptorLoaderRepository, artifactDescriptorValidatorBuilder);
  }

  @Override
  protected BundleDescriptor getBundleDescriptor(File appFolder, MuleDomainModel artifactModel,
                                                 Optional<Properties> deploymentProperties) {
    String domainName = appFolder.getName();
    return createBundleDescriptorFromName(domainName);
  }
}
