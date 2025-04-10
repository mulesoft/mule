/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.container.internal;

import static org.mule.runtime.api.util.Preconditions.checkArgument;

import org.mule.runtime.module.artifact.api.classloader.MuleDeployableArtifactClassLoader;
import org.mule.runtime.module.artifact.api.classloader.RegionClassLoader;
import org.mule.runtime.module.artifact.api.descriptor.DeployableArtifactDescriptor;

import java.net.URL;

/**
 * Dedicated {@link ClassLoader} providing complete isolation for Mule policies.
 * <p>
 * Addresses class loading limitations of the partial isolation mechanism associated with {@code mule.enable.policy.isolation}.
 * Ensures policies run in a fully independent environment, typically enabled via the
 * {@code mule.policy.isolation.separateClassLoader} system property.
 */
public class IsolatedPolicyClassLoader extends MuleDeployableArtifactClassLoader {

  private static IsolatedPolicyClassLoader instance;

  private IsolatedPolicyClassLoader(String artifactId,
                                    DeployableArtifactDescriptor artifactDescriptor,
                                    RegionClassLoader regionClassLoader) {
    super(artifactId, artifactDescriptor, new URL[0], regionClassLoader, regionClassLoader.getClassLoaderLookupPolicy());
  }

  public static IsolatedPolicyClassLoader getInstance(FilteringContainerClassLoader containerClassLoader) {
    checkArgument(containerClassLoader != null, "containerClassLoader cannot be null");
    if (instance == null) {
      RegionClassLoader regionClassLoader =
          new RegionClassLoader(containerClassLoader.getArtifactId(),
                                containerClassLoader.getArtifactDescriptor(),
                                containerClassLoader,
                                containerClassLoader.getClassLoaderLookupPolicy());

      instance = new IsolatedPolicyClassLoader(
                                               "isolated-policy-classloader",
                                               new DeployableArtifactDescriptor("isolated-policy-descriptor"),
                                               regionClassLoader);
    }
    return instance;
  }
}
