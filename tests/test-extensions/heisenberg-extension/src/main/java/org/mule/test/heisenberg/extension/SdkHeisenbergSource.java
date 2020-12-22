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
import static org.mule.runtime.api.metadata.DataType.fromType;
import static org.mule.runtime.api.util.Preconditions.checkArgument;
import static org.mule.runtime.extension.api.annotation.param.MediaType.TEXT_PLAIN;
import static org.mule.runtime.extension.api.annotation.param.Optional.PAYLOAD;
import static org.mule.runtime.extension.api.runtime.source.BackPressureMode.DROP;
import static org.mule.runtime.extension.api.runtime.source.BackPressureMode.FAIL;
import static org.mule.test.heisenberg.extension.HeisenbergExtension.HEISENBERG;
import static org.mule.test.heisenberg.extension.HeisenbergExtension.RICIN_GROUP_NAME;
import static org.mule.test.heisenberg.extension.HeisenbergNotificationAction.BATCH_DELIVERED;
import static org.mule.test.heisenberg.extension.HeisenbergNotificationAction.BATCH_DELIVERY_FAILED;
import static org.mule.test.heisenberg.extension.HeisenbergNotificationAction.BATCH_FAILED;
import static org.mule.test.heisenberg.extension.HeisenbergNotificationAction.BATCH_TERMINATED;
import static org.mule.test.heisenberg.extension.HeisenbergNotificationAction.NEW_BATCH;
import static org.mule.test.heisenberg.extension.HeisenbergNotificationAction.NEXT_BATCH;
import static org.mule.test.heisenberg.extension.HeisenbergSource.CORE_POOL_SIZE_ERROR_MESSAGE;
import static org.mule.test.heisenberg.extension.HeisenbergSource.TerminateStatus.ERROR_BODY;
import static org.mule.test.heisenberg.extension.HeisenbergSource.TerminateStatus.ERROR_INVOKE;
import static org.mule.test.heisenberg.extension.HeisenbergSource.TerminateStatus.NONE;
import static org.mule.test.heisenberg.extension.HeisenbergSource.TerminateStatus.SUCCESS;
import static org.mule.test.heisenberg.extension.HeisenbergSource.INITIAL_BATCH_NUMBER_ERROR_MESSAGE;
import static org.mule.test.heisenberg.extension.HeisenbergSource.receivedGroupOnSource;
import static org.mule.test.heisenberg.extension.HeisenbergSource.receivedInlineOnSuccess;
import static org.mule.test.heisenberg.extension.HeisenbergSource.receivedInlineOnError;
import static org.mule.test.heisenberg.extension.HeisenbergSource.terminateStatus;
import static org.mule.test.heisenberg.extension.HeisenbergSource.error;
import static org.mule.test.heisenberg.extension.HeisenbergSource.executedOnSuccess;
import static org.mule.test.heisenberg.extension.HeisenbergSource.executedOnError;
import static org.mule.test.heisenberg.extension.HeisenbergSource.executedOnTerminate;
import static org.mule.test.heisenberg.extension.HeisenbergSource.gatheredMoney;
import static org.mule.test.heisenberg.extension.HeisenbergSource.configName;
import static org.mule.test.heisenberg.extension.HeisenbergSource.location;
import static org.mule.test.heisenberg.extension.HeisenbergSource.receivedDebtProperties;
import static org.mule.test.heisenberg.extension.HeisenbergSource.receivedUsableWeapons;

import org.mule.runtime.api.component.location.ComponentLocation;
import org.mule.runtime.api.connection.ConnectionException;
import org.mule.runtime.api.connection.ConnectionProvider;
import org.mule.runtime.api.exception.MuleException;
import org.mule.runtime.api.message.Error;
import org.mule.runtime.api.metadata.DataType;
import org.mule.runtime.api.metadata.TypedValue;
import org.mule.runtime.api.scheduler.Scheduler;
import org.mule.runtime.api.scheduler.SchedulerService;
import org.mule.runtime.extension.api.annotation.Streaming;
import org.mule.runtime.extension.api.annotation.deprecated.Deprecated;
import org.mule.runtime.extension.api.annotation.execution.OnError;
import org.mule.runtime.extension.api.annotation.execution.OnSuccess;
import org.mule.runtime.extension.api.annotation.execution.OnTerminate;
import org.mule.runtime.extension.api.annotation.notification.Fires;
import org.mule.runtime.extension.api.annotation.param.Config;
import org.mule.runtime.extension.api.annotation.param.Connection;
import org.mule.runtime.extension.api.annotation.param.MediaType;
import org.mule.runtime.extension.api.annotation.param.NullSafe;
import org.mule.runtime.extension.api.annotation.param.Optional;
import org.mule.runtime.extension.api.annotation.param.Parameter;
import org.mule.runtime.extension.api.annotation.param.ParameterGroup;
import org.mule.runtime.extension.api.annotation.param.RefName;
import org.mule.runtime.extension.api.annotation.param.display.DisplayName;
import org.mule.runtime.extension.api.annotation.source.BackPressure;
import org.mule.runtime.extension.api.annotation.source.EmitsResponse;
import org.mule.runtime.extension.api.annotation.source.OnBackPressure;
import org.mule.runtime.extension.api.notification.NotificationEmitter;
import org.mule.runtime.extension.api.runtime.source.BackPressureContext;
import org.mule.runtime.extension.api.runtime.source.SourceResult;
import org.mule.sdk.api.runtime.operation.Result;
import org.mule.sdk.api.runtime.source.Source;
import org.mule.sdk.api.runtime.source.SourceCallback;
import org.mule.sdk.api.runtime.source.SourceCallbackContext;
import org.mule.test.heisenberg.extension.model.Methylamine;
import org.mule.test.heisenberg.extension.model.PersonalInfo;
import org.mule.test.heisenberg.extension.model.Weapon;

