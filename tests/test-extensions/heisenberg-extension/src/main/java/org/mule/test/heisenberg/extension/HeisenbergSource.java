/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.test.heisenberg.extension;

import static java.lang.String.format;
import static java.util.Optional.empty;
import static java.util.Optional.of;
import static java.util.concurrent.TimeUnit.MILLISECONDS;
import static org.mule.runtime.api.util.Preconditions.checkArgument;
import static org.mule.runtime.extension.api.annotation.param.MediaType.TEXT_PLAIN;
import static org.mule.runtime.extension.api.annotation.param.Optional.PAYLOAD;
import static org.mule.test.heisenberg.extension.HeisenbergExtension.HEISENBERG;
import static org.mule.test.heisenberg.extension.HeisenbergExtension.RICIN_GROUP_NAME;
import static org.mule.test.heisenberg.extension.HeisenbergSource.TerminateStatus.ERROR_BODY;
import static org.mule.test.heisenberg.extension.HeisenbergSource.TerminateStatus.ERROR_INVOKE;
import static org.mule.test.heisenberg.extension.HeisenbergSource.TerminateStatus.NONE;
import static org.mule.test.heisenberg.extension.HeisenbergSource.TerminateStatus.SUCCESS;
import org.mule.runtime.api.component.location.ComponentLocation;
import org.mule.runtime.api.connection.ConnectionException;
import org.mule.runtime.api.connection.ConnectionProvider;
import org.mule.runtime.api.exception.MuleException;
import org.mule.runtime.api.message.Error;
import org.mule.runtime.api.scheduler.Scheduler;
import org.mule.runtime.api.scheduler.SchedulerService;
import org.mule.runtime.extension.api.annotation.Alias;
import org.mule.runtime.extension.api.annotation.Streaming;
import org.mule.runtime.extension.api.annotation.execution.OnError;
import org.mule.runtime.extension.api.annotation.execution.OnSuccess;
import org.mule.runtime.extension.api.annotation.execution.OnTerminate;
import org.mule.runtime.extension.api.annotation.param.Config;
import org.mule.runtime.extension.api.annotation.param.Connection;
import org.mule.runtime.extension.api.annotation.param.MediaType;
import org.mule.runtime.extension.api.annotation.param.Optional;
import org.mule.runtime.extension.api.annotation.param.Parameter;
import org.mule.runtime.extension.api.annotation.param.ParameterGroup;
import org.mule.runtime.extension.api.annotation.param.RefName;
import org.mule.runtime.extension.api.annotation.source.EmitsResponse;
import org.mule.runtime.extension.api.runtime.operation.Result;
import org.mule.runtime.extension.api.runtime.source.Source;
import org.mule.runtime.extension.api.runtime.source.SourceCallback;
import org.mule.runtime.extension.api.runtime.source.SourceResult;
import org.mule.test.heisenberg.extension.model.Methylamine;
import org.mule.test.heisenberg.extension.model.PersonalInfo;

import java.util.concurrent.ScheduledFuture;

import javax.inject.Inject;

@Alias("ListenPayments")
@EmitsResponse
@Streaming
@MediaType(TEXT_PLAIN)
public class HeisenbergSource extends Source<String, Object> {

  public static final String CORE_POOL_SIZE_ERROR_MESSAGE = "corePoolSize cannot be a negative value";
  public static final String INITIAL_BATCH_NUMBER_ERROR_MESSAGE = "initialBatchNumber cannot be a negative value";

  public static boolean receivedGroupOnSource;
  public static boolean receivedInlineOnSuccess;
  public static boolean receivedInlineOnError;

  public static TerminateStatus terminateStatus;
  public static java.util.Optional<Error> error;

  public static boolean executedOnSuccess;
  public static boolean executedOnError;
  public static boolean executedOnTerminate;
  public static long gatheredMoney;

  public static String configName;
  public static String location;

  @Inject
  private SchedulerService schedulerService;

  private Scheduler executor;
  private ScheduledFuture<?> scheduledFuture;

  @Config
  private HeisenbergExtension heisenberg;

  @Connection
  private ConnectionProvider<HeisenbergConnection> connectionProvider;

  @Parameter
  private volatile int initialBatchNumber;

