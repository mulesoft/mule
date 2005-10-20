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
package org.mule.impl.model.seda;

import org.mule.MuleManager;
import org.mule.config.PoolingProfile;
import org.mule.config.ThreadingProfile;
import org.mule.config.i18n.Message;
import org.mule.config.i18n.Messages;
import org.mule.impl.FailedToQueueEventException;
import org.mule.impl.MuleDescriptor;
import org.mule.impl.MuleEvent;
import org.mule.impl.model.AbstractComponent;
import org.mule.impl.model.MuleProxy;
import org.mule.umo.ComponentException;
import org.mule.umo.UMOEvent;
import org.mule.umo.UMOException;
import org.mule.umo.UMOMessage;
import org.mule.umo.lifecycle.InitialisationException;
import org.mule.umo.lifecycle.LifecycleException;
import org.mule.umo.manager.UMOWorkManager;
import org.mule.util.ObjectPool;
import org.mule.util.queue.QueueSession;

import javax.resource.spi.work.Work;
import javax.resource.spi.work.WorkException;
import javax.resource.spi.work.WorkManager;
import java.util.NoSuchElementException;

/**
 * A Seda component runs inside a Seda Model and is responsible for managing
 * a Seda Queue and thread pool for a Mule sevice component.  In Seda terms
 * this is equivilent to a stage.
 *
 * @author <a href="mailto:ross.mason@symphonysoft.com">Ross Mason</a>
 * @version $Revision$
 */
public class SedaComponent extends AbstractComponent implements Work {

    /**
     * A pool of available Mule Proxies
     */
    protected ObjectPool proxyPool = null;

    protected UMOWorkManager workManager;

    protected String descriptorQueueName;

    /**
     * The time out used for taking from the Seda Queue
     */
    protected int queueTimeout = 0;

    /**
     * Default constructor
     */
    public SedaComponent(MuleDescriptor descriptor, SedaModel model) {
        super(descriptor, model);
        descriptorQueueName = descriptor.getName() + ".component";
        queueTimeout = model.getQueueTimeout();
    }

    /**
     * Initialise the component. The component will first create a Mule UMO from
     * the UMODescriptor and then initialise a pool based on the attributes in
     * the UMODescriptor.
     *
     * @throws org.mule.umo.lifecycle.InitialisationException
     *          if the component fails to initialise
     * @see org.mule.umo.UMODescriptor
     */
    public synchronized void doInitialise() throws InitialisationException {

        // Create thread pool
        ThreadingProfile tp = descriptor.getThreadingProfile();
        workManager = tp.createWorkManager(descriptor.getName());
        try {
            // Setup event Queue (used for VM execution)
            descriptor.getQueueProfile().configureQueue(descriptor.getName());

        } catch (InitialisationException e) {
            throw e;
        } catch (Throwable e) {
            throw new InitialisationException(new Message(Messages.X_FAILED_TO_INITIALISE, "Component Queue"), e, this);
        }
    }

    protected void initialisePool() throws InitialisationException {
        try {
            // Initialise the proxy pool
            proxyPool = descriptor.getPoolingProfile().getPoolFactory().createPool(descriptor);

            if (descriptor.getPoolingProfile().getInitialisationPolicy() == PoolingProfile.POOL_INITIALISE_ALL_COMPONENTS) {
                int threads = descriptor.getPoolingProfile().getMaxActive();
                for (int i = 0; i < threads; i++) {
                    proxyPool.returnObject(proxyPool.borrowObject());
                }
            } else if (descriptor.getPoolingProfile().getInitialisationPolicy() == PoolingProfile.POOL_INITIALISE_ONE_COMPONENT) {
                proxyPool.returnObject(proxyPool.borrowObject());
            }
            poolInitialised.set(true);
        } catch (Exception e) {
            throw new InitialisationException(new Message(Messages.X_FAILED_TO_INITIALISE, "Proxy Pool"), e, this);
        }
    }


    public void doForceStop() throws UMOException {
        workManager.stop();
    }

    public void doStop() throws UMOException {
        workManager.stop();
    }

    public void doStart() throws UMOException {

        try {
            // Need to initialise the pool only after all listerner have
            // been
            // registed and initialised so we need to delay until now
            if (!poolInitialised.get()) {
                initialisePool();
            }
            proxyPool.start();
            workManager.start();
            workManager.scheduleWork(this, WorkManager.INDEFINITE, null, null);
        } catch (Exception e) {
            throw new LifecycleException(new Message(Messages.FAILED_TO_START_X, "Component: "
                    + descriptor.getName()), e, this);
        }
    }


    public void doDispose() {

        try {
            // threadPool.awaitTerminationAfterShutdown();
            if (workManager != null) {
                workManager.dispose();
            }
        } catch (Exception e) {
            logger.error("Component Thread Pool did not close properly: " + e);
        }
        try {
            if (proxyPool != null) {
                proxyPool.clearPool();
            }
        } catch (Exception e) {
            logger.error("Proxy Pool did not close properly: " + e);
        }
    }

