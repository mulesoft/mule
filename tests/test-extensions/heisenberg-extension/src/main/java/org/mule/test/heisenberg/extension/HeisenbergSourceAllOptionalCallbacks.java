/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.test.heisenberg.extension;

import static org.mule.runtime.api.util.Preconditions.checkArgument;
import static org.mule.runtime.extension.api.annotation.param.MediaType.TEXT_PLAIN;
import static org.mule.runtime.extension.api.annotation.param.Optional.PAYLOAD;
import static org.mule.runtime.extension.api.runtime.source.BackPressureMode.DROP;
import static org.mule.runtime.extension.api.runtime.source.BackPressureMode.FAIL;
import static org.mule.test.heisenberg.extension.HeisenbergExtension.RICIN_GROUP_NAME;
import static org.mule.test.heisenberg.extension.HeisenbergSourceAllOptionalCallbacks.TerminateStatus.ERROR_BODY;
import static org.mule.test.heisenberg.extension.HeisenbergSourceAllOptionalCallbacks.TerminateStatus.ERROR_INVOKE;
import static org.mule.test.heisenberg.extension.HeisenbergSourceAllOptionalCallbacks.TerminateStatus.NONE;
import static org.mule.test.heisenberg.extension.HeisenbergSourceAllOptionalCallbacks.TerminateStatus.SUCCESS;

import static java.lang.String.format;
import static java.util.Optional.empty;
import static java.util.Optional.of;
import static java.util.concurrent.TimeUnit.MILLISECONDS;

import org.mule.runtime.api.connection.ConnectionProvider;
import org.mule.runtime.api.exception.MuleException;
import org.mule.runtime.api.message.Error;
import org.mule.runtime.api.scheduler.Scheduler;
import org.mule.runtime.api.scheduler.SchedulerService;
import org.mule.runtime.extension.api.annotation.Alias;
import org.mule.runtime.extension.api.annotation.Streaming;
import org.mule.runtime.extension.api.annotation.deprecated.Deprecated;
import org.mule.runtime.extension.api.annotation.execution.OnError;
import org.mule.runtime.extension.api.annotation.execution.OnSuccess;
import org.mule.runtime.extension.api.annotation.execution.OnTerminate;
import org.mule.runtime.extension.api.annotation.notification.Fires;
import org.mule.runtime.extension.api.annotation.param.Config;
import org.mule.runtime.extension.api.annotation.param.Connection;
import org.mule.runtime.extension.api.annotation.param.MediaType;
import org.mule.runtime.extension.api.annotation.param.Optional;
import org.mule.runtime.extension.api.annotation.param.ParameterGroup;
import org.mule.runtime.extension.api.annotation.param.display.DisplayName;
import org.mule.runtime.extension.api.annotation.source.BackPressure;
import org.mule.runtime.extension.api.annotation.source.EmitsResponse;
import org.mule.runtime.extension.api.annotation.source.OnBackPressure;
import org.mule.runtime.extension.api.runtime.operation.Result;
import org.mule.runtime.extension.api.runtime.source.BackPressureContext;
import org.mule.runtime.extension.api.runtime.source.Source;
import org.mule.runtime.extension.api.runtime.source.SourceCallback;
import org.mule.runtime.extension.api.runtime.source.SourceCallbackContext;
import org.mule.runtime.extension.api.runtime.source.SourceResult;
import org.mule.test.heisenberg.extension.model.Methylamine;
import org.mule.test.heisenberg.extension.model.PersonalInfoAllOptional;

import java.util.concurrent.ScheduledFuture;

import jakarta.inject.Inject;

@Alias("ListenPaymentsAllOptional")
@EmitsResponse
@Fires(SourceNotificationProvider.class)
@Streaming
@MediaType(TEXT_PLAIN)
@BackPressure(defaultMode = FAIL, supportedModes = {FAIL, DROP})
@Deprecated(message = "This source is being tapped by the DEA, it's usage is discouraged.", since = "1.6.0", toRemoveIn = "3.0.0")
public class HeisenbergSourceAllOptionalCallbacks extends Source<String, Object> {

  public static volatile boolean receivedGroupOnSource;
  public static volatile boolean receivedInlineOnSuccess;
  public static volatile boolean receivedInlineOnError;

  public static volatile PersonalInfoAllOptional receivedInlineOnSuccessData;
  public static volatile PersonalInfoAllOptional receivedInlineOnErrorData;

