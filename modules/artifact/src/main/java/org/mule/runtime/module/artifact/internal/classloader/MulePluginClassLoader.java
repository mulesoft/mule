/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 */
package org.mule.runtime.module.artifact.internal.classloader;

import org.mule.runtime.module.artifact.api.classloader.ClassLoaderLookupPolicy;
import org.mule.runtime.module.artifact.api.classloader.MuleArtifactClassLoader;
import org.mule.runtime.module.artifact.api.descriptor.ArtifactDescriptor;

import java.net.URL;
import java.util.HashSet;
import java.util.Set;

/**
 * Implementation of the ArtifactClassLoader interface for {@code mule-plugin}s, that manages shutdown listeners and has resource
 * releasers.
 */
public class MulePluginClassLoader extends MuleArtifactClassLoader implements WithAttachedClassLoaders {

  static {
    registerAsParallelCapable();
  }

  private final Set<ClassLoader> attachedClassLoaders = new HashSet<>();

  /**
   * Constructs a new {@link MulePluginClassLoader} for the given URLs
   *
   * @param artifactId         artifact unique ID. Non empty.
   * @param artifactDescriptor descriptor for the artifact owning the created class loader. Non null.
   * @param urls               the URLs from which to load classes and resources
   * @param parent             the parent class loader for delegation
   * @param lookupPolicy       policy used to guide the lookup process. Non null
   */
  public MulePluginClassLoader(String artifactId, ArtifactDescriptor artifactDescriptor, URL[] urls, ClassLoader parent,
                               ClassLoaderLookupPolicy lookupPolicy) {
    super(artifactId, artifactDescriptor, urls, parent, lookupPolicy);
  }

  @Override
  public void attachClassLoader(ClassLoader classLoader) {
    attachedClassLoaders.add(classLoader);
  }

  @Override
  public Set<ClassLoader> getAttachedClassLoaders() {
    return attachedClassLoaders;
  }
}
