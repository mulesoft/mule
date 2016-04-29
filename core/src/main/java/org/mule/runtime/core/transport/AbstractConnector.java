/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.core.transport;

import static org.apache.commons.lang.SystemUtils.LINE_SEPARATOR;

import org.mule.runtime.core.AbstractAnnotatedObject;
import org.mule.runtime.core.MessageExchangePattern;
import org.mule.runtime.core.VoidMuleEvent;
import org.mule.runtime.core.api.DefaultMuleException;
import org.mule.runtime.core.api.MessagingException;
import org.mule.runtime.core.api.MuleContext;
import org.mule.runtime.core.api.MuleEvent;
import org.mule.runtime.core.api.MuleException;
import org.mule.runtime.core.api.MuleMessage;
import org.mule.runtime.core.api.MuleRuntimeException;
import org.mule.runtime.core.api.config.MuleProperties;
import org.mule.runtime.core.api.config.ThreadingProfile;
import org.mule.runtime.core.api.connector.Connectable;
import org.mule.runtime.core.api.connector.DispatchException;
import org.mule.runtime.core.api.connector.ReplyToHandler;
import org.mule.runtime.core.api.construct.FlowConstruct;
import org.mule.runtime.core.api.context.WorkManager;
import org.mule.runtime.core.api.context.WorkManagerSource;
import org.mule.runtime.core.api.context.notification.ServerNotification;
import org.mule.runtime.core.api.endpoint.EndpointURI;
import org.mule.runtime.core.api.endpoint.ImmutableEndpoint;
import org.mule.runtime.core.api.endpoint.InboundEndpoint;
import org.mule.runtime.core.api.endpoint.OutboundEndpoint;
import org.mule.runtime.core.api.lifecycle.CreateException;
import org.mule.runtime.core.api.lifecycle.Disposable;
import org.mule.runtime.core.api.lifecycle.InitialisationException;
import org.mule.runtime.core.api.lifecycle.LifecycleCallback;
import org.mule.runtime.core.api.lifecycle.LifecycleException;
import org.mule.runtime.core.api.lifecycle.LifecycleState;
import org.mule.runtime.core.api.processor.MessageProcessor;
import org.mule.runtime.core.api.registry.ServiceException;
import org.mule.runtime.core.api.registry.ServiceType;
import org.mule.runtime.core.api.retry.RetryCallback;
import org.mule.runtime.core.api.retry.RetryContext;
import org.mule.runtime.core.api.retry.RetryPolicyTemplate;
import org.mule.runtime.core.api.transaction.Transaction;
import org.mule.runtime.core.api.transaction.TransactionException;
import org.mule.runtime.core.api.transformer.Transformer;
import org.mule.runtime.core.api.transport.Connector;
import org.mule.runtime.core.api.transport.ConnectorException;
import org.mule.runtime.core.api.transport.MessageDispatcher;
import org.mule.runtime.core.api.transport.MessageDispatcherFactory;
import org.mule.runtime.core.api.transport.MessageReceiver;
import org.mule.runtime.core.api.transport.MessageRequester;
import org.mule.runtime.core.api.transport.MessageRequesterFactory;
import org.mule.runtime.core.api.transport.MuleMessageFactory;
import org.mule.runtime.core.config.i18n.CoreMessages;
import org.mule.runtime.core.config.i18n.MessageFactory;
import org.mule.runtime.core.connector.ConnectException;
import org.mule.runtime.core.connector.DefaultReplyToHandler;
import org.mule.runtime.core.context.notification.ConnectionNotification;
import org.mule.runtime.core.context.notification.EndpointMessageNotification;
import org.mule.runtime.core.context.notification.NotificationHelper;
import org.mule.runtime.core.endpoint.outbound.OutboundNotificationMessageProcessor;
import org.mule.runtime.core.message.SessionHandler;
import org.mule.runtime.core.model.streaming.DelegatingInputStream;
import org.mule.runtime.core.processor.AbstractRedeliveryPolicy;
import org.mule.runtime.core.processor.IdempotentRedeliveryPolicy;
import org.mule.runtime.core.processor.LaxAsyncInterceptingMessageProcessor;
import org.mule.runtime.core.processor.chain.SimpleMessageProcessorChainBuilder;
import org.mule.runtime.core.retry.async.AsynchronousRetryTemplate;
import org.mule.runtime.core.routing.filters.WildcardFilter;
import org.mule.runtime.core.session.SerializeAndEncodeSessionHandler;
import org.mule.runtime.core.transaction.TransactionCoordination;
import org.mule.runtime.core.transformer.TransformerUtils;
import org.mule.runtime.core.transport.service.TransportFactory;
import org.mule.runtime.core.transport.service.TransportServiceDescriptor;
import org.mule.runtime.core.transport.service.TransportServiceException;
import org.mule.runtime.core.util.BeanUtils;
import org.mule.runtime.core.util.ClassUtils;
import org.mule.runtime.core.util.CollectionUtils;
import org.mule.runtime.core.util.ObjectNameHelper;
import org.mule.runtime.core.util.ObjectUtils;
import org.mule.runtime.core.util.StringUtils;
import org.mule.runtime.core.util.concurrent.NamedThreadFactory;
import org.mule.runtime.core.util.concurrent.ThreadNameHelper;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

import javax.resource.spi.work.WorkEvent;
import javax.resource.spi.work.WorkListener;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.commons.pool.KeyedPoolableObjectFactory;
import org.apache.commons.pool.impl.GenericKeyedObjectPool;

