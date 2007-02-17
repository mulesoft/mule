/*
 * $Id$
 * --------------------------------------------------------------------------------------
 * Copyright (c) MuleSource, Inc.  All rights reserved.  http://www.mulesource.com
 *
 * The software in this package is published under the terms of the MuleSource MPL
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule;

import org.mule.config.ConfigurationException;
import org.mule.config.MuleConfiguration;
import org.mule.config.MuleProperties;
import org.mule.config.ThreadingProfile;
import org.mule.config.i18n.Message;
import org.mule.config.i18n.Messages;
import org.mule.impl.container.MultiContainerContext;
import org.mule.impl.internal.notifications.AdminNotification;
import org.mule.impl.internal.notifications.AdminNotificationListener;
import org.mule.impl.internal.notifications.ComponentNotification;
import org.mule.impl.internal.notifications.ComponentNotificationListener;
import org.mule.impl.internal.notifications.ConnectionNotification;
import org.mule.impl.internal.notifications.ConnectionNotificationListener;
import org.mule.impl.internal.notifications.CustomNotification;
import org.mule.impl.internal.notifications.CustomNotificationListener;
import org.mule.impl.internal.notifications.ManagementNotification;
import org.mule.impl.internal.notifications.ManagementNotificationListener;
import org.mule.impl.internal.notifications.ManagerNotification;
import org.mule.impl.internal.notifications.ManagerNotificationListener;
import org.mule.impl.internal.notifications.ModelNotification;
import org.mule.impl.internal.notifications.ModelNotificationListener;
import org.mule.impl.internal.notifications.NotificationException;
import org.mule.impl.internal.notifications.SecurityNotification;
import org.mule.impl.internal.notifications.SecurityNotificationListener;
import org.mule.impl.internal.notifications.ServerNotificationManager;
import org.mule.impl.security.MuleSecurityManager;
import org.mule.impl.work.MuleWorkManager;
import org.mule.management.stats.AllStatistics;
import org.mule.registry.RegistryException;
import org.mule.registry.UMORegistry;
import org.mule.registry.impl.RegistryNotificationListener;
import org.mule.umo.UMOException;
import org.mule.umo.lifecycle.FatalException;
import org.mule.umo.lifecycle.InitialisationException;
import org.mule.umo.manager.UMOAgent;
import org.mule.umo.manager.UMOContainerContext;
import org.mule.umo.manager.UMOManager;
import org.mule.umo.manager.UMOServerNotification;
import org.mule.umo.manager.UMOServerNotificationListener;
import org.mule.umo.manager.UMOWorkManager;
import org.mule.umo.security.UMOSecurityManager;
import org.mule.util.ClassUtils;
import org.mule.util.DateUtils;
import org.mule.util.StringMessageUtils;
import org.mule.util.UUID;
import org.mule.util.queue.CachingPersistenceStrategy;
import org.mule.util.queue.MemoryPersistenceStrategy;
import org.mule.util.queue.QueueManager;
import org.mule.util.queue.QueuePersistenceStrategy;
import org.mule.util.queue.TransactionalQueueManager;

import edu.emory.mathcs.backport.java.util.concurrent.atomic.AtomicBoolean;

import java.net.InetAddress;
import java.net.UnknownHostException;
import java.nio.charset.Charset;
import java.text.DateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.jar.Manifest;

import javax.transaction.TransactionManager;

import org.apache.commons.lang.StringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

/**
 * <code>MuleManager</code> maintains and provides services for a Mule instance.
 * 
 * @deprecated There is no more singleton MuleManager in Mule 2.0, this functionality will be replaced by 
 * the MuleManagementContext or the MuleRegistry.
 */
public class MuleManager implements UMOManager
{   
    private static final String MULE_REGISTRY_CLASS = "org.mule.registry.impl.SingletonMuleRegistry";
    
    /**
     * Singleton Manager instance
     */
    private static UMOManager instance = null;

    /**
     * Singleton Registry instance
     */
    private static UMORegistry registry = null;

    /**
     * Default configuration
     */
    private static MuleConfiguration config = new MuleConfiguration();

    /**
     * Holds any application scoped environment properties set in the config
     */
    private Map applicationProps = new HashMap();

    /**
     * the unique id for this manager
     */
    private String id = UUID.getUUID();

    /**
     * The transaction Manager to use for global transactions
     */
    private TransactionManager transactionManager = null;

