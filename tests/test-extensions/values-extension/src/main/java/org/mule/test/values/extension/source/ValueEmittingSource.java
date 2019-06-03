/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.test.values.extension.source;

import static org.slf4j.LoggerFactory.getLogger;
import org.mule.runtime.extension.api.annotation.param.Parameter;
import org.mule.runtime.extension.api.runtime.operation.Result;
import org.mule.runtime.extension.api.runtime.source.SourceCallback;

import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

import org.slf4j.Logger;

public class ValueEmittingSource extends AbstractSource {

  private final Logger LOGGER = getLogger(ValueEmittingSource.class);

  private Thread emittingThread;
  private static AtomicBoolean emitEnabled = new AtomicBoolean(false);
  private static AtomicInteger emissions = new AtomicInteger(0);

  @Parameter
  String message;

  @Parameter
  Integer times;

  @Override
  public void onStart(SourceCallback<String, String> sourceCallback) {

    emitEnabled.set(true);
    emissions.set(times);

    emittingThread = new Thread(() -> {
      while (!Thread.currentThread().isInterrupted() && emissions.getAndDecrement() > 0) {
        if (emitEnabled.get()) {
          LOGGER.info("Emitting an event through flow");
          sourceCallback.handle(Result.<String, String>builder().output(message).build());
        }
        try {
          Thread.sleep(1000);
        } catch (InterruptedException e) {
          LOGGER.error("Emitting thread was interrupted: ", e);
        }
      }
    }, "Value emitting thread");
    emittingThread.start();
  }

  @Override
  public void onStop() {
    emittingThread.interrupt();
  }

  public static void setEmitEnabled(boolean value) {
    emitEnabled.set(value);
  }

  public static int getEmissions() {
    return emissions.get();
  }
}
