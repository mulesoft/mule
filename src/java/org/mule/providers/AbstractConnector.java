/*
 * $Header: /cvsroot/mule/mule/src/java/org/mule/providers/AbstractConnector.java,v 1.16 2003/12/11
 * 13:32:40 rossmason Exp $ $Revision$ $Date$
 * ------------------------------------------------------------------------------------------------------
 * 
 * Copyright (c) SymphonySoft Limited. All rights reserved. http://www.symphonysoft.com
 * 
 * The software in this package is published under the terms of the BSD style license a copy of
 * which has been included with this distribution in the LICENSE.txt file.
 *  
 */

package org.mule.providers;

import EDU.oswego.cs.dl.util.concurrent.ConcurrentHashMap;
import EDU.oswego.cs.dl.util.concurrent.SynchronizedBoolean;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.mule.MuleManager;
import org.mule.MuleRuntimeException;
import org.mule.config.ThreadingProfile;
import org.mule.config.i18n.Message;
import org.mule.config.i18n.Messages;
import org.mule.impl.AlreadyInitialisedException;
import org.mule.impl.DefaultExceptionStrategy;
import org.mule.umo.UMOComponent;
import org.mule.umo.UMOException;
import org.mule.umo.endpoint.UMOEndpoint;
import org.mule.umo.endpoint.UMOEndpointURI;
import org.mule.umo.lifecycle.DisposeException;
import org.mule.umo.lifecycle.Initialisable;
import org.mule.umo.lifecycle.InitialisationException;
import org.mule.umo.provider.*;
import org.mule.umo.transformer.UMOTransformer;

import java.beans.ExceptionListener;
import java.util.Iterator;
import java.util.Map;

/**
 * <code>AbstractConnector</code> provides base functionality for all connectors provided with
 * Mule. Connectors are the mechanism used to connect to external systems and protocols in order to
 * send and receive data.
 * <p/>
 * The <code>AbstractConnector</code> provides getter and setter methods for endpoint
 * name, transport name and protocol. It also provides methods to stop and start
 * connecotors and sets up a dispatcher threadpool which allows deriving connectors the possibility
 * to dispatch work to separate threads. This functionality is controlled with the <i>
 * doThreading</i> property on the threadingProfiles for dispachers and receivers.
 *
 * @author <a href="mailto:ross.mason@symphonysoft.com">Ross Mason</a>
 * @version $Revision$
 */
public abstract class AbstractConnector implements UMOConnector, ExceptionListener
{
    /**
     * logger used by this class
     */
    protected transient Log logger = LogFactory.getLog(getClass());

    public static final long DEFAULT_RETRY_FREQUENCY = 1000;

    public static final int DEFAULT_RETRY_COUNT = 3;

    /**
     * Specifies if the endpoint started
     */
    protected SynchronizedBoolean started = new SynchronizedBoolean(false);

    /**
     * True once the endpoint has been initialsed
     */
    protected SynchronizedBoolean initialised = new SynchronizedBoolean(false);

    /**
     * The name that identifies the endpoint
     */
    protected String name = null;

    /**
     * The exception strategy used by this endpoint
     */
    protected ExceptionListener exceptionListener = null;

    /**
     * Determines in the endpoint is alive and well
     */
    protected SynchronizedBoolean disposed = new SynchronizedBoolean(false);

    /**
     * Factory used to create dispatchers for this connector
     */
    protected UMOMessageDispatcherFactory dispatcherFactory;

    /**
     * A pool of dispatchers for this connector, the pool is keyed on endpointUri
     */
    protected Map dispatchers;

    /**
     * The collection of listeners on this connector. Keyed by entrypoint
     */
    protected ConcurrentHashMap receivers;

    /**
     * Defines the dispatcher threading model
     */
    private ThreadingProfile dispatcherThreadingProfile = null;

    /**
     * Defines the receiver threading model
     */
    private ThreadingProfile receiverThreadingProfile = null;

    /**
     * How many time to retry a connection before throwing an
     * InitialisationException
     */
    private int retryCount = 2;

    /**
     * How many milliseconds to wait beween retries
     */
    private long retryFrequency = 2000;

    /**
     * Determines whether dispatchers should be disposed straight away
     * of deferred until the connector is disposing
     */
    private boolean disposeDispatcherOnCompletion = false;

    /**
     * The service descriptor can define a default inbound transformer to
     * be used on an endpoint if no other is set
     */
    protected UMOTransformer defaultInboundTransformer = null;

    /**
     * The service descriptor can define a default outbound transformer to
     * be used on an endpoint if no other is set
     */
    protected UMOTransformer defaultOutboundTransformer = null;

    /**
     * For some connectors such as http, a response transformer is required
     * or where a replyTo needs a trnasformer
     */
    protected UMOTransformer defaultResponseTransformer = null;