  public static volatile TerminateStatus terminateStatus;
  public static java.util.Optional<Error> error;

  public static volatile boolean executedOnSuccess;
  public static volatile boolean executedOnError;
  public static volatile boolean executedOnTerminate;

  @Inject
  private SchedulerService schedulerService;

  private Scheduler executor;
  private ScheduledFuture<?> scheduledFuture;

  @Config
  private HeisenbergExtension heisenberg;

  @Connection
  private ConnectionProvider<HeisenbergConnection> connectionProvider;

  private HeisenbergConnection connection;

  public HeisenbergSourceAllOptionalCallbacks() {
    resetHeisenbergSource();
  }

  @Override
  public synchronized void onStart(SourceCallback<String, Object> sourceCallback) throws MuleException {
    checkArgument(heisenberg != null, "config not injected");
    HeisenbergExtension.sourceTimesStarted++;

    executor = schedulerService.cpuLightScheduler();
    connection = connectionProvider.connect();
    scheduledFuture = executor.scheduleAtFixedRate(() -> {
      final Result<String, Object> result = makeResult(sourceCallback);
      if (result != null) {
        SourceCallbackContext context = sourceCallback.createContext();
        sourceCallback.handle(result, context);
      }
    }, 0, 500, MILLISECONDS);
  }

  @OnSuccess
  public synchronized void onSuccess(@Optional(defaultValue = PAYLOAD) Long payment,
                                     @org.mule.sdk.api.annotation.param.Optional String sameNameParameter,
                                     @ParameterGroup(name = RICIN_GROUP_NAME) @DisplayName("Dangerous Ricin") RicinGroup ricin,
                                     @ParameterGroup(name = "Success Info", showInDsl = true) PersonalInfoAllOptional successInfo,
                                     @Optional boolean fail) {
    receivedGroupOnSource = ricin != null && ricin.getNextDoor().getAddress() != null;
    receivedInlineOnSuccess = successInfo != null && successInfo.getAge() != null && successInfo.getKnownAddresses() != null;
    receivedInlineOnSuccessData = successInfo;
    executedOnSuccess = true;

    if (fail) {
      throw new RuntimeException("Some internal exception");
    }
  }

  @OnError
  public synchronized void onError(Error error, @Optional String sameNameParameter, @Optional Methylamine methylamine,
                                   @ParameterGroup(name = RICIN_GROUP_NAME) RicinGroup ricin,
                                   @ParameterGroup(name = "Error Info", showInDsl = true) PersonalInfoAllOptional infoError,
                                   @org.mule.sdk.api.annotation.param.Optional boolean propagateError) {
    receivedGroupOnSource = ricin != null && ricin.getNextDoor() != null && ricin.getNextDoor().getAddress() != null;
    receivedInlineOnError = infoError != null && infoError.getAge() != null && infoError.getKnownAddresses() != null;
    receivedInlineOnErrorData = infoError;
    executedOnError = true;

    if (propagateError) {
      throw new RuntimeException("Some internal exception");
    }
  }

  @OnTerminate
  public synchronized void onTerminate(SourceResult sourceResult) {
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

  @OnBackPressure
  public void onBackPressure(BackPressureContext ctx) {
    heisenberg.onBackPressure(ctx);
  }

  @Override
  public synchronized void onStop() {
    receivedInlineOnSuccessData = null;
    receivedInlineOnErrorData = null;

    if (executor != null && scheduledFuture != null) {
      scheduledFuture.cancel(true);
      executor.stop();
    }

    if (connection != null && connectionProvider != null) {
      connectionProvider.disconnect(connection);
    }

    receivedGroupOnSource = false;
  }

  private Result<String, Object> makeResult(SourceCallback sourceCallback) {
    return Result.<String, Object>builder()
        .output(format("Meth Batch. If found by DEA contact %s", connection.getSaulPhoneNumber()))
        .build();
  }

  public enum TerminateStatus {
    SUCCESS, ERROR_INVOKE, ERROR_BODY, NONE
  }

  public static synchronized void resetHeisenbergSource() {
    receivedGroupOnSource = false;
    receivedInlineOnSuccess = false;
    receivedInlineOnError = false;

    terminateStatus = NONE;
    error = null;

    executedOnSuccess = false;
    executedOnError = false;
    executedOnTerminate = false;

    receivedInlineOnSuccessData = null;
    receivedInlineOnErrorData = null;

  }
}
