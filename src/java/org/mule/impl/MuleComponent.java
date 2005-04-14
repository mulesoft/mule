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
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.mule.MuleException;
import org.mule.MuleManager;
import org.mule.config.PoolingProfile;
import org.mule.config.QueueProfile;
import org.mule.config.ThreadingProfile;
import org.mule.config.i18n.Message;
import org.mule.config.i18n.Messages;
import org.mule.impl.internal.events.ComponentEvent;
import org.mule.management.stats.ComponentStatistics;
import org.mule.umo.*;
import org.mule.umo.lifecycle.InitialisationException;
import org.mule.umo.lifecycle.LifecycleException;
import org.mule.umo.manager.UMOWorkManager;
import org.mule.umo.provider.DispatchException;
import org.mule.umo.provider.UMOMessageDispatcher;
import org.mule.util.ObjectPool;
import org.mule.util.queue.BoundedPersistentQueue;

import javax.resource.spi.work.Work;
import javax.resource.spi.work.WorkException;
import javax.resource.spi.work.WorkManager;
import java.beans.ExceptionListener;
import java.util.ArrayList;
import java.util.List;
import java.util.NoSuchElementException;

/**
 * <code>MuleComponent</code> manages the interaction and distribution of
 * events for a Mule-managed component.
 *
 * @author <a href="mailto:ross.mason@symphonysoft.com">Ross Mason</a>
 * @version $Revision$
 */

public final class MuleComponent implements UMOComponent, Work
{
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

    private BoundedPersistentQueue queue;

    private ComponentStatistics stats = null;

    private UMOWorkManager workManager;

    /**
     * Determines if the component has been stopped
     */
    private SynchronizedBoolean stopped = new SynchronizedBoolean(true);

    /**
     * Determines whether stop has been called and is still in progress
     */
    private SynchronizedBoolean stopping = new SynchronizedBoolean(false);

    /**
     * Determines if the component has been paused
     */
    private SynchronizedBoolean paused = new SynchronizedBoolean(false);

    /**
     * determines if the proxy pool has been initialised
     */
    private SynchronizedBoolean poolInitialised = new SynchronizedBoolean(false);

    /**
     * The event queue profile
     */
    private QueueProfile qProfile;

