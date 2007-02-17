/*
 * $Id$
 * --------------------------------------------------------------------------------------
 * Copyright (c) MuleSource, Inc.  All rights reserved.  http://www.mulesource.com
 *
 * The software in this package is published under the terms of the MuleSource MPL
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.providers;

import org.mule.MuleManager;
import org.mule.MuleRuntimeException;
import org.mule.config.ThreadingProfile;
import org.mule.config.i18n.Message;
import org.mule.config.i18n.Messages;
import org.mule.impl.AlreadyInitialisedException;
import org.mule.impl.DefaultExceptionStrategy;
import org.mule.impl.ImmutableMuleEndpoint;
import org.mule.impl.MuleSessionHandler;
import org.mule.impl.internal.notifications.ConnectionNotification;
import org.mule.providers.service.TransportFactory;
import org.mule.providers.service.TransportServiceDescriptor;
import org.mule.providers.service.TransportServiceException;
import org.mule.registry.RegistryException;
import org.mule.registry.ServiceDescriptorFactory;
import org.mule.registry.ServiceException;
import org.mule.routing.filters.WildcardFilter;
import org.mule.umo.MessagingException;
import org.mule.umo.UMOComponent;
import org.mule.umo.UMOEvent;
import org.mule.umo.UMOException;
import org.mule.umo.UMOMessage;
import org.mule.umo.endpoint.UMOEndpoint;
import org.mule.umo.endpoint.UMOEndpointURI;
import org.mule.umo.endpoint.UMOImmutableEndpoint;
import org.mule.umo.lifecycle.DisposeException;
import org.mule.umo.lifecycle.Initialisable;
import org.mule.umo.lifecycle.InitialisationException;
import org.mule.umo.manager.UMOServerNotification;
import org.mule.umo.manager.UMOWorkManager;
import org.mule.umo.provider.ConnectorException;
import org.mule.umo.provider.DispatchException;
import org.mule.umo.provider.UMOConnectable;
import org.mule.umo.provider.UMOConnector;
import org.mule.umo.provider.UMOMessageAdapter;
import org.mule.umo.provider.UMOMessageDispatcher;
import org.mule.umo.provider.UMOMessageDispatcherFactory;
import org.mule.umo.provider.UMOMessageReceiver;
import org.mule.umo.provider.UMOSessionHandler;
import org.mule.umo.provider.UMOStreamMessageAdapter;
import org.mule.umo.transformer.UMOTransformer;
import org.mule.util.ObjectNameHelper;
import org.mule.util.PropertiesUtils;
import org.mule.util.concurrent.NamedThreadFactory;
import org.mule.util.concurrent.WaitableBoolean;

import edu.emory.mathcs.backport.java.util.concurrent.ConcurrentHashMap;
import edu.emory.mathcs.backport.java.util.concurrent.ConcurrentMap;
import edu.emory.mathcs.backport.java.util.concurrent.ScheduledExecutorService;
import edu.emory.mathcs.backport.java.util.concurrent.ScheduledThreadPoolExecutor;
import edu.emory.mathcs.backport.java.util.concurrent.ThreadFactory;
import edu.emory.mathcs.backport.java.util.concurrent.TimeUnit;
import edu.emory.mathcs.backport.java.util.concurrent.atomic.AtomicBoolean;

import java.beans.ExceptionListener;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Properties;

import javax.resource.spi.work.WorkEvent;
import javax.resource.spi.work.WorkListener;

import org.apache.commons.beanutils.BeanUtils;
import org.apache.commons.lang.StringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.commons.pool.KeyedPoolableObjectFactory;
import org.apache.commons.pool.impl.GenericKeyedObjectPool;

/**
 * <code>AbstractConnector</code> provides base functionality for all connectors
 * provided with Mule. Connectors are the mechanism used to connect to external
 * systems and protocols in order to send and receive data. <p/> The
 * <code>AbstractConnector</code> provides getter and setter methods for endpoint
 * name, transport name and protocol. It also provides methods to stop and start
 * connecotors and sets up a dispatcher threadpool which allows deriving connectors
 * the possibility to dispatch work to separate threads. This functionality is
 * controlled with the <i> doThreading</i> property on the threadingProfiles for
 * dispachers and receivers. The lifecycle for a connector is -
 * <ol>
 * <li>Create
 * <li>Initialise
 * <li>Connect
 * <li>Connect receivers
 * <li>Start
 * <li>Start Receivers
 * <li>Stop
 * <li>Stop Receivers
 * <li>Disconnect
 * <li>Disconnect Receivers
 * <li>Dispose
 * <li>Dispose Receivers
 * </ol>
 */
