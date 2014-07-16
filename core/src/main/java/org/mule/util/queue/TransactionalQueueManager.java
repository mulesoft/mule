/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.util.queue;

import org.mule.api.MuleException;
import org.mule.api.MuleRuntimeException;
import org.mule.api.lifecycle.InitialisationException;
import org.mule.api.store.ListableObjectStore;
import org.mule.api.store.ObjectStoreException;
import org.mule.util.journal.queue.LocalTxQueueTransactionJournal;
import org.mule.util.journal.queue.LocalTxQueueTransactionRecoverer;
import org.mule.util.journal.queue.XaTxQueueTransactionJournal;
import org.mule.util.xa.XaTransactionRecoverer;

import java.io.File;
import java.util.HashMap;
import java.util.Map;

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
    private Map<String, RecoverableQueueStore> queuesAccessedForRecovery = new HashMap<String, RecoverableQueueStore>();

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
        localTxTransactionJournal.close();
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

    @Override
    public RecoverableQueueStore getRecoveryQueue(String queueName)
    {
        if (queuesAccessedForRecovery.containsKey(queueName))
        {
            return queuesAccessedForRecovery.get(queueName);
        }
        DefaultQueueStore queueStore = createQueueStore(queueName, new DefaultQueueConfiguration(0, true));
        queuesAccessedForRecovery.put(queueName, queueStore);
        return queueStore;
    }

    @Override
    public void start() throws MuleException
    {
        queueXaResourceManager.start();
        localTxQueueTransactionRecoverer.recover();
        for (QueueStore queueStore : queuesAccessedForRecovery.values())
        {
            queueStore.close();
        }
        queuesAccessedForRecovery.clear();

        //Need to do this in order to open all ListableObjectStore. See MULE-7486
        openAllListableObjectStores();
    }

    private void openAllListableObjectStores()
    {
        if (getMuleContext() != null)
        {
            for (ListableObjectStore store : getMuleContext().getRegistry()
                    .lookupByType(ListableObjectStore.class)
                    .values())
            {
                try
                {
                    store.open();
                }
                catch (ObjectStoreException e)
                {
                    throw new MuleRuntimeException(e);
                }
            }
        }
    }

    @Override
    public void stop() throws MuleException
    {
        queueXaResourceManager.stop();
    }

}
