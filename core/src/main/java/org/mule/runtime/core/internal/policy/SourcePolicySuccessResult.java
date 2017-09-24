/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.core.internal.policy;

import org.mule.runtime.core.api.event.CoreEvent;

import java.util.Map;
import java.util.function.Function;
import java.util.function.Supplier;

/**
 * Result of a successful execution of a {@link SourcePolicy}.
 *
 * It contains the {@link CoreEvent} result of the flow execution and the response parameters and error response parameters to be sent
 * by the source.
 *
 * @since 4.0
 */
public class SourcePolicySuccessResult {

  private final CoreEvent result;
  private final Supplier<Map<String, Object>> responseParameters;
  private final MessageSourceResponseParametersProcessor messageSourceResponseParametersProcessor;

  /**
   * Creates a new successful result from a policy execution.
   *
   * @param result the result of the flow execution.
   * @param responseParameters the response parameters to be sent by the source.
   * @param messageSourceResponseParametersProcessor a processor to create response parameters from an {@link CoreEvent}
   */
  public SourcePolicySuccessResult(CoreEvent result, Supplier<Map<String, Object>> responseParameters,
                                   MessageSourceResponseParametersProcessor messageSourceResponseParametersProcessor) {
    this.result = result;
    this.responseParameters = responseParameters;
    this.messageSourceResponseParametersProcessor = messageSourceResponseParametersProcessor;
  }

  /**
   * @return the result of the flow execution.
   */
  public CoreEvent getResult() {
    return result;
  }

  /**
   * @return the response parameters to be sent by the source.
   */
  public Supplier<Map<String, Object>> getResponseParameters() {
    return responseParameters;
  }

  /**
   * This method generates the set of error parameters from an {@link CoreEvent} to use on the error response function.
   * 
   * Even though this class represents a successful execution it may be that when evaluating the response parameters there's a
   * failure which most likely can be an expression execution error. In such scenarios the error handler must be executed and an
   * error response must be send. This method must be used to generate the error response parameters
   * 
   * @return the set of parameters to execute the function that sends the failure response.
   */
  public Function<CoreEvent, Map<String, Object>> createErrorResponseParameters() {
    return event -> messageSourceResponseParametersProcessor.getFailedExecutionResponseParametersFunction().apply(event);
  }
}
