/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.core.internal.policy;

import org.mule.runtime.core.internal.exception.MessagingException;

import java.util.Map;
import java.util.function.Supplier;

/**
 * Result for an execution of a policy {@link org.mule.runtime.core.api.processor.Processor} which failed the executing by
 * throwing a {@link MessagingException}.
 *
 * @since 4.0
 */
public class SourcePolicyFailureResult {

  private final MessagingException messagingException;
  private final Supplier<Map<String, Object>> errorResponseParameters;

  /**
   * Creates a new failed policy result.
   *
   * @param messagingException the exception thrown by the policy chain
   * @param errorResponseParameters the error response parameters to be used by the source to send the response
   */
  public SourcePolicyFailureResult(MessagingException messagingException, Supplier<Map<String, Object>> errorResponseParameters) {
    this.messagingException = messagingException;
    this.errorResponseParameters = errorResponseParameters;
  }

  /**
   * @return the messaging exception result of the execution of the policy chain
   */
  public MessagingException getMessagingException() {
    return messagingException;
  }

  /**
   * @return the set of response parameters to be send by the message source
   */
  public Supplier<Map<String, Object>> getErrorResponseParameters() {
    return errorResponseParameters;
  }
}