    /**
     * True once the Mule Manager is initialised
     */
    private AtomicBoolean initialised = new AtomicBoolean(false);

    /**
     * True while the Mule Manager is initialising
     */
    private AtomicBoolean initialising = new AtomicBoolean(false);

    /**
     * Determines of the MuleManager has been started
     */
    private AtomicBoolean started = new AtomicBoolean(false);

    /**
     * Determines in the manager is in the process of starting
     */
    private AtomicBoolean starting = new AtomicBoolean(false);

    /**
     * Determines in the manager is in the process of stopping.
     */
    private AtomicBoolean stopping = new AtomicBoolean(false);

    /**
     * Determines if the manager has been disposed
     */
    private AtomicBoolean disposed = new AtomicBoolean(false);

    /**
     * Holds a reference to the deamon running the Manager if any
     */
    private static MuleServer server = null;

    /**
     * the date in milliseconds from when the server was started
     */
    private long startDate = 0;

    /**
     * stats used for management
     */
    private AllStatistics stats = new AllStatistics();

    /**
     * Manages all Server event notificationManager
     */
    private ServerNotificationManager notificationManager = null;

    private MultiContainerContext containerContext = null;

    private UMOSecurityManager securityManager;

    /**
     * The queue manager to use for component queues and vm connector
     */
    private QueueManager queueManager;

    private UMOWorkManager workManager;

    /**
     * Registration ID for this Manager
     */
    private String registryId = null;

    /**
     * logger used by this class
     */
    private static Log logger = LogFactory.getLog(MuleManager.class);

    /**
     * @deprecated This is now handled by the OSGi lifecycle or the Service Wrapper
     */
    private ShutdownContext shutdownContext = new ShutdownContext(true, null);

    /**
     * Default Constructor
     */
    protected MuleManager()
    {
        if (config == null)
        {
            config = new MuleConfiguration();
        }

        containerContext = new MultiContainerContext();
        securityManager = new MuleSecurityManager();
        Runtime.getRuntime().addShutdownHook(new ShutdownThread());

        // create the event manager
        notificationManager = new ServerNotificationManager();
        notificationManager.registerEventType(ManagerNotification.class, ManagerNotificationListener.class);
        notificationManager.registerEventType(ModelNotification.class, ModelNotificationListener.class);
        notificationManager.registerEventType(ComponentNotification.class,
                ComponentNotificationListener.class);
        notificationManager.registerEventType(SecurityNotification.class, SecurityNotificationListener.class);
        notificationManager.registerEventType(ManagementNotification.class,
                ManagementNotificationListener.class);
        notificationManager.registerEventType(AdminNotification.class, AdminNotificationListener.class);
        notificationManager.registerEventType(CustomNotification.class, CustomNotificationListener.class);
        notificationManager.registerEventType(ConnectionNotification.class,
                ConnectionNotificationListener.class);
        notificationManager.registerEventType(ModelNotification.class,
                RegistryNotificationListener.class);
        notificationManager.registerEventType(ComponentNotification.class,
                RegistryNotificationListener.class);

        // This is obviously just a workaround until extension modules can register
        // their own classes for notifications. Need to revisit this when the
        // ManagementContext is implemented properly.
        try
        {
            Class spaceNotificationClass = ClassUtils.loadClass(
                    "org.mule.impl.space.SpaceMonitorNotification", this.getClass());
            Class spaceListenerClass = ClassUtils.loadClass(
                    "org.mule.impl.space.SpaceMonitorNotificationListener", this.getClass());
            notificationManager.registerEventType(spaceNotificationClass, spaceListenerClass);
        }
        catch (ClassNotFoundException cnf)
        {
            // ignore - apparently not available
        }
    }

    /**
     * Getter method for the current singleton MuleManager
     *
     * @return the current singleton MuleManager
     * 
     * @deprecated There is no more singleton MuleManager in 2.x
     */
    public static synchronized UMOManager getInstance()
    {
        if (instance == null)
        {
            logger.info("Creating new MuleManager instance");
            try
            {
                instance = new MuleManager();
            }
            catch (Exception e)
            {
                throw new MuleRuntimeException(new Message(Messages.FAILED_TO_CREATE_MANAGER_INSTANCE_X,
                    MuleManager.class.getName()), e);
            }

            logger.info("Creating new MuleRegistry instance");
            try
            {
                registry = (UMORegistry) ClassUtils.instanciateClass(MULE_REGISTRY_CLASS, new Object[]{});
            }
            catch (Exception e)
            {
                throw new MuleRuntimeException(new Message(Messages.FAILED_TO_CREATE_REGISTRY_INSTANCE_X,
                    MULE_REGISTRY_CLASS), e);
            }
        }

        return instance;
    }

