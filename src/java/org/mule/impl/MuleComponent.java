/*
 * $Header: /cvsroot/mule/mule/src/java/org/mule/umo/impl/MuleSession.java,v
 * 1.26 2004/01/14 09:34:02 rossmason Exp $ $Revision$ $Date: 2004/01/14
 * 09:34:02 $
 * ------------------------------------------------------------------------------------------------------
 *
 * Copyright (c) SymphonySoft Limited. All rights reserved. http://www.symphonysoft.com
 *
 * The software in this package is published under the terms of the BSD style
 * license a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 *
 */
package org.mule.impl;

import EDU.oswego.cs.dl.util.concurrent.SynchronizedBoolean;
import EDU.oswego.cs.dl.util.concurrent.WaitableBoolean;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.mule.MuleManager;
import org.mule.config.PoolingProfile;
import org.mule.config.QueueProfile;
import org.mule.config.ThreadingProfile;
import org.mule.config.i18n.Message;
import org.mule.config.i18n.Messages;
import org.mule.impl.internal.events.ComponentEvent;
import org.mule.management.stats.ComponentStatistics;
import org.mule.umo.ComponentException;
import org.mule.umo.UMOComponent;
import org.mule.umo.UMODescriptor;
import org.mule.umo.UMOEvent;
import org.mule.umo.UMOException;
import org.mule.umo.UMOMessage;
import org.mule.umo.lifecycle.InitialisationException;
import org.mule.umo.lifecycle.LifecycleException;
import org.mule.umo.manager.UMOWorkManager;
import org.mule.umo.provider.DispatchException;
import org.mule.umo.provider.UMOMessageDispatcher;
import org.mule.util.ObjectPool;
import org.mule.util.queue.QueueSession;

import javax.resource.spi.work.Work;
import javax.resource.spi.work.WorkException;
import javax.resource.spi.work.WorkManager;
import java.beans.ExceptionListener;
import java.util.NoSuchElementException;

/**
 * <code>MuleComponent</code> manages the interaction and distribution of
 * events for a Mule-managed component.
 *
 * @author <a href="mailto:ross.mason@symphonysoft.com">Ross Mason</a>
 * @version $Revision$
 */
public final class MuleComponent implements UMOComponent, Work {
    /**
     * logger used by this class
     */
    private static transient Log logger = LogFactory.getLog(MuleComponent.class);

    /**
     * The Mule descriptor associated with the component
     */
    private MuleDescriptor descriptor = null;

    /**
     * A pool of available Mule Proxies
     */
    private ObjectPool proxyPool = null;

    private ComponentStatistics stats = null;

    private UMOWorkManager workManager;

    /**
     * Determines if the component has been stopped
     */
    private SynchronizedBoolean stopped = new SynchronizedBoolean(true);

    /**
     * Determines whether stop has been called and is still in progress
     */
    private WaitableBoolean stopping = new WaitableBoolean(false);

    /**
     * Determines if the component has been paused
     */
    private WaitableBoolean paused = new WaitableBoolean(false);

    /**
     * determines if the proxy pool has been initialised
     */
    private SynchronizedBoolean poolInitialised = new SynchronizedBoolean(false);

    /**
     * The event queue profile
     */
    private QueueProfile qProfile;

    /**
     * The exception strategy used by the component, this is provided by the
     * UMODescriptor
     */
    private ExceptionListener exceptionListener = null;

    /**
     * Determines if the component has been initilised
     */
    private SynchronizedBoolean initialised = new SynchronizedBoolean(false);

    private MuleModel model;

    /**
     * Default constructor
     */
    public MuleComponent(MuleDescriptor descriptor) {
        if (descriptor == null) {
            throw new IllegalArgumentException("Descriptor cannot be null");
        }
        this.descriptor = descriptor;
        this.model = (MuleModel) MuleManager.getInstance().getModel();
    }

