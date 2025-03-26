/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.test.petstore.extension;

import static java.util.Arrays.asList;
import static org.mule.runtime.api.meta.ExpressionSupport.NOT_SUPPORTED;
import static org.mule.runtime.extension.api.annotation.param.MediaType.TEXT_PLAIN;

import org.mule.runtime.api.exception.MuleException;
import org.mule.runtime.api.scheduler.SchedulerService;
import org.mule.runtime.extension.api.annotation.Expression;
import org.mule.runtime.extension.api.annotation.execution.OnError;
import org.mule.runtime.extension.api.annotation.execution.OnSuccess;
import org.mule.runtime.extension.api.annotation.execution.OnTerminate;
import org.mule.runtime.extension.api.annotation.metadata.MetadataScope;
import org.mule.runtime.extension.api.annotation.param.MediaType;
import org.mule.runtime.extension.api.annotation.param.Optional;
import org.mule.runtime.extension.api.annotation.param.Parameter;
import org.mule.runtime.extension.api.runtime.operation.Result;
import org.mule.runtime.extension.api.runtime.source.Source;
import org.mule.runtime.extension.api.runtime.source.SourceCallback;
import org.mule.runtime.extension.api.runtime.source.SourceCallbackContext;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ScheduledFuture;

import jakarta.inject.Inject;

@MetadataScope(outputResolver = PollingSourceMetadataResolver.class)
@MediaType(TEXT_PLAIN)
public class PetAdoptionSchedulerInParamSource extends Source<String, Void> {

  public static final List<String> ALL_PETS = asList("Grumpy Cat", "Colonel Meow", "Silvester", "Lil bub", "Macri", "Pappo");
  public static int ADOPTED_PET_COUNT;
  public static int FAILED_ADOPTION_COUNT;
  public static int COMPLETED_POLLS;
  public static int REJECTED_ADOPTIONS;
  public static int STARTED_POLLS;
  public static Long frequency;
  protected List<String> pets;

  @Parameter
  @Expression(NOT_SUPPORTED)
  // @NullSafe(defaultImplementingType = PrefetchTypeSubscriberFactory.class)
  @Optional
  private PetAdoptionPolling polling;

  @Inject
  private SchedulerService schedulerService;

  private org.mule.runtime.api.scheduler.Scheduler executor;
  private ScheduledFuture<?> scheduled;

  @Override
  public void onStart(SourceCallback<String, Void> sourceCallback) throws MuleException {
    pets = new ArrayList<>(ALL_PETS);
    resetCounters();

    executor = polling.createExecutor("adoption", schedulerService);
    scheduled = polling.getSchedulingStrategy().schedule(executor, () -> this.poll(sourceCallback));
    frequency = polling.getFrequency().get();
  }

  @Override
  public void onStop() {
    scheduled.cancel(false);
    executor.stop();

    pets.clear();
    resetCounters();
  }

  @OnSuccess
  public synchronized void onSuccess() {
    ADOPTED_PET_COUNT++;
  }

  @OnError
  public synchronized void onError() {
    FAILED_ADOPTION_COUNT++;
  }

  @OnTerminate
  public synchronized void onTerminate() {
    COMPLETED_POLLS++;
  }

  public void poll(SourceCallback<String, Void> sourceCallback) {
    STARTED_POLLS++;
    pets.stream()
        .map(p -> Result.<String, Void>builder().output(p).build())
        .forEach(sourceCallback::handle);
  }

  public void onRejectedItem(Result<String, Void> result, SourceCallbackContext context) {
    REJECTED_ADOPTIONS++;
  }

  private synchronized void resetCounters() {
    ADOPTED_PET_COUNT = FAILED_ADOPTION_COUNT = COMPLETED_POLLS = REJECTED_ADOPTIONS = STARTED_POLLS = 0;
  }

}
