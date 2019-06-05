/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.test.some.extension;

import static org.mule.runtime.extension.api.annotation.param.MediaType.TEXT_PLAIN;
import static org.slf4j.LoggerFactory.getLogger;
import org.mule.runtime.api.scheduler.SchedulerConfig;
import org.mule.runtime.api.scheduler.SchedulerService;
import org.mule.runtime.extension.api.annotation.param.MediaType;
import org.mule.runtime.extension.api.annotation.param.Parameter;
import org.mule.runtime.extension.api.runtime.operation.Result;
import org.mule.runtime.extension.api.runtime.source.Source;
import org.mule.runtime.extension.api.runtime.source.SourceCallback;

import java.util.concurrent.Future;
import java.util.concurrent.atomic.AtomicInteger;

import javax.inject.Inject;

import org.slf4j.Logger;

@MediaType(TEXT_PLAIN)
public class SomeEmittingSource extends Source<String, String> {

  private final Logger LOGGER = getLogger(SomeEmittingSource.class);

  @Inject
  private SchedulerService schedulerService;

  private static AtomicInteger emissions = new AtomicInteger(0);
  private Future<?> launchedEmitterFuture;

  @Parameter
  String message;

  @Parameter
  Integer times;

  @Override
  public void onStart(SourceCallback<String, String> sourceCallback) {
    emissions.set(times);
    launchedEmitterFuture = schedulerService.customScheduler(SchedulerConfig.config()
        .withName("Value emitting scheduler")
        .withMaxConcurrentTasks(1))
        .submit(() -> {
          while (!Thread.currentThread().isInterrupted() && emissions.getAndDecrement() > 0) {
            LOGGER.info("Emitting an event through flow");
            sourceCallback.handle(Result.<String, String>builder().output(message).build());
            try {
              Thread.sleep(1000);
            } catch (InterruptedException e) {
              LOGGER.error("Emitting thread was interrupted: ", e);
            }
          }
        });
  }

  @Override
  public void onStop() {
    launchedEmitterFuture.cancel(true);
  }

}
