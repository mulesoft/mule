/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.core.internal.policy;

import org.mule.runtime.core.api.event.CoreEvent;
import org.mule.runtime.core.internal.message.InternalEvent;
import org.mule.runtime.core.privileged.event.BaseEventContext;
import org.mule.runtime.extension.api.runtime.operation.CompletableComponentExecutor.ExecutorCallback;

public class OperationPolicyContext {

  public static final String OPERATION_POLICY_CONTEXT = "operation.policy.context";

  public static OperationPolicyContext from(CoreEvent result) {
    return ((InternalEvent) result).getInternalParameter(OPERATION_POLICY_CONTEXT);
  }

  private CoreEvent originalEvent;
  private OperationParametersProcessor operationParametersProcessor;
  private OperationExecutionFunction operationExecutionFunction;
  private BaseEventContext operationChildContext;
  private ExecutorCallback operationCallerCallback;
  private InternalEvent nextOperationResponse;

  public OperationPolicyContext(OperationParametersProcessor operationParametersProcessor,
                                OperationExecutionFunction operationExecutionFunction,
                                BaseEventContext operationChildContext,
                                ExecutorCallback operationCallerCallback) {
    this.operationParametersProcessor = operationParametersProcessor;
    this.operationExecutionFunction = operationExecutionFunction;
    this.operationChildContext = operationChildContext;
    this.operationCallerCallback = operationCallerCallback;
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

  public BaseEventContext getOperationChildContext() {
    return operationChildContext;
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
