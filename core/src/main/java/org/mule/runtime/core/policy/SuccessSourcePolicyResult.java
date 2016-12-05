/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.core.policy;

import org.mule.runtime.core.api.Event;

import java.util.Map;

/**
 * Result of a successful execution of a {@link SourcePolicy}.
 *
 * It contains the {@link Event} result of the flow execution and the response parameters and error response parameters to be sent
 * by the source.
 *
 * @since 4.0
 */
public class SuccessSourcePolicyResult {

  private final Event flowExecutionResult;
  private final Map<String, Object> responseParameters;
  private final MessageSourceResponseParametersProcessor messageSourceResponseParametersProcessor;

  /**
   * Creates a new successful result from a policy execution.
   *
   * @param flowExecutionResult the result of the flow execution.
   * @param responseParameters the response parameters to be sent by the source.
   * @param messageSourceResponseParametersProcessor a processor to create response parameters from an {@link Event}
   */
  public SuccessSourcePolicyResult(Event flowExecutionResult, Map<String, Object> responseParameters,
                                   MessageSourceResponseParametersProcessor messageSourceResponseParametersProcessor) {
    this.flowExecutionResult = flowExecutionResult;
    this.responseParameters = responseParameters;
    this.messageSourceResponseParametersProcessor = messageSourceResponseParametersProcessor;
  }

  /**
   * @return the result of the flow execution.
   */
  public Event getFlowExecutionResult() {
    return flowExecutionResult;
  }

  /**
   * @return the response parameters to be sent by the source.
   */
  public Map<String, Object> getResponseParameters() {
    return responseParameters;
  }

  /**
   * This method generates the set of error parameters from an {@link Event} to use on the error response function.
   * 
   * Even though this class represents a successful execution it may be that when evaluating the response parameters there's a
   * failure which most likely can be an expression execution error. In such scenarios the error handler must be executed and an
   * error response must be send. This method must be used to generate the error response parameters
   * 
   * @param event the event to use as source for generating the error response parameters.
   * @return the set of parameters to execute the function that sends the failure response.
   */
  public Map<String, Object> createErrorResponseParameters(Event event) {
    return messageSourceResponseParametersProcessor.getFailedExecutionResponseParametersFunction().apply(event);
  }
}
