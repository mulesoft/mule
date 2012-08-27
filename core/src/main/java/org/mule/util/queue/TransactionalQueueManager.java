/*
 * $Id$
 * --------------------------------------------------------------------------------------
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 *
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.util.queue;

import org.mule.api.MuleContext;
import org.mule.api.context.MuleContextAware;
import org.mule.api.store.ListableObjectStore;
import org.mule.api.store.ObjectStore;
import org.mule.api.store.ObjectStoreException;
import org.mule.api.store.QueueStore;
import org.mule.util.UUID;
import org.mule.util.xa.AbstractTransactionContext;
import org.mule.util.xa.AbstractXAResourceManager;
import org.mule.util.xa.ResourceManagerException;
import org.mule.util.xa.ResourceManagerSystemException;

import java.io.Serializable;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.transaction.xa.XAResource;

/**
 * The Transactional Queue Manager is responsible for creating and Managing
 * transactional Queues. Queues can also be persistent by setting a persistence
 * strategy on the manager. Default straties are provided for Memory, Jounaling,
 * Cache and File.
 */
public class TransactionalQueueManager extends AbstractXAResourceManager implements QueueManager, MuleContextAware
{
    private Map<String, QueueInfo> queues = new HashMap<String, QueueInfo>();

    private QueueConfiguration defaultQueueConfiguration;
    private MuleContext muleContext;
    private Set<QueueStore> queueObjectStores = new HashSet<QueueStore>();
    private Set<ListableObjectStore> listableObjectStores = new HashSet<ListableObjectStore>();

    @Override
    public synchronized QueueSession getQueueSession()
    {
        return new TransactionalQueueSession(this, this);
    }

    @Override
    public synchronized void setDefaultQueueConfiguration(QueueConfiguration config)
    {
        this.defaultQueueConfiguration = config;
        addStore(config.objectStore);
    }

    @Override
    public synchronized void setQueueConfiguration(String queueName, QueueConfiguration config)
    {
        getQueue(queueName, config).setConfig(config);
        addStore(config.objectStore);
    }

    protected synchronized QueueInfo getQueue(String name)
    {
        return getQueue(name, defaultQueueConfiguration);
    }

    protected synchronized QueueInfo getQueue(String name, QueueConfiguration config)
    {
        QueueInfo q = queues.get(name);
        if (q == null)
        {
            q = new QueueInfo(name, muleContext, config);
            queues.put(name, q);
        }
        return q;
    }

    public synchronized QueueInfo getQueueInfo(String name)
    {
        QueueInfo q = queues.get(name);
        return q == null ? q : new QueueInfo(q);
    }

    @Override
    protected void doStart() throws ResourceManagerSystemException
    {
        findAllListableObjectStores();
        for (ListableObjectStore store: listableObjectStores)
        {
            try
            {
                store.open();
            }
            catch (ObjectStoreException e)
            {
                throw new ResourceManagerSystemException(e);
            }
        }
    }

    @Override
    protected boolean shutdown(int mode, long timeoutMSecs)
    {
        findAllListableObjectStores();
        for (ListableObjectStore store: listableObjectStores)
        {
            try
            {
                store.close();
            }
            catch (ObjectStoreException e)
            {
                // TODO BL-405 what to do with this exception? Looking at the call graph of this method it seems that it's never called from any production code (i.e. when shutting down MuleContext)
                logger.error("Error closing persistent store", e);
            }
        }

        // Clear queues on shutdown to avoid duplicate entries on warm restarts (MULE-3678)
        synchronized (this)
        {
            queues.clear();
        }
        return super.shutdown(mode, timeoutMSecs);
    }

    @Override
    protected void recover() throws ResourceManagerSystemException
    {
        findAllQueueStores();
        for (QueueStore store: queueObjectStores)
        {
            if (!store.isPersistent())
            {
                continue;
            }

            try
            {
                List<Serializable> keys = store.allKeys();
                for (Serializable key : keys)
                {
                    // It may contain events that aren't part of a queue MULE-6007
                    if (key instanceof QueueKey)
                    {
                        QueueKey queueKey = (QueueKey) key;
                        QueueInfo queue = getQueue(queueKey.queueName);
                        if (queue.isQueueTransient())
                        {
                            queue.putNow(queueKey.id);
                        }
                    }
                }
            }
            catch (Exception e)
            {
                throw new ResourceManagerSystemException(e);
            }
        }
    }

    @Override
    protected AbstractTransactionContext createTransactionContext(Object session)
    {
        return new QueueTransactionContext(this);
    }

    @Override
    protected void doBegin(AbstractTransactionContext context)
    {
        // Nothing special to do
    }

    @Override
    protected int doPrepare(AbstractTransactionContext context)
    {
        return XAResource.XA_OK;
    }

    @Override
    protected void doCommit(AbstractTransactionContext context) throws ResourceManagerException
    {
        context.doCommit();
    }

    protected Serializable doStore(QueueInfo queue, Serializable object) throws ObjectStoreException
    {
        ObjectStore<Serializable> store = queue.getStore();

        String id = muleContext == null ? UUID.getUUID() : muleContext.getUniqueIdString();
        Serializable key = new QueueKey(queue.getName(), id);
        store.store(key, object);
        return id;
    }

    protected void doRemove(QueueInfo queue, Serializable id) throws ObjectStoreException
    {
        ObjectStore<Serializable> store = queue.getStore();

        Serializable key = new QueueKey(queue.getName(), id);
        store.remove(key);
    }

    protected Serializable doLoad(QueueInfo queue, Serializable id) throws ObjectStoreException
    {
        ObjectStore<Serializable> store = queue.getStore();

        Serializable key = new QueueKey(queue.getName(), id);
        return store.retrieve(key);
    }

    @Override
    protected void doRollback(AbstractTransactionContext context) throws ResourceManagerException
    {
        context.doRollback();
    }

    protected void findAllListableObjectStores()
    {
        if (muleContext != null)
        {
            for (ListableObjectStore store : muleContext.getRegistry().lookupByType(ListableObjectStore.class).values())
            {
                addStore(store);
            }
        }
    }

    protected synchronized void findAllQueueStores()
    {
        if (muleContext != null)
        {
            for (QueueStore store: muleContext.getRegistry().lookupByType(QueueStore.class).values())
            {
                addStore(store);
            }
        }
    }

    @Override
    public void setMuleContext(MuleContext context)
    {
        this.muleContext = context;
    }

    public MuleContext getMuleContext()
    {
        return muleContext;
    }

    private void addStore(ListableObjectStore<?> store)
    {
        if (store instanceof QueueStore)
        {
            queueObjectStores.add((QueueStore) store);
        }
        listableObjectStores.add(store);
    }
}
