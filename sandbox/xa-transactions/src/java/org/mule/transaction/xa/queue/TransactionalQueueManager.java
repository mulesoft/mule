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
import java.util.LinkedList;
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
public class TransactionalQueueManager extends AbstractXAResourceManager implements QueueManager {

	private static Log logger = LogFactory.getLog(TransactionalQueueManager.class);
	
	private Map queues = new HashMap();
	
	private QueuePersistenceStrategy memoryPersistenceStrategy = new MemoryPersistenceStrategy();
	private QueuePersistenceStrategy persistenceStrategy;
	
	private QueueConfiguration defaultQueueConfiguration = new QueueConfiguration(false);
	
	public synchronized QueueSession getQueueSession() {
		return new QueueSessionImpl();
	}
	
	public synchronized void setDefaultQueueConfiguration(QueueConfiguration config) {
		this.defaultQueueConfiguration = config;
	}
	
	public synchronized void setQueueConfiguration(String queueName, QueueConfiguration config) {
		getQueue(queueName).config = config;
	}
	
	protected synchronized QueueInfo getQueue(String name) {
		QueueInfo q = (QueueInfo) queues.get(name);
		if (q == null) {
			q = new QueueInfo();
			q.name = name;
			q.list = new LinkedList();
			q.config = defaultQueueConfiguration;
			queues.put(name, q);
		}
		return q;
	}
	
	/* (non-Javadoc)
	 * @see org.mule.transaction.xa.AbstractResourceManager#getLogger()
	 */
	protected Log getLogger() {
		return logger;
	}

