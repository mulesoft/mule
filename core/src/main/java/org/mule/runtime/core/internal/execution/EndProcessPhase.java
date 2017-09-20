/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.core.internal.execution;

import org.mule.runtime.core.privileged.execution.EndPhaseTemplate;
import org.mule.runtime.core.privileged.execution.MessageProcessTemplate;
import org.mule.runtime.core.privileged.execution.MessageProcessContext;

/**
 * This phase notifies to the {@link MessageProcessTemplate} that the message processing has ended.
 *
 * To participate on this phase {@link MessageProcessTemplate} must implement {@link EndPhaseTemplate}.
 */
public class EndProcessPhase implements MessageProcessPhase<EndPhaseTemplate> {

  @Override
  public boolean supportsTemplate(MessageProcessTemplate messageProcessTemplate) {
    return messageProcessTemplate instanceof EndPhaseTemplate;
  }

  @Override
  public void runPhase(EndPhaseTemplate messageProcessTemplate, MessageProcessContext messageProcessContext,
                       PhaseResultNotifier phaseResultNotifier) {
    messageProcessTemplate.messageProcessingEnded();
  }

}