    public static UMORegistry getRegistry()
    {
        if (registry == null)
        {
            // The MuleManager and MuleRegistry are both singletons which get initialized together.
            getInstance();
        }
        return registry;
    }
    
    /**
     * A static method to determine if there is an instance of the MuleManager. This
     * should be used instead of <code>
     * if(MuleManager.getInstance()!=null)
     * </code>
     * because getInstance never returns a null. If an istance is not available one
     * is created. This method queries the instance directly.
     *
     * @return true if the manager is instanciated
     */
    public static synchronized boolean isInstanciated()
    {
        return (instance != null);
    }

    /**
     * Sets the current singleton MuleManager
     *
     * @deprecated this will go away soon.
     */
    public static synchronized void setInstance(UMOManager manager)
    {
        instance = manager;
        if (instance == null)
        {
            config = new MuleConfiguration();
        }
    }

    /**
     * Gets all statisitcs for this instance
     *
     * @return all statisitcs for this instance
     */
    public AllStatistics getStatistics()
    {
        return stats;
    }

    /**
     * Sets statistics on this instance
     *
     * @param stat
     */
    public void setStatistics(AllStatistics stat)
    {
        this.stats = stat;
    }

    /**
     * @return the MuleConfiguration for this MuleManager. This object is immutable
     *         once the manager has initialised.
     */
    public static synchronized MuleConfiguration getConfiguration()
    {
        return config;
    }

    /**
     * Sets the configuration for the <code>MuleManager</code>.
     *
     * @param config the configuration object
     * @throws IllegalAccessError if the <code>MuleManager</code> has already been
     *                            initialised.
     * @deprecated this will go away soon.
     */
    public static synchronized void setConfiguration(MuleConfiguration config)
    {
        if (config == null)
        {
            throw new IllegalArgumentException(
                    new Message(Messages.X_IS_NULL, "MuleConfiguration object").getMessage());
        }

        MuleManager.config = config;
    }

    // Implementation methods
    // -------------------------------------------------------------------------

    /**
     * Destroys the MuleManager and all resources it maintains
     */
    public synchronized void dispose()
    {
        if (disposed.get())
        {
            return;
        }
        try
        {
            if (started.get())
            {
                stop();
            }
        }
        catch (UMOException e)
        {
            logger.error("Failed to stop manager: " + e.getMessage(), e);
        }
        disposed.set(true);

        containerContext.dispose();
        containerContext = null;
        // props.clear();
        fireSystemEvent(new ManagerNotification(id, null, null, ManagerNotification.MANAGER_DISPOSED));

        if (registry != null)
        {
            registry.dispose();
            registry = null;
        }

        // props = null;
        initialised.set(false);
        if (notificationManager != null)
        {
            notificationManager.dispose();
        }
        if (workManager != null)
        {
            workManager.dispose();
        }

        if (queueManager != null)
        {
            queueManager.close();
            queueManager = null;
        }

        if (startDate > 0)
        {
            if (logger.isInfoEnabled())
            {
                logger.info(getEndSplash());
            }
            else
            {
                System.out.println(getEndSplash());
            }
        }

        config = new MuleConfiguration();
        instance = null;
    }

    /**
     * {@inheritDoc}
     */
    public Object getProperty(Object key)
    {
        return applicationProps.get(key);
    }

    /**
     * {@inheritDoc}
     */
    public Map getProperties()
    {
        return applicationProps;
    }

    /**
     * {@inheritDoc}
     */
    public TransactionManager getTransactionManager()
    {
        return transactionManager;
    }

    /**
     * {@inheritDoc}
     */
    public void setProperty(Object key, Object value)
    {
        applicationProps.put(key, value);
    }

    public void addProperties(Map props)
    {
        applicationProps.putAll(props);
    }

    /**
     * {@inheritDoc}
     */
    public void setTransactionManager(TransactionManager newManager) throws UMOException
    {
        if (transactionManager != null)
        {
            throw new ConfigurationException(new Message(Messages.TX_MANAGER_ALREADY_SET));
        }
        transactionManager = newManager;
    }

