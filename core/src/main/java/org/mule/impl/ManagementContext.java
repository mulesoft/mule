/*
 * $Id$
 * --------------------------------------------------------------------------------------
 * Copyright (c) MuleSource, Inc.  All rights reserved.  http://www.mulesource.com
 *
 * The software in this package is published under the terms of the MuleSource MPL
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.impl;

import org.mule.MuleRuntimeException;
import org.mule.RegistryContext;
import org.mule.config.MuleConfiguration;
import org.mule.config.MuleProperties;
import org.mule.config.ThreadingProfile;
import org.mule.config.i18n.Message;
import org.mule.config.i18n.Messages;
import org.mule.config.spring.RegistryFacade;
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
import org.mule.registry.DeregistrationException;
import org.mule.registry.RegistrationException;
import org.mule.umo.UMOException;
import org.mule.umo.UMOManagementContext;
import org.mule.umo.lifecycle.FatalException;
import org.mule.umo.lifecycle.InitialisationException;
import org.mule.umo.lifecycle.LifecycleException;
import org.mule.umo.manager.UMOServerNotification;
import org.mule.umo.manager.UMOServerNotificationListener;
import org.mule.umo.manager.UMOWorkManager;
import org.mule.umo.model.UMOModel;
import org.mule.umo.security.UMOSecurityManager;
import org.mule.umo.store.UMOStore;
import org.mule.util.DateUtils;
import org.mule.util.StringMessageUtils;
import org.mule.util.queue.CachingPersistenceStrategy;
import org.mule.util.queue.MemoryPersistenceStrategy;
import org.mule.util.queue.QueueManager;
import org.mule.util.queue.QueuePersistenceStrategy;
import org.mule.util.queue.TransactionalQueueManager;

import java.net.InetAddress;
import java.net.UnknownHostException;
import java.nio.charset.Charset;
import java.text.DateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.jar.Manifest;
import java.io.File;

import javax.transaction.TransactionManager;

import org.apache.commons.lang.StringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

/**
 * TODO document
 */
public class ManagementContext implements UMOManagementContext
{
    /**
     * logger used by this class
     */
    private static transient Log logger = LogFactory.getLog(ManagementContext.class);

    /**
     * Default configuration
     */
    private MuleConfiguration config;

    /**
     * the unique id for this manager
     */
    private String id = null;

    /**
     * If this node is part of a cluster then this is the shared cluster Id
     */
    private String clusterId = null;

    /**
     * The domain name that this instance belongs to.
     */
    private String domain = null;

    /**
     * True once the Mule Manager is initialised
     */
    private volatile boolean initialised = false;

    private volatile boolean initialising = false;

    /**
     * Determines of the MuleManager has been started
     */
    private volatile boolean started = false;

    /**
     * True if this node is starting up
     */
    private volatile boolean starting = false;

    /**
     * True if this node is in the process of stopping
     */
    private volatile boolean stopping = false;

    /**
     * Determines if the manager has been disposed
     */
    private volatile boolean disposed = false;

    /**
     * Holds a reference to the deamon running the Manager if any
     * TODO RM* Can we move this out of here?
     */
    //private static MuleServer server = null;

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

    private UMOSecurityManager securityManager;

    private UMOWorkManager workManager;

    /**
     * The queue manager to use for component queues and vm queues
     */
    private QueueManager queueManager;

    /**
     * The transaction manager to use for this instance.
     */
    protected TransactionManager transactionManager;

    /**
     * Abitary set of properties that can be set on the context
     * TODO RM*: I think we can remove this
     */
    protected Map properties = new HashMap();

    protected Directories directories;

    protected String systemName;

    public ManagementContext()
    {
        securityManager = new MuleSecurityManager();
        startDate = System.currentTimeMillis();
    }

