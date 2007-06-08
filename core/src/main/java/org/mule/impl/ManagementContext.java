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
import org.mule.config.MuleManifest;
import org.mule.config.MuleProperties;
import org.mule.config.i18n.CoreMessages;
import org.mule.impl.internal.notifications.ManagerNotification;
import org.mule.impl.internal.notifications.NotificationException;
import org.mule.impl.internal.notifications.ServerNotificationManager;
import org.mule.management.stats.AllStatistics;
import org.mule.registry.DeregistrationException;
import org.mule.registry.RegistrationException;
import org.mule.umo.UMOException;
import org.mule.umo.UMOManagementContext;
import org.mule.umo.lifecycle.Disposable;
import org.mule.umo.lifecycle.FatalException;
import org.mule.umo.lifecycle.Initialisable;
import org.mule.umo.lifecycle.InitialisationException;
import org.mule.umo.lifecycle.Startable;
import org.mule.umo.lifecycle.Stoppable;
import org.mule.umo.lifecycle.UMOLifecycleManager;
import org.mule.umo.manager.UMOAgent;
import org.mule.umo.manager.UMOServerNotification;
import org.mule.umo.manager.UMOServerNotificationListener;
import org.mule.umo.manager.UMOWorkManager;
import org.mule.umo.registry.RegistryFacade;
import org.mule.umo.security.UMOSecurityManager;
import org.mule.umo.store.UMOStore;
import org.mule.util.StringMessageUtils;
import org.mule.util.StringUtils;
import org.mule.util.queue.QueueManager;

import java.io.File;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.Date;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.jar.Manifest;

