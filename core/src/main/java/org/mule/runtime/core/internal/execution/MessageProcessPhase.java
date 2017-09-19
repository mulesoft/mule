/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.core.internal.execution;

import org.mule.runtime.core.privileged.execution.MessageProcessContext;
import org.mule.runtime.core.privileged.execution.MessageProcessTemplate;

/**
 *
 * Defines a phase that process a message using a {@link MessageProcessTemplate}
 *
 * The phase will be part of a chain of responsibility were the phase can define the end of the execution of the set of phases by
 * calling: - {@link PhaseResultNotifier#phaseConsumedMessage()} which indicates that the phase has consume the message and it
 * should not be longer processed - {@link PhaseResultNotifier#phaseFailure(Exception)} which indicates that there was a failure
 * during message processing.
 *
 * Whenever a phase finish execution it must call {@link PhaseResultNotifier#phaseSuccessfully()} which will cause the next phase
 * to be executed.
 *
 * Optionally a {@link MessageProcessPhase} can implement {@link Comparable<MessageProcessPhase>} to define the order in which it
 * must be positioned in the {@link MessageProcessPhase} chain
 *
 */
public interface MessageProcessPhase<Template extends MessageProcessTemplate> {

  /**
   * Determines if a certain phase supports a given template.
   *
   * If phase does not supports the template instance then the phase will be skipped.
   *
   * @param messageProcessTemplate template to be processed
   * @return true if the phase supports this template, false otherwise
   */
  boolean supportsTemplate(MessageProcessTemplate messageProcessTemplate);

  /**
   * Process the template through the phase.
   *
   * The phase execution can not throw an exception. In case of exception {@link PhaseResultNotifier#phaseFailure(Exception)} must
   * be call.
   *
   * @param messageProcessTemplate template containing message source specific behavior
   * @param messageProcessContext provides context information for executing the message
   * @param phaseResultNotifier notifier that must be advice under certain scenarios
   */
  void runPhase(Template messageProcessTemplate, MessageProcessContext messageProcessContext,
                PhaseResultNotifier phaseResultNotifier);

}