	public void close() {
		try {
			stop(SHUTDOWN_MODE_NORMAL);
		} catch (ResourceManagerException e) {
			logger.error("Error disposing manager", e);
		}
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
    				getQueue(h.queueName).putNow(id);
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
					QueueInfo queue = (QueueInfo) entry.getKey();
					List queueAdded = (List) entry.getValue();
					if (queueAdded != null && queueAdded.size() > 0) {
						for (Iterator itAdded = queueAdded.iterator(); itAdded.hasNext();) {
							Object object = itAdded.next();
							Object id = doStore(queue, object);
							queue.putNow(id);
						}
					}
				}
			}
			if (ctx.removed != null) {
				for (Iterator it = ctx.removed.entrySet().iterator(); it.hasNext();) {
					Map.Entry entry = (Map.Entry) it.next();
					QueueInfo queue = (QueueInfo) entry.getKey();
					List queueRemoved = (List) entry.getValue();
					if (queueRemoved != null && queueRemoved.size() > 0) {
						for (Iterator itRemoved = queueRemoved.iterator(); itRemoved.hasNext();) {
							Object id = itRemoved.next();
							doRemove(queue, id);
						}
					}
				}
			}
		} catch (Exception e) {
			throw new ResourceManagerException("Could not commit transaction", e);
		}
	}
	
	protected Object doStore(QueueInfo queue, Object object) throws IOException {
		if (queue.config.persistent) {
			return persistenceStrategy.store(getHolder(queue.name, object));
		} else {
			return memoryPersistenceStrategy.store(getHolder(queue.name, object));
		}
	}

	protected void doRemove(QueueInfo queue, Object id) throws IOException {
		if (queue.config.persistent) {
			persistenceStrategy.remove(id);
		} else {
			memoryPersistenceStrategy.remove(id);
		}
	}
	
	protected Object doLoad(QueueInfo queue, Object id) throws IOException {
		if (queue.config.persistent) {
			Object h = persistenceStrategy.load(id);
			return ((Holder) h).object;
		} else {
			Object h = memoryPersistenceStrategy.load(id);
			return ((Holder) h).object;
		}
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
				QueueInfo queue = (QueueInfo) entry.getKey();
				List queueRemoved = (List) entry.getValue();
				if (queueRemoved != null && queueRemoved.size() > 0) {
					for (Iterator itRemoved = queueRemoved.iterator(); itRemoved.hasNext();) {
						Object id = itRemoved.next();
						queue.putNow(id);
					}
				}
			}
		}
	}
	
	public static class QueueInfo {
		protected LinkedList list;
		protected String name;
		protected QueueConfiguration config;
		public boolean equals(Object obj) {
			return (obj instanceof QueueInfo && name.equals(((QueueInfo) obj).name));
		}
		public int hashCode() {
			return name.hashCode();
		}
		
		public void putNow(Object o) {
			synchronized (list) {
				list.addLast(o);
				list.notifyAll();
			}
		}
		
		public void putWhenRoom(Object o, int room) throws InterruptedException {
			if (Thread.interrupted()) {
				throw new InterruptedException();
			}
			synchronized (list) {
				if (config.capacity > 0) {
					if (config.capacity <= room) {
						throw new IllegalStateException("Can not add more objects than the capacity in one time");
					}
					while (list.size() >= config.capacity - room) {
						list.wait();
					}
				}
				if (o != null) {
					list.addLast(o);
				}
				list.notifyAll();
			}
		}
		
		public Object takeBlock() throws InterruptedException {
			if (Thread.interrupted()) {
				throw new InterruptedException();
			}
			synchronized (list) {
				while (list.isEmpty()) {
					list.wait();
				}
				Object o = list.removeFirst();
				list.notifyAll();
				return o;
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

		public void put(QueueInfo queue, Object item) throws InterruptedException {
			readOnly = false;
			if (added == null) {
				added = new HashMap();
			}
			List queueAdded = (List) added.get(queue);
			if (queueAdded == null) {
				queueAdded = new ArrayList();
				added.put(queue, queueAdded);
			}
			// wait for enough room
			queue.putWhenRoom(null, queueAdded.size());
			queueAdded.add(item);
		}

		public Object take(QueueInfo queue) throws IOException, InterruptedException {
			readOnly = false;
			if (added != null) {
				List queueAdded = (List) added.get(queue);
				if (queueAdded != null) {
					return queueAdded.remove(queueAdded.size() - 1);
				}
			}
			Object o = queue.takeBlock();
			if (o != null) {
				if (removed == null) {
					removed = new HashMap();
				}
				List queueRemoved = (List) removed.get(queue);
				if (queueRemoved == null) {
					queueRemoved = new ArrayList();
					removed.put(queue, queueRemoved);
				}
				queueRemoved.add(o);
				o = doLoad(queue, o);
			}
			return o;
		}

		public int size(QueueInfo queue) {
			int sz = queue.list.size();
			if (added != null) {
				List queueAdded = (List) added.get(queue);
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
			QueueInfo queue = TransactionalQueueManager.this.getQueue(name);
			return new QueueImpl(queue);
		}
		
		protected class QueueImpl implements Queue {
			
			protected QueueInfo queue;
			
			public QueueImpl(QueueInfo queue) {
				this.queue = queue;
			}
			
			/* (non-Javadoc)
			 * @see EDU.oswego.cs.dl.util.concurrent.Channel#put(java.lang.Object)
			 */
			public void put(Object item) throws InterruptedException {
				if (localContext != null) {
					((QueueTransactionContext) localContext).put(queue, item);
				} else {
					try {
						Object id = doStore(queue, item);
						queue.putWhenRoom(id, 0);
					} catch (IOException e) {
						throw new RuntimeException(e);
					}
				}
			}
	
			/* (non-Javadoc)
			 * @see EDU.oswego.cs.dl.util.concurrent.Channel#take()
			 */
			public Object take() throws InterruptedException {
				try {
					if (localContext != null) {
						return ((QueueTransactionContext) localContext).take(queue);
					} else {
						Object id = queue.takeBlock();
						if (id != null) {
							Object item = doLoad(queue, id);
							doRemove(queue, id);
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
					return ((QueueTransactionContext) localContext).size(queue);
				} else {
					return queue.list.size();
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
