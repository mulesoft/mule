/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 */
package org.mule.runtime.module.artifact.api.classloader;

import static org.mule.runtime.api.util.Preconditions.checkArgument;
import org.mule.api.annotation.NoInstantiate;
import org.mule.runtime.module.artifact.api.descriptor.ArtifactDescriptor;

/**
 * Tracks {@link ArtifactClassLoader} created by {@link ArtifactClassLoaderFactory}
 */
@NoInstantiate
public final class TrackingArtifactClassLoaderFactory<T extends ArtifactDescriptor> implements ArtifactClassLoaderFactory<T> {

  private final ArtifactClassLoaderManager artifactClassLoaderManager;
  private final ArtifactClassLoaderFactory<T> artifactClassLoaderFactory;

  /**
   * Tracks the classloader created by another factory
   *
   * @param artifactClassLoaderManager tracks each created class loader. Non null.
   * @param artifactClassLoaderFactory factory that creates the class loaders to be tracked. Non null.
   */
  public TrackingArtifactClassLoaderFactory(ArtifactClassLoaderManager artifactClassLoaderManager,
                                            ArtifactClassLoaderFactory<T> artifactClassLoaderFactory) {
    checkArgument(artifactClassLoaderManager != null, "artifactClassLoaderManager cannot be null");
    checkArgument(artifactClassLoaderFactory != null, "artifactClassLoaderFactory cannot be null");
    this.artifactClassLoaderManager = artifactClassLoaderManager;
    this.artifactClassLoaderFactory = artifactClassLoaderFactory;
  }

  @Override
  public ArtifactClassLoader create(String artifactId, T descriptor, ClassLoader parent,
                                    ClassLoaderLookupPolicy lookupPolicy) {
    ArtifactClassLoader artifactClassLoader = artifactClassLoaderFactory.create(artifactId, descriptor, parent, lookupPolicy);

    track(artifactClassLoader);
    return artifactClassLoader;
  }

  private void track(ArtifactClassLoader artifactClassLoader) {
    artifactClassLoaderManager.register(artifactClassLoader);
    artifactClassLoader.addShutdownListener(() -> artifactClassLoaderManager.unregister(artifactClassLoader.getArtifactId()));
  }
}