/**
 * <code>AbstractConnector</code> provides base functionality for all connectors
 * provided with Mule. Connectors are the mechanism used to connect to external
 * systems and protocols in order to send and receive data.
 * <p/>
 * The <code>AbstractConnector</code> provides getter and setter methods for endpoint
 * name, transport name and protocol. It also provides methods to stop and start
 * connectors and sets up a dispatcher threadpool which allows deriving connectors
 * the possibility to dispatch work to separate threads. This functionality is
 * controlled with the <i> doThreading</i> property on the threadingProfiles for
 * dispatchers and receivers. The lifecycle for a connector is -
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
public abstract class AbstractConnector extends AbstractAnnotatedObject implements Connector, WorkListener
{
    /**
     * Default number of concurrent transactional receivers.
     */
    public static final int DEFAULT_NUM_CONCURRENT_TX_RECEIVERS = 4;

    private static final long SCHEDULER_FORCED_SHUTDOWN_TIMEOUT = 5000l;

    public static final String PROPERTY_POLLING_FREQUENCY = "pollingFrequency";
    public static final String DEFAULT_CONTEXT_START_TIMEOUT = "15000";
    public static final String MULE_CONTEXT_START_TIMEOUT_SYSTEM_PROPERTY = MuleProperties.SYSTEM_PROPERTY_PREFIX + "contextStartTimeout";

    /**
     * logger used by this class
     */
    protected final Log logger = LogFactory.getLog(getClass());

    /**
     * The name that identifies the endpoint
     */
    protected volatile String name;

    /**
     * Factory used to create dispatchers for this connector
     */
    protected volatile MessageDispatcherFactory dispatcherFactory;

    /**
     * Factory used to create requesters for this connector
     */
    protected volatile MessageRequesterFactory requesterFactory;

    /**
     * Factory used to create new {@link MuleMessage} instances
     */
    protected MuleMessageFactory muleMessageFactory;

    /**
     * A pool of dispatchers for this connector, keyed by endpoint
     */
    protected volatile ConfigurableKeyedObjectPool dispatchers;

    /**
     * A factory for creating the pool of dispatchers for this connector.
     */
    protected volatile ConfigurableKeyedObjectPoolFactory dispatcherPoolFactory;

    /**
     * A pool of requesters for this connector, keyed by endpoint
     */
    protected final GenericKeyedObjectPool requesters = new GenericKeyedObjectPool();

    /**
     * The collection of listeners on this connector. Keyed by entrypoint
     */
    protected final Map<Object, MessageReceiver> receivers = new ConcurrentHashMap<Object, MessageReceiver>();

    /**
     * Defines the dispatcher threading profile
     */
    private volatile ThreadingProfile dispatcherThreadingProfile;

    /**
     * Defines the requester threading profile
     */
    private volatile ThreadingProfile requesterThreadingProfile;

    /**
     * Defines the receiver threading profile
     */
    private volatile ThreadingProfile receiverThreadingProfile;

    /**
     * @see #isCreateMultipleTransactedReceivers()
     */
    protected volatile boolean createMultipleTransactedReceivers = true;

    /**
     * @see #getNumberOfConcurrentTransactedReceivers()
     */
    protected volatile int numberOfConcurrentTransactedReceivers = DEFAULT_NUM_CONCURRENT_TX_RECEIVERS;

    private RetryPolicyTemplate retryPolicyTemplate;

    /**
     * Optimise the handling of message notifications. If dynamic is set to false
     * then the cached notification handler implements a shortcut for message
     * notifications.
     */
    private boolean dynamicNotification = false;
    private NotificationHelper notificationHelper;

    private final List<String> supportedProtocols;

    /**
     * A shared work manager for all receivers registered with this connector.
     */
    private final AtomicReference<WorkManager> receiverWorkManager = new AtomicReference<WorkManager>();

    /**
     * A shared work manager for all requesters created for this connector.
     */
    private final AtomicReference<WorkManager> dispatcherWorkManager = new AtomicReference<WorkManager>();

    /**
     * A shared work manager for all requesters created for this connector.
     */
    private final AtomicReference<WorkManager> requesterWorkManager = new AtomicReference<WorkManager>();

    /**
     * A generic scheduling service for tasks that need to be performed periodically.
     */
    private ScheduledExecutorService scheduler;

    /**
     * Holds the service configuration for this connector
     */
    protected volatile TransportServiceDescriptor serviceDescriptor;

    /**
     * The map of service overrides that can be used to extend the capabilities of
     * the connector
     */
    protected volatile Properties serviceOverrides;

    /**
     * The strategy used for reading and writing session information to and from the
     * transport
     */
    protected volatile SessionHandler sessionHandler = new SerializeAndEncodeSessionHandler();

    protected MuleContext muleContext;

    protected ConnectorLifecycleManager lifecycleManager;

    // TODO connect and disconnect are not part of lifecycle management right now
    private AtomicBoolean connected = new AtomicBoolean(false);
    protected AtomicBoolean connecting = new AtomicBoolean(false);

    /**
     * Indicates whether the connector should start upon connecting. This is
     * necessary to support asynchronous retry policies, otherwise the start() method
     * would block until connection is successful.
     */
    protected boolean startOnConnect = false;

    /**
     * The will cause the connector not to start when {@link #start()} is called. The
     * only way to start the connector is to call
     * {@link #setInitialStateStopped(boolean)} with 'false' and then calling
     * {@link #start()}. This flag is used internally since some connectors that rely
     * on external servers may need to wait for that server to become available
     * before starting
     */
    protected boolean initialStateStopped = false;
    /**
     * Whether to test a connection on each take.
     */
    private boolean validateConnections = true;

    private Lock connectionLock = new ReentrantLock();

    public AbstractConnector(MuleContext context)
    {
        muleContext = context;
        lifecycleManager = new ConnectorLifecycleManager(this);

        setDynamicNotification(false);
        updateCachedNotificationHandler();

        // always add at least the default protocol
        supportedProtocols = new ArrayList<String>();
        supportedProtocols.add(getProtocol().toLowerCase());

        // TODO dispatcher pool configuration should be extracted, maybe even
        // moved into the factory?
        // NOTE: testOnBorrow MUST be FALSE. this is a bit of a design bug in
        // commons-pool since validate is used for both activation and passivation,
        // but has no way of knowing which way it is going.
        requesters.setTestOnBorrow(false);
        requesters.setTestOnReturn(true);
    }

    @Override
    public String getName()
    {
        return name;
    }

    @Override
    public void setName(String newName)
    {
        if (newName == null)
        {
            throw new IllegalArgumentException(CoreMessages.objectIsNull("Connector name").toString());
        }

        if (logger.isDebugEnabled())
        {
            logger.debug("Set Connector name to: " + newName);
        }

        name = newName;
    }

    //-----------------------------------------------------------------------------------------------//
    //-             LIFECYCLE METHODS
    //-----------------------------------------------------------------------------------------------//

    ConnectorLifecycleManager getLifecycleManager()
    {
        return lifecycleManager;
    }

    @Override
    public LifecycleState getLifecycleState()
    {
        return lifecycleManager.getState();
    }

    @Override
    public final synchronized void initialise() throws InitialisationException
    {
        try
        {
            lifecycleManager.fireInitialisePhase(new LifecycleCallback<Connector>()
            {
                @Override
                public void onTransition(String phaseName, Connector object) throws MuleException
                {
                    if (retryPolicyTemplate == null)
                    {
                        retryPolicyTemplate = muleContext.getRegistry().lookupObject(
                                MuleProperties.OBJECT_DEFAULT_RETRY_POLICY_TEMPLATE);
                    }

                    if (dispatcherPoolFactory == null) {
                        dispatcherPoolFactory = new DefaultConfigurableKeyedObjectPoolFactory();
                    }

                    dispatchers = dispatcherPoolFactory.createObjectPool();
                    if (dispatcherFactory != null) {
                        dispatchers.setFactory(getWrappedDispatcherFactory(dispatcherFactory));
                    }

                    // Initialise the structure of this connector
                    initFromServiceDescriptor();

                    initSessionHandler();

                    configureDispatcherPool();
                    setMaxRequestersActive(getRequesterThreadingProfile().getMaxThreadsActive());

                    try
                    {
                        initWorkManagers();
                    }
                    catch (MuleException e)
                    {
                        throw new LifecycleException(e, this);
                    }

                    doInitialise();
                }
            });
        }
        catch (InitialisationException e)
        {
            throw e;
        }
        catch (LifecycleException e)
        {
            throw new InitialisationException(e, this);
        }
        catch (MuleException e)
        {
            e.printStackTrace();
        }
    }

    private void initSessionHandler() throws InitialisationException
    {
        try
        {
            sessionHandler = muleContext.getInjector().inject(sessionHandler);
        }
        catch (MuleException e)
        {
            throw new InitialisationException(e, this);
        }
    }

    // Start (but we might not be connected yet).
    @Override
    public final synchronized void start() throws MuleException
    {
        if (isInitialStateStopped())
        {
            logger.info("Connector not started because 'initialStateStopped' is true");
            return;
        }

        if (!isConnected())
        {
            try
            {
                // startAfterConnect() will get called from the connect() method once connected.
                // This is necessary for reconnection strategies.
                startOnConnect = true;
                connect();
            }
            catch (MuleException me)
            {
                throw me;
            }
            catch (Exception e)
            {
                throw new ConnectException(e, this);
            }
        }
        else
        {
            startAfterConnect();
        }
    }

    // Start now that we're sure we're connected.
    protected synchronized void startAfterConnect() throws MuleException
    {
        // Reset this flag if it was set
        startOnConnect = false;

        // This breaks ConnectorLifecycleTestCase.testDoubleStartConnector()
        //if (isStarted())
        //{
        //    return;
        //}

        if (logger.isInfoEnabled())
        {
            logger.info("Starting: " + this);
        }

        lifecycleManager.fireStartPhase(new LifecycleCallback<Connector>()
        {
            @Override
            public void onTransition(String phaseName, Connector object) throws MuleException
            {
                initWorkManagers();
                scheduler = createScheduler();
                doStart();

                if (receivers != null)
                {
                    for (MessageReceiver receiver : receivers.values())
                    {
                        final List<MuleException> errors = new ArrayList<MuleException>();
                        try
                        {
                            if (logger.isDebugEnabled())
                            {
                                logger.debug("Starting receiver on endpoint: "
                                        + receiver.getEndpoint().getEndpointURI());
                            }
                            if (receiver.getFlowConstruct().getLifecycleState().isStarted())
                            {
                                receiver.start();
                            }
                            else if (retryPolicyTemplate instanceof AsynchronousRetryTemplate)
                            {
                                //we must be running on a different thread
                                String timeout = System.getProperty(MULE_CONTEXT_START_TIMEOUT_SYSTEM_PROPERTY, DEFAULT_CONTEXT_START_TIMEOUT);
                                if(!muleContext.waitUntilStarted(Integer.valueOf(timeout)))
                                {
                                    String errorMessage = "Timeout waiting for mule context to be completely started.";
                                    logger.error(errorMessage);
                                    errors.add(new DefaultMuleException(errorMessage));
                                }
                                else
                                {
                                    receiver.start();
                                }
                            }
                        }
                        catch (MuleException e)
                        {
                            logger.error(e);
                            errors.add(e);
                        }
                        catch (InterruptedException e)
                        {
                            Thread.currentThread().interrupt();
                            logger.error(e);
                            errors.add(new DefaultMuleException(e));
                        }

                        if (!errors.isEmpty())
                        {
                            // throw the first one in order not to break the reconnection
                            // strategy logic,
                            // every exception has been logged above already
                            // api needs refactoring to support the multi-cause exception
                            // here
                            throw errors.get(0);
                        }
                    }
                }
            }
        });
    }

    @Override
    public final synchronized void stop() throws MuleException
    {
        // This breaks ConnectorLifecycleTestCase.testDoubleStopConnector()
        //if (isStopped() || isStopping())
        //{
        //    return;
        //}

        lifecycleManager.fireStopPhase(new LifecycleCallback<Connector>()
        {
            @Override
            public void onTransition(String phaseName, Connector object) throws MuleException
            {
                 // shutdown our scheduler service
                shutdownScheduler();

                doStop();

                // Stop all the receivers on this connector
                if (receivers != null)
                {
                    for (MessageReceiver receiver : receivers.values())
                    {
                        if (logger.isDebugEnabled())
                        {
                            logger.debug("Stopping receiver on endpoint: " + receiver.getEndpoint().getEndpointURI());
                        }
                        receiver.stop();
                    }
                }

                // Now that dispatchers are borrowed/returned in worker thread we need to
                // dispose workManager before clearing object pools
                disposeWorkManagers();

                // Workaround for MULE-4553
                clearDispatchers();
                clearRequesters();

                // make sure the scheduler is gone
                scheduler = null;
            }
        });
    }

    @Override
    public final synchronized void dispose()
    {
        try
        {
            if (isStarted())
            {
                stop();
            }
            if (isConnected())
            {
                disconnect();
            }
        }
        catch (Exception e)
        {
            logger.warn(e.getMessage(), e);
        }

        try
        {
            lifecycleManager.fireDisposePhase(new LifecycleCallback<Connector>()
            {
                @Override
                public void onTransition(String phaseName, Connector object) throws MuleException
                {
                    doDispose();
                    disposeReceivers();
                }
            });
        }
        catch (MuleException e)
        {
            logger.warn(e.getMessage(), e);
        }
    }

    @Override
    public final boolean isStarted()
    {
        return lifecycleManager.getState().isStarted();
    }

    public final boolean isStarting()
    {
        return lifecycleManager.getState().isStarting();
    }

    public boolean isInitialised()
    {
        return lifecycleManager.getState().isInitialised();
    }

    public boolean isStopped()
    {
        return lifecycleManager.getState().isStopped();
    }

    public boolean isStopping()
    {
        return lifecycleManager.getState().isStopping();
    }


    //-----------------------------------------------------------------------------------------------//
    //-             END LIFECYCLE METHODS
    //-----------------------------------------------------------------------------------------------//

   protected void configureDispatcherPool()
   {
       // Normally having a the same maximum number of dispatcher objects as threads
       // is ok.
       int maxDispatchersActive = getDispatcherThreadingProfile().getMaxThreadsActive();

       // BUT if the WHEN_EXHAUSTED_RUN threading profile exhausted action is
       // configured then a single
       // additional dispatcher is required that can be used by the caller thread
       // when it executes job's itself.
       // Also See: MULE-4752
       if (ThreadingProfile.WHEN_EXHAUSTED_RUN == getDispatcherThreadingProfile().getPoolExhaustedAction())
       {
           maxDispatchersActive++;
       }
       setMaxDispatchersActive(maxDispatchersActive);
   }

    /**
     * <p>
     * Create a {@link MuleMessageFactory} from this connector's configuration,
     * typically through the transport descriptor.
     * </p>
     * <p/>
     * <b>Attention!</b> This method is not meant to be used by client code directly.
     * It is only publicly available to service message receivers which should be
     * used as <em>real</em> factories to create {@link MuleMessage} instances.
     *
     * @see MessageReceiver#createMuleMessage(Object)
     * @see MessageReceiver#createMuleMessage(Object, String)
     */
    @Override
    public MuleMessageFactory createMuleMessageFactory() throws CreateException
    {
        try
        {
            return serviceDescriptor.createMuleMessageFactory();
        }
        catch (TransportServiceException tse)
        {
            throw new CreateException(CoreMessages.failedToCreate("MuleMessageFactory"), tse, this);
        }
    }

    protected void shutdownScheduler()
    {
        if (scheduler != null)
        {
            // Disable new tasks from being submitted
            scheduler.shutdown();
            try
            {
                // Wait a while for existing tasks to terminate
                if (!scheduler.awaitTermination(muleContext.getConfiguration().getShutdownTimeout(),
                        TimeUnit.MILLISECONDS))
                {
                    // Cancel currently executing tasks and return list of pending
                    // tasks
                    List outstanding = scheduler.shutdownNow();
                    // Wait a while for tasks to respond to being cancelled
                    if (!scheduler.awaitTermination(SCHEDULER_FORCED_SHUTDOWN_TIMEOUT, TimeUnit.MILLISECONDS))
                    {
                        logger.warn(MessageFormat.format(
                                "Pool {0} did not terminate in time; {1} work items were cancelled.", name,
                                outstanding.isEmpty() ? "No" : Integer.toString(outstanding.size())));
                    }
                    else
                    {
                        if (!outstanding.isEmpty())
                        {
                            logger.warn(MessageFormat.format(
                                    "Pool {0} terminated; {1} work items were cancelled.", name,
                                    Integer.toString(outstanding.size())));
                        }
                    }

                }
            }
            catch (InterruptedException ie)
            {
                // (Re-)Cancel if current thread also interrupted
                scheduler.shutdownNow();
                // Preserve interrupt status
                Thread.currentThread().interrupt();
            }
            finally
            {
                scheduler = null;
            }
        }
    }

    protected void initWorkManagers() throws MuleException
    {
        if (receiverWorkManager.get() == null)
        {

            final String threadPrefix = ThreadNameHelper.receiver(muleContext, getName());
            WorkManager newWorkManager = this.getReceiverThreadingProfile().createWorkManager(
                    threadPrefix, muleContext.getConfiguration().getShutdownTimeout());

            if (receiverWorkManager.compareAndSet(null, newWorkManager))
            {
                newWorkManager.start();
            }
        }
        if (dispatcherWorkManager.get() == null)
        {
            ThreadingProfile dispatcherThreadingProfile = this.getDispatcherThreadingProfile();
            if (dispatcherThreadingProfile.getMuleContext() == null)
            {
                dispatcherThreadingProfile.setMuleContext(muleContext);
            }

            final String threadPrefix = ThreadNameHelper.dispatcher(muleContext, getName());
            WorkManager newWorkManager = dispatcherThreadingProfile.createWorkManager(
                    threadPrefix, muleContext.getConfiguration().getShutdownTimeout());

            if (dispatcherWorkManager.compareAndSet(null, newWorkManager))
            {
                newWorkManager.start();
            }
        }
        if (requesterWorkManager.get() == null)
        {
            final String threadPrefix = ThreadNameHelper.requester(muleContext, getName());
            WorkManager newWorkManager = this.getRequesterThreadingProfile().createWorkManager(
                    threadPrefix, muleContext.getConfiguration().getShutdownTimeout());

            if (requesterWorkManager.compareAndSet(null, newWorkManager))
            {
                newWorkManager.start();
            }
        }
    }

    protected void disposeWorkManagers()
    {
        WorkManager workManager;

        logger.debug("Disposing dispatcher work manager");
        workManager = dispatcherWorkManager.get();
        if (workManager != null)
        {
            workManager.dispose();
        }
        dispatcherWorkManager.set(null);

        logger.debug("Disposing requester work manager");
        workManager = requesterWorkManager.get();
        if (workManager != null)
        {
            workManager.dispose();
        }
        requesterWorkManager.set(null);

        logger.debug("Disposing receiver work manager");
        workManager = receiverWorkManager.get();
        if (workManager != null)
        {
            workManager.dispose();
        }
        receiverWorkManager.set(null);
    }

    protected void disposeReceivers()
    {
        if (receivers != null)
        {
            logger.debug("Disposing Receivers");

            for (MessageReceiver receiver : receivers.values())
            {
                try
                {
                    this.destroyReceiver(receiver, receiver.getEndpoint());
                }
                catch (Throwable e)
                {
                    // Just log when disposing
                    logger.error("Failed to destroy receiver: " + receiver, e);
                }
            }

            receivers.clear();
            logger.debug("Receivers Disposed");
        }
    }

    protected void clearDispatchers()
    {
        if (dispatchers != null)
        {
            logger.debug("Clearing Dispatcher pool");
            synchronized (dispatchers)
            {
                dispatchers.clear();
            }
            logger.debug("Dispatcher pool cleared");
        }
    }

    protected void clearRequesters()
    {
        if (requesters != null)
        {
            logger.debug("Clearing Requester pool");
            requesters.clear();
            logger.debug("Requester pool cleared");
        }
    }

    @Override
    public boolean isDisposed()
    {
        return lifecycleManager.getState().isDisposed();
    }

    /**
     * @return Returns the dispatcherFactory.
     */
    @Override
    public MessageDispatcherFactory getDispatcherFactory()
    {
        return dispatcherFactory;
    }

    /**
     * @param dispatcherFactory The dispatcherFactory to set.
     */
    @Override
    public void setDispatcherFactory(MessageDispatcherFactory dispatcherFactory)
    {
        KeyedPoolableObjectFactory poolFactory = getWrappedDispatcherFactory(dispatcherFactory);

        if (dispatchers !=  null) {
            this.dispatchers.setFactory(poolFactory);
        }

        // we keep a reference to the unadapted factory, otherwise people might end
        // up with ClassCastExceptions on downcast to their implementation (sigh)
        this.dispatcherFactory = dispatcherFactory;
    }

    private KeyedPoolableObjectFactory getWrappedDispatcherFactory(MessageDispatcherFactory dispatcherFactory)
    {
        KeyedPoolableObjectFactory poolFactory;
        if (dispatcherFactory instanceof KeyedPoolableObjectFactory)
        {
            poolFactory = (KeyedPoolableObjectFactory) dispatcherFactory;
        }
        else
        {
            // need to adapt the factory for use by commons-pool
            poolFactory = new KeyedPoolMessageDispatcherFactoryAdapter(dispatcherFactory);
        }

        return poolFactory;
    }

    /**
     * @return Returns the requesterFactory.
     */
    @Override
    public MessageRequesterFactory getRequesterFactory()
    {
        return requesterFactory;
    }

    /**
     * @param requesterFactory The requesterFactory to set.
     */
    @Override
    public void setRequesterFactory(MessageRequesterFactory requesterFactory)
    {
        KeyedPoolableObjectFactory poolFactory;

        if (requesterFactory instanceof KeyedPoolableObjectFactory)
        {
            poolFactory = (KeyedPoolableObjectFactory) requesterFactory;
        }
        else
        {
            // need to adapt the factory for use by commons-pool
            poolFactory = new KeyedPoolMessageRequesterFactoryAdapter(requesterFactory);
        }

        requesters.setFactory(poolFactory);

        // we keep a reference to the unadapted factory, otherwise people might end
        // up with ClassCastExceptions on downcast to their implementation (sigh)
        this.requesterFactory = requesterFactory;
    }

    /**
     * <p>The connector creates a {@link MuleMessageFactory} lazily and holds a reference to it for
     * others to use.</p>
     * <p>The typical use case is to share a single {@link MuleMessageFactory} between all
     * {@link MessageDispatcher}, {@link MessageReceiver} and {@link MessageRequester} instances
     * belonging to this connector.</p>
     */
    public MuleMessageFactory getMuleMessageFactory() throws CreateException
    {
        if (muleMessageFactory == null)
        {
            muleMessageFactory = createMuleMessageFactory();
        }
        return muleMessageFactory;
    }

    /**
     * The will cause the connector not to start when {@link #start()} is called. The
     * only way to start the connector is to call
     * {@link #setInitialStateStopped(boolean)} with 'false' and then calling
     * {@link #start()}. This flag is used internally since some connectors that rely
     * on external servers may need to wait for that server to become available
     * before starting.
     *
     * @return true if the connector is not to be started with normal lifecycle,
     *         flase otherwise
     * @since 3.0.0
     */
    public boolean isInitialStateStopped()
    {
        return initialStateStopped;
    }

    /**
     * The will cause the connector not to start when {@link #start()} is called. The
     * only way to start the connector is to call
     * {@link #setInitialStateStopped(boolean)} with 'false' and then calling
     * {@link #start()}. This flag is used internally since some connectors that rely
     * on external servers may need to wait for that server to become available
     * before starting. The only time this method should be used is when a
     * subclassing connector needs to delay the start lifecycle due to a dependence
     * on an external system. Most users can ignore this.
     *
     * @param initialStateStopped true to stop the connector starting through normal lifecycle. It will
     *             be the responsibility of the code that sets this property to start the
     *             connector
     * @since 3.0.0
     */
    public void setInitialStateStopped(boolean initialStateStopped)
    {
        this.initialStateStopped = initialStateStopped;
    }

    /**
     * Returns the maximum number of dispatchers that can be concurrently active per
     * endpoint.
     *
     * @return max. number of active dispatchers
     */
    public int getMaxDispatchersActive()
    {
        checkDispatchersInitialised();
        return this.dispatchers.getMaxActive();
    }

    /**
     * Checks if the connector was initialised or not. Some connectors fields are created
     * during connector's initialization so this check should be used before accessing them.
     */
    private void checkDispatchersInitialised()
    {
        if (dispatchers == null)
        {
            throw new IllegalStateException("Dispatchers pool was not initialised");
        }
    }

    /**
     * Returns the maximum number of dispatchers that can be concurrently active for
     * all endpoints.
     *
     * @return max. total number of active dispatchers
     */
    public int getMaxTotalDispatchers()
    {
        checkDispatchersInitialised();
        return this.dispatchers.getMaxTotal();
    }

    /**
     * Configures the maximum number of dispatchers that can be concurrently active
     * per endpoint
     *
     * @param maxActive max. number of active dispatchers
     */
    public void setMaxDispatchersActive(int maxActive)
    {
        checkDispatchersInitialised();
        this.dispatchers.setMaxActive(maxActive);
        // adjust maxIdle in tandem to avoid thrashing
        this.dispatchers.setMaxIdle(maxActive);
        // this tells the pool to expire some objects eventually if we start
        // running out. This happens if one is using a lot of dynamic endpoints.
        this.dispatchers.setMaxTotal(20 * maxActive);
    }

    protected MessageDispatcher borrowDispatcher(OutboundEndpoint endpoint) throws MuleException
    {
        if (!isStarted())
        {
            throw new LifecycleException(CoreMessages.lifecycleErrorCannotUseConnector(getName(),
                    lifecycleManager.getCurrentPhase()), this);
        }

        if (endpoint == null)
        {
            throw new IllegalArgumentException("Endpoint must not be null");
        }

        if (!supportsProtocol(endpoint.getConnector().getProtocol()))
        {
            throw new IllegalArgumentException(CoreMessages.connectorSchemeIncompatibleWithEndpointScheme(
                    this.getProtocol(), endpoint.getEndpointURI().toString()).getMessage());
        }

        MessageDispatcher dispatcher = null;
        try
        {
            if (logger.isDebugEnabled())
            {
                logger.debug("Borrowing a dispatcher for endpoint: " + endpoint.getEndpointURI());
            }

            dispatcher = (MessageDispatcher) dispatchers.borrowObject(endpoint);

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

    protected void returnDispatcher(OutboundEndpoint endpoint, MessageDispatcher dispatcher)
    {
        if (endpoint != null && dispatcher != null)
        {
            if (logger.isDebugEnabled())
            {
                logger.debug("Returning dispatcher for endpoint: " + endpoint.getEndpointURI() + " = "
                        + dispatcher.toString());
            }

            try
            {
                dispatchers.returnObject(endpoint, dispatcher);
            }
            catch (Exception e)
            {
                // ignore - if the dispatcher is broken, it will likely get cleaned
                // up by the factory
                logger.error("Failed to dispose dispatcher for endpoint: " + endpoint
                        + ". This will cause a memory leak. Please report to", e);
            }

        }
    }

    /**
     * Returns the maximum number of requesters that can be concurrently active per
     * endpoint.
     *
     * @return max. number of active requesters
     */
    public int getMaxRequestersActive()
    {
        return this.requesters.getMaxActive();
    }

    /**
     * Configures the maximum number of requesters that can be concurrently active
     * per endpoint
     *
     * @param maxActive max. number of active requesters
     */
    public void setMaxRequestersActive(int maxActive)
    {
        this.requesters.setMaxActive(maxActive);
        // adjust maxIdle in tandem to avoid thrashing
        this.requesters.setMaxIdle(maxActive);
        // this tells the pool to expire some objects eventually if we start
        // running out. This happens if one is using a lot of dynamic endpoints.
        this.requesters.setMaxTotal(20 * maxActive);
    }

    private MessageRequester getRequester(InboundEndpoint endpoint) throws MuleException
    {
        if (!isStarted())
        {
            throw new LifecycleException(CoreMessages.lifecycleErrorCannotUseConnector(getName(),
                    lifecycleManager.getCurrentPhase()), this);
        }

        if (endpoint == null)
        {
            throw new IllegalArgumentException("Endpoint must not be null");
        }

        if (!supportsProtocol(endpoint.getConnector().getProtocol()))
        {
            throw new IllegalArgumentException(CoreMessages.connectorSchemeIncompatibleWithEndpointScheme(
                    this.getProtocol(), endpoint.getEndpointURI().toString()).getMessage());
        }

        MessageRequester requester = null;
        try
        {
            if (logger.isDebugEnabled())
            {
                logger.debug("Borrowing a requester for endpoint: " + endpoint.getEndpointURI());
            }

            requester = (MessageRequester) requesters.borrowObject(endpoint);

            if (logger.isDebugEnabled())
            {
                logger.debug("Borrowed a requester for endpoint: " + endpoint.getEndpointURI() + " = "
                        + requester.toString());
            }

            return requester;
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
                    logger.debug("Borrowed requester: " + ObjectUtils.toString(requester, "null"));
                }
            }
            catch (Exception ex)
            {
                throw new ConnectorException(CoreMessages.connectorCausedError(), this, ex);
            }
        }
    }

    private void returnRequester(InboundEndpoint endpoint, MessageRequester requester)
    {
        if (endpoint != null && requester != null)
        {
            if (logger.isDebugEnabled())
            {
                logger.debug("Returning requester for endpoint: " + endpoint.getEndpointURI() + " = "
                        + requester.toString());
            }

            try
            {
                requesters.returnObject(endpoint, requester);
            }
            catch (Exception e)
            {
                // ignore - if the requester is broken, it will likely get cleaned
                // up by the factory
                logger.error("Failed to dispose requester for endpoint: " + endpoint
                        + ". This will cause a memory leak. Please report to", e);
            }
        }

    }

    @Override
    public void registerListener(InboundEndpoint endpoint,
                                 MessageProcessor messageProcessorChain,
                                 FlowConstruct flowConstruct) throws Exception
    {
        connectionLock.lock();
        try
        {
            if (endpoint == null)
            {
                throw new IllegalArgumentException("The endpoint cannot be null when registering a listener");
            }

            if (messageProcessorChain == null)
            {
                throw new IllegalArgumentException("The messageProcessorChain cannot be null when registering a listener");
            }

            EndpointURI endpointUri = endpoint.getEndpointURI();
            if (endpointUri == null)
            {
                throw new ConnectorException(CoreMessages.endpointIsNullForListener(), this);
            }

            logger.info("Registering listener: " + flowConstruct.getName() + " on endpointUri: "
                    + endpointUri.toString());

            if (getReceiver(flowConstruct, endpoint) != null)
            {
                throw new ConnectorException(CoreMessages.listenerAlreadyRegistered(endpointUri), this);
            }

            MessageReceiver receiver = createReceiver(flowConstruct, endpoint);
            receiver.setListener(messageProcessorChain);

            Object receiverKey = getReceiverKey(flowConstruct, endpoint);
            receiver.setReceiverKey(receiverKey.toString());
            // Since we're managing the creation we also need to initialise
            receiver.initialise();
            receivers.put(receiverKey, receiver);

            if (isConnected())
            {
                receiver.connect();
            }

            if (isStarted())
            {
                receiver.start();
            }
        }
        finally
        {
            connectionLock.unlock();
        }
    }

    /**
     * The method determines the key used to store the receiver against.
     *
     * @param flowConstruct the service for which the endpoint is being registered
     * @param endpoint      the endpoint being registered for the service
     * @return the key to store the newly created receiver against
     */
    protected Object getReceiverKey(FlowConstruct flowConstruct, InboundEndpoint endpoint)
    {
        return StringUtils.defaultIfEmpty(endpoint.getEndpointURI().getFilterAddress(),
                endpoint.getEndpointURI().getAddress());
    }

    @Override
    public final void unregisterListener(InboundEndpoint endpoint, FlowConstruct flowConstruct) throws Exception
    {
        if (endpoint == null)
        {
            throw new IllegalArgumentException("The endpoint must not be null when you unregister a listener");
        }

        EndpointURI endpointUri = endpoint.getEndpointURI();
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
            MessageReceiver receiver = receivers.remove(getReceiverKey(flowConstruct, endpoint));
            if (receiver != null)
            {
                // This will automatically stop and disconnect before disposing.
                destroyReceiver(receiver, endpoint);
                doUnregisterListener(flowConstruct, endpoint, receiver);
            }
        }
    }

    protected void doUnregisterListener(FlowConstruct flowConstruct, InboundEndpoint endpoint, MessageReceiver receiver)
    {
        // Template method
    }

    /**
     * Getter for property 'dispatcherThreadingProfile'.
     *
     * @return Value for property 'dispatcherThreadingProfile'.
     */
    public ThreadingProfile getDispatcherThreadingProfile()
    {
        if (dispatcherThreadingProfile == null && muleContext != null)
        {
            dispatcherThreadingProfile = muleContext.getDefaultMessageDispatcherThreadingProfile();
        }
        return dispatcherThreadingProfile;
    }

    /**
     * Setter for property 'dispatcherThreadingProfile'.
     *
     * @param dispatcherThreadingProfile Value to set for property
     *                                   'dispatcherThreadingProfile'.
     */
    public void setDispatcherThreadingProfile(ThreadingProfile dispatcherThreadingProfile)
    {
        this.dispatcherThreadingProfile = dispatcherThreadingProfile;
    }

    /**
     * Getter for property 'requesterThreadingProfile'.
     *
     * @return Value for property 'requesterThreadingProfile'.
     */
    public ThreadingProfile getRequesterThreadingProfile()
    {
        if (requesterThreadingProfile == null && muleContext != null)
        {
            requesterThreadingProfile = muleContext.getDefaultMessageRequesterThreadingProfile();
        }
        return requesterThreadingProfile;
    }

    /**
     * Setter for property 'requesterThreadingProfile'.
     *
     * @param requesterThreadingProfile Value to set for property
     *                                  'requesterThreadingProfile'.
     */
    public void setRequesterThreadingProfile(ThreadingProfile requesterThreadingProfile)
    {
        this.requesterThreadingProfile = requesterThreadingProfile;
    }

    /**
     * Getter for property 'receiverThreadingProfile'.
     *
     * @return Value for property 'receiverThreadingProfile'.
     */
    public ThreadingProfile getReceiverThreadingProfile()
    {
        if (receiverThreadingProfile == null && muleContext != null)
        {
            receiverThreadingProfile = muleContext.getDefaultMessageReceiverThreadingProfile();
        }
        return receiverThreadingProfile;
    }

    /**
     * Setter for property 'receiverThreadingProfile'.
     *
     * @param receiverThreadingProfile Value to set for property
     *                                 'receiverThreadingProfile'.
     */
    public void setReceiverThreadingProfile(ThreadingProfile receiverThreadingProfile)
    {
        this.receiverThreadingProfile = receiverThreadingProfile;
    }

    public void destroyReceiver(MessageReceiver receiver, ImmutableEndpoint endpoint) throws Exception
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
     * @throws MuleException if the method fails
     */
    protected abstract void doStart() throws MuleException;

    /**
     * Template method to perform any work when stopping the connectoe
     *
     * @throws MuleException if the method fails
     */
    protected abstract void doStop() throws MuleException;

    public List<Transformer> getDefaultInboundTransformers(ImmutableEndpoint endpoint)
    {
        if (serviceDescriptor == null)
        {
            throw new RuntimeException("serviceDescriptor not initialized");
        }
        return TransformerUtils.getDefaultInboundTransformers(serviceDescriptor, endpoint);
    }

    public List<Transformer> getDefaultResponseTransformers(ImmutableEndpoint endpoint)
    {
        if (serviceDescriptor == null)
        {
            throw new RuntimeException("serviceDescriptor not initialized");
        }
        return TransformerUtils.getDefaultResponseTransformers(serviceDescriptor, endpoint);
    }

    public List<Transformer> getDefaultOutboundTransformers(ImmutableEndpoint endpoint)
    {
        if (serviceDescriptor == null)
        {
            throw new RuntimeException("serviceDescriptor not initialized");
        }
        return TransformerUtils.getDefaultOutboundTransformers(serviceDescriptor, endpoint);
    }

    /**
     * Getter for property 'replyToHandler'.
     *
     * @return Value for property 'replyToHandler'.
     */
    public ReplyToHandler getReplyToHandler(ImmutableEndpoint endpoint)
    {
        return new DefaultReplyToHandler(endpoint.getMuleContext());
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String getCanonicalURI(EndpointURI uri)
    {
        return uri.toString();
    }

    /**
     * Fires a server notification to all registered listeners
     *
     * @param notification the notification to fire.
     */
    public void fireNotification(ServerNotification notification)
    {
        notificationHelper.fireNotification(notification);
    }

    /**
     * Fires a server notification to all registered listeners
     * on the {@link MuleContext} of the given {@code event}
     *
     * @param notification the notification to fire.
     * @param event        a {@link MuleEvent}
     */
    public void fireNotification(ServerNotification notification, MuleEvent event)
    {
        notificationHelper.fireNotification(notification, event);
    }

    @Override
    public boolean isResponseEnabled()
    {
        return false;
    }

    public MessageReceiver getReceiver(FlowConstruct flowConstruct, InboundEndpoint endpoint)
    {
        if (receivers != null)
        {
            Object key = getReceiverKey(flowConstruct, endpoint);
            if (key != null)
            {
                return receivers.get(key);
            }
            else
            {
                throw new RuntimeException("getReceiverKey() returned a null key");
            }
        }
        else
        {
            throw new RuntimeException("Connector has not been initialized.");
        }
    }

    /**
     * Getter for property 'receivers'.
     *
     * @return Value for property 'receivers'.
     */
    public Map<Object, MessageReceiver> getReceivers()
    {
        return Collections.unmodifiableMap(receivers);
    }

    public MessageReceiver lookupReceiver(String key)
    {
        if (key != null)
        {
            return receivers.get(key);
        }
        else
        {
            throw new IllegalArgumentException("Receiver key must not be null");
        }
    }

    public MessageReceiver[] getReceivers(String wildcardExpression)
    {
        WildcardFilter filter = new WildcardFilter(wildcardExpression);
        filter.setCaseSensitive(false);

        List<MessageReceiver> found = new ArrayList<MessageReceiver>();

        for (Map.Entry<Object, MessageReceiver> e : receivers.entrySet())
        {
            if (filter.accept(e.getKey()))
            {
                found.add(e.getValue());
            }
        }

        return CollectionUtils.toArrayOfComponentType(found, MessageReceiver.class);
    }

    @Override
    public void connect() throws Exception
    {
        if (lifecycleManager.getState().isDisposed())
        {
            throw new LifecycleException(CoreMessages.lifecycleErrorCannotUseConnector(getName(),
                    lifecycleManager.getCurrentPhase()), this);
        }
        if (isConnected())
        {
            return;
        }

        RetryCallback callback = new RetryCallback()
        {
            @Override
            public void doWork(RetryContext context) throws Exception
            {
                connectionLock.lock();
                try
                {
                    // Try validateConnection() rather than connect() which may be a less expensive operation while we're retrying.
                    if (validateConnections && context.getLastFailure() instanceof ConnectException)
                    {
                        Connectable failed = ((ConnectException) context.getLastFailure()).getFailed();
                        if (!failed.validateConnection(context).isOk())
                        {
                            throw new ConnectException(
                                    MessageFactory.createStaticMessage("Still unable to connect to resource " + failed.getClass().getName()),
                                    context.getLastFailure(), failed);
                        }
                    }
                    connectConnectorAndReceivers();
                }
                finally
                {
                    connectionLock.unlock();
                }
            }


            @Override
            public String getWorkDescription()
            {
                return getConnectionDescription();
            }

            @Override
            public Connector getWorkOwner()
            {
                return AbstractConnector.this;
            }
        };

        if (connecting.compareAndSet(false, true))
        {
            if (logger.isDebugEnabled())
            {
                logger.debug("Connecting: " + this);
            }
            retryPolicyTemplate.execute(callback, muleContext.getWorkManager());
        }
    }

    protected void connectConnectorAndReceivers() throws Exception
    {
        doConnect();

        if (receivers != null)
        {
            for (MessageReceiver receiver : receivers.values())
            {
                final List<MuleException> errors = new ArrayList<MuleException>();
                try
                {
                    if (logger.isDebugEnabled())
                    {
                        logger.debug("Connecting receiver on endpoint: " + receiver.getEndpoint().getEndpointURI());
                    }
                    receiver.connect();
                    if (isStarted())
                    {
                        receiver.start();
                    }
                }
                catch (MuleException e)
                {
                    logger.error(e);
                    errors.add(e);
                }

                if (!errors.isEmpty())
                {
                    // throw the first one in order not to break the reconnection
                    // strategy logic,
                    // every exception has been logged above already
                    // api needs refactoring to support the multi-cause exception
                    // here
                    throw errors.get(0);
                }
            }
        }

        setConnected(true);
        setConnecting(false);
        logger.info("Connected: " + getConnectionDescription());

        if (startOnConnect && !isStarted() && !isStarting())
        {
            startAfterConnect();
        }
    }


    /**
     * Override this method to test whether the connector is able to connect to its
     * resource(s). This will allow a retry policy to go into effect in the case of
     * failure.
     *
     * @return retry context with a success flag or failure details
     * @see RetryContext#isOk()
     * @see RetryContext#getLastFailure()
     */
    @Override
    public RetryContext validateConnection(RetryContext retryContext)
    {
        retryContext.setOk();
        return retryContext;
    }

    @Override
    public void disconnect() throws Exception
    {
        if (isStarted())
        {
            startOnConnect = true;
            stop();
        }

        if (receivers != null)
        {
            for (MessageReceiver receiver : receivers.values())
            {
                if (logger.isDebugEnabled())
                {
                    logger.debug("Disconnecting receiver on endpoint: "
                            + receiver.getEndpoint().getEndpointURI());
                }
                try
                {
                    receiver.disconnect();
                }
                catch (Exception e)
                {
                    logger.error(e.getMessage(), e);
                }
            }
        }
        try
        {
            if (isStarted() && !isStopping())
            {
                stop();
            }
            this.doDisconnect();
            if (logger.isInfoEnabled())
            {
                logger.info("Disconnected: " + this.getConnectionDescription());
            }
            this.fireNotification(new ConnectionNotification(this, getConnectEventId(),
                                                             ConnectionNotification.CONNECTION_DISCONNECTED));
        }
        catch (Exception e)
        {
            logger.error(e.getMessage());
        }
        finally
        {
            connected.set(false);
        }
    }

    @Override
    public String getConnectionDescription()
    {
        return this.toString();
    }

    @Override
    public final boolean isConnected()
    {
        return connected.get();
    }

    public final void setConnected(boolean flag)
    {
        connected.set(flag);
    }

    public final void setConnecting(boolean flag)
    {
        connecting.set(flag);
    }

    public final boolean isConnecting()
    {
        return connecting.get();
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
     * @param createMultipleTransactedReceivers
     *         if true, multiple receivers will be
     *         created for this connection
     * @see #isCreateMultipleTransactedReceivers()
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
        return this.numberOfConcurrentTransactedReceivers;
    }

    /**
     * @param count the number of concurrent transacted receivers to start
     * @see #getNumberOfConcurrentTransactedReceivers()
     */
    public void setNumberOfConcurrentTransactedReceivers(int count)
    {
        this.numberOfConcurrentTransactedReceivers = count;
    }

    public void setDynamicNotification(boolean dynamic)
    {
        dynamicNotification = dynamic;
    }

    protected void updateCachedNotificationHandler()
    {
        if (null != muleContext)
        {
            notificationHelper = new NotificationHelper(muleContext.getNotificationManager(), EndpointMessageNotification.class, dynamicNotification);
        }
    }

    public boolean isEnableMessageEvents()
    {
        return notificationHelper.isNotificationEnabled();
    }

    /**
     * Indicates if notifications are enabled for the given
     * {@code event}
     *
     * @param event a {@link MuleEvent}
     * @return {@code true} if notifications are to be fired for the given {@code event}, {@code false} otherwise
     */
    public boolean isEnableMessageEvents(MuleEvent event)
    {
        return notificationHelper.isNotificationEnabled(event);
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
     * Used by Meta endpoint descriptors to register support for endpoint of the meta
     * endpoint type. For example an RSS endpoint uses the Http connector. By
     * registering 'rss' as a supported meta protocol, this connector can be used
     * when creating RSS endpoints.
     *
     * @param protocol the meta protocol that can be used with this connector
     * @since 3.0.0
     */
    public void registerSupportedMetaProtocol(String protocol)
    {
        supportedProtocols.add(protocol.toLowerCase() + ":" + getProtocol().toLowerCase());

    }

    /**
     * Registers other protocols 'understood' by this connector. These must contain
     * scheme meta info. Unlike the {@link #registerSupportedProtocol(String)}
     * method, this allows you to register protocols that are not prefixed with the
     * connector protocol. This is useful where you use a Service Finder to discover
     * which Transport implementation to use. For example the 'wsdl' transport is a
     * generic 'finder' transport that will use Axis or CXF to create the WSDL
     * client. These transport protocols would be wsdl-axis and wsdl-cxf, but they
     * can all support 'wsdl' protocol too.
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
    @Override
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
    protected WorkManager getReceiverWorkManager() throws MuleException
    {
        return receiverWorkManager.get();
    }

    /**
     * Returns a work manager for message dispatchers.
     *
     * @throws MuleException in case of error
     */
    protected WorkManager getDispatcherWorkManager() throws MuleException
    {
        return dispatcherWorkManager.get();
    }

    /**
     * Returns a work manager for message requesters.
     *
     * @throws MuleException in case of error
     */
    protected WorkManager getRequesterWorkManager() throws MuleException
    {
        return requesterWorkManager.get();
    }

    /**
     * Returns a Scheduler service for periodic tasks, currently limited to internal
     * use. Note: getScheduler() currently conflicts with the same method in the
     * Quartz transport
     */
    public ScheduledExecutorService getScheduler()
    {
        return scheduler;
    }

    protected ScheduledExecutorService createScheduler()
    {
        ThreadFactory threadFactory = new NamedThreadFactory(this.getName() + ".scheduler");
        ScheduledThreadPoolExecutor newExecutor = new ScheduledThreadPoolExecutor(4, threadFactory);
        newExecutor.setExecuteExistingDelayedTasksAfterShutdownPolicy(false);
        newExecutor.setKeepAliveTime(this.getReceiverThreadingProfile().getThreadTTL(), TimeUnit.MILLISECONDS);
        newExecutor.allowCoreThreadTimeOut(true);
        return newExecutor;
    }

    /**
     * Getter for property 'sessionHandler'.
     *
     * @return Value for property 'sessionHandler'.
     */
    @Override
    public SessionHandler getSessionHandler()
    {
        return sessionHandler;
    }

    @Override
    public AbstractRedeliveryPolicy createDefaultRedeliveryPolicy(int maxRedelivery)
    {
        IdempotentRedeliveryPolicy idempotentRedeliveryPolicy = new IdempotentRedeliveryPolicy();
        idempotentRedeliveryPolicy.setUseSecureHash(true);
        idempotentRedeliveryPolicy.setMaxRedeliveryCount(maxRedelivery);
        return idempotentRedeliveryPolicy;
    }

    /**
     * Returns transactional resource to use based on endpoint configuration
     * and transactional context.
     *
     * Transactional resource factory must be provided by overriding method
     * getOperationResourceFactory().
     *
     * Transactional resource instantiation must be provided by overriding method
     * createOperationResource().
     *
     * @param endpoint which holds the transaction configuration
     * @param <T> Type of the transaction resource
     * @return An operation resource that can be transacted or not based on transaction configuration
     * @throws MuleException
     */
    final public <T> T getTransactionalResource(ImmutableEndpoint endpoint) throws MuleException
    {
        Transaction currentTx = TransactionCoordination.getInstance().getTransaction();
        if (currentTx != null)
        {
            if (currentTx.hasResource(this.getOperationResourceFactory()))
            {
                return (T) currentTx.getResource(this.getOperationResourceFactory());
            }
            else
            {
                Object connectionResource = this.createOperationResource(endpoint);
                if (currentTx.supports(this.getOperationResourceFactory(),connectionResource))
                {
                    currentTx.bindResource(this.getOperationResourceFactory(), connectionResource);
                }
                else if (endpoint.getTransactionConfig().isTransacted())
                {
                    throw new TransactionException(CoreMessages.createStaticMessage("Endpoint is transactional but transaction does not support it"));
                }
                return (T) connectionResource;
            }
        }
        else
        {
            return (T) createOperationResource(endpoint);
        }
    }

    protected <T> T createOperationResource(ImmutableEndpoint endpoint) throws MuleException
    {
        throw new MuleRuntimeException(CoreMessages.createStaticMessage("Operation not supported by dispatcher"));
    }

    protected <T> T getOperationResourceFactory()
    {
        throw new MuleRuntimeException(CoreMessages.createStaticMessage("Operation not supported by dispatcher"));
    }

    /**
     * Setter for property 'sessionHandler'.
     *
     * @param sessionHandler Value to set for property 'sessionHandler'.
     */
    public void setSessionHandler(SessionHandler sessionHandler)
    {
        this.sessionHandler = sessionHandler;
    }

    @Override
    public void workAccepted(WorkEvent event)
    {
        handleWorkException(event, "workAccepted");
    }

    @Override
    public void workRejected(WorkEvent event)
    {
        handleWorkException(event, "workRejected");
    }

    @Override
    public void workStarted(WorkEvent event)
    {
        handleWorkException(event, "workStarted");
    }

    @Override
    public void workCompleted(WorkEvent event)
    {
        handleWorkException(event, "workCompleted");
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

        if (e instanceof MessagingException)
        {
            MuleEvent failedEvent = ((MessagingException) e).getEvent();
            failedEvent.getFlowConstruct().getExceptionListener().handleException((MessagingException) e, failedEvent);
        }
        else if (e instanceof Exception)
        {
            muleContext.getExceptionListener().handleException((Exception) e);
        }
        else
        {
            muleContext.getExceptionListener().handleException(
                new MuleRuntimeException(CoreMessages.connectorCausedError(this.getName()), e));
        }
    }

    /**
     * This method will return the dispatcher to the pool or, if the payload is an
     * inputstream, replace the payload with a new DelegatingInputStream which
     * returns the dispatcher to the pool when the stream is closed.
     *
     * @param endpoint
     * @param dispatcher
     * @param result
     */
    protected void setupDispatchReturn(final OutboundEndpoint endpoint,
                                       final MessageDispatcher dispatcher,
                                       MuleMessage result)
    {
        if (result != null && result.getPayload() instanceof InputStream)
        {
            DelegatingInputStream is = new DelegatingInputStream((InputStream) result.getPayload())
            {
                @Override
                public void close() throws IOException
                {
                    try
                    {
                        super.close();
                    }
                    finally
                    {
                        returnDispatcher(endpoint, dispatcher);
                    }
                }
            };
            result.setPayload(is);
        }
        else
        {

            this.returnDispatcher(endpoint, dispatcher);
        }
    }

    @Override
    public MuleMessage request(InboundEndpoint endpoint, long timeout) throws Exception
    {
        MessageRequester requester = null;
        MuleMessage result = null;

        try
        {
            requester = this.getRequester(endpoint);
            result = requester.request(timeout);
            return result;
        }
        finally
        {
            setupRequestReturn(endpoint, requester, result);
        }
    }

    /**
     * This method will return the requester to the pool or, if the payload is an
     * inputstream, replace the payload with a new DelegatingInputStream which
     * returns the requester to the pool when the stream is closed.
     *
     * @param endpoint
     * @param requester
     * @param result
     */
    protected void setupRequestReturn(final InboundEndpoint endpoint,
                                      final MessageRequester requester,
                                      MuleMessage result)
    {
        if (result != null && result.getPayload() instanceof InputStream)
        {
            DelegatingInputStream is = new DelegatingInputStream((InputStream) result.getPayload())
            {
                @Override
                public void close() throws IOException
                {
                    try
                    {
                        super.close();
                    }
                    finally
                    {
                        returnRequester(endpoint, requester);
                    }
                }
            };
            result.setPayload(is, result.getDataType());
        }
        else
        {

            this.returnRequester(endpoint, requester);
        }
    }

    // -------- Methods from the removed AbstractServiceEnabled Connector

    /**
     * When this connector is created via the
     * {@link org.mule.transport.service.TransportFactory} the endpoint used to
     * determine the connector type is passed to this method so that any properties
     * set on the endpoint that can be used to initialise the connector are made
     * available.
     *
     * @param endpointUri the {@link EndpointURI} use to create this connector
     * @throws InitialisationException If there are any problems with the
     *                                 configuration set on the Endpoint or if another exception is
     *                                 thrown it is wrapped in an InitialisationException.
     */
    public void initialiseFromUrl(EndpointURI endpointUri) throws InitialisationException
    {
        if (!supportsProtocol(endpointUri.getFullScheme()))
        {
            throw new InitialisationException(CoreMessages.schemeNotCompatibleWithConnector(
                    endpointUri.getFullScheme(), this.getClass()), this);
        }
        Properties props = new Properties();
        props.putAll(endpointUri.getParams());
        // auto set username and password
        if (endpointUri.getUserInfo() != null)
        {
            props.setProperty("username", endpointUri.getUser());
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

        BeanUtils.populateWithoutFail(this, props, true);

        setName(new ObjectNameHelper(muleContext).getConnectorName(this));
        // initialise();
    }

    /**
     * Initialises this connector from its {@link TransportServiceDescriptor} This
     * will be called before the {@link #doInitialise()} method is called.
     *
     * @throws InitialisationException InitialisationException If there are any
     *                                 problems with the configuration or if another exception is thrown
     *                                 it is wrapped in an InitialisationException.
     */
    protected synchronized void initFromServiceDescriptor() throws InitialisationException
    {
        try
        {
            serviceDescriptor = (TransportServiceDescriptor) muleContext.getRegistry()
                    .lookupServiceDescriptor(ServiceType.TRANSPORT, getProtocol().toLowerCase(), serviceOverrides);
            if (serviceDescriptor == null)
            {
                throw new ServiceException(CoreMessages.noServiceTransportDescriptor(getProtocol()));
            }

            if (logger.isDebugEnabled())
            {
                logger.debug("Loading DispatcherFactory for connector: " + getName() + " ("
                        + getClass().getName() + ")");
            }

            MessageDispatcherFactory df = serviceDescriptor.createDispatcherFactory();
            if (df != null)
            {
                this.setDispatcherFactory(df);
            }
            else if (logger.isDebugEnabled())
            {
                logger.debug("Transport '" + getProtocol() + "' will not support outbound endpoints: ");
            }

            if (logger.isDebugEnabled())
            {
                logger.debug("Loading RequesterFactory for connector: " + getName() + " ("
                        + getClass().getName() + ")");
            }

            MessageRequesterFactory rf = serviceDescriptor.createRequesterFactory();
            if (rf != null)
            {
                this.setRequesterFactory(rf);
            }
            else if (logger.isDebugEnabled())
            {
                logger.debug("Transport '" + getProtocol() + "' will not support requests: ");
            }

            sessionHandler = serviceDescriptor.createSessionHandler();
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
     * @param flowConstruct the service that will receive events from this receiver, the
     *                      listener
     * @param endpoint      the endpoint that defies this inbound communication
     * @return an instance of the message receiver defined in this connectors'
     *         {@link org.mule.transport.service.TransportServiceDescriptor}
     *         initialised using the service and endpoint.
     * @throws Exception if there is a problem creating the receiver. This exception
     *                   really depends on the underlying transport, thus any exception
     *                   could be thrown
     */
    protected MessageReceiver createReceiver(FlowConstruct flowConstruct, InboundEndpoint endpoint) throws Exception
    {
        return getServiceDescriptor().createMessageReceiver(this, flowConstruct, endpoint);
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
     * Will get the output stream for this type of transport. Typically this will be
     * called only when Streaming is being used on an outbound endpoint. If Streaming
     * is not supported by this transport an {@link UnsupportedOperationException} is
     * thrown. Note that the stream MUST release resources on close. For help doing
     * so, see {@link org.mule.model.streaming.CallbackOutputStream}.
     *
     * @param endpoint the endpoint that releates to this Dispatcher
     * @param event  the current event being processed
     * @return the output stream to use for this request
     * @throws MuleException in case of any error
     */
    @Override
    public OutputStream getOutputStream(OutboundEndpoint endpoint, MuleEvent event) throws MuleException
    {
        throw new UnsupportedOperationException(CoreMessages.streamingNotSupported(this.getProtocol()).toString());
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public MuleContext getMuleContext()
    {
        return muleContext;
    }

    @Override
    public String toString()
    {
        final StringBuilder sb = new StringBuilder(120);
        sb.append(ClassUtils.getSimpleName(this.getClass()));
        // format message for multi-line output, single-line is not readable
        sb.append(LINE_SEPARATOR);
        sb.append("{");
        sb.append(LINE_SEPARATOR);
        sb.append("  name=").append(name);
        sb.append(LINE_SEPARATOR);
        sb.append("  lifecycle=").append(
                lifecycleManager == null ? "<not in lifecycle>" : lifecycleManager.getCurrentPhase());
        sb.append(LINE_SEPARATOR);
        sb.append("  this=").append(Integer.toHexString(System.identityHashCode(this)));
        sb.append(LINE_SEPARATOR);
        sb.append("  numberOfConcurrentTransactedReceivers=").append(numberOfConcurrentTransactedReceivers);
        sb.append(LINE_SEPARATOR);
        sb.append("  createMultipleTransactedReceivers=").append(createMultipleTransactedReceivers);
        sb.append(LINE_SEPARATOR);
        sb.append("  connected=").append(connected);
        sb.append(LINE_SEPARATOR);
        sb.append("  supportedProtocols=").append(supportedProtocols);
        sb.append(LINE_SEPARATOR);
        sb.append("  serviceOverrides=");
        if (serviceOverrides != null)
        {
            for (Map.Entry<Object, Object> entry : serviceOverrides.entrySet())
            {
                sb.append(LINE_SEPARATOR);
                sb.append("    ").append(String.format("%s=%s", entry.getKey(), entry.getValue()));
            }
        }
        else
        {
            sb.append("<none>");
        }
        sb.append(LINE_SEPARATOR);
        sb.append('}');
        sb.append(LINE_SEPARATOR);
        return sb.toString();
    }

    @Override
    public RetryPolicyTemplate getRetryPolicyTemplate()
    {
        return retryPolicyTemplate;
    }

    public void setRetryPolicyTemplate(RetryPolicyTemplate retryPolicyTemplate)
    {
        this.retryPolicyTemplate = retryPolicyTemplate;
    }

    /**
     * Whether to test a connection on each take from pool.
     */
    public boolean isValidateConnections()
    {
        return validateConnections;
    }

    /**
     * Whether to test a connection on each take. A result is higher availability at
     * the expense of a potential slight performance hit (when a test connection is
     * made) or be very lightweight in other cases (like sending a hearbeat ping to
     * the server).
     * <p/>
     * Disable to obtain slight performance gain or if you are absolutely sure of the
     * server availability.
     * <p/>
     * It is up to the transport implementatin to support such validation, thus it
     * should be considered a hint only.
     * <p/>
     * The default value is <code>true</code>
     */
    public void setValidateConnections(final boolean validateConnections)
    {
        this.validateConnections = validateConnections;
    }

    // MULE-4751 Expose some dispatcher and requester object pool configuration

    /**
     * Allows an ExhaustedAction to be configured on the dispatcher object pool See:
     * {@link GenericKeyedObjectPool#setWhenExhaustedAction(byte)}
     */
    public void setDispatcherPoolWhenExhaustedAction(byte whenExhaustedAction)
    {
        checkDispatchersInitialised();
        dispatchers.setWhenExhaustedAction(whenExhaustedAction);
    }

    /**
     * Allows a maxWait timeout to be configured on the dispatcher object pool See:
     * {@link GenericKeyedObjectPool#setMaxWait(long)}
     */
    public void setDispatcherPoolMaxWait(int maxWait)
    {
        checkDispatchersInitialised();
        dispatchers.setMaxWait(maxWait);
    }

    /**
     * Allows to define a factory to create the dispatchers pool that will be used in the connector
     */
    public void setDispatcherPoolFactory(ConfigurableKeyedObjectPoolFactory dispatcherPoolFactory)
    {
        this.dispatcherPoolFactory = dispatcherPoolFactory;
    }

    public ConfigurableKeyedObjectPoolFactory getDispatcherPoolFactory()
    {
        return dispatcherPoolFactory;
    }

    /**
     * Allows an ExhaustedAction to be configured on the requester object pool See:
     * {@link GenericKeyedObjectPool#setWhenExhaustedAction(byte)}
     */
    public void setRequesterPoolWhenExhaustedAction(byte whenExhaustedAction)
    {
        requesters.setWhenExhaustedAction(whenExhaustedAction);
    }

    /**
     * Allows a maxWait timeout to be configured on the requester object pool See:
     * {@link GenericKeyedObjectPool#setMaxWait(long)}
     */
    public void setRequesterPoolMaxWait(int maxWait)
    {
        requesters.setMaxWait(maxWait);
    }

    public MessageProcessor createDispatcherMessageProcessor(OutboundEndpoint endpoint) throws MuleException
    {
        if (endpoint.getExchangePattern().hasResponse() || !getDispatcherThreadingProfile().isDoThreading())
        {
            return new DispatcherMessageProcessor(endpoint);
        }
        else
        {
            SimpleMessageProcessorChainBuilder builder = new SimpleMessageProcessorChainBuilder();
            builder.setName("dispatcher processor chain for '" + endpoint.getAddress() + "'");
            LaxAsyncInterceptingMessageProcessor async = new LaxAsyncInterceptingMessageProcessor(
                new WorkManagerSource()
            {
                @Override
                public WorkManager getWorkManager() throws MuleException
                {
                    return getDispatcherWorkManager();
                }
            });
            builder.chain(async);
            builder.chain(new DispatcherMessageProcessor(endpoint));
            return builder.build();
        }
    }

    @Override
    public MessageExchangePattern getDefaultExchangePattern()
    {
        try
        {
            return serviceDescriptor.getDefaultExchangePattern();
        }
        catch (TransportServiceException tse)
        {
            throw new MuleRuntimeException(tse);
        }
    }

    @Override
    public List<MessageExchangePattern> getInboundExchangePatterns()
    {
        try
        {
            return serviceDescriptor.getInboundExchangePatterns();
        }
        catch (TransportServiceException tse)
        {
            throw new MuleRuntimeException(tse);
        }
    }

    @Override
    public List<MessageExchangePattern> getOutboundExchangePatterns()
    {
        try
        {
            return serviceDescriptor.getOutboundExchangePatterns();
        }
        catch (TransportServiceException tse)
        {
            throw new MuleRuntimeException(tse);
        }
    }

    class DispatcherMessageProcessor implements MessageProcessor, Disposable
    {
        private OutboundNotificationMessageProcessor notificationMessageProcessor;
        private OutboundEndpoint endpoint;

        public DispatcherMessageProcessor(OutboundEndpoint endpoint)
        {
           this.endpoint = endpoint;
        }

        @Override
        public MuleEvent process(MuleEvent event) throws MuleException
        {
            MessageDispatcher dispatcher = null;
            try
            {
                dispatcher = borrowDispatcher(endpoint);
                boolean fireNotification = event.isNotificationsEnabled();
                if (fireNotification)
                {
                    if (notificationMessageProcessor == null)
                    {
                        notificationMessageProcessor = new OutboundNotificationMessageProcessor(endpoint);
                    }
                    notificationMessageProcessor.dispatchNotification(notificationMessageProcessor.createBeginNotification(event), event);
                }
                MuleEvent result = dispatcher.process(event);

                if (fireNotification)
                {
                    // We need to invoke notification message processor with request
                    // message only after successful send/dispatch
                    notificationMessageProcessor.process((result != null && !VoidMuleEvent.getInstance().equals(
                            result)) ? result : event);
                }

                return result;
            }
            catch (DispatchException dex)
            {
                throw dex;
            }
            catch (MuleException ex)
            {
                throw new DispatchException(event, endpoint, ex);
            }
            finally
            {
                returnDispatcher(endpoint, dispatcher);
            }
        }

        @Override
        public String toString()
        {
            return ObjectUtils.toString(this);
        }

        @Override
        public void dispose()
        {
            try
            {
                dispatchers.clear(endpoint);
            }
            catch (Exception e)
            {
                logger.warn("Can not clear dispatchers cache for endpoint: " + endpoint);
            }
        }

    }

}
