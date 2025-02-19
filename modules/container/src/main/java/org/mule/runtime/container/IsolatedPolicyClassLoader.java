/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.container;

import org.mule.runtime.container.internal.FilteringContainerClassLoader;
import org.mule.runtime.module.artifact.api.classloader.MuleDeployableArtifactClassLoader;
import org.mule.runtime.module.artifact.api.classloader.RegionClassLoader;
import org.mule.runtime.module.artifact.api.descriptor.DeployableArtifactDescriptor;

import java.net.URL;

public class IsolatedPolicyClassLoader extends MuleDeployableArtifactClassLoader {

  private static volatile IsolatedPolicyClassLoader INSTANCE;


  private IsolatedPolicyClassLoader(String artifactId,
                                    DeployableArtifactDescriptor artifactDescriptor,
                                    RegionClassLoader regionClassLoader) {
    super(artifactId, artifactDescriptor, new URL[0], regionClassLoader, regionClassLoader.getClassLoaderLookupPolicy());
  }

  public static IsolatedPolicyClassLoader getInstance(RegionClassLoader regionClassLoader) {
    if (INSTANCE == null) {
      synchronized (IsolatedPolicyClassLoader.class) {
        if (INSTANCE == null) {
          if (regionClassLoader == null) {
            throw new IllegalStateException("regionClassLoader is not set yet, cannot create IsolatedPolicyClassLoader Instance");
          }
          INSTANCE = new IsolatedPolicyClassLoader(
                                                   "isolated-policy-classloader",
                                                   new DeployableArtifactDescriptor("isolated-policy-descriptor"),
                                                   regionClassLoader);
        }
      }
    }
    return INSTANCE;
  }


}
