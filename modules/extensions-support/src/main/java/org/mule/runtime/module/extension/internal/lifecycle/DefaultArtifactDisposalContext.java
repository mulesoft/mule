/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.module.extension.internal.lifecycle;

import static java.lang.String.format;
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
  public boolean isExtensionOwnedClassLoader(ClassLoader classLoader) {
    return isOwnedClassLoader(extensionClassLoader, classLoader);
  }

  @Override
  public boolean isArtifactOwnedClassLoader(ClassLoader classLoader) {
    return isOwnedClassLoader(artifactClassLoader, classLoader);
  }

  @Override
  public Stream<Thread> getExtensionOwnedThreads() {
    return getAllThreads().filter(this::isExtensionOwnedThread);
  }

  @Override
  public Stream<Thread> getArtifactOwnedThreads() {
    return getAllThreads().filter(this::isArtifactOwnedThread);
  }

  @Override
  public boolean isExtensionOwnedThread(Thread thread) {
    return isExtensionOwnedClassLoader(thread.getContextClassLoader());
  }

  @Override
  public boolean isArtifactOwnedThread(Thread thread) {
    return isArtifactOwnedClassLoader(thread.getContextClassLoader());
  }

  private boolean isOwnedClassLoader(ArtifactClassLoader ownerClassLoader, ClassLoader classLoader) {
    // Traverse the hierarchy for this ClassLoader searching for the same instance of the ownerClassLoader.
    while (classLoader != null) {
      if (classLoader == ownerClassLoader.getClassLoader()) {
        return true;
      } else if (classLoader instanceof CompositeClassLoader) {
        // For CompositeClassLoaders we want to search through all its delegates
        for (ClassLoader delegate : ((CompositeClassLoader) classLoader).getDelegates()) {
          if (isOwnedClassLoader(ownerClassLoader, delegate)) {
            return true;
          }
        }
      }
      classLoader = classLoader.getParent();
    }
    return false;
  }

  private ThreadGroup getTopLevelThreadGroup() {
    ThreadGroup threadGroup = currentThread().getThreadGroup();
    while (threadGroup.getParent() != null) {
      try {
        threadGroup = threadGroup.getParent();
      } catch (SecurityException e) {
        LOGGER
            .debug(format("An error occurred trying to obtain the active Threads for artifact [%s], and extension [%s]. Parent Thread Group is not accessible. Some threads may not be cleaned up.",
                          artifactClassLoader.getArtifactId(), extensionClassLoader.getArtifactId()),
                   e);
        return threadGroup;
      }
    }

    return threadGroup;
  }

  private Stream<Thread> getAllThreads() {
    try {
      ThreadGroup threadGroup = getTopLevelThreadGroup();
      Thread[] allThreads = new Thread[threadGroup.activeCount()];
      threadGroup.enumerate(allThreads);
      return stream(allThreads);
    } catch (SecurityException e) {
      LOGGER
          .warn(format("An error occurred trying to obtain the active Threads for artifact [%s], and extension [%s]. Thread cleanup will be skipped.",
                       artifactClassLoader.getArtifactId(),
                       extensionClassLoader.getArtifactId()),
                e);
      return Stream.empty();
    }
  }
}
