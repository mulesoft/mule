/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.foo.withLifecycleListener;

import static java.lang.String.format;

import static org.slf4j.LoggerFactory.getLogger;

import org.mule.sdk.api.artifact.lifecycle.ArtifactDisposalContext;
import org.mule.sdk.api.artifact.lifecycle.ArtifactLifecycleListener;

import java.util.function.Consumer;

import org.slf4j.Logger;

public class LifecycleListener implements ArtifactLifecycleListener {

  private static Logger LOGGER = getLogger(LifecycleListener.class);

  @Override
  public void onArtifactDisposal(ArtifactDisposalContext disposalContext) {
    LOGGER.error("[EZE] LifecycleListener.onArtifactDisposal");
    assertThisClassIsLoadedWithExtensionClassLoader(disposalContext);
    LOGGER.error("[EZE] LifecycleListener.onArtifactDisposal - Class loaded with Extension ClassLoader");

    // With this we make sure the ClassLoaders are still usable inside the listener code.
    callArtifactDisposalCallback(disposalContext);
    LOGGER.error("[EZE] LifecycleListener.onArtifactDisposal - Callback executed");
    assertClassCanBeLoadedWith(disposalContext.getExtensionClassLoader(), "org.foo.withLifecycleListener.LeakedThread");
    LOGGER.error("[EZE] LifecycleListener.onArtifactDisposal - Class loaded with Extension ClassLoader");

    // If one of the avobe failed, the exception will make it skip the disposal code, and the associated test will fail.

    // Iterates through the threads that are owned by the extension being disposed of, calling the graceful stop methods and
    // joining them
    disposalContext.getExtensionOwnedThreads()
      .filter(LeakedThread.class::isInstance)
      .map(LeakedThread.class::cast)
      .forEach(t -> {
        LOGGER.error("[EZE] LifecycleListener.onArtifactDisposal - Stopping thread {}", t.getName());
        t.stopPlease();
        LOGGER.error("[EZE] LifecycleListener.onArtifactDisposal - Thread {} stopped", t.getName());
        try {
          LOGGER.error("[EZE] LifecycleListener.onArtifactDisposal - Joining thread {}", t.getName());
          t.join();
          LOGGER.error("[EZE] LifecycleListener.onArtifactDisposal - Thread {} joined", t.getName());
        } catch (InterruptedException e) {
          // Does nothing
        }
      });
  }

  private void callArtifactDisposalCallback(ArtifactDisposalContext artifactDisposalContext) {
    try {
      Class<?> artifactDisposalTrackerClass = artifactDisposalContext.getArtifactClassLoader().loadClass("org.foo.ArtifactDisposalTracker");
      artifactDisposalTrackerClass
        .getMethod("onArtifactDisposal", ArtifactDisposalContext.class)
        .invoke(null, artifactDisposalContext);
    } catch (ReflectiveOperationException e) {
      throw new RuntimeException(e);
    }
  }

  private void assertClassCanBeLoadedWith(ClassLoader classLoader, String className) {
    try {
      Class<?> cls = classLoader.loadClass(className);
    } catch (ClassNotFoundException e) {
      fail(format("Expected to be able to load class %s", className), e);
    }
  }

  private void assertThisClassIsLoadedWithExtensionClassLoader(ArtifactDisposalContext disposalContext) {
    if (this.getClass().getClassLoader() != disposalContext.getExtensionClassLoader()) {
      fail("LifecycleListener was not loaded with the Extension's ClassLoader");
    }
  }

  private void fail(String message) {
    AssertionError error = new AssertionError(message);
    LOGGER.error(message, error);
    throw error;
  }

  private void fail(String message, Throwable cause) {
    AssertionError error = new AssertionError(message, cause);
    LOGGER.error(message, error);
    throw error;
  }
}
