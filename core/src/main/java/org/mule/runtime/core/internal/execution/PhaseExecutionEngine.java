/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.core.internal.execution;

import static org.mule.runtime.core.api.util.ClassUtils.withContextClassLoader;
import org.mule.runtime.core.api.exception.SystemExceptionHandler;
import org.mule.runtime.core.privileged.execution.EndPhaseTemplate;
import org.mule.runtime.core.privileged.execution.MessageProcessContext;
import org.mule.runtime.core.privileged.execution.MessageProcessTemplate;

import java.util.List;

/**
 * This class process a message through a set of {@link MessageProcessPhase} using the message
 * content and message processing context provided by {@link MessageProcessTemplate} and
 * {@link MessageProcessContext}.
 * <p>
 * This class will handle any message processing failure by calling the
 * {@link org.mule.runtime.core.api.exception.SystemExceptionHandler} defined by the application.
 * <p>
 * Each {@link MessageProcessPhase} can be executed with a different threading mechanism.
 * {@link MessageProcessPhase} implementation must guarantee that upon phase completion the method
 * {@link PhaseResultNotifier#phaseSuccessfully()} is executed, if there was a failure processing the message then the method
 * {@link PhaseResultNotifier#phaseFailure(Exception)} must be executed and if the phase consumed the message the method
 * {@link PhaseResultNotifier#phaseConsumedMessage()} must be executed.
 */
public class PhaseExecutionEngine {

  private final List<MessageProcessPhase> phaseList;
  private final SystemExceptionHandler exceptionHandler;
  private final EndProcessPhase endProcessPhase;

  public PhaseExecutionEngine(List<MessageProcessPhase> messageProcessPhaseList, SystemExceptionHandler exceptionHandler,
                              EndProcessPhase endProcessPhase) {
    this.phaseList = messageProcessPhaseList;
    this.exceptionHandler = exceptionHandler;
    this.endProcessPhase = endProcessPhase;
  }

  public void process(MessageProcessTemplate messageProcessTemplate, MessageProcessContext messageProcessContext) {
    InternalPhaseExecutionEngine internalPhaseExecutionEngine =
        new InternalPhaseExecutionEngine(messageProcessTemplate, messageProcessContext);
    internalPhaseExecutionEngine.process();
  }

  public class InternalPhaseExecutionEngine implements PhaseResultNotifier {

    private int currentPhase = 0;
    private final MessageProcessContext messageProcessContext;
    private final MessageProcessTemplate messageProcessTemplate;
    private boolean endPhaseProcessed;

    public InternalPhaseExecutionEngine(MessageProcessTemplate messageProcessTemplate,
                                        MessageProcessContext messageProcessContext) {
      this.messageProcessTemplate = messageProcessTemplate;
      this.messageProcessContext = messageProcessContext;
    }

    @Override
    public void phaseSuccessfully() {
      currentPhase++;
      if (currentPhase < phaseList.size()) {
        if (phaseList.get(currentPhase).supportsTemplate(messageProcessTemplate)) {
          phaseList.get(currentPhase).runPhase(messageProcessTemplate, messageProcessContext, this);
        } else {
          phaseSuccessfully();
        }
      } else {
        processEndPhase();
      }
    }

    @Override
    public void phaseConsumedMessage() {
      processEndPhase();
    }

    @Override
    public void phaseFailure(Exception reason) {
      exceptionHandler.handleException(reason);
      processEndPhase();
    }

    private void processEndPhase() {
      if (!endPhaseProcessed) {
        endPhaseProcessed = true;
        if (endProcessPhase.supportsTemplate(messageProcessTemplate)) {
          endProcessPhase.runPhase((EndPhaseTemplate) messageProcessTemplate, messageProcessContext, this);
        }
      }
    }

    public void process() {
      withContextClassLoader(messageProcessContext.getExecutionClassLoader(), () -> {
        for (MessageProcessPhase phase : phaseList) {
          if (phase.supportsTemplate(messageProcessTemplate)) {
            phase.runPhase(messageProcessTemplate, messageProcessContext, this);
            return;
          }
          currentPhase++;
        }
      });
    }

  }
}