  @Parameter
  @Optional(defaultValue = "1")
  private int corePoolSize;

  @RefName
  private String refName;

  private ComponentLocation componentLocation;

  private HeisenbergConnection connection;

  public HeisenbergSource() {
    receivedGroupOnSource = false;
    receivedInlineOnSuccess = false;
    receivedInlineOnError = false;

    terminateStatus = NONE;

    executedOnSuccess = false;
    executedOnError = false;
    executedOnTerminate = false;
    gatheredMoney = 0;
    location = null;
    configName = null;
  }

  @Override
  public void onStart(SourceCallback<String, Object> sourceCallback) throws MuleException {
    checkArgument(heisenberg != null, "config not injected");
    HeisenbergExtension.sourceTimesStarted++;
    configName = refName;
    location = componentLocation.getLocation();

    if (corePoolSize < 0) {
      throw new RuntimeException(CORE_POOL_SIZE_ERROR_MESSAGE);
    }

    executor = schedulerService.cpuLightScheduler();
    connection = connectionProvider.connect();
    scheduledFuture = executor.scheduleAtFixedRate(() -> {
      final Result<String, Object> result = makeResult(sourceCallback);
      if (result != null) {
        sourceCallback.handle(result);
      }
    }, 0, 300, MILLISECONDS);
  }

  @OnSuccess
  public void onSuccess(@Optional(defaultValue = PAYLOAD) Long payment, @Optional String sameNameParameter,
                        @ParameterGroup(name = RICIN_GROUP_NAME) RicinGroup ricin,
                        @ParameterGroup(name = "Success Info", showInDsl = true) PersonalInfo successInfo,
                        @Optional boolean fail) {

    gatheredMoney += payment;
    receivedGroupOnSource = ricin != null && ricin.getNextDoor().getAddress() != null;
    receivedInlineOnSuccess = successInfo != null && successInfo.getAge() != null && successInfo.getKnownAddresses() != null;
    executedOnSuccess = true;

    if (fail) {
      throw new RuntimeException("Some internal exception");
    }
  }

  @OnError
  public void onError(Error error, @Optional String sameNameParameter, @Optional Methylamine methylamine,
                      @ParameterGroup(name = RICIN_GROUP_NAME) RicinGroup ricin,
                      @ParameterGroup(name = "Error Info", showInDsl = true) PersonalInfo infoError,
                      @Optional boolean propagateError) {
    gatheredMoney = -1;
    receivedGroupOnSource = ricin != null && ricin.getNextDoor() != null && ricin.getNextDoor().getAddress() != null;
    receivedInlineOnError = infoError != null && infoError.getName() != null && !infoError.getName().equals(HEISENBERG);
    executedOnError = true;
    if (propagateError) {
      throw new RuntimeException("Some internal exception");
    }
  }

  @OnTerminate
  public void onTerminate(SourceResult sourceResult) {
    if (sourceResult.isSuccess()) {
      terminateStatus = SUCCESS;
      error = empty();
    } else {
      sourceResult.getInvocationError().ifPresent(parameterError -> {
        terminateStatus = ERROR_INVOKE;
        error = of(parameterError);
      });

      sourceResult.getResponseError().ifPresent(bodyError -> {
        terminateStatus = ERROR_BODY;
        error = of(bodyError);
      });
    }
    executedOnTerminate = true;
  }

  @Override
  public void onStop() {
    if (executor != null) {
      scheduledFuture.cancel(true);
      executor.stop();
    }

    if (connection != null) {
      connectionProvider.disconnect(connection);
    }

    receivedGroupOnSource = false;
    gatheredMoney = 0;
  }

  private Result<String, Object> makeResult(SourceCallback sourceCallback) {
    if (initialBatchNumber < 0) {
      sourceCallback.onConnectionException(new ConnectionException(INITIAL_BATCH_NUMBER_ERROR_MESSAGE));
      return null;
    }

    return Result.<String, Object>builder()
        .output(format("Meth Batch %d. If found by DEA contact %s", ++initialBatchNumber, connection.getSaulPhoneNumber()))
        .build();
  }

  public enum TerminateStatus {
    SUCCESS, ERROR_INVOKE, ERROR_BODY, NONE
  }
}
