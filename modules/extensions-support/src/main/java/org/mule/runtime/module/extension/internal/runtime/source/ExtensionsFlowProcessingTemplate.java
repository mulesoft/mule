/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.module.extension.internal.runtime.source;

import static org.mule.runtime.core.api.rx.Exceptions.propagateWrappingFatal;

import org.mule.runtime.api.component.execution.CompletableCallback;
import org.mule.runtime.api.functional.Either;
import org.mule.runtime.core.api.event.CoreEvent;
import org.mule.runtime.core.api.processor.Processor;
import org.mule.runtime.core.api.util.func.CheckedFunction;
import org.mule.runtime.core.internal.exception.MessagingException;
import org.mule.runtime.core.internal.execution.FlowProcessingTemplate;
import org.mule.runtime.core.internal.execution.NotificationFunction;
import org.mule.runtime.core.internal.execution.SourceResultAdapter;

import java.util.List;
import java.util.Map;

final class ExtensionsFlowProcessingTemplate extends FlowProcessingTemplate {

  private final SourceResultAdapter sourceMessage;
  private final SourceCompletionHandler completionHandler;

  ExtensionsFlowProcessingTemplate(SourceResultAdapter sourceMessage,
                                   Processor messageProcessor,
                                   List<NotificationFunction> notificationFunctions, SourceCompletionHandler completionHandler) {
    super(messageProcessor, notificationFunctions);
    this.sourceMessage = sourceMessage;
    this.completionHandler = completionHandler;
  }

  @Override
  public CheckedFunction<CoreEvent, Map<String, Object>> getSuccessfulExecutionResponseParametersFunction() {
    return completionHandler::createResponseParameters;
  }

  @Override
  public CheckedFunction<CoreEvent, Map<String, Object>> getFailedExecutionResponseParametersFunction() {
    return completionHandler::createFailureResponseParameters;
  }

  @Override
  public SourceResultAdapter getSourceMessage() {
    return sourceMessage;
  }

  @Override
  public void sendResponseToClient(CoreEvent response, Map<String, Object> parameters, CompletableCallback<Void> callback) {
    completionHandler.onCompletion(response, parameters, callback);
  }

  @Override
  public void sendFailureResponseToClient(MessagingException exception,
                                          Map<String, Object> parameters,
                                          CompletableCallback<Void> callback) {
    completionHandler.onFailure(exception, parameters, callback);
  }

  @Override
  public void afterPhaseExecution(Either<MessagingException, CoreEvent> either) {
    try {
      completionHandler.onTerminate(either);
    } catch (Exception e) {
      throw propagateWrappingFatal(e);
    }
  }

}
