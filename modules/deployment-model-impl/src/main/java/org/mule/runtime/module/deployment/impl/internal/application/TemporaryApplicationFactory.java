/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.runtime.module.deployment.impl.internal.application;

import org.mule.runtime.deployment.model.api.plugin.ArtifactPluginRepository;
import org.mule.runtime.deployment.model.internal.plugin.PluginDependenciesResolver;
import org.mule.runtime.module.deployment.impl.internal.plugin.ArtifactPluginDescriptorLoader;
import org.mule.runtime.module.deployment.impl.internal.policy.PolicyTemplateClassLoaderBuilderFactory;
import org.mule.runtime.module.artifact.classloader.ClassLoaderRepository;
import org.mule.runtime.module.deployment.impl.internal.domain.DomainRepository;
import org.mule.runtime.module.extension.internal.loader.ExtensionModelLoaderRepository;
import org.mule.runtime.module.reboot.MuleContainerBootstrapUtils;
import org.mule.runtime.module.service.ServiceRepository;

import java.io.File;

/**
 * Creates temporary mule applications from a location outside {@link MuleContainerBootstrapUtils#getMuleAppsDir()}.
 *
 * @since 4.0
 */
public class TemporaryApplicationFactory extends DefaultApplicationFactory {

  public TemporaryApplicationFactory(ApplicationClassLoaderBuilderFactory applicationClassLoaderBuilderFactory,
                                     ApplicationDescriptorFactory applicationDescriptorFactory,
                                     ArtifactPluginRepository artifactPluginRepository, DomainRepository domainRepository,
                                     ServiceRepository serviceRepository,
                                     ExtensionModelLoaderRepository extensionModelLoaderRepository,
                                     ClassLoaderRepository classLoaderRepository,
                                     PolicyTemplateClassLoaderBuilderFactory policyTemplateClassLoaderBuilderFactory,
                                     PluginDependenciesResolver pluginDependenciesResolver,
                                     ArtifactPluginDescriptorLoader pluginDescriptorLoader) {
    super(applicationClassLoaderBuilderFactory, applicationDescriptorFactory, artifactPluginRepository, domainRepository,
          serviceRepository, extensionModelLoaderRepository, classLoaderRepository, policyTemplateClassLoaderBuilderFactory,
          pluginDependenciesResolver, pluginDescriptorLoader);
  }

  @Override
  public File getArtifactDir() {
    throw new UnsupportedOperationException("Temporary applications don't have a common default artifact directory");
  }

}