    public AbstractConnector()
    {
        //make sure we always have an exception strategy
        exceptionListener = new DefaultExceptionStrategy();
        dispatchers = new ConcurrentHashMap();
        receivers = new ConcurrentHashMap();
    }

    /*
	 * (non-Javadoc)
	 * 
	 * @see org.mule.providers.UMOConnector#getName()
	 */
    public String getName()
    {
        return name;
    }

    /*
	 * (non-Javadoc)
	 * 
	 * @see org.mule.providers.UMOConnector#setName(java.lang.String)
	 */
    public void setName(String newName)
    {
        if (newName == null)
        {
            throw new IllegalArgumentException("Connector name cannot be null");
        }
        if (logger.isDebugEnabled())
        {
            logger.debug("Set UMOConnector name to: " + newName);
        }
        name = newName;
    }

    /* (non-Javadoc)
     * @see org.mule.providers.UMOConnector#create(java.util.HashMap)
     */
    public final synchronized void initialise() throws InitialisationException
    {
        if (initialised.get())
        {
            throw new AlreadyInitialisedException("Connector '" + getName() + "'", this);
        }
        if (logger.isInfoEnabled())
            logger.info("Initialising " + getClass().getName());

        if(exceptionListener instanceof Initialisable) {
            ((Initialisable)exceptionListener).initialise();
        }

        doInitialise();
        initialised.set(true);
    }



    public abstract String getProtocol();

    public long getRetryFrequency()
    {
        return retryFrequency;
    }

    public void setRetryFrequency(long retryFrequency)
    {
        if (retryFrequency < 1) retryFrequency = DEFAULT_RETRY_FREQUENCY;
        this.retryFrequency = retryFrequency;
    }

    public int getRetryCount()
    {
        return retryCount;
    }

    public void setRetryCount(int retryCount)
    {
        if (retryCount < 0) retryCount = DEFAULT_RETRY_COUNT;
        this.retryCount = retryCount;
    }

    /*
	 * (non-Javadoc)
	 * 
	 * @see org.mule.umo.provider.UMOConnector#start()
	 */
    public final void start() throws UMOException
    {
        if (isDisposed())
        {
            throw new ConnectorException(new Message(Messages.CANT_START_DISPOSED_CONNECTOR), this);
        }
        if (!started.get())
        {
            if (logger.isInfoEnabled()) logger.info("Starting Connector: " + getClass().getName());
            startConnector();
            started.set(true);
            if (logger.isInfoEnabled()) logger.info("Connector: " + getClass().getName() + " has been started");
        }
    }

    /*
	 * (non-Javadoc)
	 * 
	 * @see org.mule.umo.provider.UMOConnector#isStarted()
	 */
    public boolean isStarted()
    {
        return started.get();
    }

    /*
	 * (non-Javadoc)
	 * 
	 * @see org.mule.umo.provider.UMOConnector#stop()
	 */
    public final void stop() throws UMOException
    {
        if (isDisposed())
        {
            return; //throw new MuleException("Cannot stop a connector once it has been disposing");
        }
        if (started.get())
        {
            if (logger.isInfoEnabled()) logger.info("Stopping Connector: " + getClass().getName());
            stopConnector();
            started.set(false);
            if (logger.isInfoEnabled()) logger.info("Connector " + getClass().getName() + " has been stopped");
        }
    }

    /*
	 * (non-Javadoc)
	 * @see org.mule.umo.provider.UMOConnector#shutdown()
	 */
    public final synchronized void dispose()
    {
        disposed.set(true);
        if (logger.isInfoEnabled())
        {
            logger.info("Disposing Connector: " + getClass().getName());
            logger.debug("Disposing Receivers");
        }
        if (receivers != null)
        {
            Map.Entry entry;
            for (Iterator iterator = receivers.entrySet().iterator(); iterator.hasNext();)
            {
                entry = (Map.Entry) iterator.next();
                try
                {
                    destroyReceiver(((UMOMessageReceiver) entry.getValue()), null);
                } catch (Exception e)
                {
                    logger.error("Failed to destroy receiver: " + e.getMessage(), e);
                }
                receivers.remove(entry.getKey());

            }
            logger.debug("Receivers Disposed");
        }

        if (dispatchers != null)
        {
            //Map.Entry entry;
            logger.debug("Disposing Dispatchers");
            for (Iterator iterator = dispatchers.values().iterator(); iterator.hasNext();)
            {
                UMOMessageDispatcher umoMessageDispatcher = (UMOMessageDispatcher) iterator.next();
                umoMessageDispatcher.dispose();
            }
            dispatchers.clear();
            logger.debug("Dispatchers Disposed");
        }
        disposeConnector();

        if (logger.isInfoEnabled()) logger.info("Connector " + getClass().getName() + " has been disposed.");

        receivers = null;
        dispatchers = null;
    }

