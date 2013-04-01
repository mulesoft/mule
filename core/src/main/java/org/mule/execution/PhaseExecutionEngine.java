/*
 * $Id: HttpsHandshakeTimingTestCase.java 25119 2012-12-10 21:20:57Z pablo.lagreca $
 * --------------------------------------------------------------------------------------
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 *
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.execution;

import org.mule.api.exception.SystemExceptionHandler;

import java.util.List;

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
    }
}
