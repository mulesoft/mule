/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.module.deployment.impl.internal.application;

import org.mule.runtime.deployment.model.internal.plugin.PluginDependenciesResolver;
import org.mule.runtime.module.artifact.classloader.ClassLoaderRepository;
import org.mule.runtime.module.deployment.impl.internal.artifact.MuleContextListenerFactory;
import org.mule.runtime.module.deployment.impl.internal.domain.DomainRepository;
import org.mule.runtime.module.deployment.impl.internal.plugin.ArtifactPluginDescriptorLoader;
import org.mule.runtime.module.deployment.impl.internal.policy.PolicyTemplateClassLoaderBuilderFactory;
import org.mule.runtime.module.extension.internal.loader.ExtensionModelLoaderRepository;
import org.mule.runtime.module.service.ServiceRepository;

/**
 * Creates mule applications for Tooling, these kind of applications should not allow notifications.
 */
public class ToolingApplicationFactory extends DefaultApplicationFactory {

  public ToolingApplicationFactory(ApplicationClassLoaderBuilderFactory applicationClassLoaderBuilderFactory,
                                   ApplicationDescriptorFactory applicationDescriptorFactory,
                                   DomainRepository domainRepository,
                                   ServiceRepository serviceRepository,
                                   ExtensionModelLoaderRepository extensionModelLoaderRepository,
                                   ClassLoaderRepository classLoaderRepository,
                                   PolicyTemplateClassLoaderBuilderFactory policyTemplateClassLoaderBuilderFactory,
                                   PluginDependenciesResolver pluginDependenciesResolver,
                                   ArtifactPluginDescriptorLoader artifactPluginDescriptorLoader) {
    super(applicationClassLoaderBuilderFactory, applicationDescriptorFactory, domainRepository, serviceRepository,
          extensionModelLoaderRepository, classLoaderRepository, policyTemplateClassLoaderBuilderFactory,
          pluginDependenciesResolver,
          artifactPluginDescriptorLoader);
  }

  @Override
  public void setMuleContextListenerFactory(MuleContextListenerFactory muleContextListenerFactory) {
    throw new UnsupportedOperationException("Tooling applications don't support mule context notifications");
  }

}