    /**
     * The exception strategy used by the component, this is provided
     * by the UMODescriptor
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
    public MuleComponent(MuleDescriptor descriptor)
    {
        if (descriptor == null)
        {
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
    public synchronized void initialise() throws InitialisationException
    {
        if (initialised.get())
        {
            throw new InitialisationException(new Message(Messages.OBJECT_X_ALREADY_INITIALSIED, "Component '" + descriptor.getName() + "'"), this);
        }
        descriptor.initialise();

        this.exceptionListener = descriptor.getExceptionListener();

        //initialise statistics
        stats = new ComponentStatistics(getName(),
                descriptor.getPoolingProfile().getMaxActive(),
                descriptor.getThreadingProfile().getMaxThreadsActive());

        stats.setEnabled(((MuleManager) MuleManager.getInstance()).getStatistics().isEnabled());
        ((MuleManager) MuleManager.getInstance()).getStatistics().add(stats);
        stats.setOutboundRouterStat(getDescriptor().getOutboundRouter().getStatistics());
        stats.setInboundRouterStat(getDescriptor().getInboundRouter().getStatistics());

        //Create thread pool
        ThreadingProfile tp = descriptor.getThreadingProfile();
        workManager = tp.createWorkManager(descriptor.getName());
        try {
            workManager.start();
        } catch (UMOException e) {
            throw new InitialisationException(e, this);
        }
        try
        {
            //Setup event Queue (used for VM execution)
            queue = descriptor.getQueueProfile().createQueue(descriptor.getName());

            queue.setDeleteOnTake(false);
            qProfile = descriptor.getQueueProfile();

        } catch (InitialisationException e)
        {
            throw e;
        } catch (Throwable e)
        {
            throw new InitialisationException(new Message(Messages.X_FAILED_TO_INITIALISE, "Compoennt Queue"), e, this);
        }
        initialised.set(true);
        model.fireEvent(new ComponentEvent(descriptor, ComponentEvent.COMPONENT_INITIALISED));

    }

    private void initialisePool() throws InitialisationException
    {
        try
        {
            //Initialise the proxy pool
            proxyPool = descriptor.getPoolingProfile().getPoolFactory().createPool(descriptor);

            if (descriptor.getPoolingProfile().getInitialisationPolicy() == PoolingProfile.POOL_INITIALISE_ALL_COMPONENTS)
            {
                List components = new ArrayList();
                int threads = descriptor.getPoolingProfile().getMaxActive();
                for (int i = 0; i < threads; i++)
                {
                    components.add(proxyPool.borrowObject());
                }
                for (int i = 0; i < threads; i++)
                {
                    proxyPool.returnObject(components.remove(0));
                }
            } else if (descriptor.getPoolingProfile().getInitialisationPolicy() == PoolingProfile.POOL_INITIALISE_ONE_COMPONENT)
            {
                proxyPool.returnObject(proxyPool.borrowObject());
            }
            poolInitialised.set(true);
        } catch (Exception e)
        {
            throw new InitialisationException(new Message(Messages.X_FAILED_TO_INITIALISE, "Proxy Pool"), e, this);
        }
    }

    void finaliseEvent(UMOEvent event)
    {
        logger.debug("Finalising event for: " + descriptor.getName() + " event endpointUri is: " + event.getEndpoint().getEndpointURI());
        queue.remove(event);
    }

    public void stop() throws UMOException
    {
        if (!stopped.get())
        {
            logger.debug("Stopping UMOComponent");
            stopping.set(true);
//            try
//            {
//                proxyPool.stop();
//            } catch (Exception e)
//            {
//                throw new LifecycleException(new Message(Messages.FAILED_TO_STOP_X, "Component: " + descriptor.getName()), e, this);
//            }
//            if (worker != null)
//            {
//                try
//                {
//                    worker.interrupt();
//                    worker = null;
//                } catch (Exception e)
//                {
//                    logger.error("Component worker thread did not close properly: " + e);
//                }
//            }
            stopped.set(true);
            stopping.set(false);
            model.fireEvent(new ComponentEvent(descriptor, ComponentEvent.COMPONENT_STOPPED));
        }
    }


    public void start() throws UMOException
    {
        if (stopped.get())
        {
            stopped.set(false);
            try
            {
                //Need to initialise the pool only after all listerner have been
                //registed and initialised so we need to delay until now
                if (!poolInitialised.get())
                {
                    initialisePool();
                }
                proxyPool.start();
                workManager.scheduleWork(this, WorkManager.INDEFINITE, null, null);
            } catch (Exception e)
            {
                throw new LifecycleException(new Message(Messages.FAILED_TO_START_X, "Component: " + descriptor.getName()), e, this);
            }
        }
        model.fireEvent(new ComponentEvent(descriptor, ComponentEvent.COMPONENT_STARTED));
    }


    public void pause()
    {
        paused.set(true);
        model.fireEvent(new ComponentEvent(descriptor, ComponentEvent.COMPONENT_PAUSED));
    }

    public void resume()
    {
        paused.set(false);
        model.fireEvent(new ComponentEvent(descriptor, ComponentEvent.COMPONENT_RESUMED));
    }

    public void dispose()
    {
        try {
            if (!stopped.get()) stop();
        } catch (UMOException e) {
            logger.error("Failed to stop component: " + descriptor.getName(), e);
        }
        try
        {
            if (queue != null) queue.dispose();
        } catch (Exception e)
        {
            logger.error("Persistent Queue did not close properly: " + e);
        }
        try
        {
            //threadPool.awaitTerminationAfterShutdown();
            if (workManager != null) workManager.dispose();
        } catch (Exception e)
        {
            logger.error("Component Thread Pool did not close properly: " + e);
        }
        try
        {
            if (proxyPool != null) proxyPool.clearPool();
        } catch (Exception e)
        {
            logger.error("Proxy Pool did not close properly: " + e);
        }
        model.fireEvent(new ComponentEvent(descriptor, ComponentEvent.COMPONENT_DISPOSED));
        ((MuleManager) MuleManager.getInstance()).getStatistics().remove(stats);
    }

    public ComponentStatistics getStatistics()
    {
        return stats;
    }

    /*
	 * (non-Javadoc)
	 * @see org.mule.umo.UMOSession#getDescriptor()
	 */
    public UMODescriptor getDescriptor()
    {
        return descriptor;
    }

    public void dispatchEvent(UMOEvent event) throws UMOException
    {
        if(!event.getEndpoint().canReceive()) {
            UMOMessageDispatcher dispatcher = event.getEndpoint().getConnector().getDispatcher(event.getEndpoint().getEndpointURI().getAddress());
            try
            {
                dispatcher.dispatch(event);
            } catch (Exception e)
            {
                throw new DispatchException(event.getMessage(), event.getEndpoint(), e);
            }
            return;
        }
        if (stats.isEnabled())
            stats.incReceivedEventASync();

        logger.debug("Component: " + descriptor.getName() + " has received asynchronous event on: " + event.getEndpoint().getEndpointURI());

        if (queue.size() >= qProfile.getMaxOutstandingMessages())
        {
            //Block until we can queue the next event
            logger.trace("process maxQueueSize reached:" + qProfile.getMaxOutstandingMessages());
            while (queue.size() >= qProfile.getMaxOutstandingMessages())
            {

                synchronized (queue)
                {
                    try
                    {
                        Thread.yield();
                        queue.wait(qProfile.getBlockWait());
                    } catch (Exception ie)
                    {
                    }

                }
            }
        }
        try
        {
            queue.put(event);

            if (stats.isEnabled())
                stats.incQueuedEvent();

        } catch (InterruptedException e)
        {
            FailedToQueueEventException e1 = new FailedToQueueEventException(new Message(Messages.INTERRUPTED_QUEUING_EVENT_FOR_X, getName()), event.getMessage(), this, e);
            handleException(e1);
        }
        logger.trace("Event added to queue for: " + descriptor.getName());
    }

