/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.test.marvel.ironman;

import static java.util.concurrent.Executors.newSingleThreadScheduledExecutor;
import static java.util.concurrent.TimeUnit.MILLISECONDS;
import static org.mule.runtime.api.message.NullAttributes.NULL_ATTRIBUTES;
import static org.mule.runtime.api.meta.model.ExecutionType.CPU_INTENSIVE;
import org.mule.runtime.api.lifecycle.Disposable;
import org.mule.runtime.api.lifecycle.Initialisable;
import org.mule.runtime.api.lifecycle.InitialisationException;
import org.mule.runtime.api.message.NullAttributes;
import org.mule.runtime.extension.api.annotation.execution.Execution;
import org.mule.runtime.extension.api.annotation.param.Connection;
import org.mule.runtime.extension.api.annotation.param.Config;
import org.mule.runtime.extension.api.runtime.operation.Result;
import org.mule.runtime.extension.api.runtime.process.CompletionCallback;
import org.mule.test.marvel.model.Missile;
import org.mule.test.marvel.model.Villain;

import java.util.concurrent.ScheduledExecutorService;

public class IronManOperations implements Initialisable, Disposable {

  public static final int MISSILE_TRAVEL_TIME = 200;
  public static final String FLIGHT_PLAN = "Go Straight";

  private ScheduledExecutorService executorService;

  @Override
  public void initialise() throws InitialisationException {
    executorService = newSingleThreadScheduledExecutor();
  }

  @Override
  public void dispose() {
    if (executorService != null) {
      executorService.shutdownNow();
    }
  }

  public void fireMissile(@Config IronMan ironMan,
                          @Connection Missile missile,
                          Villain at,
                          CompletionCallback<String, NullAttributes> callback) {
    final Runnable launch = () -> {
      try {
        ironMan.track(missile);
        callback.success(Result.<String, NullAttributes>builder()
            .output(missile.fireAt(at))
            .attributes(NULL_ATTRIBUTES).build());
      } catch (Exception e) {
        callback.error(e);
      }
    };

    // it takes the missile some time to reach target. Don't block while you kill
    executorService.schedule(launch, MISSILE_TRAVEL_TIME, MILLISECONDS);
  }

  @Execution(CPU_INTENSIVE)
  public void computeFlightPlan(@Config IronMan ironMan, CompletionCallback<Void, NullAttributes> callback) {
    final Runnable launch = () -> {
      callback.success(Result.<Void, NullAttributes>builder().build());
      ironMan.setFlightPlan(FLIGHT_PLAN);
    };

    // building a flight plan requires a lot of computation. Don't block while you kill
    executorService.schedule(launch, MISSILE_TRAVEL_TIME, MILLISECONDS);
  }
}