    public void setRegistry(UMORegistry registry)
    {
        this.registry = registry;
    }

    /**
     * {@inheritDoc}
     */
    public synchronized void initialise() throws UMOException
    {
        registerListener(new RegistryNotificationListener(registry));

        validateEncoding();
        validateOSEncoding();

        if (!initialised.get())
        {
            initialising.set(true);
            startDate = System.currentTimeMillis();

            // if no work manager has been set create a default one
            if (workManager == null)
            {
                ThreadingProfile tp = config.getDefaultThreadingProfile();
                logger.debug("Creating default work manager using default threading profile: " + tp);
                workManager = new MuleWorkManager(tp, "UMOManager");
                workManager.start();
            }

            // Start the event manager
            notificationManager.start(workManager);

            // Fire message notifications if the option is set. This will fire
            // inbound and outbound message events that can
            // consume resources in high throughput systems
            // TODO MERGE no such method?
            //if (config.isEnableMessageEvents())
            //{
            //    notificationManager.registerEventType(MessageNotification.class,
            //            MessageNotificationListener.class);
            //}

            fireSystemEvent(new ManagerNotification(id, null, null, ManagerNotification.MANAGER_INITIALISNG));
            if (id == null)
            {
                logger.warn("No unique id has been set on this manager");
            }
            try
            {
                if (securityManager != null)
                {
                    securityManager.initialise();
                }
                if (queueManager == null)
                {
                    try
                    {
                        TransactionalQueueManager queueMgr = new TransactionalQueueManager();
                        // TODO RM: The persistence strategy should come from the user's config.
                        QueuePersistenceStrategy ps = new CachingPersistenceStrategy(new MemoryPersistenceStrategy()/*config.getPersistenceStrategy()*/);
                        queueMgr.setPersistenceStrategy(ps);
                        queueManager = queueMgr;
                    }
                    catch (Exception e)
                    {
                        throw new InitialisationException(new Message(Messages.INITIALISATION_FAILURE_X,
                                "QueueManager"), e);
                    }
                }
            }
            finally
            {
                initialised.set(true);
                initialising.set(false);
                fireSystemEvent(new ManagerNotification(id, null, null, ManagerNotification.MANAGER_INITIALISED));
            }
        }
    }

    protected void validateEncoding() throws FatalException
    {
        String encoding = System.getProperty(MuleProperties.MULE_ENCODING_SYSTEM_PROPERTY);
        if (encoding == null)
        {
            encoding = config.getDefaultEncoding();
            System.setProperty(MuleProperties.MULE_ENCODING_SYSTEM_PROPERTY, encoding);
        }
        else
        {
            config.setDefaultEncoding(encoding);
        }
        // Check we have a valid and supported encoding
        if (!Charset.isSupported(config.getDefaultEncoding()))
        {
            throw new FatalException(new Message(Messages.PROPERTY_X_HAS_INVALID_VALUE_X, "encoding",
                config.getDefaultEncoding()), this);
        }
    }

    protected void validateOSEncoding() throws FatalException
    {
        String encoding = System.getProperty(MuleProperties.MULE_OS_ENCODING_SYSTEM_PROPERTY);
        if (encoding == null)
        {
            encoding = config.getDefaultOSEncoding();
            System.setProperty(MuleProperties.MULE_OS_ENCODING_SYSTEM_PROPERTY, encoding);
        }
        else
        {
            config.setDefaultOSEncoding(encoding);
        }
        // Check we have a valid and supported encoding
        if (!Charset.isSupported(config.getDefaultOSEncoding()))
        {
            throw new FatalException(new Message(Messages.PROPERTY_X_HAS_INVALID_VALUE_X, "osEncoding",
                config.getDefaultOSEncoding()), this);
        }
    }

    /**
     * Start the <code>MuleManager</code>. This will start the connectors and
     * sessions.
     *
     * @throws UMOException if the the connectors or components fail to start
     */
    public synchronized void start() throws UMOException
    {
        initialise();

        if (!started.get())
        {
            starting.set(true);
            fireSystemEvent(new ManagerNotification(id, null, null, ManagerNotification.MANAGER_STARTING));
            if (queueManager != null)
            {
                queueManager.start();
            }

            started.set(true);
            starting.set(false);

            if (logger.isInfoEnabled())
            {
                logger.info(getStartSplash());
            }
            else
            {
                System.out.println(getStartSplash());
            }

            fireSystemEvent(new ManagerNotification(id, null, null, ManagerNotification.MANAGER_STARTED));
        }
    }

