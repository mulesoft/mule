/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 */
package org.mule.test.runner.classloader;

import org.mule.runtime.module.artifact.api.classloader.ClassLoaderLookupPolicy;
import org.mule.runtime.module.artifact.api.classloader.DefaultArtifactClassLoaderFilter;
import org.mule.runtime.module.artifact.api.classloader.RegionClassLoader;
import org.mule.runtime.module.artifact.api.descriptor.ArtifactDescriptor;

import java.util.Set;

/**
 * {@link RegionClassLoader} specialization that provides a simpler api to add classloaders to the region.
 *
 * @since 4.2
 */
public final class TestRegionClassLoader extends RegionClassLoader {

  static {
    registerAsParallelCapable();
  }

  public TestRegionClassLoader(ClassLoader parent, ClassLoaderLookupPolicy lookupPolicy) {
    super("Region", new ArtifactDescriptor("Region"), parent, lookupPolicy);
  }

  /**
   * Adds a class loader to the region.
   *
   * @see RegionClassLoader#addClassLoader(org.mule.runtime.module.artifact.api.classloader.ArtifactClassLoader,
   *      org.mule.runtime.module.artifact.api.classloader.ArtifactClassLoaderFilter)
   */
  public void addContent(ClassLoader parentClassLoader, Set<String> exportedClassPackages,
                         Set<String> exportedResources) {
    addClassLoader(new DelegatingArtifactClassLoader(parentClassLoader),
                   new DefaultArtifactClassLoaderFilter(exportedClassPackages, exportedResources));
  }
}
