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
import org.mule.impl.model.ModelFactory;
import org.mule.impl.model.ModelHelper;
import org.mule.impl.security.MuleSecurityManager;
import org.mule.impl.work.MuleWorkManager;
import org.mule.management.stats.AllStatistics;
import org.mule.registry.AbstractServiceDescriptor;
import org.mule.registry.DeregistrationException;
import org.mule.registry.RegistrationException;
import org.mule.registry.Registry;
import org.mule.registry.ServiceDescriptor;
import org.mule.registry.ServiceDescriptorFactory;
import org.mule.registry.ServiceException;
import org.mule.registry.impl.DummyRegistry;
import org.mule.registry.impl.RegistryNotificationListener;
import org.mule.umo.UMOException;
import org.mule.umo.UMOInterceptorStack;
import org.mule.umo.endpoint.UMOEndpoint;
import org.mule.umo.lifecycle.FatalException;
import org.mule.umo.lifecycle.InitialisationException;
import org.mule.umo.manager.UMOAgent;
import org.mule.umo.manager.UMOContainerContext;
import org.mule.umo.manager.UMOManager;
import org.mule.umo.manager.UMOServerNotification;
import org.mule.umo.manager.UMOServerNotificationListener;
import org.mule.umo.manager.UMOWorkManager;
import org.mule.umo.model.UMOModel;
import org.mule.umo.provider.UMOConnector;
import org.mule.umo.security.UMOSecurityManager;
import org.mule.umo.transformer.UMOTransformer;
import org.mule.util.ClassUtils;
import org.mule.util.CollectionUtils;
import org.mule.util.DateUtils;
import org.mule.util.MapUtils;
import org.mule.util.SpiUtils;
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
import java.util.Collection;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.jar.Manifest;

import javax.transaction.TransactionManager;

