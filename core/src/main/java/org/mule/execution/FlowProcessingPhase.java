/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.execution;

import static org.mule.context.notification.BaseConnectorMessageNotification.MESSAGE_ERROR_RESPONSE;
import static org.mule.context.notification.BaseConnectorMessageNotification.MESSAGE_RESPONSE;
import org.mule.api.DefaultMuleException;
import org.mule.api.MessagingException;
import org.mule.api.MuleEvent;
import org.mule.api.MuleException;
import org.mule.transaction.MuleTransactionConfig;

import java.util.concurrent.atomic.AtomicReference;

import javax.resource.spi.work.Work;
import javax.resource.spi.work.WorkException;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

/**
 * This phase routes the message through the flow.
 *
 * To participate of this phase, {@link MessageProcessTemplate} must implement {@link FlowProcessingPhaseTemplate}
 */
public class FlowProcessingPhase extends NotificationFiringProcessingPhase<FlowProcessingPhaseTemplate>
{

    protected transient Log logger = LogFactory.getLog(getClass());

    @Override
    public boolean supportsTemplate(MessageProcessTemplate messageProcessTemplate)
    {
        return messageProcessTemplate instanceof FlowProcessingPhaseTemplate;
    }

    @Override
    public void runPhase(final FlowProcessingPhaseTemplate flowProcessingPhaseTemplate, final MessageProcessContext messageProcessContext, final PhaseResultNotifier phaseResultNotifier)
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
                    try
                    {
                        final AtomicReference exceptionThrownDuringFlowProcessing = new AtomicReference();
                        TransactionalErrorHandlingExecutionTemplate transactionTemplate = TransactionalErrorHandlingExecutionTemplate.
                                createMainExecutionTemplate(messageProcessContext.getFlowConstruct().getMuleContext(),
                                                            (messageProcessContext.getTransactionConfig() == null ? new MuleTransactionConfig() : messageProcessContext.getTransactionConfig()),
                                                            messageProcessContext.getFlowConstruct().getExceptionListener());
                        MuleEvent response = transactionTemplate.execute(() -> {
                            try
                            {
                                Object message = flowProcessingPhaseTemplate.getOriginalMessage();
                                if (message == null)
                                {
                                    return null;
                                }
                                MuleEvent muleEvent = flowProcessingPhaseTemplate.getMuleEvent();
                                muleEvent = flowProcessingPhaseTemplate.beforeRouteEvent(muleEvent);
                                muleEvent = flowProcessingPhaseTemplate.routeEvent(muleEvent);
                                muleEvent = flowProcessingPhaseTemplate.afterRouteEvent(muleEvent);
                                sendResponseIfNeccessary(muleEvent, flowProcessingPhaseTemplate);
                                return muleEvent;
                            }
                            catch (Exception e)
                            {
                                exceptionThrownDuringFlowProcessing.set(e);
                                throw e;
                            }
                        });
                        if (exceptionThrownDuringFlowProcessing.get() != null && !(exceptionThrownDuringFlowProcessing.get() instanceof ResponseDispatchException))
                        {
                            sendResponseIfNeccessary(response, flowProcessingPhaseTemplate);
                        }
                        flowProcessingPhaseTemplate.afterSuccessfulProcessingFlow(response);
                    }
                    catch (ResponseDispatchException e)
                    {
                        flowProcessingPhaseTemplate.afterFailureProcessingFlow(e);
                    }
                    catch (MessagingException e)
                    {
                        sendFailureResponseIfNeccessary(e, flowProcessingPhaseTemplate);
                        flowProcessingPhaseTemplate.afterFailureProcessingFlow(e);
                    }
                    phaseResultNotifier.phaseSuccessfully();
                }
                catch (Exception e)
                {
                    MuleException me = new DefaultMuleException(e);
                    try
                    {
                        flowProcessingPhaseTemplate.afterFailureProcessingFlow(me);
                    }
                    catch (MuleException e1)
                    {
                        logger.warn("Failure during exception processing in flow template: " + e.getMessage());
                        if (logger.isDebugEnabled())
                        {
                            logger.debug(e);
                        }
                    }
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

    private void sendFailureResponseIfNeccessary(MessagingException messagingException, FlowProcessingPhaseTemplate flowProcessingPhaseTemplate) throws MuleException
    {
        if (flowProcessingPhaseTemplate instanceof RequestResponseFlowProcessingPhaseTemplate)
        {
            fireNotification(messagingException.getEvent(), MESSAGE_ERROR_RESPONSE);
            ((RequestResponseFlowProcessingPhaseTemplate) flowProcessingPhaseTemplate).sendFailureResponseToClient(messagingException);
        }
    }

    private void sendResponseIfNeccessary(MuleEvent muleEvent, FlowProcessingPhaseTemplate flowProcessingPhaseTemplate) throws MuleException
    {
        if (flowProcessingPhaseTemplate instanceof RequestResponseFlowProcessingPhaseTemplate)
        {
            fireNotification(muleEvent, MESSAGE_RESPONSE);
            ((RequestResponseFlowProcessingPhaseTemplate) flowProcessingPhaseTemplate).sendResponseToClient(muleEvent);
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
}