import java.util.Map;
import java.util.concurrent.ScheduledFuture;

import javax.inject.Inject;

@EmitsResponse
@Fires(SourceNotificationProvider.class)
@Streaming
@MediaType(TEXT_PLAIN)
@BackPressure(defaultMode = FAIL, supportedModes = {FAIL, DROP})
@Deprecated(message = "This source is being tapped by the DEA, it's usage is discouraged.", since = "1.6.0", toRemoveIn = "3.0.0")
public class SdkHeisenbergSource extends Source<String, Object> {

  private static final String BATCH_NUMBER = "batchNumber";

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

  @Parameter
  @Optional(defaultValue = "500")
  private long frequency;

  @RefName
  private String refName;

  @Parameter
  @Optional
  @NullSafe
  private Map<String, Object> debtProperties;

  @Parameter
  @Optional
  @NullSafe
  private Map<String, Weapon> usableWeapons;

  private ComponentLocation componentLocation;

  private HeisenbergConnection connection;

  public SdkHeisenbergSource() {
    resetHeisenbergSource();
  }

  @Override
  public synchronized void onStart(SourceCallback<String, Object> sourceCallback) throws MuleException {
    checkArgument(heisenberg != null, "config not injected");
    HeisenbergExtension.sourceTimesStarted++;
    configName = refName;
    location = componentLocation.getLocation();

    if (corePoolSize < 0) {
      throw new RuntimeException(CORE_POOL_SIZE_ERROR_MESSAGE);
    }

    receivedDebtProperties = debtProperties;
    receivedUsableWeapons = usableWeapons;

    executor = schedulerService.cpuLightScheduler();
    connection = connectionProvider.connect();
    scheduledFuture = executor.scheduleAtFixedRate(() -> {
      final Result<String, Object> result = makeResult(sourceCallback);
      if (result != null) {
        SourceCallbackContext context = sourceCallback.createContext();
        context.addVariable(BATCH_NUMBER, initialBatchNumber);
        context.fireOnHandle(NEW_BATCH, TypedValue.of(initialBatchNumber));
        context.fireOnHandle(NEXT_BATCH, TypedValue.of(frequency));
        sourceCallback.handle(result, context);
      }
    }, 0, frequency, MILLISECONDS);
  }

  @OnSuccess
  public synchronized void onSuccess(@Optional(defaultValue = PAYLOAD) Long payment, @Optional String sameNameParameter,
                                     @ParameterGroup(name = RICIN_GROUP_NAME) @DisplayName("Dangerous Ricin") RicinGroup ricin,
                                     @ParameterGroup(name = "Success Info", showInDsl = true) PersonalInfo successInfo,
                                     @Optional boolean fail,
                                     NotificationEmitter notificationEmitter) {

    gatheredMoney += payment;
    receivedGroupOnSource = ricin != null && ricin.getNextDoor().getAddress() != null;
    receivedInlineOnSuccess = successInfo != null && successInfo.getAge() != null && successInfo.getKnownAddresses() != null
        && successInfo.getDescription() != null;
    executedOnSuccess = true;

    notificationEmitter.fireLazy(BATCH_DELIVERED, () -> payment, fromType(Long.class));

    if (fail) {
      throw new RuntimeException("Some internal exception");
    }
  }

  @OnError
  public synchronized void onError(Error error, @Optional String sameNameParameter, @Optional Methylamine methylamine,
                                   @ParameterGroup(name = RICIN_GROUP_NAME) RicinGroup ricin,
                                   @ParameterGroup(name = "Error Info", showInDsl = true) PersonalInfo infoError,
                                   @Optional boolean propagateError,
                                   NotificationEmitter notificationEmitter) {
    gatheredMoney = -1;
    receivedGroupOnSource = ricin != null && ricin.getNextDoor() != null && ricin.getNextDoor().getAddress() != null;
    receivedInlineOnError = infoError != null && infoError.getName() != null && !infoError.getName().equals(HEISENBERG)
        && infoError.getDescription() != null;
    executedOnError = true;
    notificationEmitter.fireLazy(BATCH_DELIVERY_FAILED, () -> infoError, DataType.fromType(PersonalInfo.class));
    if (propagateError) {
      throw new RuntimeException("Some internal exception");
    }
  }

  @OnTerminate
  public synchronized void onTerminate(SourceResult sourceResult, NotificationEmitter notificationEmitter) {
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
    notificationEmitter.fireLazy(BATCH_TERMINATED, () -> sourceResult.getSourceCallbackContext().getVariable(BATCH_NUMBER).get(),
                                 fromType(Integer.class));
  }

  @OnBackPressure
  public void onBackPressure(BackPressureContext ctx, NotificationEmitter notificationEmitter) {
    notificationEmitter.fireLazy(BATCH_FAILED, () -> ctx.getSourceCallbackContext().getVariable(BATCH_NUMBER).get(),
                                 fromType(Integer.class));
    heisenberg.onBackPressure(ctx);
  }

  @Override
  public synchronized void onStop() {
    if (executor != null && scheduledFuture != null) {
      scheduledFuture.cancel(true);
      executor.stop();
    }

    if (connection != null && connectionProvider != null) {
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

  public static synchronized void resetHeisenbergSource() {
    receivedGroupOnSource = false;
    receivedInlineOnSuccess = false;
    receivedInlineOnError = false;

    receivedDebtProperties = null;
    receivedUsableWeapons = null;

    terminateStatus = NONE;
    error = null;

    executedOnSuccess = false;
    executedOnError = false;
    executedOnTerminate = false;
    gatheredMoney = 0;
    location = null;
    configName = null;
  }
}