import org.apache.commons.collections.list.CursorableLinkedList;
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
    /**
     * Service descriptor cache. 
     * 
     * @deprecated This needs to be redesigned for an OSGi environment where ServiceDescriptors may change.
     */
    // @GuardedBy("this")
    protected static Map sdCache = new HashMap();

    /**
     * singleton instance
     */
    private static UMOManager instance = null;

    /**
     * Default configuration
     */
    private static MuleConfiguration config = new MuleConfiguration();

    /**
     * Connectors registry
     */
    private Map connectors = new HashMap();

    /**
     * Endpoints registry
     */
    private Map endpointIdentifiers = new HashMap();

    /**
     * Holds any application scoped environment properties set in the config
     */
    private Map applicationProps = new HashMap();

    /**
     * Holds any registered agents
     */
    private Map agents = new LinkedHashMap();

    /**
     * Holds a list of global endpoints accessible to any client code
     */
    private Map endpoints = new HashMap();

    /**
     * The model being used
     */
    private Map models = new LinkedHashMap();

    /**
     * the unique id for this manager
     */
    private String id = UUID.getUUID();

    /**
     * The transaction Manager to use for global transactions
     */
    private TransactionManager transactionManager = null;

    /**
     * Collection for transformers registered in this component
     */
    private Map transformers = new HashMap();

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
     * Maintains a reference to any interceptor stacks configured on the manager
     */
    private Map interceptorsMap = new HashMap();

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
     * Registry
     */
    private Registry registry;

    /**
     * Registration ID for this Manager
     */
    private String registryId = null;

    /**
     * logger used by this class
     */
    private static Log logger = LogFactory.getLog(MuleManager.class);

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
                //There should always be a defualt system model registered
                instance = new MuleManager();
                UMOModel model = ModelFactory.createModel(ModelHelper.getSystemModelType());
                model.setName(ModelHelper.SYSTEM_MODEL);
                instance.registerModel(model);
            }
            catch (Exception e)
            {
                throw new MuleRuntimeException(new Message(Messages.FAILED_TO_CREATE_MANAGER_INSTANCE_X,
                    MuleManager.class.getName()), e);
            }
        }

        return instance;
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
        disposeConnectors();

        for (Iterator i = models.values().iterator(); i.hasNext();)
        {
            UMOModel model = (UMOModel) i.next();
            model.dispose();
        }

        disposeAgents();

        transformers.clear();
        endpoints.clear();
        endpointIdentifiers.clear();
        containerContext.dispose();
        containerContext = null;
        // props.clearErrors();
        fireSystemEvent(new ManagerNotification(id, null, null, ManagerNotification.MANAGER_DISPOSED));

        if (registry != null)
        {
            registry.dispose();
            registry = null;
        }

        transformers = null;
        endpoints = null;
        endpointIdentifiers = null;
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
     * Destroys all connectors
     */
    private synchronized void disposeConnectors()
    {
        fireSystemEvent(new ManagerNotification(id, null, null, ManagerNotification.MANAGER_DISPOSING_CONNECTORS));
        for (Iterator iterator = connectors.values().iterator(); iterator.hasNext();)
        {
            UMOConnector c = (UMOConnector) iterator.next();
            c.dispose();
        }
        fireSystemEvent(new ManagerNotification(id, null, null, ManagerNotification.MANAGER_DISPOSED_CONNECTORS));
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
    public UMOConnector lookupConnector(String name)
    {
        return (UMOConnector) connectors.get(name);
    }

    /**
     * {@inheritDoc}
     */
    public String lookupEndpointIdentifier(String logicalName, String defaultName)
    {
        String name = (String) endpointIdentifiers.get(logicalName);
        if (name == null)
        {
            return defaultName;
        }
        return name;
    }

    /**
     * {@inheritDoc}
     */
    public UMOEndpoint lookupEndpoint(String logicalName)
    {
        UMOEndpoint endpoint = (UMOEndpoint) endpoints.get(logicalName);
        if (endpoint != null)
        {
            return (UMOEndpoint) endpoint.clone();
        }
        else
        {
            return null;
        }
    }

    /**
     * {@inheritDoc}
     */
    public UMOEndpoint lookupEndpointByAddress(String address)
    {
        UMOEndpoint endpoint = null;
        if (address != null)
        {
            boolean found = false;
            Iterator iterator = endpoints.keySet().iterator();
            while (!found && iterator.hasNext())
            {
                endpoint = (UMOEndpoint) endpoints.get(iterator.next());
                found = (address.equals(endpoint.getEndpointURI().toString()));
            }
        }
        return endpoint;
    }

    /**
     * {@inheritDoc}
     */
    public UMOTransformer lookupTransformer(String name)
    {
         return (UMOTransformer)transformers.get(name);
    }

    /**
     * {@inheritDoc}
     */
    public void registerConnector(UMOConnector connector) throws UMOException
    {
        connectors.put(connector.getName(), connector);
        if (initialised.get() || initialising.get())
        {
            connector.initialise();
        }
        if ((started.get() || starting.get()) && !connector.isStarted())
        {
            connector.startConnector();
        }
    }

    /**
     * {@inheritDoc}
     */
    public void unregisterConnector(String connectorName) throws UMOException
    {
        UMOConnector c = (UMOConnector) connectors.remove(connectorName);
        if (c != null)
        {
            c.dispose();
        }
    }

    /**
     * {@inheritDoc}
     */
    public void registerEndpointIdentifier(String logicalName, String endpoint)
    {
        endpointIdentifiers.put(logicalName, endpoint);
    }

    /**
     * {@inheritDoc}
     */
    public void unregisterEndpointIdentifier(String logicalName)
    {
        endpointIdentifiers.remove(logicalName);
    }

    /**
     * {@inheritDoc}
     */
    public void registerEndpoint(UMOEndpoint endpoint)
    {
        endpoints.put(endpoint.getName(), endpoint);
    }

    /**
     * {@inheritDoc}
     */
    public void unregisterEndpoint(String endpointName)
    {
        UMOEndpoint p = (UMOEndpoint) endpoints.get(endpointName);
        if (p != null)
        {
            endpoints.remove(p);
        }
    }

    /**
     * {@inheritDoc}
     */
    public void registerTransformer(UMOTransformer transformer) throws InitialisationException
    {
        transformer.initialise();

        // For now at least, we don't want a registration error to affect
        // the initialisation process.
       // try
       // {
            //TODO LM: Method not implemented yet?
            //transformer.register();
       // }
       // catch (RegistrationException re)
       // {
       //     logger.warn(re);
       // }

        transformers.put(transformer.getName(), transformer);
        logger.info("Transformer " + transformer.getName() + " has been initialised successfully");
    }

    /**
     * {@inheritDoc}
     */
    public void unregisterTransformer(String transformerName)
    {
        transformers.remove(transformerName);
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

    /**
     */
    public void setRegistry(Registry registry)
    {
        this.registry = registry;
    }

    /*
     * (non-Javadoc)
     * 
     * @see org.mule.umo.lifecycle.Registerable#register()
     */
    public void register() throws RegistrationException
    {
        registryId = getRegistry().registerMuleObject(null, this).getId();
    }

    /*
     * (non-Javadoc)
     * 
     * @see org.mule.umo.lifecycle.Registerable#deregister()
     */
    public void deregister() throws DeregistrationException
    {
        getRegistry().deregisterComponent(registryId);
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

    /**
     * {@inheritDoc}
     */
    public synchronized void initialise() throws UMOException
    {
        validateEncoding();
        validateOSEncoding();

        if (!initialised.get())
        {
            initialising.set(true);
            startDate = System.currentTimeMillis();

            // Start the registry
            if (registry == null)
            {
                createRegistry();
            }

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

                initialiseConnectors();
                initialiseEndpoints();
                initialiseAgents();
                for (Iterator i = models.values().iterator(); i.hasNext();)
                {
                    UMOModel model = (UMOModel) i.next();
                    model.initialise();
                    //TODO LM: Should the model be registered before or after initialisation?
                    model.register();
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

    protected void registerAdminAgent() throws UMOException
    {
        // Allows users to disable all server components and connections
        // this can be useful for testing
        boolean disable = MapUtils.getBooleanValue(System.getProperties(),
                MuleProperties.DISABLE_SERVER_CONNECTIONS_SYSTEM_PROPERTY, false);

        // if endpointUri is blanked out do not setup server components
        //TODO RM* Admin agent should be explicit
//        if (StringUtils.isBlank(config.getServerUrl()))
//        {
//            logger.info("Server endpointUri is null, not registering Mule Admin agent");
//            disable = true;
//        }
//
//        if (disable)
//        {
//            unregisterAgent(MuleAdminAgent.AGENT_NAME);
//        }
//        else
//        {
//            if (lookupAgent(MuleAdminAgent.AGENT_NAME) == null)
//            {
//                registerAgent(new MuleAdminAgent());
//            }
//        }
    }

    protected void initialiseEndpoints() throws InitialisationException
    {
        UMOEndpoint ep;
        for (Iterator iterator = this.endpoints.values().iterator(); iterator.hasNext();)
        {
            ep = (UMOEndpoint) iterator.next();
            ep.initialise();
            // the connector has been created for this endpoint so lets
            // set the create connector to 0 so that every time this endpoint
            // is referenced we don't create another connector
            ep.setCreateConnector(0);
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
            registerAdminAgent();
            if (queueManager != null)
            {
                queueManager.start();
            }
            startConnectors();
            startAgents();
            fireSystemEvent(new ManagerNotification(id, null, null, ManagerNotification.MANAGER_STARTING_MODELS));
            for (Iterator i = models.values().iterator(); i.hasNext();)
            {
                UMOModel model = (UMOModel) i.next();
                model.start();
            }
            fireSystemEvent(new ManagerNotification(id, null, null, ManagerNotification.MANAGER_STARTED_MODELS));

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
     * Starts the connectors
     *
     * @throws MuleException if the connectors fail to start
     */
    private void startConnectors() throws UMOException
    {
        for (Iterator iterator = connectors.values().iterator(); iterator.hasNext();)
        {
            UMOConnector c = (UMOConnector) iterator.next();
            c.startConnector();
        }
        logger.info("Connectors have been started successfully");
    }

    private void initialiseConnectors() throws InitialisationException
    {
        for (Iterator iterator = connectors.values().iterator(); iterator.hasNext();)
        {
            UMOConnector c = (UMOConnector) iterator.next();
            c.initialise();
        }
        logger.info("Connectors have been initialised successfully");
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

        stopConnectors();
        stopAgents();

        if (queueManager != null)
        {
            queueManager.stop();
        }

        logger.debug("Stopping model...");
        fireSystemEvent(new ManagerNotification(id, null, null, ManagerNotification.MANAGER_STOPPING_MODELS));
        for (Iterator i = models.values().iterator(); i.hasNext();)
        {
            UMOModel model = (UMOModel) i.next();
            model.stop();
        }
        fireSystemEvent(new ManagerNotification(id, null, null, ManagerNotification.MANAGER_STOPPED_MODELS));

        if (registry != null)
        {
            registry.stop();
        }

        stopping.set(false);
        fireSystemEvent(new ManagerNotification(id, null, null, ManagerNotification.MANAGER_STOPPED));
    }

    /**
     * Stops the connectors
     *
     * @throws MuleException if any of the connectors fail to stop
     */
    private void stopConnectors() throws UMOException
    {
        logger.debug("Stopping connectors...");
        for (Iterator iterator = connectors.values().iterator(); iterator.hasNext();)
        {
            UMOConnector c = (UMOConnector) iterator.next();
            c.stopConnector();
        }
        logger.info("Connectors have been stopped successfully");
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

    public UMOModel lookupModel(String name)
    {
        // TODO LM: why are we checking if the registry is null here?  This should be done in a lifecycle phase
        if (registry == null)
        {
            createRegistry();
        }

        return (UMOModel)models.get(name);
    }

    public void registerModel(UMOModel model) throws UMOException
    {
        models.put(model.getName(), model);
        if (initialised.get())
        {
            model.register();
            model.initialise();
        }

        if (started.get())
        {
            model.start();
        }
    }

    public void unregisterModel(String name)
    {
        UMOModel model = lookupModel(name);
        if(model!=null)
        {
            models.remove(model);
            model.dispose();
        }
    }

    public Map getModels()
    {
        return Collections.unmodifiableMap(models);
    }

    /**
     * {@inheritDoc}
     */
    public void registerInterceptorStack(String name, UMOInterceptorStack stack)
    {
        interceptorsMap.put(name, stack);
    }

    /**
     * {@inheritDoc}
     */
    public UMOInterceptorStack lookupInterceptorStack(String name)
    {
        return (UMOInterceptorStack) interceptorsMap.get(name);
    }

    /**
     * {@inheritDoc}
     */
    public Map getConnectors()
    {
        return Collections.unmodifiableMap(connectors);
    }

    /**
     * {@inheritDoc}
     */
    public Map getEndpointIdentifiers()
    {
        return Collections.unmodifiableMap(endpointIdentifiers);
    }

    /**
     * {@inheritDoc}
     */
    public Map getEndpoints()
    {
        return Collections.unmodifiableMap(endpoints);
    }

    /**
     * {@inheritDoc}
     */
    public Map getTransformers()
    {
        return Collections.unmodifiableMap(transformers);
    }

    /**
     * {@inheritDoc}
     */
    public Registry getRegistry()
    {
        return registry;
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
    protected String getStartSplash()
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
     * {@inheritDoc}
     */
    public void registerAgent(UMOAgent agent) throws UMOException
    {
        logger.info("Adding agent " + agent.getName());
        agents.put(agent.getName(), agent);
        agent.registered();
        // Don't allow initialisation while the server is being initalised,
        // only when we are done. Otherwise the agent registration
        // order can be corrupted.
        if (initialised.get())
        {
            logger.info("Initialising agent " + agent.getName());
            agent.initialise();
        }
        if ((started.get() || starting.get()))
        {
            logger.info("Starting agent " + agent.getName());
            agent.start();
        }
    }

    public UMOAgent lookupAgent(String name)
    {
        return (UMOAgent) agents.get(name);
    }

    /**
     * {@inheritDoc}
     */
    public UMOAgent unregisterAgent(String name) throws UMOException
    {
        if (name == null)
        {
            return null;
        }
        UMOAgent agent = (UMOAgent) agents.remove(name);
        if (agent != null)
        {
            agent.dispose();
            agent.unregistered();
        }
        return agent;
    }

    /**
     * Initialises all registered agents
     *
     * @throws InitialisationException
     */
    protected void initialiseAgents() throws InitialisationException
    {
        logger.info("Initialising agents...");

        // Do not iterate over the map directly, as 'complex' agents
        // may spawn extra agents during initialisation. This will
        // cause a ConcurrentModificationException.
        // Use a cursorable iteration, which supports on-the-fly underlying
        // data structure changes.
        Collection agentsSnapshot = agents.values();
        CursorableLinkedList agentRegistrationQueue = new CursorableLinkedList(agentsSnapshot);
        CursorableLinkedList.Cursor cursor = agentRegistrationQueue.cursor();

        // the actual agent object refs are the same, so we are just
        // providing different views of the same underlying data

        try
        {
            while (cursor.hasNext())
            {
                UMOAgent umoAgent = (UMOAgent) cursor.next();

                int originalSize = agentsSnapshot.size();
                logger.debug("Initialising agent: " + umoAgent.getName());
                umoAgent.initialise();
                // thank you, we are done with you
                cursor.remove();

                // Direct calls to MuleManager.registerAgent() modify the original
                // agents map, re-check if the above agent registered any
                // 'child' agents.
                int newSize = agentsSnapshot.size();
                int delta = newSize - originalSize;
                if (delta > 0)
                {
                    // TODO there's some mess going on in
                    // http://issues.apache.org/jira/browse/COLLECTIONS-219
                    // watch out when upgrading the commons-collections.
                    Collection tail = CollectionUtils.retainAll(agentsSnapshot, agentRegistrationQueue);
                    Collection head = CollectionUtils.subtract(agentsSnapshot, tail);

                    // again, above are only refs, all going back to the original agents map

                    // re-order the queue
                    agentRegistrationQueue.clear();
                    // 'spawned' agents first
                    agentRegistrationQueue.addAll(head);
                    // and the rest
                    agentRegistrationQueue.addAll(tail);

                    // update agents map with a new order in case we want to re-initialise
                    // MuleManager on the fly
                    this.agents.clear();
                    for (Iterator it = agentRegistrationQueue.iterator(); it.hasNext();)
                    {
                        UMOAgent theAgent = (UMOAgent) it.next();
                        this.agents.put(theAgent.getName(), theAgent);
                    }
                }
            }
        }
        finally
        {
            // close the cursor as per JavaDoc
            cursor.close();
        }
        logger.info("Agents Successfully Initialised");
    }

    /**
     * {@inheritDoc}
     */
    protected void startAgents() throws UMOException
    {
        UMOAgent umoAgent;
        logger.info("Starting agents...");
        for (Iterator iterator = agents.values().iterator(); iterator.hasNext();)
        {
            umoAgent = (UMOAgent) iterator.next();
            logger.info("Starting agent: " + umoAgent.getDescription());
            umoAgent.start();

        }
        logger.info("Agents Successfully Started");
    }

    /**
     * {@inheritDoc}
     */
    protected void stopAgents() throws UMOException
    {
        logger.info("Stopping agents...");
        for (Iterator iterator = agents.values().iterator(); iterator.hasNext();)
        {
            UMOAgent umoAgent = (UMOAgent) iterator.next();
            logger.debug("Stopping agent: " + umoAgent.getName());
            umoAgent.stop();
        }
        logger.info("Agents Successfully Stopped");
    }

    /**
     * {@inheritDoc}
     */
    protected void disposeAgents()
    {
        UMOAgent umoAgent;
        logger.info("disposing agents...");
        for (Iterator iterator = agents.values().iterator(); iterator.hasNext();)
        {
            umoAgent = (UMOAgent) iterator.next();
            logger.debug("Disposing agent: " + umoAgent.getName());
            umoAgent.dispose();
        }
        logger.info("Agents Successfully Disposed");
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
     * Looks up the service descriptor from a singleton cache and creates a new one if not found.
     */
    public ServiceDescriptor lookupServiceDescriptor(String type, String name, Properties overrides) throws ServiceException
    {
        AbstractServiceDescriptor.Key key = new AbstractServiceDescriptor.Key(name, overrides);
        ServiceDescriptor sd = (ServiceDescriptor) sdCache.get(key);
      
        synchronized (this)
        {
            if (sd == null)
            {
                sd = createServiceDescriptor(type, name, overrides);

                sdCache.put(key, sd);
            }
        }
        return sd;
    }
        
    /**
     * @deprecated ServiceDescriptors will be created upon bundle startup for OSGi.
     */
    protected ServiceDescriptor createServiceDescriptor(String type, String name, Properties overrides) throws ServiceException
    {
        Properties props = SpiUtils.findServiceDescriptor(type, name);
        if(props==null)
        {
            throw new ServiceException(new Message(Messages.FAILED_LOAD_X, type + " " +name));
        }
        return ServiceDescriptorFactory.create(type, name, props, overrides);
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

    private void createRegistry()
    {
        try
        {
            Class clazz = Class.forName("org.mule.registry.impl.MuleRegistry");
            Object o = clazz.newInstance();
            registry = (Registry) o;
            registry.start();
            registerListener(new RegistryNotificationListener(registry));
        }
        catch (Exception e)
        {
            logger.warn("Couldn't create MuleRegistry: " + e.toString());
            logger.warn("Creating dummy registry so that things will run");
            registry = new DummyRegistry();
        }

        try
        {
            register();
        }
        catch (RegistrationException e)
        {
            logger.warn("Unable to register the manager: " + e.toString());
        }
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
