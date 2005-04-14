/* 
 * $Header$
 * $Revision$
 * $Date$
 * ------------------------------------------------------------------------------------------------------
 * 
 * Copyright (c) SymphonySoft Limited. All rights reserved.
 * http://www.symphonysoft.com
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
	
	public synchronized QueueSession getQueueSession() {
		if (persistenceStrategy == null) {
			logger.warn("No persistence strategy set, defaulting to MemoryPersistenceStrategy");
			persistenceStrategy = new MemoryPersistenceStrategy();
		}
		return new QueueSessionImpl();
	}
	
	protected Queue getQueue(String name) {
		Queue  q  = (Queue) queues.get(name);
		if (q == null) {
			q = createQueue();
			queues.put(name, q);
		}
		return q;
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
    				Queue  q  = getQueue(h.queueName);
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
		return new QueueTransactionContext();
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
				for (Iterator it = ctx.added.entrySet().iterator(); it.hasNext();) {
					Map.Entry entry = (Map.Entry) it.next();
					String queueName = (String) entry.getKey();
					List queueAdded = (List) entry.getValue();
					if (queueAdded != null && queueAdded.size() > 0) {
						Queue queue = getQueue(queueName);
						for (Iterator itAdded = queueAdded.iterator(); itAdded.hasNext();) {
							Object object = itAdded.next();
							Object id = doStore(queueName, object);
							queue.put(id);
						}
					}
				}
			}
			if (ctx.removed != null) {
				for (Iterator it = ctx.removed.entrySet().iterator(); it.hasNext();) {
					Map.Entry entry = (Map.Entry) it.next();
					String queueName = (String) entry.getKey();
					List queueRemoved = (List) entry.getValue();
					if (queueRemoved != null && queueRemoved.size() > 0) {
						for (Iterator itRemoved = queueRemoved.iterator(); itRemoved.hasNext();) {
							Object id = itRemoved.next();
							doRemove(queueName, id);
						}
					}
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
			for (Iterator it = ctx.removed.entrySet().iterator(); it.hasNext();) {
				Map.Entry entry = (Map.Entry) it.next(); 
				String queueName = (String) entry.getKey();
				List queueRemoved = (List) entry.getValue();
				if (queueRemoved != null && queueRemoved.size() > 0) {
					Queue queue = getQueue(queueName);
					for (Iterator itRemoved = queueRemoved.iterator(); itRemoved.hasNext();) {
						Object id = itRemoved.next();
						queue.put(id);
					}
				}
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
	
	protected class QueueTransactionContext extends AbstractTransactionContext {
		protected Map added;
		protected Map removed;
		
		public QueueTransactionContext() {
		}

		/* (non-Javadoc)
		 * @see EDU.oswego.cs.dl.util.concurrent.Channel#put(java.lang.Object)
		 */
		public void put(String queueName, Object item) {
			readOnly = false;
			if (added == null) {
				added = new HashMap();
			}
			List queueAdded = (List) added.get(queueName);
			if (queueAdded == null) {
				queueAdded = new ArrayList();
				added.put(queueName, queueAdded);
			}
			queueAdded.add(item);
		}

		/* (non-Javadoc)
		 * @see EDU.oswego.cs.dl.util.concurrent.Channel#take()
		 */
		public Object take(String queueName) throws IOException {
			readOnly = false;
			if (added != null) {
				List queueAdded = (List) added.get(queueName);
				if (queueAdded != null) {
					return queueAdded.remove(queueAdded.size() - 1);
				}
			}
			Queue queue = TransactionalQueueManager.this.getQueue(queueName);
			Object o = queue.take();
			if (o != null) {
				if (removed == null) {
					removed = new HashMap();
				}
				List queueRemoved = (List) removed.get(queueName);
				if (queueRemoved == null) {
					queueRemoved = new ArrayList();
					removed.put(queueName, queueRemoved);
				}
				queueRemoved.add(o);
				o = doLoad(o);
			}
			return o;
		}

		/* (non-Javadoc)
		 * @see EDU.oswego.cs.dl.util.concurrent.Channel#poll(long)
		 */
		public int size(String queueName) {
			Queue queue = TransactionalQueueManager.this.getQueue(queueName);
			int sz = queue.size();
			if (added != null) {
				List queueAdded = (List) added.get(queueName);
				if( queueAdded != null) {
					sz += queueAdded.size();
				}
			}
			return sz;
		}

	}
	
	protected class QueueSessionImpl extends AbstractSession implements QueueSession {

		/* (non-Javadoc)
		 * @see org.mule.transaction.xa.queue.QueueSession#getQueue(java.lang.String)
		 */
		public Queue getQueue(String name) {
			Queue queue = TransactionalQueueManager.this.getQueue(name);
			return new QueueImpl(queue, name);
		}
		
		protected class QueueImpl implements Queue {
			
			protected Queue queue;
			protected String queueName;
			
			public QueueImpl(Queue queue, String queueName) {
				this.queue = queue;
				this.queueName = queueName;
			}
			
			/* (non-Javadoc)
			 * @see EDU.oswego.cs.dl.util.concurrent.Channel#put(java.lang.Object)
			 */
			public void put(Object item)  {
				if (localContext != null) {
					((QueueTransactionContext) localContext).put(queueName, item);
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
				try {
					if (localContext != null) {
						return ((QueueTransactionContext) localContext).take(queueName);
					} else {
						Object id = queue.take();
						if (id != null) {
							Object item = doLoad(id);
							doRemove(queueName, id);
							return item;
						}
						return null;
					}
				} catch (IOException e) {
					throw new RuntimeException(e);
				}
			}
	
			public int size() {
				if (localContext != null) {
					return ((QueueTransactionContext) localContext).size(queueName);
				} else {
					return queue.size();
				}
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
