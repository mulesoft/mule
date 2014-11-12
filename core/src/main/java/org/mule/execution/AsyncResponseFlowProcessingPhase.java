/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.execution;

import org.mule.api.MessagingException;
import org.mule.api.MuleEvent;
import org.mule.api.exception.MessagingExceptionHandler;
import org.mule.api.execution.ExecutionCallback;
import org.mule.transaction.MuleTransactionConfig;
import org.mule.util.Preconditions;

import javax.resource.spi.work.Work;
import javax.resource.spi.work.WorkException;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

/**
 * This phase routes the message through the flow.
 *
 * To participate of this phase, {@link org.mule.execution.MessageProcessTemplate} must implement {@link org.mule.execution.FlowProcessingPhaseTemplate}
 */
public class AsyncResponseFlowProcessingPhase implements MessageProcessPhase<AsyncResponseFlowProcessingPhaseTemplate>, Comparable<MessageProcessPhase>
{

    protected transient Log logger = LogFactory.getLog(getClass());

    @Override
    public boolean supportsTemplate(MessageProcessTemplate messageProcessTemplate)
    {
        return messageProcessTemplate instanceof AsyncResponseFlowProcessingPhaseTemplate;
    }

    @Override
    public void runPhase(final AsyncResponseFlowProcessingPhaseTemplate template, final MessageProcessContext messageProcessContext, final PhaseResultNotifier phaseResultNotifier)
    {
        Preconditions.checkArgument(messageProcessContext.supportsAsynchronousProcessing(), String.format("Cannot execute %s if %s does not support asynchronous processing", this.getClass().getName(), messageProcessContext.getClass().getName()));
        Work flowExecutionWork = new Work()
        {
            @Override
            public void release()
            {
            }

            @Override
            public void run()
            {
                try
                {
                    try
                    {
                        final MessagingExceptionHandler exceptionHandler = messageProcessContext.getFlowConstruct().getExceptionListener();
                        TransactionalErrorHandlingExecutionTemplate transactionTemplate = TransactionalErrorHandlingExecutionTemplate.
                                createMainExecutionTemplate(messageProcessContext.getFlowConstruct().getMuleContext(),
                                                            (messageProcessContext.getTransactionConfig() == null ? new MuleTransactionConfig() : messageProcessContext.getTransactionConfig()),
                                                            exceptionHandler);
                        final MuleEvent response = transactionTemplate.execute(new ExecutionCallback<MuleEvent>()
                        {
                            @Override
                            public MuleEvent process() throws Exception
                            {
                                MuleEvent muleEvent = template.getMuleEvent();
                                muleEvent = template.routeEvent(muleEvent);
                                return muleEvent;
                            }
                        });
                        template.sendResponseToClient(template, response, createResponseCompletationCallback(phaseResultNotifier, exceptionHandler));
                    }
                    catch (final MessagingException e)
                    {
                        template.sendFailureResponseToClient(e, createSendFailureResponseCompletationCallback(phaseResultNotifier));
                    }
                }
                catch (Exception e)
                {
                    phaseResultNotifier.phaseFailure(e);
                }
            }
        };
        try
        {
            messageProcessContext.getFlowExecutionWorkManager().scheduleWork(flowExecutionWork);
        }
        catch (WorkException e)
        {
            try
            {
                template.afterFailureProcessingFlow(e);
            }
            finally
            {
                phaseResultNotifier.phaseFailure(e);
            }
        }
    }

    private ResponseCompletationCallback createSendFailureResponseCompletationCallback(final PhaseResultNotifier phaseResultNotifier)
    {
        return new ResponseCompletationCallback()
        {
            @Override
            public void responseSentSuccessfully()
            {
                phaseResultNotifier.phaseSuccessfully();
            }

            @Override
            public void responseSentWithFailure(Exception e, MuleEvent event)
            {
                phaseResultNotifier.phaseFailure(e);
            }
        };
    }

    private ResponseCompletationCallback createResponseCompletationCallback(final PhaseResultNotifier phaseResultNotifier, final MessagingExceptionHandler exceptionListener)
    {
        return new ResponseCompletationCallback()
        {
            @Override
            public void responseSentSuccessfully()
            {
                phaseResultNotifier.phaseSuccessfully();
            }

            @Override
            public void responseSentWithFailure(final Exception e, final MuleEvent event)
            {
                executeCallback(new Callback()
                {
                    @Override
                    public void execute() throws Exception
                    {
                        exceptionListener.handleException(e, event);
                        phaseResultNotifier.phaseSuccessfully();
                    }
                }, phaseResultNotifier);
            }
        };
    }

    public void executeCallback(final Callback callback, PhaseResultNotifier phaseResultNotifier)
    {
        try
        {
            callback.execute();
        }
        catch (Exception callbackException)
        {
            phaseResultNotifier.phaseFailure(callbackException);
        }
    }

    @Override
    public int compareTo(MessageProcessPhase messageProcessPhase)
    {
        if (messageProcessPhase instanceof ValidationPhase)
        {
            return 1;
        }
        return 0;
    }

    private interface Callback
    {

        void execute() throws Exception;

    }

    public interface ResponseCompletationCallback
    {

        void responseSentSuccessfully();

        void responseSentWithFailure(Exception e, MuleEvent event);

    }
}
