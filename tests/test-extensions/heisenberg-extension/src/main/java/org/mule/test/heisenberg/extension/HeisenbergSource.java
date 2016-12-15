/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.test.heisenberg.extension;

import static java.lang.String.format;
import static java.util.concurrent.TimeUnit.MILLISECONDS;
import static org.mule.runtime.api.util.Preconditions.checkArgument;
import static org.mule.test.heisenberg.extension.HeisenbergExtension.RICIN_GROUP_NAME;
import org.mule.runtime.api.exception.MuleException;
import org.mule.runtime.api.message.Attributes;
import org.mule.runtime.api.message.Error;
import org.mule.runtime.api.scheduler.Scheduler;
import org.mule.runtime.core.api.scheduler.SchedulerService;
import org.mule.runtime.extension.api.annotation.Alias;
import org.mule.runtime.extension.api.annotation.execution.OnError;
import org.mule.runtime.extension.api.annotation.execution.OnSuccess;
import org.mule.runtime.extension.api.annotation.param.Connection;
import org.mule.runtime.extension.api.annotation.param.Optional;
import org.mule.runtime.extension.api.annotation.param.Parameter;
import org.mule.runtime.extension.api.annotation.param.ParameterGroup;
import org.mule.runtime.extension.api.annotation.param.UseConfig;
import org.mule.runtime.extension.api.annotation.source.EmitsResponse;
import org.mule.runtime.extension.api.runtime.operation.Result;
import org.mule.runtime.extension.api.runtime.source.Source;
import org.mule.runtime.extension.api.runtime.source.SourceCallback;

import java.math.BigDecimal;

import javax.inject.Inject;


@Alias("ListenPayments")
@EmitsResponse
public class HeisenbergSource extends Source<String, Attributes> {

  public static final String CORE_POOL_SIZE_ERROR_MESSAGE = "corePoolSize cannot be a negative value";
  public static final String INITIAL_BATCH_NUMBER_ERROR_MESSAGE = "initialBatchNumber cannot be a negative value";

  @Inject
  private SchedulerService schedulerService;

  private Scheduler executor;

  @UseConfig
  private HeisenbergExtension heisenberg;

  @Connection
  private HeisenbergConnection connection;

  @Parameter
  private volatile int initialBatchNumber;

  @Parameter
  @Optional(defaultValue = "1")
  private int corePoolSize;

  @Override
  public void onStart(SourceCallback<String, Attributes> sourceCallback) throws MuleException {
    checkArgument(heisenberg != null, "config not injected");
    connection.verifyLifecycle(1, 1, 0, 0);
    HeisenbergExtension.sourceTimesStarted++;

    if (corePoolSize < 0) {
      throw new RuntimeException(CORE_POOL_SIZE_ERROR_MESSAGE);
    }

    executor = schedulerService.cpuLightScheduler();
    executor.scheduleAtFixedRate(() -> sourceCallback.handle(makeResult(sourceCallback)), 0, 100, MILLISECONDS);
  }

  @OnSuccess
  public void onResponse(@Optional(defaultValue = "#[payload]") Long payment, @Optional String sameNameParameter,
                         @ParameterGroup(RICIN_GROUP_NAME) RicinGroup ricin) {
    heisenberg.setMoney(heisenberg.getMoney().add(BigDecimal.valueOf(payment)));
  }

  @OnError
  public void onError(Error error, @Optional String sameNameParameter) {
    heisenberg.setMoney(BigDecimal.valueOf(-1));
  }

  @Override
  public void onStop() {
    if (executor != null) {
      executor.shutdown();
      try {
        executor.awaitTermination(500, MILLISECONDS);
      } catch (InterruptedException e) {
        throw new RuntimeException(e);
      }
    }
  }

  private Result<String, Attributes> makeResult(SourceCallback sourceCallback) {
    if (initialBatchNumber < 0) {
      sourceCallback.onSourceException(new RuntimeException(INITIAL_BATCH_NUMBER_ERROR_MESSAGE));
    }

    return Result.<String, Attributes>builder()
        .output(format("Meth Batch %d. If found by DEA contact %s", ++initialBatchNumber, connection.getSaulPhoneNumber()))
        .build();
  }
}
