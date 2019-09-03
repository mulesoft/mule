/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.core.internal.source.scheduler;

import org.mule.runtime.core.api.event.CoreEvent;
import org.mule.runtime.core.api.functional.Either;
import org.mule.runtime.core.api.processor.Processor;
import org.mule.runtime.core.internal.exception.MessagingException;
import org.mule.runtime.core.internal.execution.ModuleFlowProcessingTemplate;
import org.mule.runtime.core.internal.execution.NotificationFunction;

import java.util.List;

/**
 * Custom scheduler's flow processing template.
 *
 * @since 4.2.2
 */
final class SchedulerFlowProcessingTemplate extends ModuleFlowProcessingTemplate {

  private DefaultSchedulerMessageSource defaultSchedulerMessageSource;

  SchedulerFlowProcessingTemplate(Processor messageProcessor,
                                  List<NotificationFunction> notificationFunctions,
                                  DefaultSchedulerMessageSource defaultSchedulerMessageSource) {
    super(messageProcessor, notificationFunctions);
    this.defaultSchedulerMessageSource = defaultSchedulerMessageSource;
  }

  @Override
  public void afterPhaseExecution(Either<MessagingException, CoreEvent> either) {
    defaultSchedulerMessageSource.setIsExecuting(false);
  }
}
