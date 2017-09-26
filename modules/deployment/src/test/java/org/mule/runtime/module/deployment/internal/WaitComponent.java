/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
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
