/* 
 * $Header$
 * $Revision$
 * $Date$
 * ------------------------------------------------------------------------------------------------------
 * 
 * Copyright (c) Cubis Limited. All rights reserved.
 * http://www.cubis.co.uk 
 * 
 * The software in this package is published under the terms of the BSD
 * style license a copy of which has been included with this distribution in
 * the LICENSE.txt file. 
 *
 */
package org.mule.transaction.xa.queue;

import java.io.IOException;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import javax.transaction.xa.XAResource;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.mule.transaction.xa.AbstractTransactionContext;
import org.mule.transaction.xa.AbstractXAResourceManager;
import org.mule.transaction.xa.ResourceManagerException;
import org.mule.transaction.xa.ResourceManagerSystemException;

/**
 * @author <a href="mailto:gnt@codehaus.org">Guillaume Nodet</a>
 * @version $Revision$
 */
public class TransactionalQueueManager extends AbstractXAResourceManager {

	private static Log logger = LogFactory.getLog(TransactionalQueueManager.class);
	
	private Map queues = new HashMap();
	
	private QueuePersistenceStrategy persistenceStrategy;
	
	public synchronized TransactionalQueue getQueue(String name) {
		Queue queue = (Queue) queues.get(name);
		if (queue == null) {
			queue = createQueue();
			queues.put(name, queue);
		}
		if (persistenceStrategy == null) {
			logger.warn("No persistence strategy set, defaulting to MemoryPersistenceStrategy");
			persistenceStrategy = new MemoryPersistenceStrategy();
		}
		return new QueueSession(queue, name);
	}
	
	protected Queue createQueue() {
		return new BasicQueue();
	}
	
	/* (non-Javadoc)
	 * @see org.mule.transaction.xa.AbstractResourceManager#getLogger()
	 */
	protected Log getLogger() {
		return logger;
	}

    protected boolean shutdown(int mode, long timeoutMSecs) {
    	try {
	    	if (persistenceStrategy != null) {
	    		persistenceStrategy.close();
	    	}
    	} catch (IOException e) {
    		logger.error("Error closing persistent store", e);
    	}
    	return super.shutdown(mode, timeoutMSecs);
    }
	
    protected void recover() throws ResourceManagerSystemException {
    	if (persistenceStrategy != null) {
    		try {
    			List msgs = persistenceStrategy.restore();
    			for (Iterator it = msgs.iterator(); it.hasNext();) {
    				Object id = it.next();
    				Holder h  = (Holder) persistenceStrategy.load(id);
    				Queue  q  = (Queue) queues.get(h.queueName);
    				if (q == null) {
    					q = createQueue();
    					queues.put(h.queueName, q);
    				}
    				q.put(id);
    			}
    		} catch (Exception e) {
    			throw new ResourceManagerSystemException(e);
    		}
    	}
    }

	
	/* (non-Javadoc)
	 * @see org.mule.transaction.xa.AbstractResourceManager#createTransactionContext()
	 */
	protected AbstractTransactionContext createTransactionContext(Object session) {
		return new QueueTransactionContext(((QueueSession) session).queue, ((QueueSession) session).queueName);
	}

	/* (non-Javadoc)
	 * @see org.mule.transaction.xa.AbstractResourceManager#doBegin(org.mule.transaction.xa.AbstractTransactionContext)
	 */
	protected void doBegin(AbstractTransactionContext context) {
		// Nothing special to do
	}

	/* (non-Javadoc)
	 * @see org.mule.transaction.xa.AbstractResourceManager#doPrepare(org.mule.transaction.xa.AbstractTransactionContext)
	 */
	protected int doPrepare(AbstractTransactionContext context) {
		return XAResource.XA_OK;
	}

	/* (non-Javadoc)
	 * @see org.mule.transaction.xa.AbstractResourceManager#doCommit(org.mule.transaction.xa.AbstractTransactionContext)
	 */
	protected void doCommit(AbstractTransactionContext context) throws ResourceManagerException {
		try {
			QueueTransactionContext ctx = (QueueTransactionContext) context;
			if (ctx.added != null) {
				for (Iterator it = ctx.added.iterator(); it.hasNext();) {
					Object object = it.next();
					Object id = doStore(ctx.queueName, object);
					ctx.queue.put(id);
				}
			}
			if (ctx.removed != null) {
				for (Iterator it = ctx.removed.iterator(); it.hasNext();) {
					Object id = it.next();
					doRemove(ctx.queueName, id);
				}
			}
		} catch (IOException e) {
			throw new ResourceManagerException("Could not persist object", e);
		}
	}
	
