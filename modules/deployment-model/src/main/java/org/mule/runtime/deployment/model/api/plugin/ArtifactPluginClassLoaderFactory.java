/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.deployment.model.api.plugin;

import org.mule.runtime.module.artifact.api.classloader.ArtifactClassLoader;
import org.mule.runtime.module.artifact.api.classloader.ArtifactClassLoaderFactory;
import org.mule.runtime.module.artifact.api.classloader.ClassLoaderLookupPolicy;
import org.mule.runtime.module.artifact.api.descriptor.ArtifactPluginDescriptor;
import org.mule.runtime.module.artifact.internal.classloader.MulePluginClassLoader;

/**
 * Creates {@link ArtifactClassLoader} for application or domain plugin descriptors.
 */
// TODO W-10964385: make this a delegate to an ArtifactClassLoaderResolver
public final class ArtifactPluginClassLoaderFactory implements ArtifactClassLoaderFactory<ArtifactPluginDescriptor> {

  @Override
  public ArtifactClassLoader create(String artifactId, ArtifactPluginDescriptor descriptor,
                                    ClassLoader parent,
                                    ClassLoaderLookupPolicy lookupPolicy) {
    return new MulePluginClassLoader(artifactId, descriptor, descriptor.getClassLoaderConfiguration().getUrls(), parent,
                                     lookupPolicy);
  }


}
