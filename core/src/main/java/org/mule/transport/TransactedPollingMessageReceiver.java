/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.transport;

import org.mule.api.MuleException;
import org.mule.api.MuleMessage;
import org.mule.api.config.ThreadingProfile;
import org.mule.api.construct.FlowConstruct;
import org.mule.api.endpoint.InboundEndpoint;
import org.mule.api.lifecycle.CreateException;
import org.mule.api.transaction.TransactionCallback;
import org.mule.api.transport.Connector;
import org.mule.transaction.TransactionTemplate;

import java.util.List;

import javax.resource.spi.work.Work;

import edu.emory.mathcs.backport.java.util.concurrent.CountDownLatch;

/**
 * The TransactedPollingMessageReceiver is an abstract receiver that handles polling
 * and transaction management. Derived implementations of these class must be thread
 * safe as several threads can be started at once for an improved throughput.
 */
public abstract class TransactedPollingMessageReceiver extends AbstractPollingMessageReceiver
{
    /** determines whether messages will be received in a transaction template */
    private boolean receiveMessagesInTransaction = true;

    /** determines whether Multiple receivers are created to improve throughput */
    private boolean useMultipleReceivers = true;

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
        TransactionTemplate<Object> tt = new TransactionTemplate<Object>(endpoint.getTransactionConfig(), connector.getMuleContext());

        if (this.isReceiveMessagesInTransaction())
        {
            // Receive messages and process them in a single transaction
            // Do not enable threading here, but several workers
            // may have been started
            TransactionCallback<Object> cb = new TransactionCallback<Object>()
            {
                public Object doInTransaction() throws Exception
                {
                    // this is not ideal, but jdbc receiver returns a list of maps, not List<MuleMessage>
                    List messages = getMessages();
                    if (messages != null && messages.size() > 0)
                    {
                        for (Object message : messages)
                        {
                            processMessage(message);
                        }
                    }
                    return null;
                }
            };
            tt.execute(cb);
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
                                new MessageProcessorWorker(tt, countdown, message));
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

    protected class MessageProcessorWorker implements Work, TransactionCallback
    {
        private final TransactionTemplate tt;
        private final Object message;
        private final CountDownLatch latch;

        public MessageProcessorWorker(TransactionTemplate tt, CountDownLatch latch, Object message)
        {
            this.tt = tt;
            this.message = message;
            this.latch = latch;
        }

        public void release()
        {
            // nothing to do
        }

        public void run()
        {
            try
            {
                tt.execute(this);
            }
            catch (Exception e)
            {
                connector.getMuleContext().getExceptionListener().handleException(e);
            }
            finally
            {
                latch.countDown();
            }
        }

        public Object doInTransaction() throws Exception
        {
            processMessage(message);
            return null;
        }

    }

    protected abstract List<MuleMessage> getMessages() throws Exception;

    protected abstract void processMessage(Object message) throws Exception;

}