	protected Object doStore(String queueName, Object object) throws IOException {
		return persistenceStrategy.store(getHolder(queueName, object));
	}

	protected void doRemove(String queueName, Object id) throws IOException {
		persistenceStrategy.remove(id);
	}
	
	protected Object doLoad(Object id) throws IOException {
		Object h = persistenceStrategy.load(id);
		return ((Holder) h).object;
	}
	
	protected Holder getHolder(String queueName, Object object) {
		return new Holder(queueName, object);
	}

	/* (non-Javadoc)
	 * @see org.mule.transaction.xa.AbstractResourceManager#doRollback(org.mule.transaction.xa.AbstractTransactionContext)
	 */
	protected void doRollback(AbstractTransactionContext context) throws ResourceManagerException {
		QueueTransactionContext ctx = (QueueTransactionContext) context;
		if (ctx.removed != null) {
			for (Iterator it = ctx.removed.iterator(); it.hasNext();) {
				ctx.queue.put(it.next());
			}
		}
	}
	
	public static class Holder implements Serializable {
		protected String queueName;
		protected Object object;
		public Holder(String queueName, Object object) {
			this.queueName = queueName;
			this.object = object;
		}
		public boolean equals(Object obj) {
			if (obj instanceof Holder) {
				Holder h = (Holder) obj;
				return queueName.equals(h.queueName) && object.equals(h.object);
			}
			return false;
		}
		public int hashCode() {
			return queueName.hashCode() ^ object.hashCode();
		}
		public String toString() {
			return "[" + queueName + "," + object + "]";
		}
	}
	
	protected class QueueTransactionContext extends AbstractTransactionContext implements Queue {
		protected Queue queue;
		protected List added;
		protected List removed;
		protected String queueName;
		
		public QueueTransactionContext(Queue queue, String queueName) {
			this.queue = queue;
			this.queueName = queueName;
		}

		/* (non-Javadoc)
		 * @see EDU.oswego.cs.dl.util.concurrent.Channel#put(java.lang.Object)
		 */
		public void put(Object item) {
			readOnly = false;
			if (added == null) {
				added = new ArrayList();
			}
			added.add(item);
		}

		/* (non-Javadoc)
		 * @see EDU.oswego.cs.dl.util.concurrent.Channel#take()
		 */
		public Object take() {
			readOnly = false;
			if (added != null) {
				return added.remove(added.size() - 1);
			}
			Object o = queue.take();
			if (o != null) {
				if (removed == null) {
					removed = new ArrayList();
				}
				removed.add(o);
			}
			return o;
		}

		/* (non-Javadoc)
		 * @see EDU.oswego.cs.dl.util.concurrent.Channel#poll(long)
		 */
		public int size() {
			int sz = queue.size();
			if (added != null) {
				sz += added.size();
			}
			return sz;
		}

	}
	
	protected class QueueSession extends AbstractSession implements TransactionalQueue {

		protected Queue queue;
		protected String queueName;
		
		public QueueSession(Queue queue, String queueName) {
			this.queue = queue;
			this.queueName = queueName;
		}
		
		/* (non-Javadoc)
		 * @see EDU.oswego.cs.dl.util.concurrent.Channel#put(java.lang.Object)
		 */
		public void put(Object item)  {
			if (localContext != null) {
				((QueueTransactionContext) localContext).put(item);
			} else {
				try {
					Object id = doStore(queueName, item);
					queue.put(id);
				} catch (IOException e) {
					throw new RuntimeException(e);
				}
			}
		}

		/* (non-Javadoc)
		 * @see EDU.oswego.cs.dl.util.concurrent.Channel#take()
		 */
		public Object take() {
			if (localContext != null) {
				return ((QueueTransactionContext) localContext).take();
			} else {
				try {
					Object id = queue.take();
					if (id != null) {
						Object item = doLoad(id);
						doRemove(queueName, id);
						return item;
					}
					return null;
				} catch (IOException e) {
					throw new RuntimeException(e);
				}
			}
		}

		public int size() {
			if (localContext != null) {
				return ((QueueTransactionContext) localContext).size();
			} else {
				return queue.size();
			}
		}
		
	}

	/**
	 * @return Returns the persistenceStrategy.
	 */
	public QueuePersistenceStrategy getPersistenceStrategy() {
		return persistenceStrategy;
	}
	
	/**
	 * @param persistenceStrategy The persistenceStrategy to set.
	 */
	public void setPersistenceStrategy(QueuePersistenceStrategy persistenceStrategy) {
		if (this.persistenceStrategy != null) {
			throw new IllegalStateException();
		}
		this.persistenceStrategy = persistenceStrategy;
	}
}