    /**
     * Initialise the component. The component will first create a Mule UMO from
     * the UMODescriptor and then initialise a pool based on the attributes in
     * the UMODescriptor.
     *
     * @throws InitialisationException if the component fails to initialise
     * @see UMODescriptor
     */
    public synchronized void initialise() throws InitialisationException {
        if (initialised.get()) {
            throw new InitialisationException(new Message(Messages.OBJECT_X_ALREADY_INITIALISED, "Component '"
                    + descriptor.getName() + "'"), this);
        }
        descriptor.initialise();

        this.exceptionListener = descriptor.getExceptionListener();

        // initialise statistics
        stats = new ComponentStatistics(getName(),
                descriptor.getPoolingProfile().getMaxActive(),
                descriptor.getThreadingProfile().getMaxThreadsActive());

        stats.setEnabled(((MuleManager) MuleManager.getInstance()).getStatistics().isEnabled());
        ((MuleManager) MuleManager.getInstance()).getStatistics().add(stats);
        stats.setOutboundRouterStat(getDescriptor().getOutboundRouter().getStatistics());
        stats.setInboundRouterStat(getDescriptor().getInboundRouter().getStatistics());

        // Create thread pool
        ThreadingProfile tp = descriptor.getThreadingProfile();
        workManager = tp.createWorkManager(descriptor.getName());
        try {
            // Setup event Queue (used for VM execution)
            descriptor.getQueueProfile().configureQueue(descriptor.getName());
            qProfile = descriptor.getQueueProfile();

        } catch (InitialisationException e) {
            throw e;
        } catch (Throwable e) {
            throw new InitialisationException(new Message(Messages.X_FAILED_TO_INITIALISE, "Compoennt Queue"), e, this);
        }
        initialised.set(true);
        model.fireEvent(new ComponentEvent(descriptor, ComponentEvent.COMPONENT_INITIALISED));

    }

