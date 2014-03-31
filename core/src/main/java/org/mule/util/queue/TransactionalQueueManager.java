/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.util.queue;

import org.mule.api.MuleException;
import org.mule.api.lifecycle.InitialisationException;
import org.mule.util.journal.queue.LocalTxQueueTransactionJournal;
import org.mule.util.journal.queue.LocalTxQueueTransactionRecoverer;
import org.mule.util.journal.queue.XaTxQueueTransactionJournal;
import org.mule.util.xa.XaTransactionRecoverer;

import java.io.File;
import java.util.Set;
import java.util.TreeSet;

/**
 * The Transactional Queue Manager is responsible for creating and Managing
 * transactional Queues. Queues can also be persistent by setting a persistence
 * configuration for the queue.
 */
public class TransactionalQueueManager extends AbstractQueueManager
{

    private LocalTxQueueTransactionJournal localTxTransactionJournal;
    private LocalTxQueueTransactionRecoverer localTxQueueTransactionRecoverer;
    private XaTxQueueTransactionJournal xaTransactionJournal;
    private XaTransactionRecoverer xaTransactionRecoverer;
    private QueueXaResourceManager queueXaResourceManager = new QueueXaResourceManager();
    //Due to current VMConnector and TransactionQueueManager relationship we must close all the recovered queues
    //since queue configuration is applied after recovery and not taking into consideration once queues are created
    //for recovery. See https://www.mulesoft.org/jira/browse/MULE-7420
    private Set<String> queuesAccessedForRecovery = new TreeSet<String>();

    /**
     * {@inheritDoc}
     * 
     * @return an instance of {@link TransactionalQueueSession}
     */
    @Override
    public synchronized QueueSession getQueueSession()
    {
        return new TransactionalQueueSession(this, queueXaResourceManager, queueXaResourceManager, xaTransactionRecoverer, localTxTransactionJournal,getMuleContext());
    }

    protected DefaultQueueStore createQueueStore(String name, QueueConfiguration config)
    {
        return new DefaultQueueStore(name, getMuleContext(), config);
    }

    @Override
    protected void doDispose()
    {
        localTxTransactionJournal.clear();
        localTxTransactionJournal.close();
        xaTransactionJournal.clear();
        xaTransactionJournal.close();
    }

    @Override
    public void initialise() throws InitialisationException
    {
        String workingDirectory = getMuleContext().getConfiguration().getWorkingDirectory();
        localTxTransactionJournal = new LocalTxQueueTransactionJournal(workingDirectory + File.separator + "queue-tx-log", getMuleContext());
        localTxQueueTransactionRecoverer = new LocalTxQueueTransactionRecoverer(localTxTransactionJournal, this);
        xaTransactionJournal = new XaTxQueueTransactionJournal(workingDirectory + File.separator + "queue-xa-tx-log", getMuleContext());
        xaTransactionRecoverer = new XaTransactionRecoverer(xaTransactionJournal, this);
    }

    public XaTxQueueTransactionJournal getXaTransactionJournal()
    {
        return xaTransactionJournal;
    }

    @Override
    public RecoverableQueueStore getRecoveryQueue(String queueName)
    {
        queuesAccessedForRecovery.add(queueName);
        return createQueueStore(queueName, new DefaultQueueConfiguration(0, true));
    }

    @Override
    public void start() throws MuleException
    {
        queueXaResourceManager.start();
        localTxQueueTransactionRecoverer.recover();
        for (String queueName : queuesAccessedForRecovery)
        {
            getQueue(queueName).close();
            clearQueueConfiguration(queueName);
        }
    }

    @Override
    public void stop() throws MuleException
    {
        queueXaResourceManager.stop();
    }

}
