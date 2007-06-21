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

import org.mule.MuleRuntimeException;
import org.mule.RegistryContext;
import org.mule.config.ThreadingProfile;
import org.mule.config.i18n.CoreMessages;
import org.mule.impl.AlreadyInitialisedException;
import org.mule.impl.DefaultExceptionStrategy;
import org.mule.impl.MuleSessionHandler;
import org.mule.impl.internal.notifications.ConnectionNotification;
import org.mule.providers.service.TransportFactory;
import org.mule.providers.service.TransportServiceDescriptor;
import org.mule.providers.service.TransportServiceException;
import org.mule.registry.DeregistrationException;
import org.mule.registry.RegistrationException;
import org.mule.registry.ServiceDescriptorFactory;
import org.mule.registry.ServiceException;
import org.mule.routing.filters.WildcardFilter;
import org.mule.umo.MessagingException;
import org.mule.umo.UMOComponent;
import org.mule.umo.UMOEvent;
import org.mule.umo.UMOException;
import org.mule.umo.UMOManagementContext;
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
import org.mule.util.BeanUtils;
import org.mule.util.ClassUtils;
import org.mule.util.CollectionUtils;
import org.mule.util.ObjectNameHelper;
import org.mule.util.ObjectUtils;
import org.mule.util.PropertiesUtils;
import org.mule.util.StringUtils;
import org.mule.util.concurrent.NamedThreadFactory;
import org.mule.util.concurrent.WaitableBoolean;

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

