/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.runtime.module.deployment.impl.internal.policy;

import org.mule.runtime.deployment.model.internal.RegionPluginClassLoadersFactory;
import org.mule.runtime.deployment.model.internal.policy.PolicyTemplateClassLoaderBuilder;
import org.mule.runtime.module.artifact.api.classloader.DeployableArtifactClassLoaderFactory;

/**
 * Creates {@link PolicyTemplateClassLoaderBuilder} for application artifacts.
 */
public class ApplicationPolicyTemplateClassLoaderBuilderFactory implements PolicyTemplateClassLoaderBuilderFactory {

  private final DeployableArtifactClassLoaderFactory artifactClassLoaderFactory;
  private final RegionPluginClassLoadersFactory pluginClassLoadersFactory;

  /**
   * Creates a new factory instance
   *  @param artifactClassLoaderFactory factory for the classloader specific to the artifact resource and classes. Must be not
   *        null.
   * @param pluginClassLoadersFactory creates the class loaders for the plugins included in the application's region. Non null
   */
  public ApplicationPolicyTemplateClassLoaderBuilderFactory(DeployableArtifactClassLoaderFactory artifactClassLoaderFactory,
                                                            RegionPluginClassLoadersFactory pluginClassLoadersFactory) {

    this.artifactClassLoaderFactory = artifactClassLoaderFactory;
    this.pluginClassLoadersFactory = pluginClassLoadersFactory;
  }

  @Override
  public PolicyTemplateClassLoaderBuilder createArtifactClassLoaderBuilder() {
    return new PolicyTemplateClassLoaderBuilder(artifactClassLoaderFactory, pluginClassLoadersFactory);
  }
}