    /**
     * Start the <code>MuleManager</code>. This will start the connectors and
     * sessions.
     *
     * @param serverUrl the server Url for this instance
     * @throws UMOException if the the connectors or components fail to start
     */
    public void start(String serverUrl) throws UMOException
    {
        // this.createClientListener = createRequestListener;
        //TODO RM*
        //config.setServerUrl(serverUrl);
        start();
    }

    /**
     * Stops the <code>MuleManager</code> which stops all sessions and connectors
     *
     * @throws UMOException if either any of the sessions or connectors fail to stop
     */
    public synchronized void stop() throws UMOException
    {
        started.set(false);
        stopping.set(true);
        fireSystemEvent(new ManagerNotification(id, null, null, ManagerNotification.MANAGER_STOPPING));

        if (queueManager != null)
        {
            queueManager.stop();
        }

        stopping.set(false);
        fireSystemEvent(new ManagerNotification(id, null, null, ManagerNotification.MANAGER_STOPPED));
    }

    /**
     * If the <code>MuleManager</code> was started from the <code>MuleServer</code>
     * daemon then this will be called by the Server
     *
     * @param server a reference to the <code>MuleServer</code>.
     */
    void setServer(MuleServer server)
    {
        MuleManager.server = server;
    }

    /**
     * Shuts down the whole server tring to shut down all resources cleanly on the
     * way
     *
     * @param e an exception that caused the <code>shutdown()</code> method to be
     *          called. If e is null the shutdown message will just display a time
     *          when the server was shutdown. Otherwise the exception information
     *          will also be displayed.
     */
    public void shutdown(Throwable e, boolean aggressive)
    {
        shutdownContext = new ShutdownContext(aggressive, e);
        System.exit(0);
    }

    /**
     * {@inheritDoc}
     */
    public boolean isStarted()
    {
        return started.get();
    }

    /**
     * {@inheritDoc}
     */
    public boolean isInitialised()
    {
        return initialised.get();
    }

    /**
     * Determines if the server is currently initialising
     *
     * @return true if if the server is currently initialising, false otherwise
     */
    public boolean isInitialising()
    {
        return initialising.get();
    }

    /**
     * Determines in the manager is in the process of stopping.
     */
    public boolean isStopping()
    {
        return stopping.get();
    }

    /**
     * {@inheritDoc}
     */
    public long getStartDate()
    {
        return startDate;
    }

    /**
     * Returns a formatted string that is a summary of the configuration of the
     * server. This is the brock of information that gets displayed when the server
     * starts
     *
     * @return a string summary of the server information
     */
    protected String getStartSplash() throws RegistryException
    {
        String notset = new Message(Messages.NOT_SET).getMessage();

        // Mule Version, Timestamp, and Server ID
        List message = new ArrayList();
        Manifest mf = config.getManifest();
        Map att = mf.getMainAttributes();
        if (att.values().size() > 0)
        {
            message.add(StringUtils.defaultString(config.getProductDescription(), notset) + " "
                    + new Message(Messages.VERSION).getMessage() + " "
                    + StringUtils.defaultString(config.getProductVersion(), notset));

            message.add(StringUtils.defaultString(config.getVendorName(), notset));
            message.add(StringUtils.defaultString(config.getProductMoreInfo(), notset));
        }
        else
        {
            message.add(new Message(Messages.VERSION_INFO_NOT_SET).getMessage());
        }
        message.add(" ");
        DateFormat df = DateFormat.getDateTimeInstance(DateFormat.FULL, DateFormat.FULL);
        message.add(new Message(Messages.SERVER_STARTED_AT_X, df.format(new Date(getStartDate()))).getMessage());
        message.add("Server ID: " + id);

        // JDK, OS, and Host
        message.add("JDK: " + System.getProperty("java.version") + " (" + System.getProperty("java.vm.info")
                + ")");
        String patch = System.getProperty("sun.os.patch.level", null);
        message.add("OS: " + System.getProperty("os.name")
                + (patch != null && !"unknown".equalsIgnoreCase(patch) ? " - " + patch : "") + " ("
                + System.getProperty("os.version") + ", " + System.getProperty("os.arch") + ")");
        try
        {
            InetAddress host = InetAddress.getLocalHost();
            message.add("Host: " + host.getHostName() + " (" + host.getHostAddress() + ")");
        }
        catch (UnknownHostException e)
        {
            // ignore
        }

        // Mule Agents
        Map agents = registry.getAgents();
        message.add(" ");
        if (agents.size() == 0)
        {
            message.add(new Message(Messages.AGENTS_RUNNING).getMessage() + " "
                    + new Message(Messages.NONE).getMessage());
        }
        else
        {
            message.add(new Message(Messages.AGENTS_RUNNING).getMessage());
            UMOAgent umoAgent;
            for (Iterator iterator = agents.values().iterator(); iterator.hasNext();)
            {
                umoAgent = (UMOAgent) iterator.next();
                message.add("  " + umoAgent.getDescription());
            }
        }
        return StringMessageUtils.getBoilerPlate(message, '*', 70);
    }

