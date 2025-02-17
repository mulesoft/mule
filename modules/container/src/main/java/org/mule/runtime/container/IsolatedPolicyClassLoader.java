/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.container;

import org.mule.runtime.container.internal.MuleClassLoaderLookupPolicy;
import org.mule.runtime.module.artifact.api.classloader.ChildFirstLookupStrategy;
import org.mule.runtime.module.artifact.api.classloader.ClassLoaderLookupPolicy;
import org.mule.runtime.module.artifact.api.classloader.DelegateOnlyLookupStrategy;
import org.mule.runtime.module.artifact.api.classloader.LookupStrategy;
import org.mule.runtime.module.artifact.api.classloader.MuleDeployableArtifactClassLoader;
import org.mule.runtime.module.artifact.api.classloader.ParentFirstLookupStrategy;
import org.mule.runtime.module.artifact.api.descriptor.DeployableArtifactDescriptor;

import java.net.URL;
import java.util.Collections;

public class IsolatedPolicyClassLoader extends MuleDeployableArtifactClassLoader {

  private static final IsolatedPolicyClassLoader INSTANCE = new IsolatedPolicyClassLoader(
                                                                                          "isolated-policy-classloader",
                                                                                          new DeployableArtifactDescriptor("isolated-policy-descriptor"),
                                                                                          null,
                                                                                          createIsolatedPolicyLookupPolicy());

  private IsolatedPolicyClassLoader(String artifactId,
                                    DeployableArtifactDescriptor artifactDescriptor,
                                    ClassLoader parent,
                                    ClassLoaderLookupPolicy lookupPolicy) {
    super(artifactId, artifactDescriptor, new URL[0], parent, lookupPolicy);
  }

  public static IsolatedPolicyClassLoader getInstance() {
    if (INSTANCE == null) {
      // todo: fill this
    }
    return INSTANCE;
  }

  private static ClassLoaderLookupPolicy createIsolatedPolicyLookupPolicy() {
    return new MuleClassLoaderLookupPolicy(Collections.emptyMap(), Collections.emptySet()) {

      @Override
      public LookupStrategy getClassLookupStrategy(String className) {
        if (className.startsWith("java.")) {
          return new DelegateOnlyLookupStrategy(ClassLoader.getSystemClassLoader());
        } else {
          return ChildFirstLookupStrategy.CHILD_FIRST;
        }
      }
    };
  }

}
