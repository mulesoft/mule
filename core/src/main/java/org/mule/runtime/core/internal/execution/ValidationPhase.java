/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.core.internal.execution;

import org.mule.runtime.core.privileged.execution.FlowProcessingPhaseTemplate;
import org.mule.runtime.core.privileged.execution.MessageProcessContext;
import org.mule.runtime.core.privileged.execution.MessageProcessTemplate;
import org.mule.runtime.core.privileged.execution.ValidationPhaseTemplate;

/**
 * This phase validates the incoming message.
 *
 * To participate of this phase, {@link MessageProcessTemplate} must implement {@link ValidationPhaseTemplate}.
 */
public class ValidationPhase implements MessageProcessPhase<ValidationPhaseTemplate>, Comparable<MessageProcessPhase> {

  @Override
  public boolean supportsTemplate(MessageProcessTemplate messageProcessTemplate) {
    return messageProcessTemplate instanceof ValidationPhaseTemplate;
  }

  @Override
  public void runPhase(ValidationPhaseTemplate validationPhaseTemplate, MessageProcessContext messageProcessContext,
                       PhaseResultNotifier phaseResultNotifier) {
    try {
      if (!validationPhaseTemplate.validateMessage()) {
        validationPhaseTemplate.discardInvalidMessage();
        phaseResultNotifier.phaseConsumedMessage();
      } else {
        phaseResultNotifier.phaseSuccessfully();
      }
    } catch (Exception e) {
      phaseResultNotifier.phaseFailure(e);
    }
  }

  @Override
  public int compareTo(MessageProcessPhase messageProcessPhase) {
    if (messageProcessPhase instanceof FlowProcessingPhaseTemplate) {
      return -1;
    }
    return 0;
  }
}
