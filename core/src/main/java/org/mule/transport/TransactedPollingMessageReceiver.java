/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.transport;

import org.mule.api.MessagingException;
import org.mule.api.MuleEvent;
import org.mule.api.MuleException;
import org.mule.api.MuleMessage;
import org.mule.api.config.ThreadingProfile;
import org.mule.api.construct.FlowConstruct;
import org.mule.api.endpoint.InboundEndpoint;
import org.mule.api.exception.SystemExceptionHandler;
import org.mule.api.execution.ExecutionCallback;
import org.mule.api.execution.ExecutionTemplate;
import org.mule.api.lifecycle.CreateException;
import org.mule.api.transaction.Transaction;
import org.mule.api.transport.Connector;
import org.mule.routing.DefaultRouterResultsHandler;
import org.mule.transaction.TransactionCoordination;

import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.CountDownLatch;

import javax.resource.spi.work.Work;

/**
 * The TransactedPollingMessageReceiver is an abstract receiver that handles polling
 * and transaction management. Derived implementations of these class must be thread
 * safe as several threads can be started at once for an improved throughput.
 */
public abstract class TransactedPollingMessageReceiver extends AbstractPollingMessageReceiver
{
    /**
     * time to sleep when there are no messages in the queue to avoid busy waiting *
     */
    private static final long NO_MESSAGES_SLEEP_TIME = Long.parseLong(System.getProperty("mule.vm.pollingSleepWaitTime", "50"));

    /**
     * determines whether messages will be received in a transaction template
     */
    private boolean receiveMessagesInTransaction = true;

    /**
     * determines whether Multiple receivers are created to improve throughput
     */
    private boolean useMultipleReceivers = true;

    private final DefaultRouterResultsHandler defaultRouterResultsHandler = new DefaultRouterResultsHandler(false);

    public TransactedPollingMessageReceiver(Connector connector,
                                            FlowConstruct flowConstruct,
                                            final InboundEndpoint endpoint) throws CreateException
    {
        super(connector, flowConstruct, endpoint);
        this.setReceiveMessagesInTransaction(endpoint.getTransactionConfig().isTransacted());
    }

    /**
     * @deprecated please use
     *             {@link #TransactedPollingMessageReceiver(Connector, FlowConstruct, InboundEndpoint)}
     *             instead
     */
    @Deprecated
    public TransactedPollingMessageReceiver(Connector connector,
                                            FlowConstruct flowConstruct,
                                            final InboundEndpoint endpoint,
                                            long frequency) throws CreateException
    {
        this(connector, flowConstruct, endpoint);
        this.setFrequency(frequency);
    }

    public boolean isReceiveMessagesInTransaction()
    {
        return receiveMessagesInTransaction;
    }

    public void setReceiveMessagesInTransaction(boolean useTx)
    {
        receiveMessagesInTransaction = useTx;
    }

    public boolean isUseMultipleTransactedReceivers()
    {
        return useMultipleReceivers;
    }

    public void setUseMultipleTransactedReceivers(boolean useMultiple)
    {
        useMultipleReceivers = useMultiple;
    }

    @Override
    public void doStart() throws MuleException
    {
        // Connector property overrides any implied value
        this.setUseMultipleTransactedReceivers(connector.isCreateMultipleTransactedReceivers());

        ThreadingProfile tp = connector.getReceiverThreadingProfile();
        int numReceiversToStart = 1;

        if (this.isReceiveMessagesInTransaction() && this.isUseMultipleTransactedReceivers()
                && tp.isDoThreading())
        {
            numReceiversToStart = connector.getNumberOfConcurrentTransactedReceivers();
        }

        for (int i = 0; i < numReceiversToStart; i++)
        {
            super.doStart();
        }
    }

    @Override
    public void poll() throws Exception
    {
        try
        {
            ExecutionTemplate<MuleEvent> pt = createExecutionTemplate();

            if (this.isReceiveMessagesInTransaction())
            {
                if (hasNoMessages())
                {
                    if (NO_MESSAGES_SLEEP_TIME > 0)
                    {
                        Thread.sleep(NO_MESSAGES_SLEEP_TIME);
                    }
                    return;
                }
                // Receive messages and process them in a single transaction
                // Do not enable threading here, but several workers
                // may have been started
                ExecutionCallback<MuleEvent> cb = new ExecutionCallback<MuleEvent>()
                {
                    @Override
                    public MuleEvent process() throws Exception
                    {
                        // this is not ideal, but jdbc receiver returns a list of maps, not List<MuleMessage>
                        List messages = getMessages();
                        LinkedList<MuleEvent> results = new LinkedList<MuleEvent>();
                        if (messages != null && messages.size() > 0)
                        {
                            for (Object message : messages)
                            {
                                results.add(processMessage(message));
                            }
                        }
                        else
                        {
                            //If not message was processed mark exception for rollback to avoid tx timeout exceptions in XA
                            Transaction currentTx = TransactionCoordination.getInstance().getTransaction();
                            currentTx.setRollbackOnly();
                            return null;
                        }
                        return defaultRouterResultsHandler.aggregateResults(results, results.getLast(), results.getLast().getMuleContext());
                    }
                };
                pt.execute(cb);
            }
            else
            {
                // Receive messages and launch a worker for each message
                List messages = getMessages();
                if (messages != null && messages.size() > 0)
                {
                    final CountDownLatch countdown = new CountDownLatch(messages.size());
                    for (Object message : messages)
                    {
                        try
                        {
                            this.getWorkManager().scheduleWork(
                                    new MessageProcessorWorker(pt, countdown, endpoint.getMuleContext().getExceptionListener(), message));
                        }
                        catch (Exception e)
                        {
                            countdown.countDown();
                            throw e;
                        }
                    }
                    countdown.await();
                }
            }
        }
        catch (MessagingException e)
        {
            //Already handled by exception strategy
        }
        catch (Exception e)
        {
            getEndpoint().getMuleContext().handleException(e);
        }
    }

    /**
     * Return true if it can be determined that there are currently no messages to process
     */
    protected boolean hasNoMessages()
    {
        return false;
    }

    protected class MessageProcessorWorker implements Work, ExecutionCallback<MuleEvent>
    {
        private final ExecutionTemplate<MuleEvent> pt;
        private final Object message;
        private final CountDownLatch latch;
        private final SystemExceptionHandler exceptionHandler;

        public MessageProcessorWorker(ExecutionTemplate<MuleEvent> pt, CountDownLatch latch, SystemExceptionHandler exceptionHandler, Object message)
        {
            this.pt = pt;
            this.message = message;
            this.latch = latch;
            this.exceptionHandler = exceptionHandler;
        }

        @Override
        public void release()
        {
            // nothing to do
        }

        @Override
        public void run()
        {
            try
            {
                pt.execute(this);
            }
            catch (MessagingException e)
            {
                //already managed by TransactionTemplate
            }
            catch (Exception e)
            {
                exceptionHandler.handleException(e);
            }
            finally
            {
                latch.countDown();
            }
        }

        @Override
        public MuleEvent process() throws Exception
        {
            processMessage(message);
            return null;
        }
    }

    protected abstract List<MuleMessage> getMessages() throws Exception;

    protected abstract MuleEvent processMessage(Object message) throws Exception;

}
