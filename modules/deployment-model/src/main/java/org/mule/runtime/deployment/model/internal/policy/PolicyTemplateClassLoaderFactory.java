/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 */
package org.mule.runtime.deployment.model.internal.policy;

import org.mule.runtime.deployment.model.api.policy.PolicyTemplateDescriptor;
import org.mule.runtime.module.artifact.api.classloader.ArtifactClassLoader;
import org.mule.runtime.module.artifact.api.classloader.ClassLoaderLookupPolicy;
import org.mule.runtime.module.artifact.api.classloader.DeployableArtifactClassLoaderFactory;
import org.mule.runtime.module.artifact.api.classloader.MuleDeployableArtifactClassLoader;

import java.io.File;
import java.util.List;

/**
 * Creates {@link ArtifactClassLoader} for policy templates artifact.
 */
public class PolicyTemplateClassLoaderFactory implements DeployableArtifactClassLoaderFactory<PolicyTemplateDescriptor> {

  @Override
  public ArtifactClassLoader create(String artifactId, ArtifactClassLoader parent, PolicyTemplateDescriptor descriptor) {
    File rootFolder = descriptor.getRootFolder();
    if (rootFolder == null || !rootFolder.exists()) {
      throw new IllegalArgumentException("Policy folder does not exists: " + (rootFolder != null ? rootFolder.getName() : null));
    }

    final ClassLoaderLookupPolicy classLoaderLookupPolicy = parent.getClassLoaderLookupPolicy();

    MuleDeployableArtifactClassLoader deployableArtifactClassLoader =
        new MuleDeployableArtifactClassLoader(artifactId, descriptor, descriptor.getClassLoaderConfiguration().getUrls(),
                                              parent.getClassLoader(),
                                              classLoaderLookupPolicy);

    return deployableArtifactClassLoader;
  }

  @Override
  public ArtifactClassLoader create(String artifactId, ArtifactClassLoader parent, PolicyTemplateDescriptor descriptor,
                                    List<ArtifactClassLoader> artifactPluginClassLoaders) {
    return create(artifactId, parent, descriptor);
  }
}