public abstract class AbstractConnector
    implements UMOConnector, ExceptionListener, UMOConnectable, WorkListener
{
    /**
     * logger used by this class
     */
    protected transient Log logger = LogFactory.getLog(getClass());

    /**
     * Specifies if the endpoint started
     */
    protected AtomicBoolean started = new AtomicBoolean(false);

    /**
     * True once the endpoint has been initialsed
     */
    protected AtomicBoolean initialised = new AtomicBoolean(false);

    /**
     * The name that identifies the endpoint
     */
    protected String name = null;

    /**
     * The exception strategy used by this connector
     */
    protected ExceptionListener exceptionListener = null;

    /**
     * Determines in the connector is alive and well
     */
    protected AtomicBoolean disposed = new AtomicBoolean(false);

    /**
     * Determines in connector has been told to dispose
     */
    protected AtomicBoolean disposing = new AtomicBoolean(false);

    /**
     * Factory used to create dispatchers for this connector
     */
    protected UMOMessageDispatcherFactory dispatcherFactory;

    /**
     * A pool of dispatchers for this connector, the pool is keyed on endpointUri
     */
    protected GenericKeyedObjectPool dispatchers;

    /**
     * The collection of listeners on this connector. Keyed by entrypoint
     */
    protected ConcurrentMap receivers;

    /**
     * Defines the dispatcher threading model
     */
    private ThreadingProfile dispatcherThreadingProfile = null;

    /**
     * Defines the receiver threading model
     */
    private ThreadingProfile receiverThreadingProfile = null;

    /**
     * @see {@link #isCreateMultipleTransactedReceivers()}
     */
    protected boolean createMultipleTransactedReceivers = true;

    /**
     * @see {@link #getNumberOfConcurrentTransactedReceivers()}
     */
    protected int numberOfConcurrentTransactedReceivers = 4;

    /**
     * The service descriptor can define a default inbound transformer to be used on
     * an endpoint if no other is set
     */
    protected UMOTransformer defaultInboundTransformer = null;

    /**
     * The service descriptor can define a default outbound transformer to be used on
     * an endpoint if no other is set
     */
    protected UMOTransformer defaultOutboundTransformer = null;

    /**
     * For some connectors such as http, a response transformer is required or where
     * a replyTo needs a trnasformer
     */
    protected UMOTransformer defaultResponseTransformer = null;

    private ConnectionStrategy connectionStrategy;

    protected WaitableBoolean connected = new WaitableBoolean(false);

    protected WaitableBoolean connecting = new WaitableBoolean(false);

    /**
     * If the connect method was called via the start method, this will be set so
     * that when the connector comes on line it will be started
     */
    protected WaitableBoolean startOnConnect = new WaitableBoolean(false);

    /**
     * Whether to fire message notifications for every message that is sent or
     * received from this connector
     */
    private boolean enableMessageEvents = false;

    private final List supportedProtocols;

    /**
     * A shared work manager for all receivers registered with this connector.
     */
    private UMOWorkManager receiverWorkManager = null;

    /**
     * A shared work manager for all dispatchers created for this connector.
     */
    private UMOWorkManager dispatcherWorkManager = null;

    /**
     * A generic scheduling service for tasks that need to be performed periodically.
     */
    private ScheduledExecutorService scheduler = null;

    /**
     * Holds the service configuration for this connector
     */
    protected TransportServiceDescriptor serviceDescriptor;

    /**
     * The map of service overrides that can e used to extend the capabilities of the
     * connector
     */
    protected Properties serviceOverrides;

    /**
     * The strategy used for reading and writing session information to and fromt he
     * transport
     */
    protected UMOSessionHandler sessionHandler = new MuleSessionHandler();

    
    public AbstractConnector()
    {
        super();

        // make sure we always have an exception strategy
        exceptionListener = new DefaultExceptionStrategy();
        connectionStrategy = MuleManager.getConfiguration().getDefaultConnectionStrategy();
        //Todo RM*
        //enableMessageEvents = MuleManager.getConfiguration().isEnableMessageEvents();

        // always add at least the default protocol
        supportedProtocols = new ArrayList();
        supportedProtocols.add(getProtocol().toLowerCase());

        // container for dispatchers
        dispatchers = new GenericKeyedObjectPool();

        // TODO HH: dispatcher pool configuration needs to be extracted, maybe even
        // moved into the factory?
        // NOTE: testOnBorrow MUST be FALSE. this is a bit of a design bug in
        // commons-pool since validate is used for both activation and passivation,
        // but has no way of knowing which way it is going.
        dispatchers.setTestOnBorrow(false);
        dispatchers.setTestOnReturn(true);

        // container for receivers
        receivers = new ConcurrentHashMap();
    }

    public String getId()
    {
        return getClass().getName() + "." + getName();
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
            throw new IllegalArgumentException(new Message(Messages.X_IS_NULL, "Connector name").toString());
        }

        if (logger.isDebugEnabled())
        {
            logger.debug("Set UMOConnector name to: " + newName);
        }

        name = newName;
    }

    /*
     * (non-Javadoc)
     *
     * @see org.mule.providers.UMOConnector#create(java.util.HashMap)
     */
    public final synchronized void initialise() throws InitialisationException
    {
        if (initialised.get())
        {
            throw new AlreadyInitialisedException("Connector '" + getName() + "'", this);
        }

        if (logger.isInfoEnabled())
        {
            logger.info("Initialising " + getClass().getName());
        }
        // Initialise the structure of this connector
        initFromServiceDescriptor();

        // we clearErrors out any registered dispatchers and receivers without resetting
        // the actual containers since this it might actually be a re-initialise
        // (e.g. as in JmsConnector)
        this.disposeDispatchers();
        this.disposeReceivers();

        this.doInitialise();

        if (exceptionListener instanceof Initialisable)
        {
            ((Initialisable)exceptionListener).initialise();
        }

        initialised.set(true);
    }

    public abstract String getProtocol();

    /*
     * (non-Javadoc)
     *
     * @see org.mule.umo.provider.UMOConnector#start()
     */
    public final synchronized void startConnector() throws UMOException
    {
        checkDisposed();

        if (!isStarted())
        {
            if (!isConnected())
            {
                startOnConnect.set(true);
                this.getConnectionStrategy().connect(this);
                // Only start once we are connected
                return;
            }

            if (logger.isInfoEnabled())
            {
                logger.info("Starting Connector: " + getClass().getName());
            }

            // the scheduler is recreated after stopConnector()
            if (scheduler == null || scheduler.isShutdown())
            {
                scheduler = this.getScheduler();
            }

            this.doStart();
            started.set(true);

            if (receivers != null)
            {
                for (Iterator iterator = receivers.values().iterator(); iterator.hasNext();)
                {
                    UMOMessageReceiver mr = (UMOMessageReceiver)iterator.next();
                    if (logger.isDebugEnabled())
                    {
                        logger.debug("Starting receiver on endpoint: " + mr.getEndpoint().getEndpointURI());
                    }
                    mr.start();
                }
            }

            if (logger.isInfoEnabled())
            {
                logger.info("Connector: " + getClass().getName() + " has been started");
            }
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
    public final synchronized void stopConnector() throws UMOException
    {
        if (isDisposed())
        {
            return;
        }

        if (isStarted())
        {
            if (logger.isInfoEnabled())
            {
                logger.info("Stopping Connector: " + getClass().getName());
            }

            this.doStop();
            started.set(false);

            // Stop all the receivers on this connector (this will cause them to
            // disconnect too)
            if (receivers != null)
            {
                for (Iterator iterator = receivers.values().iterator(); iterator.hasNext();)
                {
                    UMOMessageReceiver mr = (UMOMessageReceiver)iterator.next();
                    if (logger.isDebugEnabled())
                    {
                        logger.debug("Stopping receiver on endpoint: " + mr.getEndpoint().getEndpointURI());
                    }
                    mr.stop();
                }
            }

            // shutdown our scheduler service
            scheduler.shutdown();
            scheduler = null;
        }

        if (isConnected())
        {
            try
            {
                disconnect();
            }
            catch (Exception e)
            {
                logger.error("Failed to disconnect: " + e.getMessage(), e);
            }
        }

        if (logger.isInfoEnabled())
        {
            logger.info("Connector " + getClass().getName() + " has been stopped");
        }
    }

    /*
     * (non-Javadoc)
     *
     * @see org.mule.umo.provider.UMOConnector#shutdown()
     */
    public final synchronized void dispose()
    {
        disposing.set(true);

        if (logger.isInfoEnabled())
        {
            logger.info("Disposing Connector: " + getClass().getName());
        }

        try
        {
            this.stopConnector();
        }
        catch (UMOException e)
        {
            logger.warn("Failed to stop during shutdown: " + e.getMessage(), e);
        }

        this.disposeReceivers();
        this.disposeDispatchers();

        this.doDispose();
        disposed.set(true);

        if (logger.isInfoEnabled())
        {
            logger.info("Connector " + getClass().getName() + " has been disposed.");
        }
    }

    protected void disposeReceivers()
    {
        if (receivers != null)
        {
            logger.debug("Disposing Receivers");

            for (Iterator iterator = receivers.values().iterator(); iterator.hasNext();)
            {
                UMOMessageReceiver receiver = (UMOMessageReceiver)iterator.next();

                try
                {
                    this.destroyReceiver(receiver, receiver.getEndpoint());
                }
                catch (Throwable e)
                {
                    logger.error("Failed to destroy receiver: " + receiver, e);
                }
            }

            receivers.clear();
            logger.debug("Receivers Disposed");
        }
    }

    protected void disposeDispatchers()
    {
        if (dispatchers != null)
        {
            logger.debug("Disposing Dispatchers");

            try
            {
                dispatchers.clear();
            }
            catch (Exception ex)
            {
                // ignored
            }

            logger.debug("Dispatchers Disposed");
        }
    }

    /*
     * (non-Javadoc)
     *
     * @see org.mule.umo.provider.UMOConnector#isAlive()
     */
    public boolean isDisposed()
    {
        return disposed.get();
    }

    /*
     * (non-Javadoc)
     *
     * @see org.mule.umo.provider.UMOConnector#handleException(java.lang.Object,
     *      java.lang.Throwable)
     */
    public void handleException(Exception exception)
    {
        if (exceptionListener == null)
        {
            throw new MuleRuntimeException(new Message(
                Messages.EXCEPTION_ON_CONNECTOR_X_NO_EXCEPTION_LISTENER, getName()), exception);
        }
        else
        {
            exceptionListener.exceptionThrown(exception);
        }
    }

    /*
     * (non-Javadoc)
     *
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
     * @param dispatcherFactory The dispatcherFactory to set.
     */
    public void setDispatcherFactory(UMOMessageDispatcherFactory dispatcherFactory)
    {
        KeyedPoolableObjectFactory poolFactory;

        if (dispatcherFactory instanceof KeyedPoolableObjectFactory)
        {
            poolFactory = (KeyedPoolableObjectFactory)dispatcherFactory;
        }
        else
        {
            // need to adapt the UMOMessageDispatcherFactory for use by commons-pool
            poolFactory = new KeyedPoolMessageDispatcherFactoryAdapter(dispatcherFactory);
        }

        this.dispatchers.setFactory(poolFactory);

        // we keep a reference to the unadapted factory, otherwise people might end
        // up with ClassCastExceptions on downcast to their implementation (sigh)
        this.dispatcherFactory = dispatcherFactory;
    }

    private UMOMessageDispatcher getDispatcher(UMOImmutableEndpoint endpoint) throws UMOException
    {
        this.checkDisposed();

        if (endpoint == null)
        {
            throw new IllegalArgumentException("Endpoint must not be null");
        }

        if (!supportsProtocol(endpoint.getConnector().getProtocol()))
        {
            throw new IllegalArgumentException(new Message(
                Messages.CONNECTOR_SCHEME_X_INCOMPATIBLE_WITH_ENDPOINT_SCHEME_X, getProtocol(), endpoint
                    .getEndpointURI().toString()).getMessage());
        }

        try
        {
            if (logger.isDebugEnabled())
            {
                logger.debug("Borrowing a dispatcher for endpoint: " + endpoint.getEndpointURI());
            }

            UMOMessageDispatcher dispatcher = (UMOMessageDispatcher)dispatchers.borrowObject(endpoint);

            if (logger.isDebugEnabled())
            {
                logger.debug("Borrowed a dispatcher for endpoint: " + endpoint.getEndpointURI() + " = "
                                + dispatcher.toString());
            }

            return dispatcher;
        }
        catch (Exception ex)
        {
            throw new ConnectorException(new Message(Messages.CONNECTOR_CAUSED_ERROR), this, ex);
        }
    }

    private void returnDispatcher(UMOImmutableEndpoint endpoint, UMOMessageDispatcher dispatcher)
    {
        if (endpoint != null && dispatcher != null)
        {
            try
            {
                if (logger.isDebugEnabled())
                {
                    logger.debug("Returning dispatcher for endpoint: " + endpoint.getEndpointURI() + " = "
                                    + dispatcher.toString());
                }

                dispatchers.returnObject(endpoint, dispatcher);
            }
            catch (Exception ex)
            {
                // ignored; this will go away in commons-pool
            }
        }
    }

    protected void checkDisposed() throws DisposeException
    {
        if (this.isDisposed())
        {
            throw new DisposeException(new Message(Messages.CANT_USE_DISPOSED_CONNECTOR), this);
        }
    }

    public UMOMessageReceiver registerListener(UMOComponent component, UMOEndpoint endpoint) throws Exception
    {
        if (endpoint == null)
        {
            throw new IllegalArgumentException("The endpoint cannot be null when registering a listener");
        }

        if (component == null)
        {
            throw new IllegalArgumentException("The component cannot be null when registering a listener");
        }

        UMOEndpointURI endpointUri = endpoint.getEndpointURI();
        if (endpointUri == null)
        {
            throw new ConnectorException(new Message(Messages.ENDPOINT_NULL_FOR_LISTENER), this);
        }

        logger.info("Registering listener: " + component.getDescriptor().getName() + " on endpointUri: "
                        + endpointUri.toString());

        UMOMessageReceiver receiver = this.getReceiver(component, endpoint);

        if (receiver != null)
        {
            throw new ConnectorException(new Message(Messages.LISTENER_ALREADY_REGISTERED, endpointUri), this);
        }
        else
        {
            receiver = createReceiver(component, endpoint);
            Object receiverKey = getReceiverKey(component, endpoint);
            receiver.setReceiverKey(receiverKey.toString());
            receivers.put(receiverKey, receiver);
            //receivers.put(getReceiverKey(component, endpoint), receiver);
        }

        return receiver;
    }

    /**
     * The method determines the key used to store the receiver against.
     *
     * @param component the component for which the endpoint is being registered
     * @param endpoint the endpoint being registered for the component
     * @return the key to store the newly created receiver against
     */
    protected Object getReceiverKey(UMOComponent component, UMOEndpoint endpoint)
    {
        return StringUtils.defaultIfEmpty(endpoint.getEndpointURI().getFilterAddress(), endpoint
            .getEndpointURI().getAddress());
    }

    public final void unregisterListener(UMOComponent component, UMOEndpoint endpoint) throws Exception
    {
        if (component == null)
        {
            throw new IllegalArgumentException(
                "The component must not be null when you unregister a listener");
        }

        if (endpoint == null)
        {
            throw new IllegalArgumentException("The endpoint must not be null when you unregister a listener");
        }

        UMOEndpointURI endpointUri = endpoint.getEndpointURI();
        if (endpointUri == null)
        {
            throw new IllegalArgumentException(
                "The endpointUri must not be null when you unregister a listener");
        }

        if (logger.isInfoEnabled())
        {
            logger.info("Removing listener on endpointUri: " + endpointUri);
        }

        if (receivers != null && !receivers.isEmpty())
        {
            UMOMessageReceiver receiver = (UMOMessageReceiver)receivers.remove(getReceiverKey(component,
                endpoint));
            if (receiver != null)
            {
                destroyReceiver(receiver, endpoint);
                receiver.dispose();
            }
        }
    }

    public synchronized ThreadingProfile getDispatcherThreadingProfile()
    {
        if (dispatcherThreadingProfile == null)
        {
            dispatcherThreadingProfile = MuleManager.getConfiguration()
                .getDefaultMessageDispatcherThreadingProfile();
        }
        return dispatcherThreadingProfile;
    }

    public synchronized void setDispatcherThreadingProfile(ThreadingProfile dispatcherThreadingProfile)
    {
        this.dispatcherThreadingProfile = dispatcherThreadingProfile;
    }

    public synchronized ThreadingProfile getReceiverThreadingProfile()
    {
        if (receiverThreadingProfile == null)
        {
            receiverThreadingProfile = MuleManager.getConfiguration().getDefaultMessageReceiverThreadingProfile();
        }

        return receiverThreadingProfile;
    }

    public synchronized void setReceiverThreadingProfile(ThreadingProfile receiverThreadingProfile)
    {
        this.receiverThreadingProfile = receiverThreadingProfile;
    }

    public void destroyReceiver(UMOMessageReceiver receiver, UMOEndpoint endpoint) throws Exception
    {
        receiver.dispose();
    }

    protected abstract void doInitialise() throws InitialisationException;

    /**
     * Template method to perform any work when destroying the connectoe
     */
    protected abstract void doDispose();

    /**
     * Template method to perform any work when starting the connectoe
     *
     * @throws UMOException if the method fails
     */
    protected abstract void doStart() throws UMOException;

    /**
     * Template method to perform any work when stopping the connectoe
     *
     * @throws UMOException if the method fails
     */
    protected abstract void doStop() throws UMOException;

    public UMOTransformer getDefaultInboundTransformer()
    {
        if (defaultInboundTransformer != null)
        {
            try
            {
                return (UMOTransformer)defaultInboundTransformer.clone();
            }
            catch (CloneNotSupportedException e)
            {
                logger.error("Failed to clone default Inbound transformer");
            }
        }

        return null;
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
                return (UMOTransformer)defaultResponseTransformer.clone();
            }
            catch (CloneNotSupportedException e)
            {
                logger.error("Failed to clone default Outbound transformer");
            }
        }

        return null;
    }

    public UMOTransformer getDefaultOutboundTransformer()
    {
        if (defaultOutboundTransformer != null)
        {
            try
            {
                return (UMOTransformer)defaultOutboundTransformer.clone();
            }
            catch (CloneNotSupportedException e)
            {
                logger.error("Failed to clone default Outbound transformer");
            }
        }

        return null;
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

    /**
     * Fires a server notification to all registered
     * {@link org.mule.impl.internal.notifications.CustomNotificationListener}
     * eventManager.
     *
     * @param notification the notification to fire. This must be of type
     *            {@link org.mule.impl.internal.notifications.CustomNotification}
     *            otherwise an exception will be thrown.
     * @throws UnsupportedOperationException if the notification fired is not a
     *             {@link org.mule.impl.internal.notifications.CustomNotification}
     */
    public void fireNotification(UMOServerNotification notification)
    {
        MuleManager.getInstance().fireNotification(notification);
    }

    public ConnectionStrategy getConnectionStrategy()
    {
        // not happy with this but each receiver needs its own instance
        // of the connection strategy and using a factory just introduces extra
        // implementation
        try
        {
            return (ConnectionStrategy)BeanUtils.cloneBean(connectionStrategy);
        }
        catch (Exception e)
        {
            throw new MuleRuntimeException(new Message(Messages.FAILED_TO_CLONE_X, "connectionStrategy"), e);
        }
    }

    public void setConnectionStrategy(ConnectionStrategy connectionStrategy)
    {
        this.connectionStrategy = connectionStrategy;
    }

    public boolean isDisposing()
    {
        return disposing.get();
    }

    public boolean isRemoteSyncEnabled()
    {
        return false;
    }

    public AbstractMessageReceiver getReceiver(UMOComponent component, UMOEndpoint endpoint)
    {
        return (AbstractMessageReceiver)receivers.get(getReceiverKey(component, endpoint));
    }

    public Map getReceivers()
    {
        return Collections.unmodifiableMap(receivers);
    }

    public UMOMessageReceiver lookupReceiver(String key)
    {
        if (key != null)
        {
            return (UMOMessageReceiver)receivers.get(key);
        }
        else
        {
            throw new IllegalArgumentException("Receiver key must not be null");
        }
    }

    public AbstractMessageReceiver[] getReceivers(String wildcardExpression)
    {
        List temp = new ArrayList();
        WildcardFilter filter = new WildcardFilter(wildcardExpression);
        filter.setCaseSensitive(false);
        for (Iterator iterator = receivers.keySet().iterator(); iterator.hasNext();)
        {
            Object o = iterator.next();
            if (filter.accept(o))
            {
                temp.add(receivers.get(o));
            }
        }
        AbstractMessageReceiver[] result = new AbstractMessageReceiver[temp.size()];
        return (AbstractMessageReceiver[])temp.toArray(result);
    }

    public void connect() throws Exception
    {
        if (connected.get())
        {
            return;
        }

        this.checkDisposed();

        if (connecting.compareAndSet(false, true))
        {
            connectionStrategy.connect(this);
            logger.info("Connected: " + getConnectionDescription());
            // This method calls itself so the the connecting flag is set first, then
            // the connection is made on the second call
            return;
        }

        try
        {
            this.doConnect();
            fireNotification(new ConnectionNotification(this, getConnectEventId(),
                ConnectionNotification.CONNECTION_CONNECTED));
        }
        catch (Exception e)
        {
            fireNotification(new ConnectionNotification(this, getConnectEventId(),
                ConnectionNotification.CONNECTION_FAILED));

            if (e instanceof ConnectException)
            {
                throw (ConnectException)e;
            }
            else
            {
                throw new ConnectException(e, this);
            }
        }

        connected.set(true);
        connecting.set(false);

        if (startOnConnect.get())
        {
            this.startConnector();
        }
        else
        {
            for (Iterator iterator = receivers.values().iterator(); iterator.hasNext();)
            {
                AbstractMessageReceiver amr = (AbstractMessageReceiver)iterator.next();
                if (logger.isDebugEnabled())
                {
                    logger.debug("Connecting receiver on endpoint: " + amr.getEndpoint().getEndpointURI());
                }
                amr.connect();
            }
        }
    }

    public void disconnect() throws Exception
    {
        startOnConnect.set(isStarted());

        fireNotification(new ConnectionNotification(this, getConnectEventId(),
            ConnectionNotification.CONNECTION_DISCONNECTED));

        connected.set(false);

        try
        {
            doDisconnect();
        }
        finally
        {
            stopConnector();
        }

        logger.info("Disconnected: " + getConnectionDescription());
    }

    public String getConnectionDescription()
    {
        return toString();
    }

    public final boolean isConnected()
    {
        return connected.get();
    }

    /**
     * Template method where any connections should be made for the connector
     *
     * @throws Exception
     */
    protected abstract void doConnect() throws Exception;

    /**
     * Template method where any connected resources used by the connector should be
     * disconnected
     *
     * @throws Exception
     */
    protected abstract void doDisconnect() throws Exception;

    /**
     * The resource id used when firing ConnectEvents from this connector
     *
     * @return the resource id used when firing ConnectEvents from this connector
     */
    protected String getConnectEventId()
    {
        return getName();
    }

    /**
     * For better throughput when using TransactedMessageReceivers this will enable a
     * number of concurrent receivers, based on the value returned by
     * {@link #getNumberOfConcurrentTransactedReceivers()}. This property is used by
     * transports that support transactions, specifically receivers that extend the
     * TransactedPollingMessageReceiver.
     *
     * @return true if multiple receivers will be enabled for this connection
     */
    public boolean isCreateMultipleTransactedReceivers()
    {
        return createMultipleTransactedReceivers;
    }

    /**
     * @see {@link #isCreateMultipleTransactedReceivers()}
     * @param createMultipleTransactedReceivers if true, multiple receivers will be
     *            created for this connection
     */
    public void setCreateMultipleTransactedReceivers(boolean createMultipleTransactedReceivers)
    {
        this.createMultipleTransactedReceivers = createMultipleTransactedReceivers;
    }

    /**
     * Returns the number of concurrent receivers that will be launched when
     * {@link #isCreateMultipleTransactedReceivers()} returns <code>true</code>.
     * By default the number is <strong>4</strong>.
     */
    public int getNumberOfConcurrentTransactedReceivers()
    {
        return numberOfConcurrentTransactedReceivers;
    }

    /**
     * @see {@link #getNumberOfConcurrentTransactedReceivers()}
     * @param count the number of concurrent transacted receivers to start
     */
    public void setNumberOfConcurrentTransactedReceivers(int count)
    {
        numberOfConcurrentTransactedReceivers = count;
    }

    /**
     * Whether to fire message notifications for every message that is sent or
     * received from this connector
     */
    public boolean isEnableMessageEvents()
    {
        return enableMessageEvents;
    }

    /**
     * Whether to fire message notifications for every message that is sent or
     * received from this connector
     *
     * @param enableMessageEvents
     */
    public void setEnableMessageEvents(boolean enableMessageEvents)
    {
        this.enableMessageEvents = enableMessageEvents;
    }

    /**
     * Registers other protocols 'understood' by this connector. These must contain
     * scheme meta info. Any protocol registered must begin with the protocol of this
     * connector, i.e. If the connector is axis the protocol for jms over axis will
     * be axis:jms. Here, 'axis' is the scheme meta info and 'jms' is the protocol.
     * If the protocol argument does not start with the connector's protocol, it will
     * be appended.
     *
     * @param protocol the supported protocol to register
     */
    public void registerSupportedProtocol(String protocol)
    {
        protocol = protocol.toLowerCase();
        if (protocol.startsWith(getProtocol().toLowerCase()))
        {
            registerSupportedProtocolWithoutPrefix(protocol);
        }
        else
        {
            supportedProtocols.add(getProtocol().toLowerCase() + ":" + protocol);
        }
    }

    /**
     * Registers other protocols 'understood' by this connector. These must contain
     * scheme meta info. Unlike the <code>registerSupportedProtolcol</code> method,
     * this allows you to register protocols that are not prefixed with the connector
     * protocol. This is useful where you use a Service Finder to discover which
     * Transport implementation to use. For example the 'wsdl' transport is a generic
     * 'finder' transport that will use Axis, Xfire or Glue to create the WSDL
     * client. These transport protocols would be wsdl-axis, wsdl-xfire and
     * wsdl-glue, but they can all support 'wsdl' protocol too.
     *
     * @param protocol the supported protocol to register
     */
    protected void registerSupportedProtocolWithoutPrefix(String protocol)
    {
        supportedProtocols.add(protocol.toLowerCase());
    }

    public void unregisterSupportedProtocol(String protocol)
    {
        protocol = protocol.toLowerCase();
        if (protocol.startsWith(getProtocol().toLowerCase()))
        {
            supportedProtocols.remove(protocol);
        }
        else
        {
            supportedProtocols.remove(getProtocol().toLowerCase() + ":" + protocol);
        }
    }

    /**
     * @return true if the protocol is supported by this connector.
     */
    public boolean supportsProtocol(String protocol)
    {
        return supportedProtocols.contains(protocol.toLowerCase());
    }

    /**
     * Returns an unmodifiable list of the protocols supported by this connector
     *
     * @return an unmodifiable list of the protocols supported by this connector
     */
    public List getSupportedProtocols()
    {
        return Collections.unmodifiableList(supportedProtocols);
    }

    /**
     * Sets A list of protocols that the connector can accept
     *
     * @param supportedProtocols
     */
    public void setSupportedProtocols(List supportedProtocols)
    {
        for (Iterator iterator = supportedProtocols.iterator(); iterator.hasNext();)
        {
            String s = (String)iterator.next();
            registerSupportedProtocol(s);
        }
    }

    /**
     * Returns a work manager for message receivers.
     */
    synchronized UMOWorkManager getReceiverWorkManager(String receiverName) throws UMOException
    {
        // lazily created because ThreadingProfile was not yet set in Constructor
        if (receiverWorkManager == null)
        {
            receiverWorkManager = this.getReceiverThreadingProfile().createWorkManager(
                this.getName() + '.' + receiverName);
            receiverWorkManager.start();
        }

        return receiverWorkManager;
    }

    /**
     * Returns a work manager for message dispatchers.
     */
    synchronized UMOWorkManager getDispatcherWorkManager() throws UMOException
    {
        // lazily created because ThreadingProfile was not yet set in Constructor
        if (dispatcherWorkManager == null)
        {
            dispatcherWorkManager = this.getDispatcherThreadingProfile().createWorkManager(
                getName() + ".dispatcher");
            dispatcherWorkManager.start();
        }

        return dispatcherWorkManager;
    }

    /**
     * Returns a Scheduler service for periodic tasks, currently limited to internal
     * use. Note: getScheduler() currently conflicts with the same method in the
     * Quartz transport
     */
    public synchronized ScheduledExecutorService getScheduler()
    {
        if (scheduler == null)
        {
            ThreadFactory stf = new NamedThreadFactory(this.getName() + ".scheduler");
            ScheduledThreadPoolExecutor stpe = new ScheduledThreadPoolExecutor(1, stf);
            stpe.setExecuteExistingDelayedTasksAfterShutdownPolicy(false);
            stpe.setKeepAliveTime(getReceiverThreadingProfile().getThreadTTL(), TimeUnit.MILLISECONDS);
            scheduler = stpe;
        }

        return scheduler;
    }

    public UMOSessionHandler getSessionHandler()
    {
        return sessionHandler;
    }

    public void setSessionHandler(UMOSessionHandler sessionHandler)
    {
        this.sessionHandler = sessionHandler;
    }

    public void workAccepted(WorkEvent event)
    {
        handleWorkException(event, "workAccepted");
    }

    public void workRejected(WorkEvent event)
    {
        handleWorkException(event, "workRejected");
    }

    public void workStarted(WorkEvent event)
    {
        handleWorkException(event, "workStarted");
    }

    public void workCompleted(WorkEvent event)
    {
        handleWorkException(event, "workCompleted");
    }

    protected void handleWorkException(WorkEvent event, String type)
    {
        Throwable e;

        if (event != null && event.getException() != null)
        {
            e = event.getException();
        }
        else
        {
            return;
        }

        if (event.getException().getCause() != null)
        {
            e = event.getException().getCause();
        }

        logger.error("Work caused exception on '" + type + "'. Work being executed was: "
                        + event.getWork().toString());

        if (e instanceof Exception)
        {
            handleException((Exception)e);
        }
        else
        {
            throw new MuleRuntimeException(new Message(Messages.CONNECTOR_CAUSED_ERROR, getName()), e);
        }
    }

    // TODO HH: the following methods should probably be lifecycle-enabled;
    // for now they are only stubs to get the refactoring going.

    public void dispatch(UMOImmutableEndpoint endpoint, UMOEvent event) throws DispatchException
    {
        UMOMessageDispatcher dispatcher = null;

        try
        {
            dispatcher = this.getDispatcher(endpoint);
            dispatcher.dispatch(event);
        }
        catch (DispatchException dex)
        {
            throw dex;
        }
        catch (UMOException ex)
        {
            throw new DispatchException(event.getMessage(), endpoint, ex);
        }
        finally
        {
            this.returnDispatcher(endpoint, dispatcher);
        }
    }

    public UMOMessage receive(UMOEndpointURI endpointUri, long timeout) throws Exception
    {
        return this.receive(new ImmutableMuleEndpoint(endpointUri.toString(), true), timeout);
    }

    public UMOMessage receive(UMOImmutableEndpoint endpoint, long timeout) throws Exception
    {
        UMOMessageDispatcher dispatcher = null;

        try
        {
            dispatcher = this.getDispatcher(endpoint);
            return dispatcher.receive(timeout);
        }
        finally
        {
            this.returnDispatcher(endpoint, dispatcher);
        }
    }

    public UMOMessage send(UMOImmutableEndpoint endpoint, UMOEvent event) throws DispatchException
    {
        UMOMessageDispatcher dispatcher = null;

        try
        {
            dispatcher = this.getDispatcher(endpoint);
            return dispatcher.send(event);
        }
        catch (DispatchException dex)
        {
            throw dex;
        }
        catch (UMOException ex)
        {
            throw new DispatchException(event.getMessage(), endpoint, ex);
        }
        finally
        {
            this.returnDispatcher(endpoint, dispatcher);
        }
    }

    // -------- Methods from the removed AbstractServiceEnabled Connector

    /**
     * When this connector is created via the
     * {@link org.mule.providers.service.TransportFactory} the endpoint used to
     * determine the connector type is passed to this method so that any properties
     * set on the endpoint that can be used to initialise the connector are made
     * available.
     *
     * @param endpointUri the {@link UMOEndpointURI} use to create this connector
     * @throws InitialisationException If there are any problems with the
     *             configuration set on the Endpoint or if another exception is
     *             thrown it is wrapped in an InitialisationException.
     */
    public void initialiseFromUrl(UMOEndpointURI endpointUri) throws InitialisationException
    {
        if (!supportsProtocol(endpointUri.getFullScheme()))
        {
            throw new InitialisationException(new Message(Messages.SCHEME_X_NOT_COMPATIBLE_WITH_CONNECTOR_X,
                endpointUri.getFullScheme(), getClass().getName()), this);
        }
        Properties props = new Properties();
        props.putAll(endpointUri.getParams());
        // auto set username and password
        if (endpointUri.getUserInfo() != null)
        {
            props.setProperty("username", endpointUri.getUsername());
            String passwd = endpointUri.getPassword();
            if (passwd != null)
            {
                props.setProperty("password", passwd);
            }
        }
        String host = endpointUri.getHost();
        if (host != null)
        {
            props.setProperty("hostname", host);
            props.setProperty("host", host);
        }
        if (endpointUri.getPort() > -1)
        {
            props.setProperty("port", String.valueOf(endpointUri.getPort()));
        }

        org.mule.util.BeanUtils.populateWithoutFail(this, props, true);

        try
        {
            setName(ObjectNameHelper.getConnectorName(this));
        }
        catch (RegistryException e)
        {
            throw new InitialisationException(e, this);
        }
    }

    /**
     * Initialises this connector from its {@link TransportServiceDescriptor} This
     * will be called before the {@link #doInitialise()} method is called.
     *
     * @throws InitialisationException InitialisationException If there are any
     *             problems with the configuration or if another exception is thrown
     *             it is wrapped in an InitialisationException.
     */
    protected synchronized void initFromServiceDescriptor() throws InitialisationException
    {
        try
        {
            serviceDescriptor = (TransportServiceDescriptor)
                MuleManager.getRegistry().lookupServiceDescriptor(ServiceDescriptorFactory.PROVIDER_SERVICE_TYPE, getProtocol().toLowerCase(), serviceOverrides);
            if (serviceDescriptor == null)
            {
                throw new ServiceException(Message.createStaticMessage("No service descriptor found for transport: " + getProtocol() + ".  This transport does not appear to be installed."));
            }

            if (logger.isDebugEnabled())
            {
                logger.debug("Loading DispatcherFactory for connector: " + getName() + " (" + getClass().getName() + ")");
            }
            this.setDispatcherFactory(serviceDescriptor.createDispatcherFactory());

                try
                {
                    this.setDispatcherFactory(serviceDescriptor.createDispatcherFactory());
                }
                catch (TransportServiceException e)
                {
                    logger.debug("Transport '" + getProtocol() + "' will not support outbound endpoints: " + e.getMessage());
                }

            defaultInboundTransformer = serviceDescriptor.createInboundTransformer();
            defaultOutboundTransformer = serviceDescriptor.createOutboundTransformer();
            defaultResponseTransformer = serviceDescriptor.createResponseTransformer();

            sessionHandler = serviceDescriptor.createSessionHandler();

            // Set any manager default properties for the connector. These are set on
            // the Manager with a protocol e.g. jms.specification=1.1
            // This provides a really convenient way to set properties on an object
            // from unit tests
            Map props = new HashMap();
            PropertiesUtils.getPropertiesWithPrefix(MuleManager.getInstance().getProperties(), getProtocol()
                .toLowerCase(), props);
            if (props.size() > 0)
            {
                props = PropertiesUtils.removeNamespaces(props);
                org.mule.util.BeanUtils.populateWithoutFail(this, props, true);
            }
        }
        catch (Exception e)
        {
            throw new InitialisationException(e, this);
        }
    }

    /**
     * Get the {@link TransportServiceDescriptor} for this connector. This will be
     * null if the connector was created by the developer. To create a connector the
     * proper way the developer should use the {@link TransportFactory} and pass in
     * an endpoint.
     *
     * @return the {@link TransportServiceDescriptor} for this connector
     */
    protected TransportServiceDescriptor getServiceDescriptor()
    {
        if (serviceDescriptor == null)
        {
            throw new IllegalStateException("This connector has not yet been initialised: " + name);
        }
        return serviceDescriptor;
    }

    /**
     * Create a Message receiver for this connector
     *
     * @param component the component that will receive events from this receiver,
     *            the listener
     * @param endpoint the endpoint that defies this inbound communication
     * @return an instance of the message receiver defined in this connectors'
     *         {@link org.mule.providers.service.TransportServiceDescriptor}
     *         initialised using the component and endpoint.
     * @throws Exception if there is a problem creating the receiver. This exception
     *             really depends on the underlying transport, thus any exception
     *             could be thrown
     */
    protected UMOMessageReceiver createReceiver(UMOComponent component, UMOEndpoint endpoint)
        throws Exception
    {
        return getServiceDescriptor().createMessageReceiver(this, component, endpoint);
    }

    /**
     * Gets a <code>UMOMessageAdapter</code> for the endpoint for the given message
     * (data)
     *
     * @param message the data with which to initialise the
     *            <code>UMOMessageAdapter</code>
     * @return the <code>UMOMessageAdapter</code> for the endpoint
     * @throws org.mule.umo.MessagingException if the message parameter is not
     *             supported
     * @see org.mule.umo.provider.UMOMessageAdapter
     */
    public UMOMessageAdapter getMessageAdapter(Object message) throws MessagingException
    {
        try
        {
            return serviceDescriptor.createMessageAdapter(message);
        }
        catch (TransportServiceException e)
        {
            throw new MessagingException(new Message(Messages.FAILED_TO_CREATE_X, "Message Adapter"),
                message, e);
        }
    }

    /**
     * Gets a {@link UMOStreamMessageAdapter} from the connector for the given
     * message. This Adapter will correctly handle data streaming for this type of
     * connector
     *
     * @param in the input stream to read the data from
     * @param out the outputStream to write data to. This can be null.
     * @return the {@link UMOStreamMessageAdapter} for the endpoint
     * @throws MessagingException if the message parameter is not supported
     * @see UMOStreamMessageAdapter
     */
    public UMOStreamMessageAdapter getStreamMessageAdapter(InputStream in, OutputStream out)
        throws MessagingException
    {
        try
        {
            return serviceDescriptor.createStreamMessageAdapter(in, out);
        }
        catch (TransportServiceException e)
        {
            throw new MessagingException(new Message(Messages.FAILED_TO_CREATE_X, "Stream Message Adapter"),
                in, e);
        }
    }

    /**
     * A map of fully qualified class names that should override those in the
     * connectors' service descriptor This map will be null if there are no overrides
     *
     * @return a map of override values or null
     */
    public Map getServiceOverrides()
    {
        return serviceOverrides;
    }

    /**
     * Set the Service overrides on this connector.
     *
     * @param serviceOverrides the override values to use
     */
    public void setServiceOverrides(Map serviceOverrides)
    {
        this.serviceOverrides = new Properties();
        this.serviceOverrides.putAll(serviceOverrides);
    }

    /**
     * Well get the output stream (if any) for this type of transport. Typically this
     * will be called only when Streaming is being used on an outbound endpoint.
     * If Streaming is not supported by this transport an {@link UnsupportedOperationException}
     * is thrown
     *
     * @param endpoint the endpoint that releates to this Dispatcher
     * @param message the current message being processed
     * @return the output stream to use for this request or null if the transport
     *         does not support streaming
     * @throws org.mule.umo.UMOException
     */
    public OutputStream getOutputStream(UMOImmutableEndpoint endpoint, UMOMessage message)
        throws UMOException
    {
        throw new UnsupportedOperationException(new Message(Messages.STREAMING_NOT_SUPPORTED_FOR_X, getProtocol()).toString());
    }


    public String toString()
    {
        final StringBuffer sb = new StringBuffer();
        sb.append("AbstractConnector");
        sb.append("{started=").append(started);
        sb.append(", initialised=").append(initialised);
        sb.append(", name='").append(name).append('\'');
        sb.append(", disposed=").append(disposed);
        sb.append(", numberOfConcurrentTransactedReceivers=").append(numberOfConcurrentTransactedReceivers);
        sb.append(", createMultipleTransactedReceivers=").append(createMultipleTransactedReceivers);
        sb.append(", connected=").append(connected);
        sb.append(", supportedProtocols=").append(supportedProtocols);
        sb.append(", serviceOverrides=").append(serviceOverrides);
        sb.append(", registryId='").append(getId()).append('\'');
        sb.append('}');
        return sb.toString();
    }
}
