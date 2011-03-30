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
import org.mule.util.queue.QueuePersistenceStrategy.Holder;
import org.mule.util.xa.AbstractTransactionContext;
import org.mule.util.xa.AbstractXAResourceManager;
import org.mule.util.xa.ResourceManagerException;
import org.mule.util.xa.ResourceManagerSystemException;

import java.io.IOException;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

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

    private QueuePersistenceStrategy memoryPersistenceStrategy = new MemoryPersistenceStrategy();
    private QueuePersistenceStrategy persistenceStrategy;

    private QueueConfiguration defaultQueueConfiguration = new QueueConfiguration(false);
    private MuleContext muleContext;

    public void setMuleContext(MuleContext context)
    {
        this.muleContext = context;
    }

    public MuleContext getMuleContext()
    {
        return muleContext;
    }

    public synchronized QueueSession getQueueSession()
    {
        return new TransactionalQueueSession(this, this);
    }

    public synchronized void setDefaultQueueConfiguration(QueueConfiguration config)
    {
        this.defaultQueueConfiguration = config;
    }

    public synchronized void setQueueConfiguration(String queueName, QueueConfiguration config)
    {
        getQueue(queueName).config = config;
    }

    protected synchronized QueueInfo getQueue(String name)
    {
        QueueInfo q = queues.get(name);
        if (q == null)
        {
            q = new QueueInfo();
            q.name = name;
            q.list = new LinkedList<Object>();
            q.config = defaultQueueConfiguration;

            queues.put(name, q);
        }
        return q;
    }

    @Override
    protected void doStart() throws ResourceManagerSystemException
    {
        if (persistenceStrategy != null)
        {
            try
            {
                persistenceStrategy.open();
            }
            catch (IOException e)
            {
                throw new ResourceManagerSystemException(e);
            }
        }
    }

    @Override
    protected boolean shutdown(int mode, long timeoutMSecs)
    {
        try
        {
            if (persistenceStrategy != null)
            {
                persistenceStrategy.close();
            }
        }
        catch (IOException e)
        {
            // TODO MULE-863: What should we really do?
            logger.error("Error closing persistent store", e);
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
        if (persistenceStrategy != null)
        {
            try
            {
                List msgs = persistenceStrategy.restore();
                for (Object msg : msgs)
                {
                    Holder h = (Holder) msg;
                    getQueue(h.getQueue()).putNow(h.getId());
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
        QueueTransactionContext ctx = (QueueTransactionContext) context;
        try
        {
            if (ctx.added != null)
            {
                for (Map.Entry<QueueInfo, List<Object>> entry : ctx.added.entrySet())
                {
                    QueueInfo queue = entry.getKey();
                    List<Object> queueAdded = entry.getValue();
                    if (queueAdded != null && queueAdded.size() > 0)
                    {
                        for (Object object : queueAdded)
                        {
                            Object id = doStore(queue, object);
                            queue.putNow(id);
                        }
                    }
                }
            }
            if (ctx.removed != null)
            {
                for (Map.Entry<QueueInfo, List<Object>> entry : ctx.removed.entrySet())
                {
                    QueueInfo queue = entry.getKey();
                    List<Object> queueRemoved = entry.getValue();
                    if (queueRemoved != null && queueRemoved.size() > 0)
                    {
                        for (Object id : queueRemoved)
                        {
                            doRemove(queue, id);
                        }
                    }
                }
            }
        }
        catch (Exception e)
        {
            throw new ResourceManagerException(e);
        }
        finally
        {
            ctx.added = null;
            ctx.removed = null;
        }
    }

    protected Object doStore(QueueInfo queue, Object object) throws IOException
    {
        QueuePersistenceStrategy ps = (queue.config.persistent)
                        ? persistenceStrategy : memoryPersistenceStrategy;
        Object id = ps.store(queue.name, object);
        return id;
    }

    protected void doRemove(QueueInfo queue, Object id) throws IOException
    {
        QueuePersistenceStrategy ps = (queue.config.persistent)
                        ? persistenceStrategy : memoryPersistenceStrategy;
        ps.remove(queue.name, id);
    }

    protected Object doLoad(QueueInfo queue, Object id) throws IOException
    {
        QueuePersistenceStrategy ps = (queue.config.persistent)
                        ? persistenceStrategy : memoryPersistenceStrategy;
        Object obj = ps.load(queue.name, id);
        return obj;
    }

    @Override
    protected void doRollback(AbstractTransactionContext context) throws ResourceManagerException
    {
        QueueTransactionContext ctx = (QueueTransactionContext) context;
        if (ctx.removed != null)
        {
            for (Map.Entry<QueueInfo, List<Object>> entry : ctx.removed.entrySet())
            {
                QueueInfo queue = entry.getKey();
                List<Object> queueRemoved = entry.getValue();
                if (queueRemoved != null && queueRemoved.size() > 0)
                {
                    for (Object id : queueRemoved)
                    {
                        queue.putNow(id);
                    }
                }
            }
        }
        ctx.added = null;
        ctx.removed = null;
    }

    public QueuePersistenceStrategy getPersistenceStrategy()
    {
        return persistenceStrategy;
    }

    public void setPersistenceStrategy(QueuePersistenceStrategy persistenceStrategy)
    {
        if (operationMode != OPERATION_MODE_STOPPED)
        {
            throw new IllegalStateException();
        }
        this.persistenceStrategy = persistenceStrategy;
    }

    /**
     * @deprecated
     */
    @Deprecated
    public QueuePersistenceStrategy getMemoryPersistenceStrategy()
    {
        return memoryPersistenceStrategy;
    }

    /**
     * @deprecated
     */
    @Deprecated
    public void setMemoryPersistenceStrategy(QueuePersistenceStrategy memoryPersistenceStrategy)
    {
        if (operationMode != OPERATION_MODE_STOPPED)
        {
            throw new IllegalStateException();
        }
        this.memoryPersistenceStrategy = memoryPersistenceStrategy;
    }

}