    /*
	 * (non-Javadoc)
	 * @see org.mule.umo.provider.UMOConnector#isAlive()
	 */
    public boolean isDisposed()
    {
        return disposed.get();
    }

    /*
	 * (non-Javadoc)
	 * @see org.mule.umo.provider.UMOConnector#handleException(java.lang.Object,
	 *      java.lang.Throwable)
	 */
    public void handleException(Exception exception)
    {
        if (exceptionListener == null)
        {
            throw new MuleRuntimeException(new Message(Messages.EXCEPTION_ON_CONNECTOR_X_NO_EXCEPTION_LISTENER, getName()), exception);
        } else
        {
            exceptionListener.exceptionThrown(exception);
        }
    }

    /*
	 * (non-Javadoc)
	 * @see org.mule.util.ExceptionListener#onException(java.lang.Throwable)
	 */
    public void exceptionThrown(Exception e)
    {
        handleException(e);
    }

    /**
     * @return the ExceptionStrategy for this endpoint
     * @see ExceptionListener
     */
    public ExceptionListener getExceptionListener()
    {
        return exceptionListener;
    }

    /**
     * @param listener the ExceptionStrategy to use with this endpoint
     * @see ExceptionListener
     */
    public void setExceptionListener(ExceptionListener listener)
    {
        exceptionListener = listener;
    }

    /**
     * @return Returns the dispatcherFactory.
     */
    public UMOMessageDispatcherFactory getDispatcherFactory()
    {
        return dispatcherFactory;
    }

    /**
     * @param dispatcerFactory The dispatcherFactory to set.
     */
    public void setDispatcherFactory(UMOMessageDispatcherFactory dispatcerFactory)
    {
        this.dispatcherFactory = dispatcerFactory;
    }

    public synchronized UMOMessageDispatcher getDispatcher(String endpoint) throws UMOException
    {
        checkDisposed();
        UMOMessageDispatcher dispatcher = null;
        if(endpoint==null || "".equals(endpoint)) endpoint= "ANY";
        if ("ANY".equals(endpoint) && dispatchers.size() > 0)
        {
            Map.Entry entry;
            for (Iterator iterator = dispatchers.entrySet().iterator(); iterator.hasNext();)
            {
                entry = (Map.Entry)iterator.next();
                if(((UMOMessageDispatcher)entry.getValue()).isDisposed()) {
                    dispatchers.remove(entry.getKey());
                } else {
                    dispatcher = (UMOMessageDispatcher)entry.getValue();
                    break;
                }
            }
        } else
        {
            if(dispatchers==null) {
                throw new NullPointerException("Dispatchers are null for connector: " + name);
            }
            dispatcher = (UMOMessageDispatcher) dispatchers.get(endpoint);
            if(dispatcher!=null && dispatcher.isDisposed()) {
                dispatchers.values().remove(dispatcher);
                dispatcher = null;
            }
        }

        if (dispatcher == null)
        {
            dispatcher = createDispatcher();
            dispatchers.put(endpoint, dispatcher);
        }
        return dispatcher;
    }

    protected void checkDisposed() throws DisposeException
    {
        if(isDisposed()) throw new DisposeException(new Message(Messages.CANT_START_DISPOSED_CONNECTOR), this);
    }

    protected UMOMessageDispatcher createDispatcher() throws UMOException
    {
        if(dispatcherFactory==null) {
            throw new ConnectorException(new Message(Messages.CONNECTOR_NOT_STARTED, name), this);
        }
        UMOMessageDispatcher dispatcher = dispatcherFactory.create(this);
        return dispatcher;
    }

    public UMOMessageReceiver registerListener(UMOComponent component, UMOEndpoint endpoint) throws Exception
    {
        if (endpoint == null || component == null)
            throw new IllegalArgumentException("The endpoint and component cannot be null when registering a listener");

        UMOEndpointURI endpointUri = endpoint.getEndpointURI();
        if (endpointUri == null)
        {
            throw new ConnectorException(new Message(Messages.ENDPOINT_NULL_FOR_LISTENER), this);
        }
        logger.info("registering listener: " + component.getDescriptor().getName() + " on endpointUri: " + endpointUri.toString());

        UMOMessageReceiver receiver = (UMOMessageReceiver) receivers.get(getReceiverKey(component, endpoint));
        if (receiver != null)
        {
            throw new ConnectorException(new Message(Messages.LISTENER_ALREADY_REGISTERED, endpointUri), this);
        } else
        {
            receiver = createReceiver(component, endpoint);
            receivers.put(getReceiverKey(component, endpoint), receiver);
        }
        return receiver;
    }

    protected Object getReceiverKey(UMOComponent component, UMOEndpoint endpoint)
    {
        if(endpoint.getEndpointURI().getFilterAddress()!=null) {
            return endpoint.getEndpointURI().getFilterAddress();
        } else {
            return endpoint.getEndpointURI().getAddress();
        }
    }