    private void initialisePool() throws InitialisationException {
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

    void finaliseEvent(UMOEvent event) {
        logger.debug("Finalising event for: " + descriptor.getName() + " event endpointUri is: "
                + event.getEndpoint().getEndpointURI());
        // queue.remove(event);
    }

    public void forceStop() throws UMOException {
        if (!stopped.get()) {
            logger.debug("Stopping UMOComponent");
            stopping.set(true);
            workManager.stop();
            stopped.set(true);
            stopping.set(false);
            model.fireEvent(new ComponentEvent(descriptor, ComponentEvent.COMPONENT_STOPPED));
        }
    }

    public void stop() throws UMOException {
        if (!stopped.get()) {
            logger.debug("Stopping UMOComponent");
            stopping.set(true);
            if (MuleManager.getInstance().getQueueManager().getQueueSession().getQueue(descriptor.getName() + ".component").size() > 0) {
                try {

                    stopping.whenFalse(null);
                } catch (InterruptedException e) {
                    //we can ignore this
                }
            }

            workManager.stop();
            stopped.set(true);
            model.fireEvent(new ComponentEvent(descriptor, ComponentEvent.COMPONENT_STOPPED));
        }
    }

    public void start() throws UMOException {
        if (stopped.get()) {
            stopped.set(false);
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
        model.fireEvent(new ComponentEvent(descriptor, ComponentEvent.COMPONENT_STARTED));
    }

    public void pause() {
        paused.set(true);
        model.fireEvent(new ComponentEvent(descriptor, ComponentEvent.COMPONENT_PAUSED));
    }

    public void resume() {
        paused.set(false);
        model.fireEvent(new ComponentEvent(descriptor, ComponentEvent.COMPONENT_RESUMED));
    }

    public void dispose() {
        try {
            if (!stopped.get()) {
                stop();
            }
        } catch (UMOException e) {
            logger.error("Failed to stop component: " + descriptor.getName(), e);
        }
        /*
         * try { if (queue != null) queue.dispose(); } catch (Exception e) {
         * logger.error("Persistent Queue did not close properly: " + e); }
         */
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
        model.fireEvent(new ComponentEvent(descriptor, ComponentEvent.COMPONENT_DISPOSED));
        ((MuleManager) MuleManager.getInstance()).getStatistics().remove(stats);
    }

    public ComponentStatistics getStatistics() {
        return stats;
    }

    /*
     * (non-Javadoc)
     * 
     * @see org.mule.umo.UMOSession#getDescriptor()
     */
    public UMODescriptor getDescriptor() {
        return descriptor;
    }

    public void dispatchEvent(UMOEvent event) throws UMOException {
        if (stopping.get() || stopped.get()) {
            throw new ComponentException(new Message(Messages.COMPONENT_X_IS_STOPPED, getDescriptor().getName()), event.getMessage(), this);
        }
        // Dispatching event to an inbound endpoint
        // in the MuleSession#dispatchEvent
        if (!event.getEndpoint().canReceive()) {
            UMOMessageDispatcher dispatcher = event.getEndpoint().getConnector().getDispatcher(event.getEndpoint()
                    .getEndpointURI()
                    .getAddress());
            try {
                dispatcher.dispatch(event);
            } catch (Exception e) {
                throw new DispatchException(event.getMessage(), event.getEndpoint(), e);
            }
            return;
        }

        // Dispatching event to the component
        if (stats.isEnabled()) {
            stats.incReceivedEventASync();
        }
        if (logger.isDebugEnabled()) {
            logger.debug("Component: " + descriptor.getName() + " has received asynchronous event on: "
                    + event.getEndpoint().getEndpointURI());
        }

        /*
         * if (logger.isTraceEnabled()) { if (queue.size() >=
         * qProfile.getMaxOutstandingMessages()) { logger.trace("process
         * maxQueueSize reached:" + qProfile.getMaxOutstandingMessages()); } }
         */
        // Block until we can queue the next event
        try {
            QueueSession session = MuleManager.getInstance().getQueueManager().getQueueSession();
            session.getQueue(descriptor.getName() + ".component").put(event);
            if (stats.isEnabled()) {
                stats.incQueuedEvent();
            }
        } catch (InterruptedException e) {
            FailedToQueueEventException e1 = new FailedToQueueEventException(new Message(Messages.INTERRUPTED_QUEUING_EVENT_FOR_X,
                    getName()),
                    event.getMessage(),
                    this,
                    e);
            handleException(e1);
        }
        logger.trace("Event added to queue for: " + descriptor.getName());
    }

    public UMOMessage sendEvent(UMOEvent event) throws UMOException {
        if (stopping.get() || stopped.get()) {
            throw new ComponentException(new Message(Messages.COMPONENT_X_IS_STOPPED, getDescriptor().getName()), event.getMessage(), this);
        }
        if (logger.isDebugEnabled() && paused.get()) {
            logger.debug("Component: " + descriptor.getName() + " is paused. Blocking call until resume is called");
        }
        try {
            paused.whenFalse(null);
        } catch (InterruptedException e) {
            throw new ComponentException(event.getMessage(), this, e);
        }

        if (stats.isEnabled()) {
            stats.incReceivedEventSync();
        }
        if (logger.isDebugEnabled()) {
            logger.debug("Component: " + descriptor.getName() + " has received synchronous event on: "
                    + event.getEndpoint().getEndpointURI());
        }
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
     * @return the Mule descriptor name which is associated with the component
     */
    public String getName() {
        return descriptor.getName();
    }

    /**
     * @return the pool of Mule UMOs initialised in this component
     */
    ObjectPool getProxyPool() {
        return proxyPool;
    }

    /*
     * (non-Javadoc)
     * 
     * @see java.lang.Object#toString()
     */
    public String toString() {
        return descriptor.getName();
    }

    public int getQueueSize() {
        QueueSession queueSession = MuleManager.getInstance().getQueueManager().getQueueSession();
        return queueSession.getQueue(descriptor.getName()).size();
    }

    public boolean isStopped() {
        return stopped.get();
    }

    public boolean isStopping() {
        return stopping.get();
    }

    public boolean isPaused() {
        return paused.get();
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
                // Wait until an event is available
                queueSession = MuleManager.getInstance().getQueueManager().getQueueSession();
                //If we're doing a draining stop, read all events from the queue
                //before stopping
                if (stopping.get()) {
                    if (queueSession.getQueue(descriptor.getName() + ".component").size() == 0) {
                        stopping.set(false);
                        break;
                    }
                }
                event = (MuleEvent) queueSession.getQueue(descriptor.getName() + ".component").take();

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
            } catch (Exception e) {
                if (proxy != null) {
                    try {
                        proxyPool.returnObject(proxy);
                    } catch (Exception e2) {
                        logger.info("Failed to return proxy to pool", e2);
                    }
                }
                /*
                 * if (queueSession != null) { try { queueSession.rollback(); }
                 * catch (Exception e2) { logger.info("Error rolling back queue
                 * session", e2); } }
                 */
                if (e instanceof InterruptedException) {
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
            }
        }
    }

    public void release() {
    }

    protected void handleException(Exception e) {
        RequestContext.getEvent().getEndpoint().getConnector().getExceptionListener().exceptionThrown(e);
        if (exceptionListener instanceof DefaultComponentExceptionStrategy) {
            if (((DefaultComponentExceptionStrategy) exceptionListener).getComponent() == null) {
                ((DefaultComponentExceptionStrategy) exceptionListener).setComponent(this);
            }
        }
        exceptionListener.exceptionThrown(e);
    }
}