    public UMOMessage sendEvent(UMOEvent event) throws UMOException
    {
        while (paused.get())
        {
            if (logger.isDebugEnabled()) logger.debug("Component: " + descriptor.getName() + " is paused. Blocking call until resume is called");
            try
            {
                Thread.sleep(1000);
            } catch (InterruptedException e)
            {
                //ignore
            }
        }

        if (stats.isEnabled())
            stats.incReceivedEventSync();


        logger.debug("Component: " + descriptor.getName() + " has received synchronous event on: " + event.getEndpoint().getEndpointURI());
        UMOMessage result = null;
        MuleProxy proxy = null;
        try
        {
            proxy = (MuleProxy) proxyPool.borrowObject();
            getStatistics().setComponentPoolSize(proxyPool.getSize());
            proxy.setStatistics(getStatistics());

            if (logger.isDebugEnabled())
            {
                logger.debug(this + " : got proxy for " + event.getId()
                        + " = " + proxy);
            }
            result = (UMOMessage) proxy.onCall(event);
            proxyPool.returnObject(proxy);
        } catch (Exception e)
        {
            try
            {
                proxyPool.returnObject(proxy);
            } catch (Exception ignore)
            {
            }
            if (e instanceof UMOException)
            {
                throw (MuleException) e;
            } else
            {
                throw new ComponentException(event.getMessage(), this, e);
            }
        }
        return result;
    }

    /**
     * @return the Mule descriptor name which is associated with the component
     */
    public String getName()
    {
        return descriptor.getName();
    }


    /**
     * @return the pool of Mule UMOs initialised in this component
     */
    ObjectPool getProxyPool()
    {
        return proxyPool;
    }

    /*
	 * (non-Javadoc)
	 *
	 * @see java.lang.Object#toString()
	 */
    public String toString()
    {
        return descriptor.getName();
    }

    public int getQueueSize()
    {
        return queue.size();
    }

    public boolean isStopped()
    {
        return stopped.get();
    }

    public boolean isPaused()
    {
        return paused.get();
    }

    /**
     * While the component isn't stopped this runs a continuous loop checking for
     * new events in the queue
     */
    public void run()
    {
        MuleEvent event = null;
        MuleProxy proxy = null;

        while (!stopped.get() && !stopping.get())
        {
            if (!paused.get())
            {

                try
                {
                    event = (MuleEvent) queue.take();
                } catch (InterruptedException e)
                {
                    break;
                }
                if (stats.isEnabled())
                    stats.decQueuedEvent();

                if (event != null)
                {
                    logger.debug("Component: " + descriptor.getName() + " dequeued event on: " + event.getEndpoint().getEndpointURI());

                    try
                    {
                        proxy = (MuleProxy) proxyPool.borrowObject();
                        getStatistics().setComponentPoolSize(proxyPool.getSize());
                        proxy.setStatistics(getStatistics());
                    } catch (NoSuchElementException e)
                    {
                        handleException(new ComponentException(new Message(Messages.PROXY_POOL_TIMED_OUT), event.getMessage(), this, e));
                    } catch (UMOException e)
                    {
                        handleException(e);
                    } catch (Exception e)
                    {
                        handleException(new ComponentException(new Message(Messages.FAILED_TO_GET_POOLED_OBJECT), event.getMessage(), this, e));
                    }

                    if (proxy == null)
                    {
                        handleException(new ComponentException(new Message(Messages.FAILED_TO_GET_POOLED_OBJECT), event.getMessage(), this));
                    }

                    if (!proxy.isStarted())
                    {
                        try
                        {
                            proxy.start();
                        } catch (UMOException e)
                        {
                            handleException(e);
                        }
                    }
                    proxy.onEvent(event);
                    try
                    {
                        workManager.scheduleWork(proxy, WorkManager.INDEFINITE, null, null);
                    } catch (WorkException e)
                    {
                        handleException(new ComponentException(new Message(Messages.EVENT_PROCIESSING_FAILED_FOR_X, descriptor.getName()), event.getMessage(), this, e));
                    }
                }
            }
        }
    }

    public void release() {
    }

    protected void handleException(Exception e)
    {
        RequestContext.getEvent().getEndpoint().getConnector().getExceptionListener().exceptionThrown(e);
        if (exceptionListener instanceof DefaultComponentExceptionStrategy)
        {
            if (((DefaultComponentExceptionStrategy) exceptionListener).getComponent() == null)
            {
                ((DefaultComponentExceptionStrategy) exceptionListener).setComponent(this);
            }
        }
        exceptionListener.exceptionThrown(e);
    }
}
