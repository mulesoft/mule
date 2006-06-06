/*
 * $Id$
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

package org.mule.providers;

import edu.emory.mathcs.backport.java.util.concurrent.ConcurrentHashMap;
import edu.emory.mathcs.backport.java.util.concurrent.ConcurrentMap;
import edu.emory.mathcs.backport.java.util.concurrent.atomic.AtomicBoolean;

import org.apache.commons.beanutils.BeanUtils;
import org.apache.commons.lang.StringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.mule.MuleManager;
import org.mule.MuleRuntimeException;
import org.mule.config.ThreadingProfile;
import org.mule.config.i18n.Message;
import org.mule.config.i18n.Messages;
import org.mule.impl.AlreadyInitialisedException;
import org.mule.impl.DefaultExceptionStrategy;
import org.mule.impl.MuleSessionHandler;
import org.mule.impl.internal.notifications.ConnectionNotification;
import org.mule.management.mbeans.EndpointService;
import org.mule.routing.filters.WildcardFilter;
import org.mule.umo.UMOComponent;
import org.mule.umo.UMOException;
import org.mule.umo.endpoint.UMOEndpoint;
import org.mule.umo.endpoint.UMOEndpointURI;
import org.mule.umo.endpoint.UMOImmutableEndpoint;
import org.mule.umo.lifecycle.DisposeException;
import org.mule.umo.lifecycle.Initialisable;
import org.mule.umo.lifecycle.InitialisationException;
import org.mule.umo.manager.UMOServerNotification;
import org.mule.umo.manager.UMOWorkManager;
import org.mule.umo.provider.ConnectorException;
import org.mule.umo.provider.UMOConnectable;
import org.mule.umo.provider.UMOConnector;
import org.mule.umo.provider.UMOMessageDispatcher;
import org.mule.umo.provider.UMOMessageDispatcherFactory;
import org.mule.umo.provider.UMOMessageReceiver;
import org.mule.umo.provider.UMOSessionHandler;
import org.mule.umo.transformer.UMOTransformer;
import org.mule.util.concurrent.WaitableBoolean;

import javax.resource.spi.work.WorkEvent;
import javax.resource.spi.work.WorkListener;

import java.beans.ExceptionListener;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

/**
 * <code>AbstractConnector</code> provides base functionality for all connectors
 * provided with Mule. Connectors are the mechanism used to connect to external systems
 * and protocols in order to send and receive data. <p/> The
 * <code>AbstractConnector</code> provides getter and setter methods for endpoint name,
 * transport name and protocol. It also provides methods to stop and start connecotors and
 * sets up a dispatcher threadpool which allows deriving connectors the possibility to
 * dispatch work to separate threads. This functionality is controlled with the <i>
 * doThreading</i> property on the threadingProfiles for dispachers and receivers.
 *
 * The lifecycle for a connector is -
 *
 * 1. Create 2. Initialise 3. Connect 3a. Connect receivers 4. Start 4a. Start Receivers
 * 5. Stop 5a. Stop Receivers 6. Disconnect 6a. Disconnect Receivers 7. Dispose 7a.
 * Dispose Receivers
 *
 * @author <a href="mailto:ross.mason@symphonysoft.com">Ross Mason</a>
 * @version $Revision$
 */
