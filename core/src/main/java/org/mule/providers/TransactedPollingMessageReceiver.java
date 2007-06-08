/*
 * $Id$
 * --------------------------------------------------------------------------------------
 * Copyright (c) MuleSource, Inc.  All rights reserved.  http://www.mulesource.com
 *
 * The software in this package is published under the terms of the MuleSource MPL
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.providers;

import org.mule.config.ThreadingProfile;
import org.mule.transaction.TransactionCallback;
import org.mule.transaction.TransactionTemplate;
import org.mule.umo.UMOComponent;
import org.mule.umo.UMOException;
import org.mule.umo.endpoint.UMOEndpoint;
import org.mule.umo.lifecycle.InitialisationException;
import org.mule.umo.provider.UMOConnector;

import edu.emory.mathcs.backport.java.util.concurrent.CountDownLatch;
import edu.emory.mathcs.backport.java.util.concurrent.TimeUnit;

import java.util.Iterator;
import java.util.List;

import javax.resource.spi.work.Work;

/**
 * The TransactedPollingMessageReceiver is an abstract receiver that handles polling
 * and transaction management. Derived implementations of these class must be thread
 * safe as several threads can be started at once for an improveded throuput.
 */
public abstract class TransactedPollingMessageReceiver extends AbstractPollingMessageReceiver
{
    /** determines whether messages will be received in a transaction template */
    private boolean receiveMessagesInTransaction = true;

    /** determines whether Multiple receivers are created to improve throughput */
    private boolean useMultipleReceivers = true;

    public TransactedPollingMessageReceiver(UMOConnector connector,
                                            UMOComponent component,
                                            final UMOEndpoint endpoint) throws InitialisationException
    {
        super(connector, component, endpoint);
        this.setReceiveMessagesInTransaction(endpoint.getTransactionConfig().getFactory() != null);
    }

    /**
     * @deprecated please use
     *             {@link #TransactedPollingMessageReceiver(UMOConnector, UMOComponent, UMOEndpoint, long, TimeUnit)}
     *             instead
     */
    public TransactedPollingMessageReceiver(UMOConnector connector,
                                            UMOComponent component,
                                            final UMOEndpoint endpoint,
                                            long frequency) throws InitialisationException
    {
        this(connector, component, endpoint);
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

    // @Override
    public void doStart() throws UMOException
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

    public void poll() throws Exception
    {
        TransactionTemplate tt = new TransactionTemplate(endpoint.getTransactionConfig(),
            connector.getExceptionListener(), connector.getManagementContext());

        if (this.isReceiveMessagesInTransaction())
        {
            // Receive messages and process them in a single transaction
            // Do not enable threading here, but several workers
            // may have been started
            TransactionCallback cb = new TransactionCallback()
            {
                public Object doInTransaction() throws Exception
                {
                    List messages = getMessages();
                    if (messages != null && messages.size() > 0)
                    {
                        for (Iterator it = messages.iterator(); it.hasNext();)
                        {
                            TransactedPollingMessageReceiver.this.processMessage(it.next());
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
                for (Iterator it = messages.iterator(); it.hasNext();)
                {
                    try
                    {
                        this.getWorkManager().scheduleWork(
                            new MessageProcessorWorker(tt, countdown, it.next()));
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
                handleException(e);
            }
            finally
            {
                latch.countDown();
            }
        }

        public Object doInTransaction() throws Exception
        {
            TransactedPollingMessageReceiver.this.processMessage(message);
            return null;
        }

    }

    protected abstract List getMessages() throws Exception;

    protected abstract void processMessage(Object message) throws Exception;

}
