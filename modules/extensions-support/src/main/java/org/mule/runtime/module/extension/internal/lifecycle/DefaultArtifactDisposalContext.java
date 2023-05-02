/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.module.extension.internal.lifecycle;

import static java.lang.Thread.getAllStackTraces;

import static org.slf4j.LoggerFactory.getLogger;

import org.mule.runtime.core.internal.util.CompositeClassLoader;
import org.mule.runtime.module.artifact.api.classloader.ArtifactClassLoader;
import org.mule.sdk.api.artifact.lifecycle.ArtifactDisposalContext;
import org.mule.sdk.api.artifact.lifecycle.ArtifactLifecycleListener;

import java.util.stream.Stream;

import org.slf4j.Logger;

/**
 * Default implementation of {@link ArtifactDisposalContext}.
 */
public class DefaultArtifactDisposalContext implements ArtifactDisposalContext {

  private static final Logger LOGGER = getLogger(DefaultArtifactDisposalContext.class);

  private final ArtifactClassLoader artifactClassLoader;
  private final ArtifactClassLoader extensionClassLoader;

  /**
   * Creates an {@link ArtifactDisposalContext} from the given {@code artifactClassLoader} and {@code extensionClassLoader} pair.
   *
   * @param artifactClassLoader  the {@link ArtifactClassLoader} of the artifact being disposed.
   * @param extensionClassLoader the {@link ArtifactClassLoader} of the extension the {@link ArtifactLifecycleListener} is
   *                             associated with.
   */
  public DefaultArtifactDisposalContext(ArtifactClassLoader artifactClassLoader, ArtifactClassLoader extensionClassLoader) {
    this.artifactClassLoader = artifactClassLoader;
    this.extensionClassLoader = extensionClassLoader;
  }

  @Override
  public ClassLoader getExtensionClassLoader() {
    return extensionClassLoader.getClassLoader();
  }

  @Override
  public ClassLoader getArtifactClassLoader() {
    return artifactClassLoader.getClassLoader();
  }

  @Override
  public Stream<Thread> getArtifactOwnedThreads() {
    try {
      // This may be expensive because it needs to build all the stack traces which we don't actually need.
      // However, other alternatives like Thread#enumerate would require us to traverse up to the root Thread Group in order
      // to actually get all threads.
      // Because this is only executed on disposal, we prefer readability to marginal performance gains.
      return getAllStackTraces().keySet().stream().filter(this::isArtifactOwnedThread);
    } catch (SecurityException e) {
      LOGGER
          .warn("An error occurred trying to obtain the active Threads for artifact [{}], and extension [{}]. Thread cleanup will be skipped.",
                artifactClassLoader.getArtifactId(),
                extensionClassLoader.getArtifactId(),
                e);
      return Stream.empty();
    }
  }

  @Override
  public boolean isArtifactClassLoader(ClassLoader classLoader) {
    // Traverse the hierarchy for this ClassLoader searching for a matching artifact ID.
    while (classLoader != null) {
      if (classLoader instanceof ArtifactClassLoader) {
        String artifactId = ((ArtifactClassLoader) classLoader).getArtifactId();
        if (artifactClassLoader.getArtifactId().equals(artifactId) || extensionClassLoader.getArtifactId().equals(artifactId))
          return true;
      } else if (classLoader instanceof CompositeClassLoader) {
        // For CompositeClassLoaders we want to search through all its delegates
        for (ClassLoader delegate : ((CompositeClassLoader) classLoader).getDelegates()) {
          if (isArtifactClassLoader(delegate)) {
            return true;
          }
        }
      }
      classLoader = classLoader.getParent();
    }
    return false;
  }

  @Override
  public boolean isArtifactOwnedThread(Thread thread) {
    return isArtifactClassLoader(thread.getContextClassLoader());
  }
}