    private String getEndSplash()
    {
        List message = new ArrayList(2);
        long currentTime = System.currentTimeMillis();
        DateFormat df = DateFormat.getDateTimeInstance(DateFormat.FULL, DateFormat.FULL);
        message.add(new Message(Messages.SHUTDOWN_NORMALLY_ON_X, df.format(new Date())).getMessage());
        long duration = 10;
        if (startDate > 0)
        {
            duration = currentTime - startDate;
        }
        message.add(new Message(Messages.SERVER_WAS_UP_FOR_X, DateUtils.getFormattedDuration(duration)).getMessage());

        return StringMessageUtils.getBoilerPlate(message, '*', 78);
    }

    /**
     * associates a Dependency Injector container or Jndi container with Mule. This
     * can be used to integrate container managed resources with Mule resources
     *
     * @param container a Container context to use. By default, there is a default
     *                  Mule container <code>MuleContainerContext</code> that will assume
     *                  that the reference key for an oblect is a classname and will try to
     *                  instanciate it.
     */
    public void setContainerContext(UMOContainerContext container) throws UMOException
    {
        if (container == null)
        {
            if (containerContext != null)
            {
                containerContext.dispose();
            }
            containerContext = new MultiContainerContext();
        }
        else
        {
            container.initialise();
            containerContext.addContainer(container);
        }
    }

    /**
     * associates a Dependency Injector container with Mule. This can be used to
     * integrate container managed resources with Mule resources
     *
     * @return the container associated with the Manager
     */
    public UMOContainerContext getContainerContext()
    {
        return containerContext;
    }

    /**
     * {@inheritDoc}
     */
    public void registerListener(UMOServerNotificationListener l) throws NotificationException
    {
        registerListener(l, null);
    }

    public void registerListener(UMOServerNotificationListener l, String resourceIdentifier)
            throws NotificationException
    {
        if (notificationManager == null)
        {
            throw new NotificationException(new Message(Messages.SERVER_EVENT_MANAGER_NOT_ENABLED));
        }
        notificationManager.registerListener(l, resourceIdentifier);
    }

    /**
     * {@inheritDoc}
     */
    public void unregisterListener(UMOServerNotificationListener l)
    {
        if (notificationManager != null)
        {
            notificationManager.unregisterListener(l);
        }
    }

    /**
     * Fires a mule 'system' event. These are notifications that are fired because
     * something within the Mule instance happened such as the Model started or the
     * server is being disposed.
     *
     * @param e the event that occurred
     */
    protected void fireSystemEvent(UMOServerNotification e)
    {
        if (notificationManager != null)
        {
            notificationManager.fireEvent(e);
        }
        else if (logger.isDebugEnabled())
        {
            logger.debug("Event Manager is not enabled, ignoring event: " + e);
        }
    }

    /**
     * Fires a server notification to all registered
     * {@link org.mule.impl.internal.notifications.CustomNotificationListener}
     * notificationManager.
     *
     * TODO RM: This method now duplicates #fireSystemEvent() completely
     *
     * @param notification the notification to fire. This must be of type
     *                     {@link org.mule.impl.internal.notifications.CustomNotification}
     *                     otherwise an exception will be thrown.
     * @throws UnsupportedOperationException if the notification fired is not a
     *                                       {@link org.mule.impl.internal.notifications.CustomNotification}
     */
    public void fireNotification(UMOServerNotification notification)
    {
        // if(notification instanceof CustomNotification) {
        if (notificationManager != null)
        {
            notificationManager.fireEvent(notification);
        }
        else if (logger.isDebugEnabled())
        {
            logger.debug("Event Manager is not enabled, ignoring notification: " + notification);
        }
        // } else {
        // throw new UnsupportedOperationException(new
        // Message(Messages.ONLY_CUSTOM_EVENTS_CAN_BE_FIRED).getMessage());
        // }
    }

