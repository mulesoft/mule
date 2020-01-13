/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.core.internal.policy;

import org.mule.runtime.core.api.event.CoreEvent;
import org.mule.runtime.core.internal.message.EventInternalContext;
import org.mule.runtime.core.internal.message.InternalEvent;
import org.mule.runtime.extension.api.runtime.operation.CompletableComponentExecutor.ExecutorCallback;

/**
 * Holds all the context information for an operation policy to function
 *
 * @since 4.3.0
 */
public class OperationPolicyContext implements EventInternalContext<OperationPolicyContext> {

  /**
   * Extracts an instance stored as an internal parameter in the given {@code event}.
   *
   * @param event
   * @return an {@link OperationPolicyContext} or {@code null} if none was set on the event
   */
  public static OperationPolicyContext from(CoreEvent event) {
    return (OperationPolicyContext) ((InternalEvent) event).<OperationPolicyContext>getOperationPolicyContext();
  }

  private CoreEvent originalEvent;
  private final OperationParametersProcessor operationParametersProcessor;
  private final OperationExecutionFunction operationExecutionFunction;
  private final ExecutorCallback operationCallerCallback;
  private InternalEvent nextOperationResponse;

  public OperationPolicyContext(OperationParametersProcessor operationParametersProcessor,
                                OperationExecutionFunction operationExecutionFunction,
                                ExecutorCallback operationCallerCallback) {
    this.operationParametersProcessor = operationParametersProcessor;
    this.operationExecutionFunction = operationExecutionFunction;
    this.operationCallerCallback = operationCallerCallback;
  }

  @Override
  public OperationPolicyContext copy() {
    return this;
  }

  public CoreEvent getOriginalEvent() {
    return originalEvent;
  }

  public void setOriginalEvent(CoreEvent originalEvent) {
    this.originalEvent = originalEvent;
  }

  public OperationParametersProcessor getOperationParametersProcessor() {
    return operationParametersProcessor;
  }

  public OperationExecutionFunction getOperationExecutionFunction() {
    return operationExecutionFunction;
  }

  public ExecutorCallback getOperationCallerCallback() {
    return operationCallerCallback;
  }

  public InternalEvent getNextOperationResponse() {
    return nextOperationResponse;
  }

  public void setNextOperationResponse(InternalEvent nextOperationResponse) {
    this.nextOperationResponse = nextOperationResponse;
  }
}
