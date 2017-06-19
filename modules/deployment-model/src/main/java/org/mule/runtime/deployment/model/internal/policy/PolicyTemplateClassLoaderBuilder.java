/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.runtime.deployment.model.internal.policy;

import static org.apache.commons.lang3.StringUtils.isEmpty;
import static org.mule.runtime.api.util.Preconditions.checkArgument;
import org.mule.runtime.deployment.model.internal.AbstractArtifactClassLoaderBuilder;
import org.mule.runtime.module.artifact.classloader.ArtifactClassLoader;
import org.mule.runtime.module.artifact.classloader.ArtifactClassLoaderFactory;
import org.mule.runtime.module.artifact.classloader.DeployableArtifactClassLoaderFactory;
import org.mule.runtime.module.artifact.classloader.MuleDeployableArtifactClassLoader;
import org.mule.runtime.module.artifact.classloader.RegionClassLoader;
import org.mule.runtime.module.artifact.descriptor.ArtifactDescriptor;

import java.io.IOException;

/**
 * Builds the class loader to use on a {@link org.mule.runtime.deployment.model.api.policy.PolicyTemplate}
 */
public class PolicyTemplateClassLoaderBuilder extends AbstractArtifactClassLoaderBuilder<PolicyTemplateClassLoaderBuilder> {

  private final DeployableArtifactClassLoaderFactory artifactClassLoaderFactory;
  private ArtifactClassLoader parentClassLoader;

  /**
   * Creates an {@link AbstractArtifactClassLoaderBuilder}.
   * 
   * @param artifactClassLoaderFactory factory for the classloader specific to the artifact resource and classes. Must be not
   *        null.
   * @param artifactPluginClassLoaderFactory factory to create class loaders for each used plugin. Non be not null.
   */
  public PolicyTemplateClassLoaderBuilder(DeployableArtifactClassLoaderFactory artifactClassLoaderFactory,
                                          ArtifactClassLoaderFactory artifactPluginClassLoaderFactory) {
    super(artifactPluginClassLoaderFactory);
    this.artifactClassLoaderFactory = artifactClassLoaderFactory;
  }


  @Override
  protected ArtifactClassLoader createArtifactClassLoader(String artifactId, RegionClassLoader regionClassLoader) {
    return artifactClassLoaderFactory.create(artifactId, regionClassLoader, artifactDescriptor, artifactPluginClassLoaders);
  }

  @Override
  protected ArtifactClassLoader getParentClassLoader() {
    return parentClassLoader;
  }

  @Override
  public MuleDeployableArtifactClassLoader build() throws IOException {
    return (MuleDeployableArtifactClassLoader) super.build();
  }

  public PolicyTemplateClassLoaderBuilder setParentClassLoader(ArtifactClassLoader parentClassLoader) {
    this.parentClassLoader = parentClassLoader;

    return this;
  }

  @Override
  protected String getArtifactId(ArtifactDescriptor artifactDescriptor) {
    return getPolicyId(artifactDescriptor.getName());
  }

  public String getPolicyId(String policyName) {
    checkArgument(!isEmpty(policyName), "policyName cannot be empty");

    return parentClassLoader.getArtifactId() + "/policy/" + policyName;
  }
}