    public void setId(String id)
    {
        this.id = id;
    }

    public String getId()
    {
        return id;
    }

    /**
     * Sets the security manager used by this Mule instance to authenticate and
     * authorise incoming and outgoing event traffic and service invocations
     *
     * @param securityManager the security manager used by this Mule instance to
     *                        authenticate and authorise incoming and outgoing event traffic and
     *                        service invocations
     */
    public void setSecurityManager(UMOSecurityManager securityManager) throws InitialisationException
    {
        this.securityManager = securityManager;
        if (securityManager != null && isInitialised())
        {
            this.securityManager.initialise();
        }
    }

    /**
     * Gets the security manager used by this Mule instance to authenticate and
     * authorise incoming and outgoing event traffic and service invocations
     *
     * @return he security manager used by this Mule instance to authenticate and
     *         authorise incoming and outgoing event traffic and service invocations
     */
    public UMOSecurityManager getSecurityManager()
    {
        return securityManager;
    }

    /**
     * Obtains a workManager instance that can be used to schedule work in a thread
     * pool. This will be used primarially by UMOAgents wanting to schedule work.
     * This work Manager must <b>never</b> be used by provider implementations as
     * they have their own workManager accible on the connector. If a workManager has
     * not been set by the time the <code>initialise()</code> method has been
     * called a default <code>MuleWorkManager</code> will be created using the
     * <i>DefaultThreadingProfile</i> on the <code>MuleConfiguration</code>
     * object.
     *
     * @return a workManager instance used by the current MuleManager
     * @see org.mule.config.ThreadingProfile
     * @see MuleConfiguration
     */
    public UMOWorkManager getWorkManager()
    {
        return workManager;
    }

    /**
     * Obtains a workManager instance that can be used to schedule work in a thread
     * pool. This will be used primarially by UMOAgents wanting to schedule work.
     * This work Manager must <b>never</b> be used by provider implementations as
     * they have their own workManager accible on the connector. If a workManager has
     * not been set by the time the <code>initialise()</code> method has been
     * called a default <code>MuleWorkManager</code> will be created using the
     * <i>DefaultThreadingProfile</i> on the <code>MuleConfiguration</code>
     * object.
     *
     * @param workManager the workManager instance used by the current MuleManager
     * @throws IllegalStateException if the workManager has already been set.
     * @see org.mule.config.ThreadingProfile
     * @see MuleConfiguration
     * @see MuleWorkManager
     */
    public void setWorkManager(UMOWorkManager workManager)
    {
        if (this.workManager != null)
        {
            throw new IllegalStateException(new Message(Messages.CANT_SET_X_ONCE_IT_HAS_BEEN_SET,
                    "workManager").getMessage());
        }
        this.workManager = workManager;
    }

    public QueueManager getQueueManager()
    {
        return queueManager;
    }

    public void setQueueManager(QueueManager queueManager)
    {
        this.queueManager = queueManager;
    }

    /**
     * The shutdown thread used by the server when its main thread is terminated
     * 
     * @deprecated This is now handled by the OSGi lifecycle or the Service Wrapper
     */
    private class ShutdownThread extends Thread
    {
        Throwable t;
        boolean aggressive = true;

        public ShutdownThread()
        {
            super();
            this.t = shutdownContext.getException();
            this.aggressive = shutdownContext.isAggressive();
        }

        /*
         * (non-Javadoc)
         *
         * @see java.lang.Runnable#run()
         */
        public void run()
        {
            dispose();
            if (!aggressive)
            {
                // FIX need to check if there are any outstanding
                // operations to be done?
            }

            if (server != null)
            {
                if (t != null)
                {
                    server.shutdown(t);
                }
                else
                {
                    server.shutdown();
                }
            }
        }
    }

    /**
     * @deprecated This is now handled by the OSGi lifecycle or the Service Wrapper
     */
    private class ShutdownContext
    {
        private boolean aggressive = false;
        private Throwable exception = null;

        public ShutdownContext(boolean aggressive, Throwable exception)
        {
            this.aggressive = aggressive;
            this.exception = exception;
        }

        public boolean isAggressive()
        {
            return aggressive;
        }

        public Throwable getException()
        {
            return exception;
        }
    }
}