    protected void doDispatch(UMOEvent event) throws UMOException
    {
        // Dispatching event to the component
        if (stats.isEnabled()) {
            stats.incReceivedEventASync();
        }
        if (logger.isDebugEnabled()) {
            logger.debug("Component: " + descriptor.getName() + " has received asynchronous event on: "
                    + event.getEndpoint().getEndpointURI());
        }

        // Block until we can queue the next event
        try {
            enqueue(event);
            if (stats.isEnabled()) {
                stats.incQueuedEvent();
            }
        } catch (Exception e) {
            FailedToQueueEventException e1 = new FailedToQueueEventException(new Message(Messages.INTERRUPTED_QUEUING_EVENT_FOR_X,
                    getName()),
                    event.getMessage(),
                    this,
                    e);
            handleException(e1);
        }

        if (logger.isTraceEnabled()) {
        	logger.trace("Event added to queue for: " + descriptor.getName());
        }
    }

    public UMOMessage doSend(UMOEvent event) throws UMOException {

        UMOMessage result = null;
        MuleProxy proxy = null;
        try {
            proxy = (MuleProxy) proxyPool.borrowObject();
            getStatistics().setComponentPoolSize(proxyPool.getSize());
            proxy.setStatistics(getStatistics());

            if (logger.isDebugEnabled()) {
                logger.debug(this + " : got proxy for " + event.getId() + " = " + proxy);
            }
            result = (UMOMessage) proxy.onCall(event);
        } catch (UMOException e) {
            throw e;
        } catch (Exception e) {
            throw new ComponentException(event.getMessage(), this, e);
        } finally {
            try {
                if (proxy != null) {
                    proxyPool.returnObject(proxy);
                }
            } catch (Exception e) {
                throw new ComponentException(event.getMessage(), this, e);
            }
            getStatistics().setComponentPoolSize(proxyPool.getSize());
        }
        return result;
    }

    /**
     * @return the pool of Mule UMOs initialised in this component
     */
    ObjectPool getProxyPool() {
        return proxyPool;
    }


    public int getQueueSize() {
        QueueSession queueSession = MuleManager.getInstance().getQueueManager().getQueueSession();
        return queueSession.getQueue(descriptor.getName()).size();
    }

    /**
     * While the component isn't stopped this runs a continuous loop checking
     * for new events in the queue
     */
    public void run() {
        MuleEvent event = null;
        MuleProxy proxy = null;
        QueueSession queueSession = null;

        while (!stopped.get()) {
            try {
                // Wait if the component is paused
                paused.whenFalse(null);

                //If we're doing a draining stop, read all events from the queue
                //before stopping
                if (stopping.get()) {
                    if (queueSession.getQueue(descriptor.getName() + ".component").size() == 0) {
                        stopping.set(false);
                        break;
                    }
                }
                event = (MuleEvent) dequeue();
                if(event!=null) {
                    if (stats.isEnabled()) {
                        stats.decQueuedEvent();
                    }
                    if (logger.isDebugEnabled()) {
                        logger.debug("Component: " + descriptor.getName() + " dequeued event on: "
                                + event.getEndpoint().getEndpointURI());
                    }

                    proxy = (MuleProxy) proxyPool.borrowObject();
                    getStatistics().setComponentPoolSize(proxyPool.getSize());
                    proxy.setStatistics(getStatistics());
                    proxy.start();
                    proxy.onEvent(queueSession, event);
                    workManager.scheduleWork(proxy, WorkManager.INDEFINITE, null, null);
                }
            } catch (Exception e) {
                if (proxy != null) {
                    try {
                        proxyPool.returnObject(proxy);
                    } catch (Exception e2) {
                        logger.info("Failed to return proxy to pool", e2);
                    }
                }

                if (e instanceof InterruptedException) {
                    stopping.set(false);
                    break;
                } else if (e instanceof NoSuchElementException) {
                    handleException(new ComponentException(new Message(Messages.PROXY_POOL_TIMED_OUT),
                            event.getMessage(),
                            this,
                            e));
                } else if (e instanceof UMOException) {
                    handleException(e);
                } else if (e instanceof WorkException) {
                    handleException(new ComponentException(new Message(Messages.EVENT_PROCESSING_FAILED_FOR_X,
                            descriptor.getName()),
                            event.getMessage(),
                            this,
                            e));
                } else {
                    handleException(new ComponentException(new Message(Messages.FAILED_TO_GET_POOLED_OBJECT),
                            event.getMessage(),
                            this,
                            e));
                }
            } finally {
            	stopping.set(false);
            }
        }
    }

    public void release() {
        stopping.set(false);
    }

    protected void enqueue(UMOEvent event) throws Exception {
        QueueSession session = MuleManager.getInstance().getQueueManager().getQueueSession();
        session.getQueue(descriptorQueueName).put(event);
    }

    protected UMOEvent dequeue() throws Exception {
        // Wait until an event is available
        QueueSession queueSession = MuleManager.getInstance().getQueueManager().getQueueSession();
        return (UMOEvent)queueSession.getQueue(descriptorQueueName).poll(queueTimeout);
    }
}
