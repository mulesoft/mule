/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.core.privileged.execution;

import org.mule.runtime.core.internal.execution.EndProcessPhase;
import org.mule.runtime.core.internal.execution.FlowProcessingPhase;
import org.mule.runtime.core.internal.execution.ValidationPhase;

/**
 * In charge of processing messages through mule.
 */
public interface MessageProcessingManager {

  /**
   * Process a message through a set of execution phases. At bear minimum three phases will be executed: - {@link ValidationPhase}
   * which will validates the message content. If message context is invalid then it will discard the message -
   * {@link FlowProcessingPhase} which will route the message through it's flow configuration - {@link EndProcessPhase} which will
   * be executed after all the phases have been executed
   *
   * @param messageProcessTemplate contains template methods that will be executed by each phase in specific parts of the phase so
   *        the {@link org.mule.runtime.core.api.source.MessageSource} can apply custom logic during message processing. The
   *        message will participate only on those phases were the template defines the required template methods
   * @param messageProcessContext defines the context of execution of the message
   */
  void processMessage(MessageProcessTemplate messageProcessTemplate, MessageProcessContext messageProcessContext);

}
