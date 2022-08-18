/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.runtime.module.deployment.impl.internal.policy;

import org.mule.runtime.api.config.FeatureFlaggingService;
import org.mule.runtime.deployment.model.api.builder.RegionPluginClassLoadersFactory;
import org.mule.runtime.deployment.model.internal.policy.PolicyTemplateClassLoaderBuilder;
import org.mule.runtime.module.artifact.api.classloader.DeployableArtifactClassLoaderFactory;

/**
 * Creates {@link PolicyTemplateClassLoaderBuilder} for application artifacts.
 */
public class ApplicationPolicyTemplateClassLoaderBuilderFactory implements PolicyTemplateClassLoaderBuilderFactory {

  private final DeployableArtifactClassLoaderFactory artifactClassLoaderFactory;
  private final RegionPluginClassLoadersFactory pluginClassLoadersFactory;
  private final FeatureFlaggingService featureFlaggingService;

  /**
   * Creates a new factory instance
   * 
   * @param artifactClassLoaderFactory factory for the classloader specific to the artifact resource and classes. Must be not
   *                                   null.
   * @param pluginClassLoadersFactory  creates the class loaders for the plugins included in the application's region. Non null
   * @param featureFlaggingService     the feature flagging service. Not null.
   */
  public ApplicationPolicyTemplateClassLoaderBuilderFactory(DeployableArtifactClassLoaderFactory artifactClassLoaderFactory,
                                                            RegionPluginClassLoadersFactory pluginClassLoadersFactory,
                                                            FeatureFlaggingService featureFlaggingService) {

    this.artifactClassLoaderFactory = artifactClassLoaderFactory;
    this.pluginClassLoadersFactory = pluginClassLoadersFactory;
    this.featureFlaggingService = featureFlaggingService;
  }

  @Override
  public PolicyTemplateClassLoaderBuilder createArtifactClassLoaderBuilder() {
    return new PolicyTemplateClassLoaderBuilder(artifactClassLoaderFactory, pluginClassLoadersFactory, featureFlaggingService);
  }
}
