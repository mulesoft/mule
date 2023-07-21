/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 */
package org.mule.runtime.module.deployment.internal;

import org.mule.runtime.api.lifecycle.Initialisable;
import org.mule.runtime.api.lifecycle.InitialisationException;
import org.mule.runtime.api.util.concurrent.Latch;

public class WaitComponent implements Initialisable {

  public static Latch componentInitializedLatch = new Latch();
  public static Latch waitLatch = new Latch();

  @Override
  public void initialise() throws InitialisationException {
    try {
      componentInitializedLatch.release();
      waitLatch.await();
    } catch (InterruptedException e) {
      throw new InitialisationException(e, this);
    }
  }

  public static void reset() {
    componentInitializedLatch = new Latch();
    waitLatch = new Latch();
  }
}