import edu.emory.mathcs.backport.java.util.concurrent.ConcurrentHashMap;
import edu.emory.mathcs.backport.java.util.concurrent.ConcurrentMap;
import edu.emory.mathcs.backport.java.util.concurrent.ScheduledExecutorService;
import edu.emory.mathcs.backport.java.util.concurrent.ScheduledThreadPoolExecutor;
import edu.emory.mathcs.backport.java.util.concurrent.ThreadFactory;
import edu.emory.mathcs.backport.java.util.concurrent.TimeUnit;
import edu.emory.mathcs.backport.java.util.concurrent.atomic.AtomicBoolean;
import edu.emory.mathcs.backport.java.util.concurrent.atomic.AtomicReference;
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
     * Default number of concurrent transactional receivers.
     */
    public static final int DEFAULT_NUM_CONCURRENT_TX_RECEIVERS = 4;

    /**
     * logger used by this class
     */
    protected final Log logger = LogFactory.getLog(getClass());

    /**
     * Specifies if the endpoint started
     */
    protected final AtomicBoolean started = new AtomicBoolean(false);

    /**
     * True once the endpoint has been initialsed
     */
    protected final AtomicBoolean initialised = new AtomicBoolean(false);

    /**
     * The name that identifies the endpoint
     */
    protected volatile String name;

    /**
     * The exception strategy used by this connector
     */
    protected volatile ExceptionListener exceptionListener;

    /**
     * Determines in the connector is alive and well
     */
    protected final AtomicBoolean disposed = new AtomicBoolean(false);

    /**
     * Determines in connector has been told to dispose
     */
    protected final AtomicBoolean disposing = new AtomicBoolean(false);

    /**
     * Factory used to create dispatchers for this connector
     */
    protected volatile UMOMessageDispatcherFactory dispatcherFactory;

    /**
     * A pool of dispatchers for this connector, keyed by endpoint
     */
    protected final GenericKeyedObjectPool dispatchers = new GenericKeyedObjectPool();

    /**
     * The collection of listeners on this connector. Keyed by entrypoint
     */
    protected final ConcurrentMap receivers = new ConcurrentHashMap();

    /**
     * Defines the dispatcher threading profile
     */
    private volatile ThreadingProfile dispatcherThreadingProfile = RegistryContext.getConfiguration()
        .getDefaultMessageDispatcherThreadingProfile();

    /**
     * Defines the receiver threading profile
     */
    private volatile ThreadingProfile receiverThreadingProfile = RegistryContext.getConfiguration()
        .getDefaultMessageReceiverThreadingProfile();

    /**
     * @see {@link #isCreateMultipleTransactedReceivers()}
     */
    protected volatile boolean createMultipleTransactedReceivers = true;

    /**
     * @see {@link #getNumberOfConcurrentTransactedReceivers()}
     */
    protected volatile int numberOfConcurrentTransactedReceivers = DEFAULT_NUM_CONCURRENT_TX_RECEIVERS;


    protected volatile ConnectionStrategy connectionStrategy;

    protected final WaitableBoolean connected = new WaitableBoolean(false);

    protected final WaitableBoolean connecting = new WaitableBoolean(false);

    /**
     * If the connect method was called via the start method, this will be set so
     * that when the connector comes on line it will be started
     */
    protected final WaitableBoolean startOnConnect = new WaitableBoolean(false);

    /**
     * Whether to fire message notifications for every message that is sent or
     * received from this connector
     */
    private volatile boolean enableMessageEvents;

    private final List supportedProtocols;

    /**
     * A shared work manager for all receivers registered with this connector.
     */
    private final AtomicReference/*<UMOWorkManager>*/ receiverWorkManager = new AtomicReference();

    /**
     * A shared work manager for all dispatchers created for this connector.
     */
    private final AtomicReference/*<UMOWorkManager>*/ dispatcherWorkManager = new AtomicReference();

    /**
     * A generic scheduling service for tasks that need to be performed periodically.
     */
    private final AtomicReference/*<ScheduledExecutorService>*/ scheduler = new AtomicReference();

    /**
     * Holds the service configuration for this connector
     */
    protected volatile TransportServiceDescriptor serviceDescriptor;

    /**
     * The map of service overrides that can e used to extend the capabilities of the
     * connector
     */
    protected volatile Properties serviceOverrides;

    /**
     * The strategy used for reading and writing session information to and fromt he
     * transport
     */
    protected volatile UMOSessionHandler sessionHandler = new MuleSessionHandler();

    /**
     * Registry ID
     */
    protected String registryId = null;

    protected UMOManagementContext managementContext;

    public AbstractConnector()
    {
        // make sure we always have an exception strategy
        exceptionListener = new DefaultExceptionStrategy();
        //Todo RM*
        //enableMessageEvents = RegistryContext.getConfiguration().isEnableMessageEvents();

        // always add at least the default protocol
        supportedProtocols = new ArrayList();
        supportedProtocols.add(getProtocol().toLowerCase());

        //TODO RM* URGENT
        connectionStrategy = new SingleAttemptConnectionStrategy();



        // TODO HH: dispatcher pool configuration needs to be extracted, maybe even
        // moved into the factory?
        // NOTE: testOnBorrow MUST be FALSE. this is a bit of a design bug in
        // commons-pool since validate is used for both activation and passivation,
        // but has no way of knowing which way it is going.
        dispatchers.setTestOnBorrow(false);
        dispatchers.setTestOnReturn(true);
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
            throw new IllegalArgumentException(CoreMessages.objectIsNull("Connector name").toString());
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
            throw new AlreadyInitialisedException("Connector '" + getProtocol() + "." + getName() + "'", this);
        }

        if (logger.isInfoEnabled())
        {
            logger.info("Initialising: " + this);
        }

        // Initialise the structure of this connector
        this.initFromServiceDescriptor();

        // we clearErrors out any registered dispatchers and receivers without resetting
        // the actual containers since this it might actually be a re-initialise
        // (e.g. as in JmsConnector)
        // TODO this should definitely not call dispose from this method. If anything,
        // extract the common parts, otherwise intialise() logs dispose messages (?!)
        this.disposeDispatchers();
        this.disposeReceivers();

        this.doInitialise();

        if (exceptionListener instanceof Initialisable)
        {
            ((Initialisable) exceptionListener).initialise();
        }

        initialised.set(true);
    }

    /*
     * (non-Javadoc)
     *
     * @see org.mule.umo.lifecycle.Registerable#register()
     */
    public void register() throws RegistrationException
    {
        //TODO
        registryId = RegistryContext.getRegistry().registerMuleObject(managementContext, this).getId();
    }

    /*
     * (non-Javadoc)
     *
     * @see org.mule.umo.lifecycle.Registerable#deregister()
     */
    public void deregister() throws DeregistrationException
    {
        RegistryContext.getRegistry().deregisterComponent(registryId);
        registryId = null;
    }

    /*
     * (non-Javadoc)
     *
     * @see org.mule.umo.lifecycle.Registerable#getRegistryId()
     */
    public String getRegistryId()
    {
        return registryId;
    }

    /*
     * (non-Javadoc)
     *
     * @see org.mule.umo.provider.UMOConnector#start()
     */
    public final synchronized void start() throws UMOException
    {
        this.checkDisposed();

        if (!this.isStarted())
        {
            //TODO: Not sure about this.  Right now the connector will connect only once
            // there is an endpoint associated with it and that endpoint is connected.
            // Do we also need the option of connecting the connector without any endpoints?
//            if (!this.isConnected())
//            {
//                startOnConnect.set(true);
//                this.getConnectionStrategy().connect(this);
//                // Only start once we are connected
//                return;
//            }
            if (!this.isConnected())
            {
                startOnConnect.set(true);
                // Don't call getConnectionStrategy(), it clones the connection strategy.
                // Connectors should have a single reconnection thread, unlike per receiver/dispatcher
                connectionStrategy.connect(this);
                // Only start once we are connected
                return;
            }

            if (logger.isInfoEnabled())
            {
                logger.info("Starting: " + this);
            }

            // the scheduler is recreated after stop()
            ScheduledExecutorService currentScheduler = (ScheduledExecutorService) scheduler.get();
            if (currentScheduler == null || currentScheduler.isShutdown())
            {
                scheduler.set(this.getScheduler());
            }

            this.doStart();
            started.set(true);

            if (receivers != null)
            {
                for (Iterator iterator = receivers.values().iterator(); iterator.hasNext();)
                {
                    UMOMessageReceiver mr = (UMOMessageReceiver) iterator.next();
                    if (logger.isDebugEnabled())
                    {
                        logger.debug("Starting receiver on endpoint: " + mr.getEndpoint().getEndpointURI());
                    }
                    mr.start();
                }
            }

            if (logger.isInfoEnabled())
            {
                logger.info("Started: " + this);
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
    public final synchronized void stop() throws UMOException
    {
        if (this.isDisposed())
        {
            return;
        }

        if (this.isStarted())
        {
            if (logger.isInfoEnabled())
            {
                logger.info("Stopping: " + this);
            }

            // shutdown our scheduler service
            ((ScheduledExecutorService) scheduler.get()).shutdown();

            this.doStop();
            started.set(false);

            // Stop all the receivers on this connector (this will cause them to
            // disconnect too)
            if (receivers != null)
            {
                for (Iterator iterator = receivers.values().iterator(); iterator.hasNext();)
                {
                    UMOMessageReceiver mr = (UMOMessageReceiver) iterator.next();
                    if (logger.isDebugEnabled())
                    {
                        logger.debug("Stopping receiver on endpoint: " + mr.getEndpoint().getEndpointURI());
                    }
                    mr.stop();
                }
            }
        }

        if (this.isConnected())
        {
            try
            {
                this.disconnect();
            }
            catch (Exception e)
            {
                // TODO MULE-863: What should we really do?
                logger.error("Failed to disconnect: " + e.getMessage(), e);
            }
        }

        // make sure the scheduler is gone
        scheduler.set(null);

        this.initialised.set(false);
        // started=false already issued above right after doStop()
        if (logger.isInfoEnabled())
        {
            logger.info("Stopped: " + this);
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
            logger.info("Disposing: " + this);
        }

        try
        {
            this.stop();
        }
        catch (UMOException e)
        {
            // TODO MULE-863: What should we really do?
            logger.warn("Failed to stop during shutdown: " + e.getMessage(), e);
        }

        this.disposeReceivers();
        this.disposeDispatchers();

        this.doDispose();
        disposed.set(true);

        if (logger.isInfoEnabled())
        {
            logger.info("Disposed: " + this);
        }
    }

    protected void disposeReceivers()
    {
        if (receivers != null)
        {
            logger.debug("Disposing Receivers");

            for (Iterator iterator = receivers.values().iterator(); iterator.hasNext();)
            {
                UMOMessageReceiver receiver = (UMOMessageReceiver) iterator.next();

                try
                {
                    this.destroyReceiver(receiver, receiver.getEndpoint());
                }
                catch (Throwable e)
                {
                    // TODO MULE-863: What should we really do?
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
                // disposeDispatcher() is also called from initialise() and
                // JMSConnector.onNotification(), so we need to handle this
                // differently
                if (this.isDisposing())
                {
                    // close() implies clear()
                    dispatchers.close();
                }
                else
                {
                    dispatchers.clear();
                }
            }
            catch (Exception ex)
            {
                // TODO MULE-863: What should we really do?
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
            throw new MuleRuntimeException(
                CoreMessages.exceptionOnConnectorNotExceptionListener(this.getName()), exception);
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
            poolFactory = (KeyedPoolableObjectFactory) dispatcherFactory;
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

    /**
     * Returns the maximum number of dispatchers that can be concurrently active per
     * endpoint.
     *
     * @return max. number of active dispatchers
     */
    public int getMaxDispatchersActive()
    {
        return this.dispatchers.getMaxActive();
    }

    /**
     * Configures the maximum number of dispatchers that can be concurrently active
     * per endpoint
     *
     * @param maxActive max. number of active dispatchers
     */
    public void setMaxDispatchersActive(int maxActive)
    {
        this.dispatchers.setMaxActive(maxActive);
        // adjust maxIdle in tandem to avoid thrashing
        this.dispatchers.setMaxIdle(maxActive);
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
            throw new IllegalArgumentException(
                CoreMessages.connectorSchemeIncompatibleWithEndpointScheme(this.getProtocol(),
                    endpoint.getEndpointURI().toString()).getMessage());
        }

        UMOMessageDispatcher dispatcher = null;
        try
        {
            if (logger.isDebugEnabled())
            {
                logger.debug("Borrowing a dispatcher for endpoint: " + endpoint.getEndpointURI());
            }

            dispatcher = (UMOMessageDispatcher)dispatchers.borrowObject(endpoint);

            if (logger.isDebugEnabled())
            {
                logger.debug("Borrowed a dispatcher for endpoint: " + endpoint.getEndpointURI() + " = "
                                + dispatcher.toString());
            }

            return dispatcher;
        }
        catch (Exception ex)
        {
            throw new ConnectorException(CoreMessages.connectorCausedError(), this, ex);
        }
        finally
        {
            try
            {
                if (logger.isDebugEnabled())
                {
                    logger.debug("Borrowed dispatcher: " + ObjectUtils.toString(dispatcher, "null"));
                }
            }
            catch (Exception ex)
            {
                throw new ConnectorException(CoreMessages.connectorCausedError(), this, ex);
            }
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

            }
            catch (Exception ex)
            {
                //Logging failed
            }
            finally
            {
                try
                {
                    dispatchers.returnObject(endpoint, dispatcher);
                }
                catch (Exception e)
                {
                    // TODO MULE-863: What should we really do?
                    // ignore - if the dispatcher is broken, it will likely get cleaned
                    // up by the factory
                    //RM* I think we should at least log this error so give some indication of what is failing
                    logger.error("Failed to dispose dispatcher for endpoint: " + endpoint +
                            ". This will cause a memory leak. Please report to", e);
                }
            }
        }
    }

    protected void checkDisposed() throws DisposeException
    {
        if (this.isDisposed())
        {
            throw new DisposeException(CoreMessages.cannotUseDisposedConnector(), this);
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
            throw new ConnectorException(CoreMessages.endpointIsNullForListener(), this);
        }

        logger.info("Registering listener: " + component.getDescriptor().getName() + " on endpointUri: "
                        + endpointUri.toString());

        UMOMessageReceiver receiver = this.getReceiver(component, endpoint);

        if (receiver != null)
        {
            throw new ConnectorException(CoreMessages.listenerAlreadyRegistered(endpointUri), this);
        }
        else
        {

            receiver = this.createReceiver(component, endpoint);
            Object receiverKey = getReceiverKey(component, endpoint);
            receiver.setReceiverKey(receiverKey.toString());
            receivers.put(receiverKey, receiver);
            // receivers.put(getReceiverKey(component, endpoint), receiver);
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

    /**
     * Getter for property 'dispatcherThreadingProfile'.
     *
     * @return Value for property 'dispatcherThreadingProfile'.
     */
    public ThreadingProfile getDispatcherThreadingProfile()
    {
        return dispatcherThreadingProfile;
    }

    /**
     * Setter for property 'dispatcherThreadingProfile'.
     *
     * @param dispatcherThreadingProfile Value to set for property
     *            'dispatcherThreadingProfile'.
     */
    public void setDispatcherThreadingProfile(ThreadingProfile dispatcherThreadingProfile)
    {
        this.dispatcherThreadingProfile = dispatcherThreadingProfile;
    }

    /**
     * Getter for property 'receiverThreadingProfile'.
     *
     * @return Value for property 'receiverThreadingProfile'.
     */
    public ThreadingProfile getReceiverThreadingProfile()
    {
        return receiverThreadingProfile;
    }

    /**
     * Setter for property 'receiverThreadingProfile'.
     *
     * @param receiverThreadingProfile Value to set for property
     *            'receiverThreadingProfile'.
     */
    public void setReceiverThreadingProfile(ThreadingProfile receiverThreadingProfile)
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
        try
        {
            UMOTransformer transformer = serviceDescriptor.createInboundTransformer();
            if(transformer==null) return null;
            transformer.initialise();
            return transformer;
        }
        catch (UMOException e)
        {
            logger.debug(e.getMessage(), e);
            return null;
        }
    }

    public UMOTransformer getDefaultResponseTransformer()
    {
        try
        {
            UMOTransformer transformer = serviceDescriptor.createResponseTransformer();
            if(transformer==null) return null;
            transformer.initialise();
            return transformer;
        }
        catch (UMOException e)
        {
            logger.debug(e.getMessage(), e);
            return null;
        }
    }

    public UMOTransformer getDefaultOutboundTransformer()
    {
        try
        {
            UMOTransformer transformer = serviceDescriptor.createOutboundTransformer();
            if(transformer==null) return null;
            transformer.initialise();
            return transformer;
        }
        catch (UMOException e)
        {
            logger.debug(e.getMessage(), e);
            return null;
        }

    }

    /**
     * Getter for property 'replyToHandler'.
     *
     * @return Value for property 'replyToHandler'.
     */
    public ReplyToHandler getReplyToHandler()
    {
        return new DefaultReplyToHandler(getDefaultResponseTransformer());
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
        managementContext.fireNotification(notification);
    }

    /**
     * Getter for property 'connectionStrategy'.
     *
     * @return Value for property 'connectionStrategy'.
     */
    //TODO RM* REMOVE
    public ConnectionStrategy getConnectionStrategy()
    {
        // not happy with this but each receiver needs its own instance
        // of the connection strategy and using a factory just introduces extra
        // implementation
        try
        {
            return (ConnectionStrategy) BeanUtils.cloneBean(connectionStrategy);
        }
        catch (Exception e)
        {
            throw new MuleRuntimeException(CoreMessages.failedToClone("connectionStrategy"), e);
        }
    }

    /**
     * Setter for property 'connectionStrategy'.
     *
     * @param connectionStrategy Value to set for property 'connectionStrategy'.
     */
    public void setConnectionStrategy(ConnectionStrategy connectionStrategy)
    {
        this.connectionStrategy = connectionStrategy;
    }

    /** {@inheritDoc} */
    public boolean isDisposing()
    {
        return disposing.get();
    }

    public boolean isRemoteSyncEnabled()
    {
        return false;
    }

    public UMOMessageReceiver getReceiver(UMOComponent component, UMOEndpoint endpoint)
    {
        return (UMOMessageReceiver) receivers.get(this.getReceiverKey(component, endpoint));
    }

    /**
     * Getter for property 'receivers'.
     *
     * @return Value for property 'receivers'.
     */
    public Map getReceivers()
    {
        return Collections.unmodifiableMap(receivers);
    }

    public UMOMessageReceiver lookupReceiver(String key)
    {
        if (key != null)
        {
            return (UMOMessageReceiver) receivers.get(key);
        }
        else
        {
            throw new IllegalArgumentException("Receiver key must not be null");
        }
    }

    public UMOMessageReceiver[] getReceivers(String wildcardExpression)
    {
        WildcardFilter filter = new WildcardFilter(wildcardExpression);
        filter.setCaseSensitive(false);

        List found = new ArrayList();

        for (Iterator iterator = receivers.entrySet().iterator(); iterator.hasNext();)
        {
            Map.Entry e = (Map.Entry) iterator.next();
            if (filter.accept(e.getKey()))
            {
                found.add(e.getValue());
            }
        }

        return (UMOMessageReceiver[]) CollectionUtils.toArrayOfComponentType(found,
            UMOMessageReceiver.class);
    }

    public void connect() throws Exception
    {
        this.checkDisposed();

        if (connected.get())
        {
            return;
        }

        /*
            Until the recursive startConnector() -> connect() -> doConnect() -> connect()
            calls are unwound between a connector and connection strategy, this call has
            to be here, and not below (commented out currently). Otherwise, e.g. WebspherMQ
            goes into an endless reconnect thrashing loop, see MULE-1150 for more details.
        */
        try
        {
            if (connecting.get())
            {
                this.doConnect();
            }
            if (connecting.compareAndSet(false, true))
            {
                if (logger.isDebugEnabled())
                {
                    logger.debug("Connecting: " + this);
                }

                connectionStrategy.connect(this);

                logger.info("Connected: " + getConnectionDescription());
                // This method calls itself so the connecting flag is set first, then
                // the connection is made on the second call
                return;
            }


            // see the explanation above
            //this.doConnect();
            connected.set(true);
            connecting.set(false);

            this.fireNotification(new ConnectionNotification(this, getConnectEventId(),
                ConnectionNotification.CONNECTION_CONNECTED));
        }
        catch (Exception e)
        {
            connected.set(false);
            connecting.set(false);

            this.fireNotification(new ConnectionNotification(this, getConnectEventId(),
                ConnectionNotification.CONNECTION_FAILED));

            if (e instanceof ConnectException || e instanceof FatalConnectException)
            {
                // rethrow
                throw e;
            }
            else
            {
                throw new ConnectException(e, this);
            }
        }

        if (startOnConnect.get())
        {
            this.start();
        }
        //TODO RM*. If the connection strategy is called on the receivers, the connector strategy gets called too,
        //to ensure its connected. Therefore the connect method on the connector needs to be idempotent and not try
        //and connect dispatchers or receivers

//        else
//        {
//            for (Iterator iterator = receivers.values().iterator(); iterator.hasNext();)
//            {
//                UMOMessageReceiver receiver = (UMOMessageReceiver)iterator.next();
//                if (logger.isDebugEnabled())
//                {
//                    logger.debug("Connecting receiver on endpoint: "
//                                    + receiver.getEndpoint().getEndpointURI());
//                }
//                receiver.connect();
//            }
//        }
    }

    public void disconnect() throws Exception
    {
        startOnConnect.set(this.isStarted());

        this.fireNotification(new ConnectionNotification(this, getConnectEventId(),
            ConnectionNotification.CONNECTION_DISCONNECTED));

        connected.set(false);

        try
        {
            this.doDisconnect();
        }
        finally
        {
            this.stop();
        }

        logger.info("Disconnected: " + this.getConnectionDescription());
    }

    public String getConnectionDescription()
    {
        return this.toString();
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
     *
     * @see #DEFAULT_NUM_CONCURRENT_TX_RECEIVERS
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
     * scheme meta info. Unlike the {@link #registerSupportedProtocol(String)} method,
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
            String s = (String) iterator.next();
            registerSupportedProtocol(s);
        }
    }

    /**
     * Returns a work manager for message receivers.
     */
    protected UMOWorkManager getReceiverWorkManager(String receiverName) throws UMOException
    {
        // lazily created because ThreadingProfile was not yet set in Constructor
        if (receiverWorkManager.get() == null)
        {
            UMOWorkManager newWorkManager = this.getReceiverThreadingProfile().createWorkManager(
                this.getName() + '.' + receiverName);

            if (receiverWorkManager.compareAndSet(null, newWorkManager))
            {
                newWorkManager.start();
            }
        }

        return (UMOWorkManager) receiverWorkManager.get();
    }

    /**
     * Returns a work manager for message dispatchers.
     *
     * @throws UMOException in case of error
     */
    protected UMOWorkManager getDispatcherWorkManager() throws UMOException
    {
        // lazily created because ThreadingProfile was not yet set in Constructor
        if (dispatcherWorkManager.get() == null)
        {
            UMOWorkManager newWorkManager = this.getDispatcherThreadingProfile().createWorkManager(
                getName() + ".dispatcher");

            if (dispatcherWorkManager.compareAndSet(null, newWorkManager))
            {
                newWorkManager.start();
            }
        }

        return (UMOWorkManager) dispatcherWorkManager.get();
    }

    /**
     * Returns a Scheduler service for periodic tasks, currently limited to internal
     * use. Note: getScheduler() currently conflicts with the same method in the
     * Quartz transport
     */
    public ScheduledExecutorService getScheduler()
    {
        if (scheduler.get() == null)
        {
            ThreadFactory threadFactory = new NamedThreadFactory(this.getName() + ".scheduler");
            ScheduledThreadPoolExecutor newExecutor = new ScheduledThreadPoolExecutor(1, threadFactory);
            newExecutor.setExecuteExistingDelayedTasksAfterShutdownPolicy(false);
            newExecutor.setKeepAliveTime(this.getReceiverThreadingProfile().getThreadTTL(),
                TimeUnit.MILLISECONDS);

            if (!scheduler.compareAndSet(null, newExecutor))
            {
                // someone else was faster, ditch our copy.
                newExecutor.shutdown();
            }
        }

        return (ScheduledExecutorService) scheduler.get();
    }

    /**
     * Getter for property 'sessionHandler'.
     *
     * @return Value for property 'sessionHandler'.
     */
    public UMOSessionHandler getSessionHandler()
    {
        return sessionHandler;
    }

    /**
     * Setter for property 'sessionHandler'.
     *
     * @param sessionHandler Value to set for property 'sessionHandler'.
     */
    public void setSessionHandler(UMOSessionHandler sessionHandler)
    {
        this.sessionHandler = sessionHandler;
    }

    public void workAccepted(WorkEvent event)
    {
        this.handleWorkException(event, "workAccepted");
    }

    public void workRejected(WorkEvent event)
    {
        this.handleWorkException(event, "workRejected");
    }

    public void workStarted(WorkEvent event)
    {
        this.handleWorkException(event, "workStarted");
    }

    public void workCompleted(WorkEvent event)
    {
        this.handleWorkException(event, "workCompleted");
    }

    protected void handleWorkException(WorkEvent event, String type)
    {
        if (event == null)
        {
            return;
        }

        Throwable e = event.getException();

        if (e == null)
        {
            return;
        }

        if (e.getCause() != null)
        {
            e = e.getCause();
        }

        logger.error("Work caused exception on '" + type + "'. Work being executed was: "
                        + event.getWork().toString());

        if (e instanceof Exception)
        {
            this.handleException((Exception) e);
        }
        else
        {
            throw new MuleRuntimeException(CoreMessages.connectorCausedError(this.getName()), e);
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
        return this.receive(getManagementContext().getRegistry().getOrCreateEndpointForUri(
                endpointUri.toString(), UMOImmutableEndpoint.ENDPOINT_TYPE_RECEIVER), timeout);
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
            throw new InitialisationException(
                CoreMessages.schemeNotCompatibleWithConnector(endpointUri.getFullScheme(),
                    this.getClass()), this);
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

        setName(ObjectNameHelper.getConnectorName(this));
        //initialise();
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
                RegistryContext.getRegistry().lookupServiceDescriptor(ServiceDescriptorFactory.PROVIDER_SERVICE_TYPE, getProtocol().toLowerCase(), serviceOverrides);
            if (serviceDescriptor == null)
            {
                throw new ServiceException(CoreMessages.noServiceTransportDescriptor(getProtocol()));
            }

            if (logger.isDebugEnabled())
            {
                logger.debug("Loading DispatcherFactory for connector: " + getName() + " (" + getClass().getName() + ")");
            }

            UMOMessageDispatcherFactory df = serviceDescriptor.createDispatcherFactory();
            if(df!=null)
            {
                this.setDispatcherFactory(df);
            }
            else if (logger.isDebugEnabled())
            {
                logger.debug("Transport '" + getProtocol() + "' will not support outbound endpoints: ");
            }


            sessionHandler = serviceDescriptor.createSessionHandler();

            // Set any manager default properties for the connector. These are set on
            // the Manager with a protocol e.g. jms.specification=1.1
            // This provides a really convenient way to set properties on an object
            // from unit tests
            Map props = new HashMap();
            PropertiesUtils.getPropertiesWithPrefix(managementContext.getRegistry().lookupProperties(), getProtocol()
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
            throw new MessagingException(CoreMessages.failedToCreate("Message Adapter"),
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
            throw new MessagingException(CoreMessages.failedToCreate("Stream Message Adapter"),
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
     * Will get the output stream for this type of transport. Typically this
     * will be called only when Streaming is being used on an outbound endpoint.
     * If Streaming is not supported by this transport an {@link UnsupportedOperationException}
     * is thrown.   Note that the stream MUST release resources on close.  For help doing so, see
     * {@link org.mule.impl.model.streaming.CallbackOutputStream}.
     *
     * @param endpoint the endpoint that releates to this Dispatcher
     * @param message the current message being processed
     * @return the output stream to use for this request
     * @throws UMOException in case of any error
     */
    public OutputStream getOutputStream(UMOImmutableEndpoint endpoint, UMOMessage message)
        throws UMOException
    {
        throw new UnsupportedOperationException(
            CoreMessages.streamingNotSupported(this.getProtocol()).toString());
    }

    public UMOManagementContext getManagementContext()
    {
        return managementContext;
    }

    public void setManagementContext(UMOManagementContext context)
    {
        this.managementContext = context;
    }

    // @Override
    public String toString()
    {
        final StringBuffer sb = new StringBuffer(120);
        sb.append(ClassUtils.getSimpleName(this.getClass()));
        sb.append("{this=").append(Integer.toHexString(System.identityHashCode(this)));
        sb.append(", started=").append(started);
        sb.append(", initialised=").append(initialised);
        sb.append(", name='").append(name).append('\'');
        sb.append(", disposed=").append(disposed);
        sb.append(", numberOfConcurrentTransactedReceivers=").append(numberOfConcurrentTransactedReceivers);
        sb.append(", createMultipleTransactedReceivers=").append(createMultipleTransactedReceivers);
        sb.append(", connected=").append(connected);
        sb.append(", supportedProtocols=").append(supportedProtocols);
        sb.append(", serviceOverrides=").append(serviceOverrides);
        sb.append(", registryId='").append(registryId).append('\'');
        sb.append('}');
        return sb.toString();
    }
}