import javax.transaction.TransactionManager;

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

    protected UMOLifecycleManager lifecycleManager;

    protected Directories directories;

    protected String systemName;


    public ManagementContext(UMOLifecycleManager lifecycleManager)

    {
        if (lifecycleManager == null)
        {
            throw new NullPointerException(CoreMessages.objectIsNull("lifecycleManager").getMessage());
        }


        this.lifecycleManager = lifecycleManager;
        startDate = System.currentTimeMillis();

    }

    public void initialise() throws InitialisationException
    {
        lifecycleManager.checkPhase(Initialisable.PHASE_NAME);
        if (securityManager == null)
        {
            throw new NullPointerException(CoreMessages.objectIsNull("securityManager").getMessage());
        }

        if (notificationManager == null)
        {
            throw new NullPointerException(CoreMessages.objectIsNull("notificationManager").getMessage());
        }

        if (queueManager == null)
        {
            throw new NullPointerException(CoreMessages.objectIsNull("queueManager").getMessage());
        }

        if (workManager == null)
        {
            throw new NullPointerException(CoreMessages.objectIsNull("workManager").getMessage());
        }

        config = getRegistry().getConfiguration();
        if (config == null)
        {
            logger.info("A mule configuration object was not registered. Using defualt configuration");
            config = new MuleConfiguration();
        }

        try
        {
            setupIds();
            validateEncoding();
            validateOSEncoding();
            directories = new Directories(new File(config.getWorkingDirectory()));

            //We need to start the work manager straight away since we need it to fire notifications
            workManager.start();
            notificationManager.start(workManager);

            fireSystemEvent(new ManagerNotification(this, ManagerNotification.MANAGER_INITIALISING));

            directories.createDirectories();
            lifecycleManager.firePhase(this, Initialisable.PHASE_NAME);

            fireSystemEvent(new ManagerNotification(this, ManagerNotification.MANAGER_INITIALISED));
        }
        catch (Exception e)
        {
            throw new InitialisationException(e, this);
        }
    }


    protected void setupIds() throws InitialisationException
    {
        id = config.getId();
        clusterId = config.getClusterId();
        domain = config.getDomainId();

        if (id == null)
        {
            throw new InitialisationException(CoreMessages.objectIsNull("Instance ID"), this);
        }
        if (clusterId == null)
        {
            clusterId = CoreMessages.notClustered().getMessage();
        }
        if (domain == null)
        {
            try
            {
                domain = InetAddress.getLocalHost().getHostName();
            }
            catch (UnknownHostException e)
            {
                throw new InitialisationException(e, this);
            }
        }
        systemName = domain + "." + clusterId + "." + id;
    }

    public synchronized void start() throws UMOException
    {
        lifecycleManager.checkPhase(Startable.PHASE_NAME);
        if (!isStarted())
        {
            fireSystemEvent(new ManagerNotification(this, ManagerNotification.MANAGER_STARTING));

            directories.deleteMarkedDirectories();

            lifecycleManager.firePhase(this, Startable.PHASE_NAME);

            if (logger.isInfoEnabled())
            {
                logger.info(getStartSplash());
            }
            fireSystemEvent(new ManagerNotification(this, ManagerNotification.MANAGER_STARTED));
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
        lifecycleManager.checkPhase(Stoppable.PHASE_NAME);

        fireSystemEvent(new ManagerNotification(this, ManagerNotification.MANAGER_STOPPING));
        lifecycleManager.firePhase(this, Stoppable.PHASE_NAME);
        //TODO RM* :I dont think the Registry should be started or stopped...
        //getRegistry().stop();

        fireSystemEvent(new ManagerNotification(this, ManagerNotification.MANAGER_STOPPED));
    }

    public void dispose()
    {
       //TODO lifecycleManager.checkPhase(Disposable.PHASE_NAME);

        fireSystemEvent(new ManagerNotification(this, ManagerNotification.MANAGER_DISPOSING));


        if (isDisposed())
        {
            return;
        }
        try
        {
            if (isStarted())
            {
                stop();
            }
        }
        catch (UMOException e)
        {
            logger.error("Failed to stop manager: " + e.getMessage(), e);
        }

        try
        {
            lifecycleManager.firePhase(this, Disposable.PHASE_NAME);
        }
        catch (UMOException e)
        {
            logger.debug("Failed to cleanly dispose Mule: " + e.getMessage(), e);
        }

        fireSystemEvent(new ManagerNotification(this, ManagerNotification.MANAGER_DISPOSED));

        if (getRegistry() != null)
        {
            getRegistry().dispose();
        }

        if ((startDate > 0) && logger.isInfoEnabled())
        {
            logger.info(getEndSplash());
        }
        lifecycleManager.reset();
    }


    /**
     * Determines if the server has been initialised
     *
     * @return true if the server has been initialised
     */
    public boolean isInitialised()
    {
        return lifecycleManager.isPhaseComplete(Initialisable.PHASE_NAME);
    }

    /**
     * Determines if the server is being initialised
     *
     * @return true if the server is beening initialised
     */
    public boolean isInitialising()
    {
        return Disposable.PHASE_NAME.equals(lifecycleManager.getExecutingPhase());
    }

    protected boolean isStopped()
    {
        return lifecycleManager.isPhaseComplete(Stoppable.PHASE_NAME);
    }

    protected boolean isStopping()
    {
        return Stoppable.PHASE_NAME.equals(lifecycleManager.getExecutingPhase());
    }

    /**
     * Determines if the server has been started
     *
     * @return true if the server has been started
     */
    public boolean isStarted()
    {
        return lifecycleManager.isPhaseComplete(Startable.PHASE_NAME);
    }

    protected boolean isStarting()
    {
        return Startable.PHASE_NAME.equals(lifecycleManager.getExecutingPhase());
    }

    public boolean isDisposed()
    {
        return lifecycleManager.isPhaseComplete(Disposable.PHASE_NAME);
    }

    public boolean isDisposing()
    {
        return Disposable.PHASE_NAME.equals(lifecycleManager.getExecutingPhase());
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
            throw new FatalException(CoreMessages.propertyHasInvalidValue("encoding", config.getDefaultEncoding()), this);
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
            throw new FatalException(CoreMessages.propertyHasInvalidValue("osEncoding",
                    config.getDefaultOSEncoding()), this);
        }
    }


    public UMOLifecycleManager getLifecycleManager()
    {
        return lifecycleManager;
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
            throw new MuleRuntimeException(CoreMessages.serverNotificationManagerNotEnabled());
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
        if (StringUtils.isBlank(id))
        {
            throw new IllegalArgumentException("Management Context ID can't be null or empty");
        }
        checkLifecycleForPropertySet("id", Startable.PHASE_NAME);
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
        checkLifecycleForPropertySet("domain", Initialisable.PHASE_NAME);
        this.domain = domain;
    }

    public String getClusterId()
    {
        return clusterId;
    }

    public void setClusterId(String clusterId)
    {
        checkLifecycleForPropertySet("clusterId", Initialisable.PHASE_NAME);
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
        checkLifecycleForPropertySet("securityManager", Initialisable.PHASE_NAME);
        this.securityManager = securityManager;
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
        checkLifecycleForPropertySet("workManager", Initialisable.PHASE_NAME);
        this.workManager = workManager;
    }

    public QueueManager getQueueManager()
    {
        return queueManager;
    }

    public void setQueueManager(QueueManager queueManager)
    {
        checkLifecycleForPropertySet("queueManager", Initialisable.PHASE_NAME);
        this.queueManager = queueManager;
    }

    public ServerNotificationManager getNotificationManager()
    {
        return notificationManager;
    }

    public void setNotificationManager(ServerNotificationManager notificationManager)
    {
        checkLifecycleForPropertySet("notificationManager", Initialisable.PHASE_NAME);
        this.notificationManager = notificationManager;
    }

    /**
     * Sets the Jta Transaction Manager to use with this Mule server instance
     *
     * @param manager the manager to use
     * @throws Exception
     */
    public void setTransactionManager(TransactionManager manager) throws Exception
    {
        //checkLifecycleForPropertySet("transactionManager");
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
     * Returns a formatted string that is a summary of the configuration of the
     * server. This is the brock of information that gets displayed when the server
     * starts
     *
     * @return a string summary of the server information
     */
    private String getStartSplash()
    {
        String notset = CoreMessages.notSet().getMessage();

        // Mule Version, Timestamp, and Server ID
        List message = new ArrayList();
        Manifest mf = MuleManifest.getManifest();
        Map att = mf.getMainAttributes();
        if (att.values().size() > 0)
        {
            message.add(StringUtils.defaultString(MuleManifest.getProductDescription(), notset) + " "
                    + CoreMessages.version().getMessage() + " "
                    + StringUtils.defaultString(MuleManifest.getProductVersion(), notset));

            message.add(StringUtils.defaultString(MuleManifest.getVendorName(), notset));
            message.add(StringUtils.defaultString(MuleManifest.getProductMoreInfo(), notset));
        }
        else
        {
            message.add(CoreMessages.versionNotSet().getMessage());
        }
        message.add(" ");
        message.add(CoreMessages.serverStartedAt(getStartDate()).getMessage());
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
        //List agents
        Map agents = getRegistry().getAgents();
        if (agents.size() == 0)
        {
            message.add(CoreMessages.agentsRunning().getMessage() + " "
                        + CoreMessages.none().getMessage());
        }
        else
        {
            message.add(CoreMessages.agentsRunning().getMessage());
            UMOAgent umoAgent;
            for (Iterator iterator = agents.values().iterator(); iterator.hasNext();)
            {
                umoAgent = (UMOAgent)iterator.next();
                message.add("  " + umoAgent.getDescription());
            }
        }
        return StringMessageUtils.getBoilerPlate(message, '*', 70);
    }

    private String getEndSplash()
    {
        List message = new ArrayList(2);
        long currentTime = System.currentTimeMillis();
        message.add(CoreMessages.shutdownNormally(new Date()).getMessage());
        long duration = 10;
        if (startDate > 0)
        {
            duration = currentTime - startDate;
        }
        message.add(CoreMessages.serverWasUpForDuration(duration).getMessage());

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


    protected void checkLifecycleForPropertySet(String propertyName, String phase) throws IllegalStateException
    {
        if (lifecycleManager.isPhaseComplete(phase))
        {
            throw new IllegalStateException("Cannot set property: '" + propertyName + "' once the server has been gone through the " + phase + " phase.");
        }
    }
}
