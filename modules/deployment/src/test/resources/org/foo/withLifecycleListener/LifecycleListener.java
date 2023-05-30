/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.foo.withLifecycleListener;

import static java.lang.String.format;

import static org.slf4j.LoggerFactory.getLogger;

import org.mule.sdk.api.artifact.lifecycle.ArtifactDisposalContext;
import org.mule.sdk.api.artifact.lifecycle.ArtifactLifecycleListener;

import org.slf4j.Logger;

public class LifecycleListener implements ArtifactLifecycleListener {

  private static Logger LOGGER = getLogger(LifecycleListener.class);

  @Override
  public void onArtifactDisposal(ArtifactDisposalContext disposalContext) {
    assertThisClassIsLoadedWithExtensionClassLoader(disposalContext);

    // With this we make sure the ClassLoaders are still usable inside the listener code.
    assertClassCanBeLoadedWith(disposalContext.getArtifactClassLoader(), "org.foo.EchoTest");
    assertClassCanBeLoadedWith(disposalContext.getExtensionClassLoader(), "org.foo.withLifecycleListener.LeakedThread");

    // If one of the avobe failed, the exception will make it skip the disposal code, and the associated test will fail.

    // Iterates through the threads that are owned by the extension being disposed of, calling the graceful stop methods and
    // joining them
    disposalContext.getExtensionOwnedThreads()
      .filter(LeakedThread.class::isInstance)
      .map(LeakedThread.class::cast)
      .forEach(t -> {
        t.stopPlease();
        try {
          t.join();
        } catch (InterruptedException e) {
          // Does nothing
        }
      });
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
