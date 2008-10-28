/*
 * $Id$
 * --------------------------------------------------------------------------------------
 * Copyright (c) MuleSource, Inc.  All rights reserved.  http://www.mulesource.com
 *
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.util.queue;

import org.mule.util.queue.QueuePersistenceStrategy.Holder;
import org.mule.util.xa.AbstractTransactionContext;
import org.mule.util.xa.AbstractXAResourceManager;
import org.mule.util.xa.ResourceManagerException;
import org.mule.util.xa.ResourceManagerSystemException;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import javax.transaction.xa.XAResource;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

/**
 * The Transactional Queue Manager is responsible for creating and Managing
 * transactional Queues. Queues can also be persistent by setting a persistence
 * strategy on the manager. Default straties are provided for Memory, Jounaling,
 * Cache and File.
 */
public class TransactionalQueueManager extends AbstractXAResourceManager implements QueueManager
{

    private static Log logger = LogFactory.getLog(TransactionalQueueManager.class);

    private Map<String, QueueInfo> queues = new HashMap<String, QueueInfo>();

    private QueuePersistenceStrategy memoryPersistenceStrategy = new MemoryPersistenceStrategy();
    private QueuePersistenceStrategy persistenceStrategy;

    private QueueConfiguration defaultQueueConfiguration = new QueueConfiguration(false);
    
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
            q.list = new LinkedList();
            q.config = defaultQueueConfiguration;
            queues.put(name, q);
        }
        return q;
    }

    /*
     * (non-Javadoc)
     * 
     * @see org.mule.transaction.xa.AbstractResourceManager#getLogger()
     */
    protected Log getLogger()
    {
        return logger;
    }

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

    /*
     * (non-Javadoc)
     * 
     * @see org.mule.transaction.xa.AbstractResourceManager#createTransactionContext()
     */
    protected AbstractTransactionContext createTransactionContext(Object session)
    {
        return new QueueTransactionContext();
    }

    /*
     * (non-Javadoc)
     * 
     * @see org.mule.transaction.xa.AbstractResourceManager#doBegin(org.mule.transaction.xa.AbstractTransactionContext)
     */
    protected void doBegin(AbstractTransactionContext context)
    {
        // Nothing special to do
    }

    /*
     * (non-Javadoc)
     * 
     * @see org.mule.transaction.xa.AbstractResourceManager#doPrepare(org.mule.transaction.xa.AbstractTransactionContext)
     */
    protected int doPrepare(AbstractTransactionContext context)
    {
        return XAResource.XA_OK;
    }

    /*
     * (non-Javadoc)
     * 
     * @see org.mule.transaction.xa.AbstractResourceManager#doCommit(org.mule.transaction.xa.AbstractTransactionContext)
     */
    protected void doCommit(AbstractTransactionContext context) throws ResourceManagerException
    {
        QueueTransactionContext ctx = (QueueTransactionContext) context;
        try
        {
            if (ctx.added != null)
            {
                for (Object o : ctx.added.entrySet())
                {
                    Map.Entry entry = (Map.Entry) o;
                    QueueInfo queue = (QueueInfo) entry.getKey();
                    List queueAdded = (List) entry.getValue();
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
                for (Object o : ctx.removed.entrySet())
                {
                    Map.Entry entry = (Map.Entry) o;
                    QueueInfo queue = (QueueInfo) entry.getKey();
                    List queueRemoved = (List) entry.getValue();
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
            // throw new ResourceManagerException("Could not commit
            // transaction", e);
            // TODO: add an i18n Message
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

    /*
     * (non-Javadoc)
     * 
     * @see org.mule.transaction.xa.AbstractResourceManager#doRollback(org.mule.transaction.xa.AbstractTransactionContext)
     */
    protected void doRollback(AbstractTransactionContext context) throws ResourceManagerException
    {
        QueueTransactionContext ctx = (QueueTransactionContext) context;
        if (ctx.removed != null)
        {
            for (Object o : ctx.removed.entrySet())
            {
                Map.Entry entry = (Map.Entry) o;
                QueueInfo queue = (QueueInfo) entry.getKey();
                List queueRemoved = (List) entry.getValue();
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

    protected class QueueTransactionContext extends AbstractTransactionContext
    {
        protected Map added;
        protected Map removed;

        @SuppressWarnings("unchecked")
        public boolean offer(QueueInfo queue, Object item, long timeout) throws InterruptedException
        {
            readOnly = false;
            if (added == null)
            {
                added = new HashMap();
            }
            List queueAdded = (List) added.get(queue);
            if (queueAdded == null)
            {
                queueAdded = new ArrayList();
                added.put(queue, queueAdded);
            }
            // wait for enough room
            if (queue.offer(null, queueAdded.size(), timeout))
            {
                queueAdded.add(item);
                return true;
            }
            else
            {
                return false;
            }
        }

        @SuppressWarnings("unchecked")
        public Object poll(QueueInfo queue, long timeout) throws IOException, InterruptedException
        {
            readOnly = false;
            if (added != null)
            {
                List queueAdded = (List)added.get(queue);
                if (queueAdded != null)
                {
                    return queueAdded.remove(queueAdded.size() - 1);
                }
            }
            Object o = queue.poll(timeout);
            if (o != null)
            {
                if (removed == null)
                {
                    removed = new HashMap();
                }
                List queueRemoved = (List) removed.get(queue);
                if (queueRemoved == null)
                {
                    queueRemoved = new ArrayList();
                    removed.put(queue, queueRemoved);
                }
                queueRemoved.add(o);
                o = doLoad(queue, o);
            }
            return o;
        }

        public Object peek(QueueInfo queue) throws IOException, InterruptedException
        {
            readOnly = false;
            if (added != null)
            {
                List queueAdded = (List) added.get(queue);
                if (queueAdded != null)
                {
                    return queueAdded.get(queueAdded.size() - 1);
                }
            }
            Object o = queue.peek();
            if (o != null)
            {
                o = doLoad(queue, o);
            }
            return o;
        }

        public int size(QueueInfo queue)
        {
            int sz = queue.list.size();
            if (added != null)
            {
                List queueAdded = (List) added.get(queue);
                if (queueAdded != null)
                {
                    sz += queueAdded.size();
                }
            }
            return sz;
        }

    }

    /**
     * @return Returns the persistenceStrategy.
     */
    public QueuePersistenceStrategy getPersistenceStrategy()
    {
        return persistenceStrategy;
    }

    /**
     * @param persistenceStrategy The persistenceStrategy to set.
     */
    public void setPersistenceStrategy(QueuePersistenceStrategy persistenceStrategy)
    {
        if (operationMode != OPERATION_MODE_STOPPED)
        {
            throw new IllegalStateException();
        }
        this.persistenceStrategy = persistenceStrategy;
    }

    public QueuePersistenceStrategy getMemoryPersistenceStrategy()
    {
        return memoryPersistenceStrategy;
    }

    public void setMemoryPersistenceStrategy(QueuePersistenceStrategy memoryPersistenceStrategy)
    {
        if (operationMode != OPERATION_MODE_STOPPED)
        {
            throw new IllegalStateException();
        }
        this.memoryPersistenceStrategy = memoryPersistenceStrategy;
    }
}
