/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.core.internal.source.scheduler;

import static org.mule.runtime.api.config.MuleRuntimeFeature.DISABLE_SCHEDULER_LOGGING;

import org.mule.runtime.api.component.execution.CompletableCallback;
import org.mule.runtime.api.config.FeatureFlaggingService;
import org.mule.runtime.api.functional.Either;
import org.mule.runtime.core.api.event.CoreEvent;
import org.mule.runtime.core.api.processor.Processor;
import org.mule.runtime.core.internal.exception.MessagingException;
import org.mule.runtime.core.internal.execution.FlowProcessingTemplate;
import org.mule.runtime.core.internal.execution.NotificationFunction;

import java.util.List;
import java.util.Map;

/**
 * Custom scheduler's flow processing template.
 *
 * @since 4.3.0
 */
final class SchedulerFlowProcessingTemplate extends FlowProcessingTemplate {

  private final DefaultSchedulerMessageSource defaultSchedulerMessageSource;
  private final FeatureFlaggingService featureFlaggingService;

  SchedulerFlowProcessingTemplate(Processor messageProcessor,
                                  List<NotificationFunction> notificationFunctions,
                                  DefaultSchedulerMessageSource defaultSchedulerMessageSource,
                                  FeatureFlaggingService featureFlaggingService) {
    super(messageProcessor, notificationFunctions);
    this.defaultSchedulerMessageSource = defaultSchedulerMessageSource;
    this.featureFlaggingService = featureFlaggingService;
  }

  @Override
  public void afterPhaseExecution(Either<MessagingException, CoreEvent> either) {
    defaultSchedulerMessageSource.setIsExecuting(false);
  }

  @Override
  public void sendResponseToClient(CoreEvent response, Map<String, Object> parameters, CompletableCallback<Void> callback) {
    callback.complete(null);
  }

  @Override
  public void sendFailureResponseToClient(MessagingException exception, Map<String, Object> parameters,
                                          CompletableCallback<Void> callback) {
    if (featureFlaggingService != null && featureFlaggingService.isEnabled(DISABLE_SCHEDULER_LOGGING)) {
      exception.getExceptionInfo().setAlreadyLogged(true);
    }
    callback.error(exception);
  }
}