    public final void unregisterListener(UMOComponent component, UMOEndpoint endpoint) throws Exception
    {
        if (endpoint == null || component == null || endpoint.getEndpointURI() == null)
            throw new IllegalArgumentException("The endpoint and component and endpointUri cannot be null when you unregister a listener");

        UMOEndpointURI endpointUri = endpoint.getEndpointURI();
        if (logger.isInfoEnabled())
            logger.info("removing listener on endpointUri: " + endpointUri);

        UMOMessageReceiver receiver = (UMOMessageReceiver) receivers.remove(getReceiverKey(component, endpoint));
        if (receiver != null)
        {
            destroyReceiver(receiver, endpoint);
            receiver.dispose();
        }
    }

    public ThreadingProfile getDispatcherThreadingProfile()
    {
        if (dispatcherThreadingProfile == null)
        {
            dispatcherThreadingProfile = MuleManager.getConfiguration().getMessageReceiverThreadingProfile();

        }
        return dispatcherThreadingProfile;
    }

    public void setDispatcherThreadingProfile(ThreadingProfile dispatcherThreadingProfile)
    {
        this.dispatcherThreadingProfile = dispatcherThreadingProfile;
    }

    public ThreadingProfile getReceiverThreadingProfile()
    {
        if (receiverThreadingProfile == null)
        {
            receiverThreadingProfile = MuleManager.getConfiguration().getMessageReceiverThreadingProfile();
        }
        return receiverThreadingProfile;
    }

    public void setReceiverThreadingProfile(ThreadingProfile receiverThreadingProfile)
    {
        this.receiverThreadingProfile = receiverThreadingProfile;
    }


    public abstract UMOMessageReceiver createReceiver(UMOComponent component, UMOEndpoint endpoint) throws Exception;

    public void destroyReceiver(UMOMessageReceiver receiver, UMOEndpoint endpoint) throws Exception
    {
        receiver.dispose();
    }

    /**
     * Template method to perform any work when starting the connectoe
     *
     * @throws UMOException if the method fails
     */
    protected void startConnector() throws UMOException
    {
    }

    /**
     * Template method to perform any work when stopping the connectoe
     *
     * @throws UMOException if the method fails
     */
    protected void stopConnector() throws UMOException
    {
    }

    public boolean isDisposeDispatcherOnCompletion()
    {
        return disposeDispatcherOnCompletion;
    }

    public void setDisposeDispatcherOnCompletion(boolean disposeDispatcherOnCompletion)
    {
        this.disposeDispatcherOnCompletion = disposeDispatcherOnCompletion;
    }

    /**
     * Template method to perform any work when destroying the connectoe
     *
     */
    protected void disposeConnector()
    {
        try {
            stopConnector();
        } catch (UMOException e) {
            logger.warn("Fialed to stop during shutdown: " + e.getMessage(), e);
        }
    }

    public void doInitialise() throws InitialisationException
    {
    }

    public UMOTransformer getDefaultInboundTransformer()
    {
        if (defaultInboundTransformer != null)
        {
            try
            {
                return (UMOTransformer) defaultInboundTransformer.clone();
            } catch (CloneNotSupportedException e)
            {
                logger.error("Failed to clone default Inbound transformer");
                return null;
            }
        } else
        {
            return null;
        }
    }

    public void setDefaultInboundTransformer(UMOTransformer defaultInboundTransformer)
    {
        this.defaultInboundTransformer = defaultInboundTransformer;
    }


    public UMOTransformer getDefaultResponseTransformer()
    {
        if (defaultResponseTransformer != null)
        {
            try
            {
                return (UMOTransformer) defaultResponseTransformer.clone();
            } catch (CloneNotSupportedException e)
            {
                logger.error("Failed to clone default Outbound transformer");
                return null;
            }
        } else
        {
            return null;
        }
    }

    public UMOTransformer getDefaultOutboundTransformer()
    {
        if (defaultOutboundTransformer != null)
        {
            try
            {
                return (UMOTransformer) defaultOutboundTransformer.clone();
            } catch (CloneNotSupportedException e)
            {
                logger.error("Failed to clone default Outbound transformer");
                return null;
            }
        } else
        {
            return null;
        }
    }

    public void setDefaultOutboundTransformer(UMOTransformer defaultOutboundTransformer)
    {
        this.defaultOutboundTransformer = defaultOutboundTransformer;
    }

    public void setDefaultResponseTransformer(UMOTransformer defaultResponseTransformer)
    {
        this.defaultResponseTransformer = defaultResponseTransformer;
    }

    public ReplyToHandler getReplyToHandler()
    {
        return new DefaultReplyToHandler(defaultResponseTransformer);
    }

    public Map getDispatchers()
    {
        return dispatchers;
    }
}
