/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.runtime.deployment.model.internal.policy;

import org.mule.runtime.deployment.model.api.policy.PolicyTemplateDescriptor;
import org.mule.runtime.module.artifact.classloader.ArtifactClassLoader;
import org.mule.runtime.module.artifact.classloader.ArtifactClassLoaderFactory;
import org.mule.runtime.module.artifact.classloader.ClassLoaderLookupPolicy;
import org.mule.runtime.module.artifact.classloader.MuleArtifactClassLoader;

import java.io.File;

/**
 * Creates {@link ArtifactClassLoader} for policy templates artifact.
 */
public class PolicyTemplateClassLoaderFactory implements ArtifactClassLoaderFactory<PolicyTemplateDescriptor> {

  @Override
  public ArtifactClassLoader create(String artifactId, ArtifactClassLoader parent, PolicyTemplateDescriptor descriptor) {
    File rootFolder = descriptor.getRootFolder();
    if (rootFolder == null || !rootFolder.exists()) {
      throw new IllegalArgumentException("Policy folder does not exists: " + (rootFolder != null ? rootFolder.getName() : null));
    }

    final ClassLoaderLookupPolicy classLoaderLookupPolicy = parent.getClassLoaderLookupPolicy();

    return new MuleArtifactClassLoader(artifactId, descriptor,
                                       descriptor.getClassLoaderModel().getUrls(),
                                       parent.getClassLoader(),
                                       classLoaderLookupPolicy);
  }
}
