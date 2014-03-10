/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.execution;

import org.mule.api.exception.SystemExceptionHandler;

import java.util.List;

/**
 * This class process a message through a set of {@link org.mule.execution.MessageProcessPhase} using
 * the message content and message processing context provided by {@link org.mule.execution.MessageProcessTemplate} and {@link org.mule.execution.MessageProcessContext}.
 * 
 * This class will handle any message processing failure by calling the {@link org.mule.api.exception.SystemExceptionHandler} defined by the application.
 * 
 * Each {@link org.mule.execution.MessageProcessPhase} can be executed with a different threading mechanism. 
 * {@link org.mule.execution.MessageProcessPhase} implementation must guarantee that upon phase completion the method {@link PhaseResultNotifier#phaseSuccessfully()}  is executed,
 * if there was a failure processing the message then the method {@link PhaseResultNotifier#phaseFailure(Exception)} must be executed and if the phase consumed the message the method
 * {@link org.mule.execution.PhaseResultNotifier#phaseConsumedMessage()} must be executed.
 */
public class PhaseExecutionEngine
{

    private final List<MessageProcessPhase> phaseList;
    private final SystemExceptionHandler exceptionHandler;
    private final EndProcessPhase endProcessPhase;

    public PhaseExecutionEngine(List<MessageProcessPhase> messageProcessPhaseList, SystemExceptionHandler exceptionHandler, EndProcessPhase endProcessPhase)
    {
        this.phaseList = messageProcessPhaseList;
        this.exceptionHandler = exceptionHandler;
        this.endProcessPhase = endProcessPhase;
    }

    public void process(MessageProcessTemplate messageProcessTemplate, MessageProcessContext messageProcessContext)
    {
        InternalPhaseExecutionEngine internalPhaseExecutionEngine = new InternalPhaseExecutionEngine(messageProcessTemplate, messageProcessContext);
        internalPhaseExecutionEngine.process();
    }

    public class InternalPhaseExecutionEngine implements PhaseResultNotifier
    {
        private int currentPhase = 0;
        private final MessageProcessContext messageProcessContext;
        private final MessageProcessTemplate messageProcessTemplate;
        private boolean endPhaseProcessed;

        public InternalPhaseExecutionEngine(MessageProcessTemplate messageProcessTemplate, MessageProcessContext messageProcessContext)
        {
            this.messageProcessTemplate = messageProcessTemplate;
            this.messageProcessContext = messageProcessContext;
        }

        @Override
        public void phaseSuccessfully()
        {
            currentPhase++;
            if (currentPhase < phaseList.size())
            {
                if (phaseList.get(currentPhase).supportsTemplate(messageProcessTemplate))
                {
                    phaseList.get(currentPhase).runPhase(messageProcessTemplate,messageProcessContext,this);
                }
                else
                {
                    phaseSuccessfully();
                }
            }
            else
            {
                processEndPhase();
            }
        }

        @Override
        public void phaseConsumedMessage()
        {
            processEndPhase();
        }

        @Override
        public void phaseFailure(Exception reason)
        {
            exceptionHandler.handleException(reason);
            processEndPhase();
        }

        private void processEndPhase()
        {
            if (!endPhaseProcessed)
            {
                endPhaseProcessed = true;
                if (endProcessPhase.supportsTemplate(messageProcessTemplate))
                {
                    endProcessPhase.runPhase((EndPhaseTemplate) messageProcessTemplate, messageProcessContext,this);
                }
            }
        }

        public void process()
        {
            ClassLoader originalClassLoader = null;
            try
            {
                originalClassLoader = Thread.currentThread().getContextClassLoader();
                Thread.currentThread().setContextClassLoader(messageProcessContext.getExecutionClassLoader());
                for (MessageProcessPhase phase : phaseList)
                {
                    if (phase.supportsTemplate(messageProcessTemplate))
                    {
                        phase.runPhase(messageProcessTemplate, messageProcessContext, this);
                        return;
                    }
                    currentPhase++;
                }
            }
            finally
            {
                Thread.currentThread().setContextClassLoader(originalClassLoader);
            }
        }

    }
}
