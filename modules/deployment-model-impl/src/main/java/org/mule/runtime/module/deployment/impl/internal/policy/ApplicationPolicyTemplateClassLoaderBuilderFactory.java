/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.runtime.module.deployment.impl.internal.policy;

import org.mule.runtime.deployment.model.api.plugin.ArtifactPluginRepository;
import org.mule.runtime.deployment.model.internal.plugin.PluginDependenciesResolver;
import org.mule.runtime.deployment.model.internal.policy.PolicyTemplateClassLoaderBuilder;
import org.mule.runtime.module.artifact.classloader.ArtifactClassLoaderFactory;
import org.mule.runtime.module.artifact.classloader.DeployableArtifactClassLoaderFactory;

/**
 * Creates {@link PolicyTemplateClassLoaderBuilder} for application artifacts.
 */
public class ApplicationPolicyTemplateClassLoaderBuilderFactory implements PolicyTemplateClassLoaderBuilderFactory {

  private final DeployableArtifactClassLoaderFactory artifactClassLoaderFactory;
  private final ArtifactPluginRepository artifactPluginRepository;
  private final ArtifactClassLoaderFactory artifactPluginClassLoaderFactory;
  private final PluginDependenciesResolver pluginDependenciesResolver;

  /**
   * Creates a new factory instance
   *
   * @param artifactClassLoaderFactory factory for the classloader specific to the artifact resource and classes. Must be not
   *        null.
   * @param artifactPluginRepository repository of plugins contained by the runtime. Must be not null.
   * @param artifactPluginClassLoaderFactory factory to create class loaders for each used plugin. Non be not null.
   * @param pluginDependenciesResolver resolves artifact plugin dependencies. Non null
   */
  public ApplicationPolicyTemplateClassLoaderBuilderFactory(DeployableArtifactClassLoaderFactory artifactClassLoaderFactory,
                                                            ArtifactPluginRepository artifactPluginRepository,
                                                            ArtifactClassLoaderFactory artifactPluginClassLoaderFactory,
                                                            PluginDependenciesResolver pluginDependenciesResolver) {
    this.artifactClassLoaderFactory = artifactClassLoaderFactory;
    this.artifactPluginRepository = artifactPluginRepository;
    this.artifactPluginClassLoaderFactory = artifactPluginClassLoaderFactory;
    this.pluginDependenciesResolver = pluginDependenciesResolver;
  }

  @Override
  public PolicyTemplateClassLoaderBuilder createArtifactClassLoaderBuilder() {
    return new PolicyTemplateClassLoaderBuilder(artifactClassLoaderFactory, artifactPluginRepository,
                                                artifactPluginClassLoaderFactory,
                                                pluginDependenciesResolver);
  }
}
