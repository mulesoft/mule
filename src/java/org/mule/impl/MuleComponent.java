/*
 * $Header: /cvsroot/mule/mule/src/java/org/mule/umo/impl/MuleSession.java,v
 * 1.26 2004/01/14 09:34:02 rossmason Exp $ $Revision$ $Date: 2004/01/14
 * 09:34:02 $
 * ------------------------------------------------------------------------------------------------------
 *
 * Copyright (c) Cubis Limited. All rights reserved. http://www.cubis.co.uk
 *
 * The software in this package is published under the terms of the BSD style
 * license a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 *
 */
package org.mule.impl;

import EDU.oswego.cs.dl.util.concurrent.PooledExecutor;
import EDU.oswego.cs.dl.util.concurrent.SynchronizedBoolean;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.mule.InitialisationException;
import org.mule.MuleException;
import org.mule.MuleManager;
import org.mule.config.PoolingProfile;
import org.mule.config.QueueProfile;
import org.mule.config.ThreadingProfile;
import org.mule.impl.internal.events.ComponentEvent;
import org.mule.management.stats.ComponentStatistics;
import org.mule.umo.ComponentException;
import org.mule.umo.UMOComponent;
import org.mule.umo.UMODescriptor;
import org.mule.umo.UMOEvent;
import org.mule.umo.UMOException;
import org.mule.umo.UMOExceptionStrategy;
import org.mule.umo.UMOMessage;
import org.mule.umo.provider.DispatchException;
import org.mule.umo.provider.UMOMessageDispatcher;
import org.mule.util.ObjectPool;
import org.mule.util.queue.BoundedPersistentQueue;

import java.util.ArrayList;
import java.util.List;
import java.util.NoSuchElementException;

/**
 * <code>MuleComponent</code> manages the interaction and distribution of
 * events for a Mule-managed component.
 *
 * @author <a href="mailto:ross.mason@cubis.co.uk">Ross Mason</a>
 * @version $Revision$
 */

public final class MuleComponent implements UMOComponent
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
     * Thread pool of Mule Proxies used to process events
     */
    private PooledExecutor threadPool = null;

    /**
     * A pool of available Mule Proxies
     */
    private ObjectPool proxyPool = null;

    private BoundedPersistentQueue queue;

    private ComponentStatistics stats = null;

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
     * The worker thread in which to intercept the component
     */
    private Thread worker = null;

    /**
     * The event queue profile
     */
    private QueueProfile qProfile;

    /**
     * The exception strategy used by the component, this is provided
     * by the UMODescriptor
     */
    private UMOExceptionStrategy exceptionStrategy = null;

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
            throw new InitialisationException("Component: " + descriptor.getName() + " has already bean initialised");
        }
        descriptor.initialise();

        this.exceptionStrategy = descriptor.getExceptionStrategy();

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
        threadPool = tp.createPool(descriptor.getName());

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
            throw new InitialisationException("Failed to component queue: " + e.getMessage(), e);
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
            throw new InitialisationException("Mule proxy pool failed to initialise: " + e, e);
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
            try
            {
                proxyPool.stop();
            } catch (Exception e)
            {
                throw new ComponentException("Failed to start component", this, e);
            }
            if (worker != null)
            {
                try
                {
                    worker.interrupt();
                    worker = null;
                } catch (Exception e)
                {
                    logger.error("Component worker thread did not close properly: " + e);
                }
            }
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
                worker = new Thread(this, descriptor.getName() + ".component");
                worker.setPriority(Thread.NORM_PRIORITY);
                worker.start();
            } catch (Exception e)
            {
                throw new ComponentException("Failed to start proxy pool", this, e);
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

    public void dispose() throws UMOException
    {
        if (!stopped.get()) stop();
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
            if (threadPool != null) threadPool.shutdownNow();
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
                throw new DispatchException(e.getMessage(), e);
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
            FailedToQueueEventException e1 = new FailedToQueueEventException("Interrupted while queue event for: " + getName(), e);
            handleException(event, e1);
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
            if (e instanceof MuleException)
            {
                throw (MuleException) e;
            } else
            {
                throw new MuleException("Failed to send event through session: " + e, e);
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
                        handleException(event, new ComponentException("Proxy pool timed out. " + e, this, e));
                    } catch (Exception e)
                    {
                        handleException(event, new ComponentException("Failed to borrow object from pool: " + e.getMessage(), this, e));
                    }

                    if (proxy == null)
                    {
                        handleException(event, new ComponentException("No proxy was found", this));
                    }

                    if (!proxy.isStarted())
                    {
                        try
                        {
                            proxy.start();
                        } catch (UMOException e)
                        {
                            handleException(event, e);
                        }
                    }
                    proxy.onEvent(event);
                    try
                    {
                        threadPool.execute(proxy);
                    } catch (InterruptedException e)
                    {
                        handleException(event, e);
                    }
                }
            }
        }
    }

    protected void handleException(Object msg, Throwable t)
    {
        if (exceptionStrategy instanceof DefaultComponentExceptionStrategy)
        {
            if (((DefaultComponentExceptionStrategy) exceptionStrategy).getComponent() == null)
            {
                ((DefaultComponentExceptionStrategy) exceptionStrategy).setComponent(this);
            }
        }
        exceptionStrategy.handleException(msg, t);
    }
}
