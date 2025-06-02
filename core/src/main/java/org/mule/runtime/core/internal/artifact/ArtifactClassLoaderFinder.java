/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.core.internal.artifact;

import static java.util.Optional.empty;

import java.util.Optional;
import java.util.ServiceLoader;

/**
 * Allows to resolve the proper classloader to use on certain use cases, unwinding the classloader hierarchy of the deployable
 * artifacts in the Mule Runtime.
 * 
 * @since 4.10
 */
public interface ArtifactClassLoaderFinder {

  static ArtifactClassLoaderFinder artifactClassLoaderFinder() {
    final var iterator =
        ServiceLoader.load(ArtifactClassLoaderFinder.class, ArtifactClassLoaderFinder.class.getClassLoader()).iterator();
    if (iterator.hasNext()) {
      return iterator.next();
    }

    return () -> empty();
  }

  /**
   * @return the {@code regionClassLoader} of the TCCL if it is for a {@code deployableArtifact}, {@link Optional#empty() empty}
   *         otherwise.
   */
  Optional<ClassLoader> findRegionContextClassLoader();

}
