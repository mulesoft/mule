/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.container;

import static org.mule.runtime.api.util.Preconditions.checkArgument;

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

  public static synchronized IsolatedPolicyClassLoader getInstance(RegionClassLoader regionClassLoader) {
    checkArgument(regionClassLoader != null, "regionClassLoader cannot be null");
    if (INSTANCE == null) {
      INSTANCE = new IsolatedPolicyClassLoader(
                                               "isolated-policy-classloader",
                                               new DeployableArtifactDescriptor("isolated-policy-descriptor"),
                                               regionClassLoader);
    }
    return INSTANCE;
  }
}
