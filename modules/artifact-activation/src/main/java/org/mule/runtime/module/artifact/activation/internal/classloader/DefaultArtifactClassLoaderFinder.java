/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.module.artifact.activation.internal.classloader;

import static java.lang.Thread.currentThread;
import static java.util.Optional.empty;
import static java.util.Optional.of;

import org.mule.runtime.core.internal.artifact.ArtifactClassLoaderFinder;
import org.mule.runtime.module.artifact.activation.api.classloader.ApplicationClassLoader;
import org.mule.runtime.module.artifact.activation.api.classloader.DomainClassLoader;

import java.util.Optional;

public class DefaultArtifactClassLoaderFinder implements ArtifactClassLoaderFinder {

  @Override
  public Optional<ClassLoader> findRegionContextClassLoader() {
    final ClassLoader tccl = currentThread().getContextClassLoader();

    if (tccl != null && tccl.getParent() != null
        && (tccl instanceof ApplicationClassLoader
            || tccl instanceof DomainClassLoader)) {
      return of(tccl.getParent());
    } else {
      return empty();
    }
  }

}
