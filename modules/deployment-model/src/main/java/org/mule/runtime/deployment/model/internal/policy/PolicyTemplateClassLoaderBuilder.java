/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.runtime.deployment.model.internal.policy;

import static org.apache.commons.lang.StringUtils.isEmpty;
import static org.mule.runtime.api.util.Preconditions.checkArgument;
import org.mule.runtime.deployment.model.api.plugin.ArtifactPluginRepository;
import org.mule.runtime.deployment.model.internal.AbstractArtifactClassLoaderBuilder;
import org.mule.runtime.deployment.model.internal.plugin.PluginDependenciesResolver;
import org.mule.runtime.module.artifact.classloader.ArtifactClassLoader;
import org.mule.runtime.module.artifact.classloader.ArtifactClassLoaderFactory;
import org.mule.runtime.module.artifact.classloader.RegionClassLoader;
import org.mule.runtime.module.artifact.descriptor.ArtifactDescriptor;

/**
 * Builds the class loader to use on a {@link org.mule.runtime.deployment.model.api.policy.PolicyTemplate}
 */
public class PolicyTemplateClassLoaderBuilder extends AbstractArtifactClassLoaderBuilder {

  private final ArtifactClassLoaderFactory artifactClassLoaderFactory;
  private ArtifactClassLoader parentClassLoader;

  /**
   * Creates an {@link AbstractArtifactClassLoaderBuilder}.
   * 
   * @param artifactClassLoaderFactory factory for the classloader specific to the artifact resource and classes. Must be not
   *        null.
   * @param artifactPluginRepository repository of plugins contained by the runtime. Must be not null.
   * @param artifactPluginClassLoaderFactory factory to create class loaders for each used plugin. Non be not null.
   * @param pluginDependenciesResolver resolves artifact plugin dependencies. Non null
   */
  public PolicyTemplateClassLoaderBuilder(ArtifactClassLoaderFactory artifactClassLoaderFactory,
                                          ArtifactPluginRepository artifactPluginRepository,
                                          ArtifactClassLoaderFactory artifactPluginClassLoaderFactory,
                                          PluginDependenciesResolver pluginDependenciesResolver) {
    super(artifactPluginRepository, artifactPluginClassLoaderFactory, pluginDependenciesResolver);
    this.artifactClassLoaderFactory = artifactClassLoaderFactory;
  }


  @Override
  protected ArtifactClassLoader createArtifactClassLoader(String artifactId, RegionClassLoader regionClassLoader) {
    return artifactClassLoaderFactory.create(artifactId, regionClassLoader, artifactDescriptor);
  }

  @Override
  protected ArtifactClassLoader getParentClassLoader() {
    return parentClassLoader;
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