public abstract class AbstractConnector
        implements
            UMOConnector,
            ExceptionListener,
            UMOConnectable,
            WorkListener
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
    protected ConcurrentMap dispatchers;

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
     * Determines whether dispatchers should be disposed straight away of after every
     * request or cached
     */
    protected boolean createDispatcherPerRequest = false;

    /**
     * For better throughput when using TransactedMessageReceivers. This will create an
     * number of receiver threads based on the ThreadingProfile configured fro the
     * receiver. This property is user by transports that support transactions,
     * specifically MessageReceivers that extend the TransactedPollingMessageReceiver.
     */
    protected boolean createMultipleTransactedReceivers = true;

    /**
     * The service descriptor can define a default inbound transformer to be used on an
     * endpoint if no other is set
     */
    protected UMOTransformer defaultInboundTransformer = null;

    /**
     * The service descriptor can define a default outbound transformer to be used on an
     * endpoint if no other is set
     */
    protected UMOTransformer defaultOutboundTransformer = null;

    /**
     * For some connectors such as http, a response transformer is required or where a
     * replyTo needs a trnasformer
     */
    protected UMOTransformer defaultResponseTransformer = null;

    private ConnectionStrategy connectionStrategy;

    protected WaitableBoolean connected = new WaitableBoolean(false);

    protected WaitableBoolean connecting = new WaitableBoolean(false);

    /**
     * If the connect method was called via the start method, this will be set so that
     * when the connector comes on line it will be started
     */
    protected WaitableBoolean startOnConnect = new WaitableBoolean(false);

    /**
     * Whether to fire message notifications for every message that is sent or received
     * from this connector
     */
    private boolean enableMessageEvents = false;

    private List supportedProtocols;

    /**
     * A shared work manager for all receivers registered with this connector if
     * <code>useSingleReceiverThreadPool</code> is set to true
     */
    private UMOWorkManager receiverWorkManager = null;

    /**
     * A shared work manager for all dispatchers created for this connector if
     * <code>useSingleDispatcherThreadPool</code> is set to true
     */
    private UMOWorkManager dispatcherWorkManager = null;

    /**
     * Should a single receiver thread pool be created for all receivers It is recommended
     * that if you have a lot of receivers being registered per connector that this should
     * be set to true
     */
    private boolean useSingleReceiverThreadPool = false;

    /**
     * Should a single dispatcher thread pool be created for all distachers It is
     * recommended that if you have a lot of dispatcher being created per connector that
     * this should be set to true i.e. many different outbound endpoints
     */
    private boolean useSingleDispatcherThreadPool = false;

    /**
     * The flag determines if the connector is being used on the server side or client. If
     * true receiver threads will be given a slightly higher priority.
     */
    protected boolean serverSide = true;

    /**
     * The strategy used for reading and writing session information to and fromt he
     * transport
     */
    protected UMOSessionHandler sessionHandler = new MuleSessionHandler();

    public AbstractConnector()
    {
        // make sure we always have an exception strategy
        exceptionListener = new DefaultExceptionStrategy();
        dispatchers = new ConcurrentHashMap();
        receivers = new ConcurrentHashMap();
        connectionStrategy = MuleManager.getConfiguration().getConnectionStrategy();
        enableMessageEvents = MuleManager.getConfiguration().isEnableMessageEvents();
        supportedProtocols = new ArrayList();

        // Always add the default protocol
        supportedProtocols.add(getProtocol().toLowerCase());

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
        if (newName == null) {
            throw new IllegalArgumentException(new Message(Messages.X_IS_NULL, "Connector name")
                    .toString());
        }
        if (logger.isDebugEnabled()) {
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
        if (initialised.get()) {
            throw new AlreadyInitialisedException("Connector '" + getName() + "'", this);
        }

        if (logger.isInfoEnabled()) {
            logger.info("Initialising " + getClass().getName());
        }

        doInitialise();
        if (exceptionListener instanceof Initialisable) {
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
    public final void startConnector() throws UMOException
    {
        checkDisposed();
        if (!isStarted()) {
            if (!isConnected()) {
                startOnConnect.set(true);
                getConnectionStrategy().connect(this);
                // Only start once we are connected
                return;
            }
            if (logger.isInfoEnabled()) {
                logger.info("Starting Connector: " + getClass().getName());
            }
            doStart();
            started.set(true);
            if (receivers != null) {
                for (Iterator iterator = receivers.values().iterator(); iterator.hasNext();) {
                    AbstractMessageReceiver amr = (AbstractMessageReceiver)iterator.next();
                    if (logger.isDebugEnabled()) {
                        logger.debug("Starting receiver on endpoint: "
                                + amr.getEndpoint().getEndpointURI());
                    }
                    amr.start();
                }
            }

            if (logger.isInfoEnabled()) {
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
    public final void stopConnector() throws UMOException
    {
        if (isDisposed()) {
            return;
        }

        if (isStarted()) {
            if (logger.isInfoEnabled()) {
                logger.info("Stopping Connector: " + getClass().getName());
            }
            doStop();
            started.set(false);

            // Stop all the receivers on this connector (this will cause them to
            // disconnect too)
            if (receivers != null) {
                for (Iterator iterator = receivers.values().iterator(); iterator.hasNext();) {
                    UMOMessageReceiver mr = (UMOMessageReceiver)iterator.next();
                    if (logger.isDebugEnabled()) {
                        logger.debug("Stopping receiver on endpoint: "
                                + mr.getEndpoint().getEndpointURI());
                    }
                    mr.stop();
                }
            }
        }

        if (isConnected()) {
            try {
                disconnect();
            }
            catch (Exception e) {
                logger.error("Failed to disconnect: " + e.getMessage(), e);
            }
        }
        if (logger.isInfoEnabled()) {
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
        if (logger.isInfoEnabled()) {
            logger.info("Disposing Connector: " + getClass().getName());
            logger.debug("Disposing Receivers");
        }
        disposeReceivers();
        disposeDispatchers();

        doDispose();
        disposed.set(true);

        if (logger.isInfoEnabled()) {
            logger.info("Connector " + getClass().getName() + " has been disposed.");
        }
    }

    protected void disposeReceivers()
    {
        if (receivers != null) {
            UMOMessageReceiver receiver;
            for (Iterator iterator = receivers.values().iterator(); iterator.hasNext();) {
                receiver = (UMOMessageReceiver)iterator.next();
                try {
                    destroyReceiver(receiver, receiver.getEndpoint());
                }
                catch (Throwable e) {
                    logger.error("Failed to destroy receiver: " + e.getMessage(), e);
                }
            }
            receivers.clear();
            receivers = null;
            logger.debug("Receivers Disposed");
        }
    }

    protected void disposeDispatchers()
    {
        if (dispatchers != null) {
            logger.debug("Disposing Dispatchers");
            for (Iterator iterator = dispatchers.values().iterator(); iterator.hasNext();) {
                UMOMessageDispatcher umoMessageDispatcher = (UMOMessageDispatcher)iterator.next();
                umoMessageDispatcher.dispose();
            }
            dispatchers.clear();
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
        if (exceptionListener == null) {
            throw new MuleRuntimeException(new Message(
                    Messages.EXCEPTION_ON_CONNECTOR_X_NO_EXCEPTION_LISTENER, getName()), exception);
        }
        else {
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
     * @param listener
     *            the ExceptionStrategy to use with this endpoint
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
     * @param dispatcherFactory
     *            The dispatcherFactory to set.
     */
    public void setDispatcherFactory(UMOMessageDispatcherFactory dispatcherFactory)
    {
        this.dispatcherFactory = dispatcherFactory;
    }

    public UMOMessageDispatcher getDispatcher(UMOImmutableEndpoint endpoint) throws UMOException
    {
        checkDisposed();

        if (endpoint == null) {
            throw new IllegalArgumentException("Endpoint must not be null");
        }

        if (!supportsProtocol(endpoint.getConnector().getProtocol())) {
            throw new IllegalArgumentException(new Message(
                    Messages.CONNECTOR_SCHEME_X_INCOMPATIBLE_WITH_ENDPOINT_SCHEME_X, getProtocol(),
                    endpoint.getEndpointURI().toString()).getMessage());
        }

        if (dispatchers == null) {
            throw new IllegalStateException("Dispatchers are null for connector: " + name);
        }

        synchronized (endpoint) {
            String endpointUriKey = endpoint.getEndpointURI().toString();
            UMOMessageDispatcher dispatcher = (UMOMessageDispatcher)dispatchers.get(endpointUriKey);
    
            if (dispatcher == null || dispatcher.isDisposed()) {
                dispatcher = createDispatcher(endpoint);
                dispatchers.put(endpointUriKey, dispatcher);
            }

            return dispatcher;
        }
    }

    protected void checkDisposed() throws DisposeException
    {
        if (isDisposed()) {
            throw new DisposeException(new Message(Messages.CANT_USE_DISPOSED_CONNECTOR), this);
        }
    }

    protected UMOMessageDispatcher createDispatcher(UMOImmutableEndpoint endpoint) throws UMOException
    {
        if (dispatcherFactory == null) {
            throw new ConnectorException(new Message(Messages.CONNECTOR_NOT_STARTED, name), this);
        }

        return dispatcherFactory.create(endpoint);
    }

    public UMOMessageReceiver registerListener(UMOComponent component, UMOEndpoint endpoint)
            throws Exception
    {
        if (endpoint == null) {
            throw new IllegalArgumentException("The endpoint cannot be null when registering a listener");
        }

        if (component == null) {
            throw new IllegalArgumentException(
                    "The component cannot be null when registering a listener");
        }

        UMOEndpointURI endpointUri = endpoint.getEndpointURI();
        if (endpointUri == null) {
            throw new ConnectorException(new Message(Messages.ENDPOINT_NULL_FOR_LISTENER), this);
        }
        logger.info("registering listener: " + component.getDescriptor().getName() + " on endpointUri: "
                + endpointUri.toString());

        UMOMessageReceiver receiver = getReceiver(component, endpoint);
        if (receiver != null) {
            throw new ConnectorException(new Message(Messages.LISTENER_ALREADY_REGISTERED, endpointUri),
                    this);
        }
        else {
            receiver = createReceiver(component, endpoint);
            receivers.put(getReceiverKey(component, endpoint), receiver);
        }
        return receiver;
    }

    /**
     * The method determines the key used to store the receiver against.
     *
     * @param component
     *            the component for which the endpoint is being registered
     * @param endpoint
     *            the endpoint being registered for the component
     * @return the key to store the newly created receiver against
     */
    protected Object getReceiverKey(UMOComponent component, UMOEndpoint endpoint)
    {
        return StringUtils.defaultIfEmpty(endpoint.getEndpointURI().getFilterAddress(), endpoint
                .getEndpointURI().getAddress());
    }

    public final void unregisterListener(UMOComponent component, UMOEndpoint endpoint) throws Exception
    {
        if (component == null) {
            throw new IllegalArgumentException(
                    "The component must not be null when you unregister a listener");
        }

        if (endpoint == null) {
            throw new IllegalArgumentException(
                    "The endpoint must not be null when you unregister a listener");
        }

        UMOEndpointURI endpointUri = endpoint.getEndpointURI();
        if (endpointUri == null) {
            throw new IllegalArgumentException(
                    "The endpointUri must not be null when you unregister a listener");
        }

        if (logger.isInfoEnabled()) {
            logger.info("removing listener on endpointUri: " + endpointUri);
        }

        UMOMessageReceiver receiver = (UMOMessageReceiver)receivers.remove(getReceiverKey(component,
                endpoint));
        if (receiver != null) {
            destroyReceiver(receiver, endpoint);
            receiver.dispose();
        }
    }

    public ThreadingProfile getDispatcherThreadingProfile()
    {
        if (dispatcherThreadingProfile == null) {
            dispatcherThreadingProfile = MuleManager.getConfiguration()
                    .getMessageDispatcherThreadingProfile();

        }
        return dispatcherThreadingProfile;
    }

    public void setDispatcherThreadingProfile(ThreadingProfile dispatcherThreadingProfile)
    {
        this.dispatcherThreadingProfile = dispatcherThreadingProfile;
    }

    public ThreadingProfile getReceiverThreadingProfile()
    {
        if (receiverThreadingProfile == null) {
            receiverThreadingProfile = MuleManager.getConfiguration()
                    .getMessageReceiverThreadingProfile();
        }
        return receiverThreadingProfile;
    }

    public void setReceiverThreadingProfile(ThreadingProfile receiverThreadingProfile)
    {
        this.receiverThreadingProfile = receiverThreadingProfile;
    }

    public abstract UMOMessageReceiver createReceiver(UMOComponent component, UMOEndpoint endpoint)
            throws Exception;

    public void destroyReceiver(UMOMessageReceiver receiver, UMOEndpoint endpoint) throws Exception
    {
        receiver.dispose();
    }

    /**
     * Template method to perform any work when starting the connectoe
     *
     * @throws UMOException
     *             if the method fails
     */
    protected void doStart() throws UMOException
    {
        // template method
    }

    /**
     * Template method to perform any work when stopping the connectoe
     *
     * @throws UMOException
     *             if the method fails
     */
    protected void doStop() throws UMOException
    {
        // template method
    }

    /**
     * Template method to perform any work when destroying the connectoe
     *
     */
    protected void doDispose()
    {
        try {
            stopConnector();
        }
        catch (UMOException e) {
            logger.warn("Failed to stop during shutdown: " + e.getMessage(), e);
        }
    }

    public void doInitialise() throws InitialisationException
    {
        // template method
    }

    public UMOTransformer getDefaultInboundTransformer()
    {
        if (defaultInboundTransformer != null) {
            try {
                return (UMOTransformer)defaultInboundTransformer.clone();
            }
            catch (CloneNotSupportedException e) {
                logger.error("Failed to clone default Inbound transformer");
                return null;
            }
        }
        else {
            return null;
        }
    }

    public void setDefaultInboundTransformer(UMOTransformer defaultInboundTransformer)
    {
        this.defaultInboundTransformer = defaultInboundTransformer;
    }

    public UMOTransformer getDefaultResponseTransformer()
    {
        if (defaultResponseTransformer != null) {
            try {
                return (UMOTransformer)defaultResponseTransformer.clone();
            }
            catch (CloneNotSupportedException e) {
                logger.error("Failed to clone default Outbound transformer");
                return null;
            }
        }
        else {
            return null;
        }
    }

    public UMOTransformer getDefaultOutboundTransformer()
    {
        if (defaultOutboundTransformer != null) {
            try {
                return (UMOTransformer)defaultOutboundTransformer.clone();
            }
            catch (CloneNotSupportedException e) {
                logger.error("Failed to clone default Outbound transformer");
                return null;
            }
        }
        else {
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

    /**
     * Fires a server notification to all registered
     * {@link org.mule.impl.internal.notifications.CustomNotificationListener}
     * eventManager.
     *
     * @param notification
     *            the notification to fire. This must be of type
     *            {@link org.mule.impl.internal.notifications.CustomNotification}
     *            otherwise an exception will be thrown.
     * @throws UnsupportedOperationException
     *             if the notification fired is not a
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
        try {
            return (ConnectionStrategy)BeanUtils.cloneBean(connectionStrategy);
        }
        catch (Exception e) {
            throw new MuleRuntimeException(
                    new Message(Messages.FAILED_TO_CLONE_X, "connectionStrategy"), e);
        }
    }

    public void setConnectionStrategy(ConnectionStrategy connectionStrategy)
    {
        this.connectionStrategy = connectionStrategy;
    }

    public List getEndpointMBeans()
    {
        // for now only return receiver endpoints as those are the ones we can
        // control
        // in terms of connecting/disconnecting
        List beans = new ArrayList(receivers.size());
        for (Iterator iterator = receivers.values().iterator(); iterator.hasNext();) {
            UMOMessageReceiver receiver = (UMOMessageReceiver)iterator.next();
            beans.add(new EndpointService(receiver));
        }
        return beans;
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

    public AbstractMessageReceiver getReceiver(String key)
    {
        return (AbstractMessageReceiver)receivers.get(key);
    }

    public AbstractMessageReceiver[] getReceivers(String wildcardExpression)
    {

        List temp = new ArrayList();
        WildcardFilter filter = new WildcardFilter(wildcardExpression);
        filter.setCaseSensitive(false);
        for (Iterator iterator = receivers.keySet().iterator(); iterator.hasNext();) {
            Object o = iterator.next();
            if (filter.accept(o)) {
                temp.add(receivers.get(o));
            }
        }
        AbstractMessageReceiver[] result = new AbstractMessageReceiver[temp.size()];
        return (AbstractMessageReceiver[])temp.toArray(result);
    }

    public void connect() throws Exception
    {
        if (connected.get()) {
            return;
        }
        checkDisposed();
        if (connecting.commit(false, true)) {
            connectionStrategy.connect(this);
            logger.info("Connected: " + getConnectionDescription());
            // This method calls itself so the the connecting vflag is set first, the the
            // connection is made on the second call
            return;
        }

        try {
            doConnect();
            fireNotification(new ConnectionNotification(this, getConnectEventId(),
                    ConnectionNotification.CONNECTION_CONNECTED));
        }
        catch (Exception e) {
            fireNotification(new ConnectionNotification(this, getConnectEventId(),
                    ConnectionNotification.CONNECTION_FAILED));
            if (e instanceof ConnectException) {
                throw (ConnectException)e;
            }
            else {
                throw new ConnectException(e, this);
            }
        }
        connected.set(true);
        connecting.set(false);
        if (startOnConnect.get()) {
            startConnector();
        }
        else {
            for (Iterator iterator = receivers.values().iterator(); iterator.hasNext();) {
                AbstractMessageReceiver amr = (AbstractMessageReceiver)iterator.next();
                if (logger.isDebugEnabled()) {
                    logger.debug("Connecting receiver on endpoint: "
                            + amr.getEndpoint().getEndpointURI());
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
        try {
            doDisconnect();
        }
        finally {
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
    public void doConnect() throws Exception
    {
        // template method
    }

    /**
     * Template method where any connected resources used by the connector should be
     * disconnected
     *
     * @throws Exception
     */
    public void doDisconnect() throws Exception
    {
        // template method
    }

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
     * controls whether dispatchers or cached or created per request Note that if an
     * exception occurs in the Dispatcher it is automatically disposed of and a new one is
     * created for the next request. This allows dispatchers to recover from loss of
     * connection and other faults.
     *
     * @param createDispatcherPerRequest
     *            whether a new dispatcher is created for every request or not
     */
    public void setCreateDispatcherPerRequest(boolean createDispatcherPerRequest)
    {
        this.createDispatcherPerRequest = createDispatcherPerRequest;
    }

    /**
     * controls whether dispatchers or cached or created per request Note that if an
     * exception occurs in the Dispatcher it is automatically disposed of and a new one is
     * created for the next request. This allows dispatchers to recover from loss of
     * connection and other faults.
     *
     * @return true if a anew dispatcher is created for every request
     */
    public boolean isCreateDispatcherPerRequest()
    {
        return createDispatcherPerRequest;
    }

    /**
     * For better throughput when using TransactedMessageReceivers. This will create an
     * number of receiver threads based on the ThreadingProfile configured fro the
     * receiver. This property is user by transports that support transactions,
     * specifically MessageReceivers that extend the TransactedPollingMessageReceiver.
     *
     * @return true if multiple receiver threads will be created for receivers on this
     *         connection
     */
    public boolean isCreateMultipleTransactedReceivers()
    {
        return createMultipleTransactedReceivers;
    }

    /**
     * For better throughput when using TransactedMessageReceivers. This will create an
     * number of receiver threads based on the ThreadingProfile configured fro the
     * receiver. This property is user by transports that support transactions,
     * specifically MessageReceivers that extend the TransactedPollingMessageReceiver.
     *
     * @param createMultipleTransactedReceivers
     *            true if multiple receiver threads will be created for receivers on this
     *            connection
     */
    public void setCreateMultipleTransactedReceivers(boolean createMultipleTransactedReceivers)
    {
        this.createMultipleTransactedReceivers = createMultipleTransactedReceivers;
    }

    /**
     * Whether to fire message notifications for every message that is sent or received
     * from this connector
     */
    public boolean isEnableMessageEvents()
    {
        return enableMessageEvents;
    }

    /**
     * Whether to fire message notifications for every message that is sent or received
     * from this connector
     *
     * @param enableMessageEvents
     */
    public void setEnableMessageEvents(boolean enableMessageEvents)
    {
        this.enableMessageEvents = enableMessageEvents;
    }

    /**
     * Registers other protocols 'understood' by this connector. These must contain scheme
     * meta info. Any protocol registered must begin with the protocol of this connector,
     * i.e. If the connector is axis the protocol for jms over axis will be axis:jms.
     * Here, 'axis' is the scheme meta info and 'jms' is the protocol. If the protocol
     * argument does not start with the connector's protocol, it will be appended.
     *
     * @param protocol
     *            the supported protocol to register
     */
    public void registerSupportedProtocol(String protocol)
    {
        protocol = protocol.toLowerCase();
        if (protocol.startsWith(getProtocol().toLowerCase())) {
            registerSupportedProtocolWithotPrefix(protocol);
        }
        else {
            supportedProtocols.add(getProtocol().toLowerCase() + ":" + protocol);
        }
    }

    /**
     * Registers other protocols 'understood' by this connector. These must contain scheme
     * meta info. Unlike the <code>registerSupportedProtolcol</code> method, this allows
     * you to register protocols that are not prefixed with the connector protocol. This
     * is useful where you use a Service Finder to discover which Transport implementation
     * to use. For example the 'wsdl' transport is a generic 'finder' transport that will
     * use Axis, Xfire or Glue to create the WSDL client. These transport protocols would
     * be wsdl-axis, wsdl-xfire and wsdl-glue, but they can all support 'wsdl' protocol
     * too.
     *
     * @param protocol
     *            the supported protocol to register
     */
    protected void registerSupportedProtocolWithotPrefix(String protocol)
    {
        supportedProtocols.add(protocol.toLowerCase());
    }

    public void unregisterSupportedProtocol(String protocol)
    {
        protocol = protocol.toLowerCase();
        if (protocol.startsWith(getProtocol().toLowerCase())) {
            supportedProtocols.remove(protocol);
        }
        else {
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
        for (Iterator iterator = supportedProtocols.iterator(); iterator.hasNext();) {
            String s = (String)iterator.next();
            registerSupportedProtocol(s);
        }
    }

    /**
     * Creates a work manager for a Message receiver. If
     * <code>useSingleReceiverThreadPool</code> has been set the same workManager of all
     * receivers will be used
     *
     * @param name
     *            The name to associate with the thread pool. No that the connector name
     *            will be prepended and ".receiver" will be appended
     * @return A new work manager of an existing one if the work manager is being shared
     */
    UMOWorkManager createReceiverWorkManager(String name)
    {
        UMOWorkManager wm;
        if (useSingleReceiverThreadPool && receiverWorkManager != null) {
            wm = receiverWorkManager;
        }
        else {
            ThreadingProfile tp = getReceiverThreadingProfile();
            if (serverSide) {
                tp.setThreadPriority(Thread.NORM_PRIORITY + 2);
            }
            wm = tp.createWorkManager(getName() + "." + name + ".receiver");
            if (useSingleReceiverThreadPool) {
                receiverWorkManager = wm;
            }
        }
        return wm;
    }

    /**
     * Creates a work manager for a Message dispatcher. If
     * <code>useSingleDispatcherThreadPool</code> has been set the same workManager of
     * all dispatchers will be used
     *
     * @param name
     *            The name to associate with the thread pool. No that the connector name
     *            will be prepended and ".dispatcher" will be appended
     * @return A new work manager of an existing one if the work manager is being shared
     */
    UMOWorkManager createDispatcherWorkManager(String name)
    {
        UMOWorkManager wm = null;
        if (useSingleDispatcherThreadPool && dispatcherWorkManager != null) {
            wm = dispatcherWorkManager;
        }
        else {
            ThreadingProfile tp = getReceiverThreadingProfile();
            wm = tp.createWorkManager(getName() + "." + name + ".dispatcher");
            if (useSingleDispatcherThreadPool) {
                dispatcherWorkManager = wm;
            }
        }
        return wm;
    }

    /**
     * Should a single receiver thread pool be created for all receivers It is recommended
     * that if you have a lot of receivers being registered per connector that this should
     * be set to true
     *
     * @return true is a single thread pool is being used for all receivers on this
     *         connector
     */
    public boolean isUseSingleReceiverThreadPool()
    {
        return useSingleReceiverThreadPool;
    }

    /**
     * Should a single dispatcher thread pool be created for all recivers It is
     * recommended that if you have a lot of receivers being registered per connector that
     * this should be set to true
     *
     * @param useSingleReceiverThreadPool
     *            true is a single thread pool is being used for all receivers on this
     *            connector
     */
    public void setUseSingleReceiverThreadPool(boolean useSingleReceiverThreadPool)
    {
        this.useSingleReceiverThreadPool = useSingleReceiverThreadPool;
    }

    /**
     * Should a single dispatcher thread pool be created for all distachers It is
     * recommended that if you have a lot of dispatcher being created per connector that
     * this should be set to true i.e. many different outbound endpoints
     *
     * @return true is a single thread pool is being used for all dispatchers on this
     *         connector
     */
    public boolean isUseSingleDispatcherThreadPool()
    {
        return useSingleDispatcherThreadPool;
    }

    /**
     * Should a single dispatcher thread pool be created for all distachers It is
     * recommended that if you have a lot of dispatcher being created per connector that
     * this should be set to true i.e. many different outbound endpoints
     *
     * @param useSingleDispatcherThreadPool
     *            true is a single thread pool is being used for all dispatchers on this
     *            connector
     */
    public void setUseSingleDispatcherThreadPool(boolean useSingleDispatcherThreadPool)
    {
        this.useSingleDispatcherThreadPool = useSingleDispatcherThreadPool;
    }

    /**
     * The flag determines if the connector is being used on the server side or client. If
     * true receiver threads will be given a slightly higher priority.
     *
     * @return true if running on the server side (default)
     */
    public boolean isServerSide()
    {
        return serverSide;
    }

    /**
     * The flag determines if the connector is being used on the server side or client. If
     * true receiver threads will be given a slightly higher priority.
     *
     * @param serverSide
     *            true if running on the server side
     */
    public void setServerSide(boolean serverSide)
    {
        this.serverSide = serverSide;
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
        if (event != null && event.getException() != null) {
            e = event.getException();
        }
        else {
            return;
        }
        if (event.getException().getCause() != null) {
            e = event.getException().getCause();
        }
        logger.error("Work caused exception on '" + type + "'. Work being executed was: "
                + event.getWork().toString());
        if (e instanceof Exception) {
            handleException((Exception)e);
        }
        else {
            throw new MuleRuntimeException(new Message(Messages.CONNECTOR_CAUSED_ERROR, getName()), e);
        }
    }
}