    public void initialise() throws UMOException
    {
        if (!initialised)
        {
            initialising = true;

            config = getRegistry().getConfiguration();
            validateEncoding();
            validateOSEncoding();
            directories = new Directories(new File(config.getWorkingDirectory()));            

            if(getRegistry().lookupModel(ModelHelper.SYSTEM_MODEL)==null)
            {
                UMOModel system = ModelFactory.createModel(getRegistry().getConfiguration().getSystemModelType());
                system.setName(ModelHelper.SYSTEM_MODEL);
                getRegistry().registerModel(system);
            }
            // if no work manager has been set create a default one
            if (workManager == null)
            {
                ThreadingProfile tp = config.getDefaultThreadingProfile();
                logger.debug("Creating default work manager using default threading profile: " + tp);
                workManager = new MuleWorkManager(tp, "UMOManager");
                workManager.start();
            }

            // create the event manager
            notificationManager = new ServerNotificationManager();
            //Todo these should be configurable
            notificationManager.registerEventType(ManagerNotification.class, ManagerNotificationListener.class);
            notificationManager.registerEventType(ModelNotification.class, ModelNotificationListener.class);
            notificationManager.registerEventType(ComponentNotification.class, ComponentNotificationListener.class);
            notificationManager.registerEventType(SecurityNotification.class, SecurityNotificationListener.class);
            notificationManager.registerEventType(ManagementNotification.class, ManagementNotificationListener.class);
            notificationManager.registerEventType(AdminNotification.class, AdminNotificationListener.class);
            notificationManager.registerEventType(CustomNotification.class, CustomNotificationListener.class);
            notificationManager.registerEventType(ConnectionNotification.class, ConnectionNotificationListener.class);
            notificationManager.start(workManager);
            // TODO MERGE no such method?
            //if (config.isEnableMessageEvents())
            //{
            //    notificationManager.registerEventType(MessageNotification.class, MessageNotificationListener.class);
            //}

            fireSystemEvent(new ManagerNotification(id, clusterId, domain, ManagerNotification.MANAGER_INITIALISNG));
            id = config.getId();
            clusterId = config.getClusterId();
            domain = config.getDomainId();

            if (id == null)
            {
                logger.warn("No unique id has been set on this manager");
            }

            try
            {
                if (securityManager != null)
                {
                    securityManager.initialise(this);
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
                        throw new InitialisationException(new Message(Messages.INITIALISATION_FAILURE_X, "QueueManager"),
                                e);
                    }
                }

                directories.createDirectories();
                //TODO LM: we still need the MuleManager until the Registry Looks after these objects
                //initialise a Mule instance that will manage our connections
                //TODO LM: Load Registry
                //btw the registry should handle multiple models

                //TODO LM: Grab TransactionManager from the registry

                if (id == null)
                {
                    throw new InitialisationException(new Message(Messages.X_IS_NULL, "Instance ID"), this);
                }
                if (clusterId == null)
                {
                    clusterId = new Message(Messages.NOT_CLUSTERED).toString();
                }
                if (domain == null)
                {
                    domain = InetAddress.getLocalHost().getHostName();
                }

                systemName = domain + "." + clusterId + "." + id;
            }
            catch (UMOException e)
            {
                initialising = false;

                throw e;
            }
            catch (Exception e)
            {
                initialising = false;
                throw new LifecycleException(e, this);
            }
            finally
            {
                fireSystemEvent(new ManagerNotification(id, clusterId, domain, ManagerNotification.MANAGER_INITIALISED));
            }
        }
        getRegistry().initialise(this);
        
