/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.execution;

import static org.mule.context.notification.ConnectorMessageNotification.MESSAGE_ERROR_RESPONSE;
import static org.mule.context.notification.ConnectorMessageNotification.MESSAGE_RECEIVED;
import static org.mule.context.notification.ConnectorMessageNotification.MESSAGE_RESPONSE;

import org.mule.DefaultMuleEvent;
import org.mule.NonBlockingVoidMuleEvent;
import org.mule.OptimizedRequestContext;
import org.mule.api.MessagingException;
import org.mule.api.MuleEvent;
import org.mule.api.MuleException;
import org.mule.api.MuleMessage;
import org.mule.api.MuleRuntimeException;
import org.mule.api.connector.ReplyToHandler;
import org.mule.api.exception.MessagingExceptionHandler;
import org.mule.api.source.MessageSource;
import org.mule.transaction.MuleTransactionConfig;

import javax.resource.spi.work.Work;
import javax.resource.spi.work.WorkException;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

/**
 * This phase routes the message through the flow.
 * <p>
 * To participate of this phase, {@link org.mule.execution.MessageProcessTemplate} must implement {@link org.mule.execution.FlowProcessingPhaseTemplate}
 */
public class AsyncResponseFlowProcessingPhase extends NotificationFiringProcessingPhase<AsyncResponseFlowProcessingPhaseTemplate>
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
                    MessageSource messageSource = messageProcessContext.getMessageSource();
                    try
                    {
                        final MessagingExceptionHandler exceptionHandler = messageProcessContext.getFlowConstruct().getExceptionListener();
                        TransactionalErrorHandlingExecutionTemplate transactionTemplate = TransactionalErrorHandlingExecutionTemplate.
                                createMainExecutionTemplate(messageProcessContext.getFlowConstruct().getMuleContext(),
                                                            (messageProcessContext.getTransactionConfig() == null ? new MuleTransactionConfig() : messageProcessContext.getTransactionConfig()),
                                                            exceptionHandler);
                        final MuleEvent response = transactionTemplate.execute(() -> {
                            MuleEvent muleEvent = template.getMuleEvent();
                            fireNotification(messageSource, muleEvent, MESSAGE_RECEIVED);
                            if (muleEvent.isAllowNonBlocking())
                            {
                                muleEvent = new DefaultMuleEvent(muleEvent, new ExceptionHandlingReplyToHandlerDecorator(new FlowProcessingNonBlockingReplyToHandler(template, phaseResultNotifier, exceptionHandler),
                                                                                                                         messageProcessContext.getFlowConstruct().getExceptionListener()));
                                // Update RequestContext ThreadLocal for backwards compatibility
                                OptimizedRequestContext.unsafeSetEvent(muleEvent);
                            }
                            return template.routeEvent(muleEvent);
                        });

                        if (response != NonBlockingVoidMuleEvent.getInstance())
                        {
                            fireNotification(messageSource, response, MESSAGE_RESPONSE);
                            template.sendResponseToClient(response, createResponseCompletationCallback(phaseResultNotifier, exceptionHandler));
                        }
                    }
                    catch (final MessagingException e)
                    {
                        fireNotification(messageSource, e.getEvent(), MESSAGE_ERROR_RESPONSE);
                        template.sendFailureResponseToClient(e, createSendFailureResponseCompletationCallback(phaseResultNotifier));
                    }
                }
                catch (Exception e)
                {
                    phaseResultNotifier.phaseFailure(e);
                }
            }
        };

        if (messageProcessContext.supportsAsynchronousProcessing())
        {
            try
            {
                messageProcessContext.getFlowExecutionWorkManager().scheduleWork(flowExecutionWork);
            }
            catch (WorkException e)
            {
                phaseResultNotifier.phaseFailure(e);
            }
        }
        else
        {
            flowExecutionWork.run();
        }
    }

    private ResponseCompletionCallback createSendFailureResponseCompletationCallback(final PhaseResultNotifier phaseResultNotifier)
    {
        return new ResponseCompletionCallback()
        {
            @Override
            public void responseSentSuccessfully()
            {
                phaseResultNotifier.phaseSuccessfully();
            }

            @Override
            public MuleEvent responseSentWithFailure(Exception e, MuleEvent event)
            {
                phaseResultNotifier.phaseFailure(e);
                return event;
            }
        };
    }

    private ResponseCompletionCallback createResponseCompletationCallback(final PhaseResultNotifier phaseResultNotifier, final MessagingExceptionHandler exceptionListener)
    {
        return new ResponseCompletionCallback()
        {
            @Override
            public void responseSentSuccessfully()
            {
                phaseResultNotifier.phaseSuccessfully();
            }

            @Override
            public MuleEvent responseSentWithFailure(final Exception e, final MuleEvent event)
            {
                return executeCallback(() ->
                {
                    ((DefaultMuleEvent) event).resetAccessControl();
                    final MuleEvent exceptionStrategyResult = exceptionListener.handleException(e, event);
                    phaseResultNotifier.phaseSuccessfully();
                    return exceptionStrategyResult;
                }, phaseResultNotifier);
            }
        };
    }

    private MuleEvent executeCallback(final Callback callback, PhaseResultNotifier phaseResultNotifier)
    {
        try
        {
            return callback.execute();
        }
        catch (Exception callbackException)
        {
            phaseResultNotifier.phaseFailure(callbackException);
            throw new MuleRuntimeException(callbackException);
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

        MuleEvent execute() throws Exception;

    }

    class FlowProcessingNonBlockingReplyToHandler implements ReplyToHandler
    {

        private final AsyncResponseFlowProcessingPhaseTemplate template;
        private final PhaseResultNotifier phaseResultNotifier;
        private final MessagingExceptionHandler exceptionHandler;

        public FlowProcessingNonBlockingReplyToHandler(AsyncResponseFlowProcessingPhaseTemplate template,
                                                       PhaseResultNotifier
                                                               phaseResultNotifier, MessagingExceptionHandler exceptionHandler)
        {
            this.template = template;
            this.phaseResultNotifier = phaseResultNotifier;
            this.exceptionHandler = exceptionHandler;
        }

        @Override
        public void processReplyTo(MuleEvent event, MuleMessage returnMessage, Object replyTo) throws MuleException
        {
            fireNotification(null, event, MESSAGE_RESPONSE);
            template.sendResponseToClient(event, createResponseCompletationCallback(phaseResultNotifier,
                                                                                    exceptionHandler));
        }

        @Override
        public void processExceptionReplyTo(MessagingException exception, Object replyTo)
        {
            try
            {
                fireNotification(null, exception.getEvent(), MESSAGE_ERROR_RESPONSE);
                template.sendFailureResponseToClient(exception, createSendFailureResponseCompletationCallback(phaseResultNotifier));
            }
            catch (MuleException e)
            {
                phaseResultNotifier.phaseFailure(e);
            }
        }
    }
}
