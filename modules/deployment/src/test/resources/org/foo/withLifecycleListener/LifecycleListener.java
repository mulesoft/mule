/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.foo.withLifecycleListener;

import org.mule.sdk.api.artifact.lifecycle.ArtifactDisposalContext;
import org.mule.sdk.api.artifact.lifecycle.ArtifactLifecycleListener;

public class LifecycleListener implements ArtifactLifecycleListener {

  @Override
  public void onArtifactDisposal(ArtifactDisposalContext disposalContext) {
    try {
      // With this we make sure the ClassLoaders are still usable inside the listener code.
      Class<?> muleContextClass = disposalContext.getArtifactClassLoader().loadClass("org.foo.EchoTest");
      Class<?> leakingThreadClass = disposalContext.getExtensionClassLoader().loadClass("org.foo.withLifecycleListener.LeakingThread");
    } catch (ClassNotFoundException e) {
      // If one of the avobe failed, we will skip the disposal code, and the associated test will fail.
      throw new RuntimeException(e);
    }

    // Iterates through the threads that are owned by the artifact being disposed of, calling the graceful stop methods and
    // joining them
    disposalContext.getArtifactOwnedThreads()
      .filter(LeakingThread.class::isInstance)
      .map(LeakingThread.class::cast)
      .forEach(t -> {
        t.stopPlease();
        try {
          t.join();
        } catch (InterruptedException e) {
          // Does nothing
        }
      });
  }
}
