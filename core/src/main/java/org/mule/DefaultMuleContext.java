/*
 * $Id$
 * --------------------------------------------------------------------------------------
 * Copyright (c) MuleSource, Inc.  All rights reserved.  http://www.mulesource.com
 *
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule;

import org.mule.api.MuleException;
import org.mule.api.MuleContext;
import org.mule.api.MuleRuntimeException;
import org.mule.api.agent.Agent;
import org.mule.api.config.MuleProperties;
import org.mule.api.context.WorkManager;
import org.mule.api.context.notification.ServerNotification;
import org.mule.api.context.notification.ServerNotificationListener;
import org.mule.api.lifecycle.Disposable;
import org.mule.api.lifecycle.FatalException;
import org.mule.api.lifecycle.Initialisable;
import org.mule.api.lifecycle.InitialisationException;
import org.mule.api.lifecycle.LifecycleManager;
import org.mule.api.lifecycle.Startable;
import org.mule.api.lifecycle.Stoppable;
import org.mule.api.lifecycle.LifecycleTransitionResult;
import org.mule.api.registry.RegistrationException;
import org.mule.api.registry.Registry;
import org.mule.api.security.SecurityManager;
import org.mule.api.store.Store;
import org.mule.api.transaction.TransactionManagerFactory;
import org.mule.config.MuleConfiguration;
import org.mule.config.MuleManifest;
import org.mule.config.i18n.CoreMessages;
import org.mule.context.notification.ManagerNotification;
import org.mule.context.notification.NotificationException;
import org.mule.context.notification.ServerNotificationManager;
import org.mule.management.stats.AllStatistics;
import org.mule.util.FileUtils;
import org.mule.util.StringMessageUtils;
import org.mule.util.StringUtils;
import org.mule.util.queue.QueueManager;

import java.net.InetAddress;
import java.net.UnknownHostException;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.jar.Manifest;

import javax.transaction.TransactionManager;
import javax.xml.parsers.SAXParserFactory;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

public class DefaultMuleContext implements MuleContext
{

    /** logger used by this class */
    private static transient Log logger = LogFactory.getLog(DefaultMuleContext.class);

    /** Default configuration */
    private MuleConfiguration config;

    /** the unique id for this manager */
    private String id = null;

    /** If this node is part of a cluster then this is the shared cluster Id */
    private String clusterId = null;

    /** The domain name that this instance belongs to. */
    private String domain = null;

    /** the date in milliseconds from when the server was started */
    private long startDate = 0;

    /** stats used for management */
    private AllStatistics stats = new AllStatistics();

    private WorkManager workManager;

    /**
     * LifecycleManager for the MuleContext.  Note: this is NOT the same lifecycle manager
     * as the one in the Registry.
     */
    protected LifecycleManager lifecycleManager;

    protected Directories directories;

    protected String systemName;
    
    protected ServerNotificationManager notificationManager;

    public DefaultMuleContext(LifecycleManager lifecycleManager)
    {
        if (lifecycleManager == null)
        {
            throw new NullPointerException(CoreMessages.objectIsNull("lifecycleManager").getMessage());
        }
        this.lifecycleManager = lifecycleManager;

        startDate = System.currentTimeMillis();
    }

    public LifecycleTransitionResult initialise() throws InitialisationException
    {
        lifecycleManager.checkPhase(Initialisable.PHASE_NAME);

        if (getNotificationManager() == null)
        {
            throw new NullPointerException(CoreMessages.objectIsNull(
                MuleProperties.OBJECT_NOTIFICATION_MANAGER).getMessage());
        }
        if (workManager == null)
        {
            throw new NullPointerException(CoreMessages.objectIsNull("workManager").getMessage());
        }
        if (config == null)
        {
            logger.info("A mule configuration object was not registered. Using default configuration");
            config = new MuleConfiguration();
        }

        try
        {
            setupIds();
            validateEncoding();
            validateOSEncoding();
            validateXML();

            directories = new Directories(FileUtils.newFile(config.getWorkingDirectory()));

            //We need to start the work manager straight away since we need it to fire notifications
            workManager.start();
            getNotificationManager().start(workManager);

            fireNotification(new ManagerNotification(this, ManagerNotification.MANAGER_INITIALISING));

            directories.createDirectories();
            lifecycleManager.firePhase(this, Initialisable.PHASE_NAME);

            fireNotification(new ManagerNotification(this, ManagerNotification.MANAGER_INITIALISED));
        }
        catch (Exception e)
        {
            throw new InitialisationException(e, this);
        }
        return LifecycleTransitionResult.OK;
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

    public synchronized LifecycleTransitionResult start() throws MuleException
    {
        lifecycleManager.checkPhase(Startable.PHASE_NAME);
        if (!isStarted())
        {
            if (getSecurityManager() == null)
            {
                throw new NullPointerException(CoreMessages.objectIsNull("securityManager").getMessage());
            }
            if (getQueueManager() == null)
            {
                throw new NullPointerException(CoreMessages.objectIsNull("queueManager").getMessage());
            }

            fireNotification(new ManagerNotification(this, ManagerNotification.MANAGER_STARTING));

            directories.deleteMarkedDirectories();

            lifecycleManager.firePhase(this, Startable.PHASE_NAME);

            if (logger.isInfoEnabled())
            {
                logger.info(getStartSplash());
            }
            fireNotification(new ManagerNotification(this, ManagerNotification.MANAGER_STARTED));
        }
        return LifecycleTransitionResult.OK;
    }

    /**
     * Stops the <code>MuleManager</code> which stops all sessions and
     * connectors
     *
     * @throws MuleException if either any of the sessions or connectors fail to
     *                      stop
     */
    public synchronized LifecycleTransitionResult stop() throws MuleException
    {
        lifecycleManager.checkPhase(Stoppable.PHASE_NAME);
        fireNotification(new ManagerNotification(this, ManagerNotification.MANAGER_STOPPING));
        lifecycleManager.firePhase(this, Stoppable.PHASE_NAME);
        fireNotification(new ManagerNotification(this, ManagerNotification.MANAGER_STOPPED));
        return LifecycleTransitionResult.OK;
    }

    public void dispose()
    {        
        if (isDisposing())
        {
            return;
        }
             
        ServerNotificationManager notificationManager = getNotificationManager();
        lifecycleManager.checkPhase(Disposable.PHASE_NAME);
        fireNotification(new ManagerNotification(this, ManagerNotification.MANAGER_DISPOSING));

        try
        {
            if (isStarted())
            {
                stop();
            }
        }
        catch (MuleException e)
        {
            logger.error("Failed to stop manager: " + e.getMessage(), e);
        }

        try
        {
            lifecycleManager.firePhase(this, Disposable.PHASE_NAME);
        }
        catch (MuleException e)
        {
            logger.debug("Failed to cleanly dispose Mule: " + e.getMessage(), e);
        }

        notificationManager.fireNotification(new ManagerNotification(this, ManagerNotification.MANAGER_DISPOSED));

        if ((startDate > 0) && logger.isInfoEnabled())
        {
            logger.info(getEndSplash());
        }
        //lifecycleManager.reset();
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

    /**
     * Mule needs a proper JAXP implementation and will complain when run with a plain JDK
     * 1.4. Use the supplied launcher or specify a proper JAXP implementation via
     * <code>-Djava.endorsed.dirs</code>. See the following URLs for more information:
     * <ul>
     * <li> {@link http://xerces.apache.org/xerces2-j/faq-general.html#faq-4}
     * <li> {@link http://xml.apache.org/xalan-j/faq.html#faq-N100D6}
     * <li> {@link http://java.sun.com/j2se/1.4.2/docs/guide/standards/}
     * </ul>
     */
    protected void validateXML() throws FatalException
    {
        SAXParserFactory f = SAXParserFactory.newInstance();
        if (f == null || f.getClass().getName().indexOf("crimson") != -1)
        {
            throw new FatalException(CoreMessages.valueIsInvalidFor(f.getClass().getName(),
                "javax.xml.parsers.SAXParserFactory"), this);
        }
    }

    public LifecycleManager getLifecycleManager()
    {
        return lifecycleManager;
    }

    public String getSystemName()
    {
        return systemName;
    }

    public void setSystemName(String systemName)
    {
        this.systemName = systemName;
    }

    public Store getStore(String name) throws MuleException
    {
        //TODO LM: get store from registry
        return null;
    }

    public Store createStore(String name) throws MuleException
    {
        //TODO LM: backed by registry
        return null;
    }

    public void removeStore(Store store)
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

    /** Sets statistics on this instance */
    public void setStatistics(AllStatistics stat)
    {
        this.stats = stat;
    }

    public Directories getDirectories()
    {
        return directories;
    }

    public void registerListener(ServerNotificationListener l) throws NotificationException
    {
        registerListener(l, null);
    }

    public void registerListener(ServerNotificationListener l, String resourceIdentifier) throws NotificationException
    {
        ServerNotificationManager notificationManager = getNotificationManager();
        if (notificationManager == null)
        {
            throw new MuleRuntimeException(CoreMessages.serverNotificationManagerNotEnabled());
        }
        notificationManager.addListenerSubscription(l, resourceIdentifier);
    }

    public void unregisterListener(ServerNotificationListener l)
    {
        ServerNotificationManager notificationManager = getNotificationManager();
        if (notificationManager != null)
        {
            notificationManager.removeListener(l);
        }
    }

    /**
     * Fires a server notification to all registered
     * {@link org.mule.api.context.notification.listener.CustomNotificationListener} notificationManager.
     *
     * @param notification the notification to fire. This must be of type
     *                     {@link org.mule.context.notification.CustomNotification} otherwise an
     *                     exception will be thrown.
     * @throws UnsupportedOperationException if the notification fired is not a
     *                                       {@link org.mule.context.notification.CustomNotification}
     */
    public void fireNotification(ServerNotification notification)
    {
        ServerNotificationManager notificationManager = getNotificationManager();
        if (notificationManager != null)
        {
            notificationManager.fireNotification(notification);
        }
        else if (logger.isDebugEnabled())
        {
            logger.debug("MuleEvent Manager is not enabled, ignoring notification: " + notification);
        }
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
    public void setSecurityManager(SecurityManager securityManager) throws RegistrationException
    {
        checkLifecycleForPropertySet(MuleProperties.OBJECT_SECURITY_MANAGER, Initialisable.PHASE_NAME);
        getRegistry().registerObject(MuleProperties.OBJECT_SECURITY_MANAGER, securityManager);
    }

    /**
     * Gets the security manager used by this Mule instance to authenticate and
     * authorise incoming and outgoing event traffic and service invocations
     *
     * @return he security manager used by this Mule instance to authenticate
     *         and authorise incoming and outgoing event traffic and service
     *         invocations
     */
    public SecurityManager getSecurityManager()
    {
        SecurityManager securityManager = (SecurityManager) getRegistry().lookupObject(
            MuleProperties.OBJECT_SECURITY_MANAGER);
        if (securityManager == null)
        {
            Collection temp = getRegistry().lookupObjects(SecurityManager.class);
            if (temp.size() > 0)
            {
                securityManager = ((SecurityManager) temp.iterator().next());
            }
        }
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
     * @see org.mule.api.config.ThreadingProfile
     * @see MuleConfiguration
     */
    public WorkManager getWorkManager()
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
     * @see org.mule.api.config.ThreadingProfile
     * @see MuleConfiguration
     * @see org.mule.work.MuleWorkManager
     */
    public void setWorkManager(WorkManager workManager)
    {
        checkLifecycleForPropertySet("workManager", Initialisable.PHASE_NAME);
        this.workManager = workManager;
    }

    public QueueManager getQueueManager()
    {
        QueueManager queueManager = (QueueManager) getRegistry().lookupObject(MuleProperties.OBJECT_QUEUE_MANAGER);
        if (queueManager == null)
        {
            Collection temp = getRegistry().lookupObjects(QueueManager.class);
            if (temp.size() > 0)
            {
                queueManager = ((QueueManager) temp.iterator().next());
            }
        }
        return queueManager;
    }

    public void setQueueManager(QueueManager queueManager) throws RegistrationException
    {
        checkLifecycleForPropertySet(MuleProperties.OBJECT_QUEUE_MANAGER, Initialisable.PHASE_NAME);
        getRegistry().registerObject(MuleProperties.OBJECT_QUEUE_MANAGER, queueManager);
    }

    public ServerNotificationManager getNotificationManager()
    {
        return notificationManager;
    }

    public void setNotificationManager(ServerNotificationManager notificationManager)
    {
        this.notificationManager = notificationManager;
    }

    /**
     * Sets the Jta Transaction Manager to use with this Mule server instance
     *
     * @param manager the manager to use
     * @throws Exception
     */
    public void setTransactionManager(TransactionManager manager) throws RegistrationException
    {
        //checkLifecycleForPropertySet(MuleProperties.OBJECT_TRANSACTION_MANAGER, Initialisable.PHASE_NAME);
        getRegistry().registerObject(MuleProperties.OBJECT_TRANSACTION_MANAGER, manager);
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
        TransactionManager transactionManager = (TransactionManager) getRegistry().lookupObject(
            MuleProperties.OBJECT_TRANSACTION_MANAGER);
        if (transactionManager == null)
        {
            Collection temp = getRegistry().lookupObjects(TransactionManagerFactory.class);
            if (temp.size() > 0)
            {
                try
                {
                    transactionManager = (((TransactionManagerFactory) temp.iterator().next()).create());
                }
                catch (Exception e)
                {
                    // TODO Auto-generated catch block
                    e.printStackTrace();
                    throw new MuleRuntimeException(CoreMessages.createStaticMessage("Unable to create transaction manager"));
                }
            }
            else
            {
                temp = getRegistry().lookupObjects(TransactionManager.class);
                if (temp.size() > 0)
                {
                    transactionManager = (((TransactionManager) temp.iterator().next()));
                }
            }
        }
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
            message.add(StringUtils.defaultString(MuleManifest.getProductDescription(), notset));
            message.add(CoreMessages.version().getMessage() + " Build: "
                    + StringUtils.defaultString(MuleManifest.getBuildNumber(), notset));

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
        Collection agents = RegistryContext.getRegistry().lookupObjects(Agent.class);
        if (agents.size() == 0)
        {
            message.add(CoreMessages.agentsRunning().getMessage() + " "
                    + CoreMessages.none().getMessage());
        }
        else
        {
            message.add(CoreMessages.agentsRunning().getMessage());
            Agent umoAgent;
            for (Iterator iterator = agents.iterator(); iterator.hasNext();)
            {
                umoAgent = (Agent) iterator.next();
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

    public void deregister() throws RegistrationException
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

    public void setLifecycleManager(LifecycleManager lifecycleManager)
    {
        this.lifecycleManager = lifecycleManager;
    }

    /**
     * Resolve and return a handle to the registry.
     * This should eventually be more intelligent (handle remote registries, clusters of Mule instances, etc.)
     * For now the registry is just a local singleton.
     */
    public Registry getRegistry()
    {
        return RegistryContext.getRegistry();
    }

    /**
     * Apply current phase of the LifecycleManager.  Note: this is NOT the same lifecycle manager
     * as the one in the Registry.
     */
    public void applyLifecycle(Object object) throws MuleException
    {
        lifecycleManager.applyLifecycle(this, object);
    }
    
    public MuleConfiguration getConfiguration()
    {
        return config;
    }

    public void setConfiguration(MuleConfiguration config)
    {
        this.config = config;
    }

}
