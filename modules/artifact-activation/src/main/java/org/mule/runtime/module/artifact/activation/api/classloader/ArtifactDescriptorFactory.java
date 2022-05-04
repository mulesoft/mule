/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.module.artifact.activation.api.classloader;

import org.mule.runtime.api.deployment.meta.MulePluginModel;
import org.mule.runtime.module.artifact.activation.api.plugin.PluginClassLoaderResolver;
import org.mule.runtime.module.artifact.activation.api.plugin.PluginDescriptorResolver;
import org.mule.runtime.module.artifact.activation.api.plugin.PluginModelResolver;
import org.mule.runtime.module.artifact.api.classloader.ArtifactClassLoader;
import org.mule.runtime.module.artifact.api.descriptor.ApplicationDescriptor;
import org.mule.runtime.module.artifact.api.descriptor.ArtifactPluginDescriptor;
import org.mule.runtime.module.artifact.api.descriptor.DeployableArtifactDescriptor;
import org.mule.runtime.module.artifact.api.descriptor.DomainDescriptor;

import java.util.Map;

/**
 * Provides methods to create the descriptors of different kind of artifacts (applications, plugins, domains) from the
 * {@link DeployableProjectModel}.
 *
 * @since 4.5
 */
public interface ArtifactDescriptorFactory {

  /**
   * @param deploymentProperties      properties that affect how the artifact is deployed.
   * @param pluginModelResolver       resolves {@link MulePluginModel} from a dependency. Default implementation is
   *                                  {@link PluginModelResolver#mavenDeployablePluginModelResolver()}.
   * @param pluginClassLoaderResolver allows the user to provide a class loader for the given plugin, otherwise it will be
   *                                  created. Default implementation is
   *                                  {@link PluginClassLoaderResolver#resolve(ArtifactClassLoader, ArtifactPluginDescriptor)}
   */
  ApplicationDescriptor createApplicationDescriptor(DeployableProjectModel model,
                                                    Map<String, String> deploymentProperties,
                                                    PluginModelResolver pluginModelResolver,
                                                    PluginDescriptorResolver pluginDescriptorResolver);

  /**
   * @param deploymentProperties      properties that affect how the artifact is deployed.
   * @param pluginModelResolver       resolves {@link MulePluginModel} from a dependency. Default implementation is
   *                                  {@link PluginModelResolver#mavenDeployablePluginModelResolver()}.
   * @param pluginClassLoaderResolver allows the user to provide a class loader for the given plugin, otherwise it will be
   *                                  created. Default implementation is
   *                                  {@link PluginClassLoaderResolver#resolve(ArtifactClassLoader, ArtifactPluginDescriptor)}
   */
  DomainDescriptor createDomainDescriptor(DeployableProjectModel model,
                                          Map<String, String> deploymentProperties,
                                          PluginModelResolver pluginModelResolver,
                                          PluginDescriptorResolver pluginDescriptorResolver);

  ArtifactPluginDescriptor createPluginDescriptor(MulePluginModel pluginModel,
                                                  DeployableArtifactDescriptor ownerDescriptor);

}
