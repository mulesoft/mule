/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.core.policy;

import org.mule.runtime.core.exception.MessagingException;

import java.util.Map;

public class FailureSourcePolicyResult {

  private final MessageSourceResponseParametersProcessor messageSourceResponseParametersProcessor;
  private final MessagingException messagingException;
  private final Map<String, Object> errorResponseParameters;

  public FailureSourcePolicyResult(MessagingException messagingException, Map<String, Object> errorResponseParameters,
                                   MessageSourceResponseParametersProcessor messageSourceResponseParametersProcessor) {
    this.messagingException = messagingException;
    this.errorResponseParameters = errorResponseParameters;
    this.messageSourceResponseParametersProcessor = messageSourceResponseParametersProcessor;
  }

  public MessagingException getMessagingException() {
    return messagingException;
  }

  public Map<String, Object> getErrorResponseParameters() {
    return errorResponseParameters;
  }
}
