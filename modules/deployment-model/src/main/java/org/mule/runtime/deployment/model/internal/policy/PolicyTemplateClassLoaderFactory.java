/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.deployment.model.internal.policy;

import static org.mule.runtime.api.config.MuleRuntimeFeature.ENABLE_POLICY_ISOLATION;
import static org.mule.runtime.module.artifact.api.classloader.ChildFirstLookupStrategy.CHILD_FIRST;
import static org.mule.runtime.module.artifact.api.classloader.ChildOnlyLookupStrategy.CHILD_ONLY;
import static org.mule.runtime.module.artifact.internal.util.FeatureFlaggingUtils.isFeatureEnabled;

import org.mule.runtime.deployment.model.api.policy.PolicyTemplateDescriptor;
import org.mule.runtime.module.artifact.api.classloader.ArtifactClassLoader;
import org.mule.runtime.module.artifact.api.classloader.ClassLoaderLookupPolicy;
import org.mule.runtime.module.artifact.api.classloader.DeployableArtifactClassLoaderFactory;
import org.mule.runtime.module.artifact.api.classloader.MuleDeployableArtifactClassLoader;
import org.mule.runtime.module.artifact.api.descriptor.ArtifactPluginDescriptor;

import java.io.File;
import java.util.ArrayList;
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

    List<String> packages = new ArrayList<>(descriptor.getClassLoaderConfiguration().getExportedPackages());
    if (descriptor.getPlugins() != null) {
      for (ArtifactPluginDescriptor artifactPluginDescriptor : descriptor.getPlugins()) {
        packages.addAll(artifactPluginDescriptor.getClassLoaderConfiguration().getExportedPackages());
      }
    }

    ClassLoaderLookupPolicy classLoaderLookupPolicy;
    if (isFeatureEnabled(ENABLE_POLICY_ISOLATION, descriptor)) {
      classLoaderLookupPolicy = parent.getClassLoaderLookupPolicy().extend(packages.stream(), CHILD_FIRST, true);
    } else {
      classLoaderLookupPolicy = parent.getClassLoaderLookupPolicy();
    }

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
