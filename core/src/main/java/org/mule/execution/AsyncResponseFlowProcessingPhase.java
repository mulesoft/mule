/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.execution;

import static org.mule.context.notification.BaseConnectorMessageNotification.MESSAGE_RECEIVED;
import static org.mule.context.notification.BaseConnectorMessageNotification.MESSAGE_RESPONSE;
import org.mule.RequestContext;
import org.mule.api.MessagingException;
import org.mule.api.MuleEvent;
import org.mule.api.exception.MessagingExceptionHandler;
import org.mule.api.execution.ExecutionCallback;
import org.mule.context.notification.ConnectorMessageNotification;
import org.mule.context.notification.NotificationHelper;
import org.mule.context.notification.ServerNotificationManager;
import org.mule.transaction.MuleTransactionConfig;

import java.util.concurrent.ConcurrentHashMap;

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

    private ConcurrentHashMap<ServerNotificationManager, NotificationHelper> notificationHelpers = new ConcurrentHashMap<>();

    @Override
    public boolean supportsTemplate(MessageProcessTemplate messageProcessTemplate)
    {
        return messageProcessTemplate instanceof AsyncResponseFlowProcessingPhaseTemplate;
    }

    @Override
    public void runPhase(final AsyncResponseFlowProcessingPhaseTemplate template, final MessageProcessContext messageProcessContext, final PhaseResultNotifier phaseResultNotifier)
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
                        fireNotification(muleEvent, MESSAGE_RECEIVED);
                        muleEvent = template.routeEvent(muleEvent);
                        return muleEvent;
                    }
                });
                fireNotification(response, MESSAGE_RESPONSE);
                template.sendResponseToClient(response, createResponseCompletationCallback(phaseResultNotifier, exceptionHandler));
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

    private void fireNotification(MuleEvent event, int action)
    {
        try
        {
            if (event == null)
            {
                //Null result only happens when there's a filter in the chain.
                //Unfortunately a filter causes the whole chain to return null
                //and there's no other way to retrieve the last event but using the RequestContext.
                //see https://www.mulesoft.org/jira/browse/MULE-8670
                event = RequestContext.getEvent();
                if (event == null)
                {
                    return;
                }
            }
            getNotificationHelper(event.getMuleContext().getNotificationManager()).fireNotification(
                    event,
                    event.getMessageSourceURI().toString(),
                    event.getFlowConstruct(),
                    action);
        }
        catch (Exception e)
        {
            logger.warn("Could not fire notification. Action: " + action, e);
        }
    }

    private NotificationHelper getNotificationHelper(ServerNotificationManager serverNotificationManager)
    {
        NotificationHelper notificationHelper = notificationHelpers.get(serverNotificationManager);
        if (notificationHelper==null)
        {
            notificationHelper = new NotificationHelper(serverNotificationManager, ConnectorMessageNotification.class, false);
            notificationHelpers.putIfAbsent(serverNotificationManager, notificationHelper);
        }
        return notificationHelper;
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
            public void responseSentWithFailure(Exception e, MuleEvent event)
            {
                phaseResultNotifier.phaseFailure(e);
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

    private void executeCallback(final Callback callback, PhaseResultNotifier phaseResultNotifier)
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

}