        initialised = true;
    }


    public synchronized void start() throws UMOException
    {
        initialise();

        if (!started)
        {
            starting = true;
            fireSystemEvent(new ManagerNotification(id, clusterId, domain, ManagerNotification.MANAGER_STARTING));
            if (queueManager != null)
            {
                queueManager.start();
            }

            //TODO LM: start the registry
            getRegistry().start();
            directories.deleteMarkedDirectories();

            starting = false;
            started = true;
            if (logger.isInfoEnabled())
            {
                logger.info(getStartSplash());
            }
            else
            {
                System.out.println(getStartSplash());
            }
            fireSystemEvent(new ManagerNotification(id, clusterId, domain, ManagerNotification.MANAGER_STARTED));
        }
    }

    /**
     * Stops the <code>MuleManager</code> which stops all sessions and
     * connectors
     *
     * @throws UMOException if either any of the sessions or connectors fail to
     *                      stop
     */
    public synchronized void stop() throws UMOException
    {
        started = false;
        stopping = true;
        fireSystemEvent(new ManagerNotification(id, clusterId, domain, ManagerNotification.MANAGER_STOPPING));

        if (queueManager != null)
        {
            queueManager.stop();
        }

        getRegistry().stop();

        stopping = false;
        fireSystemEvent(new ManagerNotification(id, clusterId, domain, ManagerNotification.MANAGER_STOPPED));
    }

    public void dispose()
    {
        fireSystemEvent(new ManagerNotification(id, clusterId, domain, ManagerNotification.MANAGER_DISPOSING));


        if (disposed)
        {
            return;
        }
        try
        {
            if (started)
            {
                stop();
            }
        }
        catch (UMOException e)
        {
            logger.error("Failed to stop manager: " + e.getMessage(), e);
        }
        disposed = true;
        // props.clearErrors();
        fireSystemEvent(new ManagerNotification(id, clusterId, domain, ManagerNotification.MANAGER_DISPOSED));

        if (getRegistry() != null)
        {
            getRegistry().dispose();
        }

        initialised = false;
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

        if (logger.isInfoEnabled())
        {
            logger.info(getEndSplash());
        }
        else
        {
            System.out.println(getEndSplash());
        }
        config = new MuleConfiguration();
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
        //Check we have a valid and supported encoding
        if (!Charset.isSupported(config.getDefaultEncoding()))
        {
            throw new FatalException(new Message(Messages.PROPERTY_X_HAS_INVALID_VALUE_X, "encoding", config.getDefaultEncoding()), this);
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

    public RegistryFacade getRegistry()
    {
        return RegistryContext.getRegistry();
    }

    public void setRegistry(RegistryFacade registry)
    {
        RegistryContext.setRegistry(registry);
    }

    public String getSystemName()
    {
        return systemName;
    }

    public void setSystemName(String systemName)
    {
        this.systemName = systemName;
    }

    public UMOStore getStore(String name) throws UMOException
    {
        //TODO LM: get store from registry
        return null;
    }

    public UMOStore createStore(String name) throws UMOException
    {
        //TODO LM: backed by registry
        return null;
    }

    public void removeStore(UMOStore store)
    {
        //TODO LM: get store from registry
        store.dispose();
    }

    /**
     * Getter for the envionment parameters declared in the mule-config.xml
     *
     * @param key the propery name
     * @return the property value
     */
    public Object getProperty(Object key)
    {
        return properties.get(key);
    }

    /**
     * Sets an Mule environment parameter in the <code>MuleManager</code>.
     *
     * @param key   the parameter name
     * @param value the parameter value
     */
    public void setProperty(Object key, Object value)
    {
        properties.put(key, value);
    }

    /**
     * Sets the Jta Transaction Manager to use with this Mule server instance
     *
     * @param manager the manager to use
     * @throws Exception
     */
    public void setTransactionManager(TransactionManager manager) throws Exception
    {
        transactionManager = manager;
    }

    /**
     * Returns the Jta transaction manager used by this Mule server instance. or
     * null if a transaction manager has not been set
     *
     * @return the Jta transaction manager used by this Mule server instance. or
     *         null if a transaction manager has not been set
     */
    public TransactionManager getTransactionManager()
    {
        return transactionManager;
    }

    /**
     * Gets all properties associated with the UMOManager
     *
     * @return a map of properties on the Manager
     */
    public Map getProperties()
    {
        return properties;
    }

    public void addProperties(Map props)
    {
        properties.putAll(props);
    }

    /**
     * Determines if the server has been started
     *
     * @return true if the server has been started
     */
    public boolean isStarted()
    {
        return started;
    }

    /**
     * Determines if the server has been initialised
     *
     * @return true if the server has been initialised
     */
    public boolean isInitialised()
    {
        return initialised;
    }

    /**
     * Returns the long date when the server was started
     *
     * @return the long date when the server was started
     */
    public long getStartDate()
    {
        return startDate;
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

    public Directories getDirectories()
    {
        return directories;
    }

    public void registerListener(UMOServerNotificationListener l) throws NotificationException
    {
        registerListener(l, null);
    }

    public void registerListener(UMOServerNotificationListener l, String resourceIdentifier) throws NotificationException
    {
        if (notificationManager == null)
        {
            throw new MuleRuntimeException(new Message(Messages.SERVER_EVENT_MANAGER_NOT_ENABLED));
        }
        notificationManager.registerListener(l, resourceIdentifier);
    }

    public void unregisterListener(UMOServerNotificationListener l)
    {
        if (notificationManager != null)
        {
            notificationManager.unregisterListener(l);
        }
    }

    /**
     * Fires a mule 'system' event. These are notifications that are fired because
     * something within the Mule instance happened such as the Model started or
     * the server is being disposed.
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
     * {@link org.mule.impl.internal.notifications.CustomNotificationListener} notificationManager.
     *
     * @param notification the notification to fire. This must be of type
     *                     {@link org.mule.impl.internal.notifications.CustomNotification} otherwise an
     *                     exception will be thrown.
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


    public String getDomain()
    {
        return domain;
    }

    public void setDomain(String domain)
    {
        this.domain = domain;
    }

    public String getClusterId()
    {
        return clusterId;
    }

    public void setClusterId(String clusterId)
    {
        this.clusterId = clusterId;
    }

    /**
     * Sets the security manager used by this Mule instance to authenticate and
     * authorise incoming and outgoing event traffic and service invocations
     *
     * @param securityManager the security manager used by this Mule instance to
     *                        authenticate and authorise incoming and outgoing event traffic
     *                        and service invocations
     */
    public void setSecurityManager(UMOSecurityManager securityManager) throws InitialisationException
    {
        this.securityManager = securityManager;
        if (securityManager != null && isInitialised())
        {
            this.securityManager.initialise(this);
        }
    }

    /**
     * Gets the security manager used by this Mule instance to authenticate and
     * authorise incoming and outgoing event traffic and service invocations
     *
     * @return he security manager used by this Mule instance to authenticate
     *         and authorise incoming and outgoing event traffic and service
     *         invocations
     */
    public UMOSecurityManager getSecurityManager()
    {
        return securityManager;
    }

    /**
     * Obtains a workManager instance that can be used to schedule work in a
     * thread pool. This will be used primarially by UMOAgents wanting to
     * schedule work. This work Manager must <b>never</b> be used by provider
     * implementations as they have their own workManager accible on the
     * connector.
     * <p/>
     * If a workManager has not been set by the time the
     * <code>initialise()</code> method has been called a default
     * <code>MuleWorkManager</code> will be created using the
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
     * Obtains a workManager instance that can be used to schedule work in a
     * thread pool. This will be used primarially by UMOAgents wanting to
     * schedule work. This work Manager must <b>never</b> be used by provider
     * implementations as they have their own workManager accible on the
     * connector.
     * <p/>
     * If a workManager has not been set by the time the
     * <code>initialise()</code> method has been called a default
     * <code>MuleWorkManager</code> will be created using the
     * <i>DefaultThreadingProfile</i> on the <code>MuleConfiguration</code>
     * object.
     *
     * @param workManager the workManager instance used by the current
     *                    MuleManager
     * @throws IllegalStateException if the workManager has already been set.
     * @see org.mule.config.ThreadingProfile
     * @see MuleConfiguration
     * @see org.mule.impl.work.MuleWorkManager
     */
    public void setWorkManager(UMOWorkManager workManager)
    {
        if (this.workManager != null)
        {
            throw new IllegalStateException(new Message(Messages.CANT_SET_X_ONCE_IT_HAS_BEEN_SET, "workManager").getMessage());
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
     * Determines if the server is being initialised
     *
     * @return true if the server is beening initialised
     */
    public boolean isInitialising()
    {
        return initialising;
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
        //TODO LM: get agensts from the registry
//        if (agents.size() == 0)
//        {
//            message.add(new Message(Messages.AGENTS_RUNNING).getMessage() + " "
//                        + new Message(Messages.NONE).getMessage());
//        }
//        else
//        {
//            message.add(new Message(Messages.AGENTS_RUNNING).getMessage());
//            UMOAgent umoAgent;
//            for (Iterator iterator = agents.values().iterator(); iterator.hasNext();)
//            {
//                umoAgent = (UMOAgent)iterator.next();
//                message.add("  " + umoAgent.getDescription());
//            }
//        }
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


    public void register() throws RegistrationException
    {
        throw new UnsupportedOperationException("register");
    }

    public void deregister() throws DeregistrationException
    {
        throw new UnsupportedOperationException("deregister");
    }

    public String getRegistryId()
    {
        throw new UnsupportedOperationException("registryId");
    }
}
