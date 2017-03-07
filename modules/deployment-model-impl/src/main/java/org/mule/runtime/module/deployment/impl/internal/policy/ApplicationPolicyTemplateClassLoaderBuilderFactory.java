/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.runtime.module.deployment.impl.internal.policy;

import org.mule.runtime.deployment.model.internal.policy.PolicyTemplateClassLoaderBuilder;
import org.mule.runtime.module.artifact.classloader.ArtifactClassLoaderFactory;
import org.mule.runtime.module.artifact.classloader.DeployableArtifactClassLoaderFactory;

/**
 * Creates {@link PolicyTemplateClassLoaderBuilder} for application artifacts.
 */
public class ApplicationPolicyTemplateClassLoaderBuilderFactory implements PolicyTemplateClassLoaderBuilderFactory {

  private final DeployableArtifactClassLoaderFactory artifactClassLoaderFactory;
  private final ArtifactClassLoaderFactory artifactPluginClassLoaderFactory;

  /**
   * Creates a new factory instance
   *
   * @param artifactClassLoaderFactory factory for the classloader specific to the artifact resource and classes. Must be not
   *        null.
   * @param artifactPluginClassLoaderFactory factory to create class loaders for each used plugin. Non be not null.
   */
  public ApplicationPolicyTemplateClassLoaderBuilderFactory(DeployableArtifactClassLoaderFactory artifactClassLoaderFactory,
                                                            ArtifactClassLoaderFactory artifactPluginClassLoaderFactory) {
    this.artifactClassLoaderFactory = artifactClassLoaderFactory;
    this.artifactPluginClassLoaderFactory = artifactPluginClassLoaderFactory;
  }

  @Override
  public PolicyTemplateClassLoaderBuilder createArtifactClassLoaderBuilder() {
    return new PolicyTemplateClassLoaderBuilder(artifactClassLoaderFactory,
                                                artifactPluginClassLoaderFactory);
  }
}
