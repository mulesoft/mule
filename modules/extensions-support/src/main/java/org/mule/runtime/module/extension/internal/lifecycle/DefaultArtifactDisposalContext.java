/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.module.extension.internal.lifecycle;

import static java.lang.Thread.currentThread;
import static java.util.Arrays.stream;

import static org.slf4j.LoggerFactory.getLogger;

import org.mule.runtime.core.internal.util.CompositeClassLoader;
import org.mule.runtime.module.artifact.api.classloader.ArtifactClassLoader;
import org.mule.sdk.api.artifact.lifecycle.ArtifactDisposalContext;
import org.mule.sdk.api.artifact.lifecycle.ArtifactLifecycleListener;

import java.util.stream.Stream;

import org.slf4j.Logger;

/**
 * Default implementation of {@link ArtifactDisposalContext}.
 *
 * @since 4.5.0
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
      ThreadGroup threadGroup = getTopLevelThreadGroup();
      Thread[] allThreads = new Thread[threadGroup.activeCount()];
      threadGroup.enumerate(allThreads);
      return stream(allThreads).filter(this::isArtifactOwnedThread);
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

  private ThreadGroup getTopLevelThreadGroup() {
    ThreadGroup threadGroup = currentThread().getThreadGroup();
    while (threadGroup.getParent() != null) {
      try {
        threadGroup = threadGroup.getParent();
      } catch (SecurityException e) {
        LOGGER
            .warn("An error occurred trying to obtain the active Threads for artifact [{}], and extension [{}]. Parent Thread Group is not accessible. Some threads may not be cleaned up.",
                  artifactClassLoader.getArtifactId(),
                  extensionClassLoader.getArtifactId(),
                  e);
        return threadGroup;
      }
    }

    return threadGroup;
  }
}
