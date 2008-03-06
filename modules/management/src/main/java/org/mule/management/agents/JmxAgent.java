/*
 * $Id$
 * --------------------------------------------------------------------------------------
 * Copyright (c) MuleSource, Inc.  All rights reserved.  http://www.mulesource.com
 *
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.management.agents;

import org.mule.AbstractAgent;
import org.mule.api.MuleException;
import org.mule.api.MuleRuntimeException;
import org.mule.api.context.notification.ManagerNotificationListener;
import org.mule.api.context.notification.ServerNotification;
import org.mule.api.lifecycle.InitialisationException;
import org.mule.api.lifecycle.LifecycleTransitionResult;
import org.mule.api.model.Model;
import org.mule.api.service.Service;
import org.mule.api.transport.Connector;
import org.mule.api.transport.MessageReceiver;
import org.mule.config.i18n.CoreMessages;
import org.mule.context.notification.ManagerNotification;
import org.mule.context.notification.NotificationException;
import org.mule.management.i18n.ManagementMessages;
import org.mule.management.mbeans.ConnectorService;
import org.mule.management.mbeans.ConnectorServiceMBean;
import org.mule.management.mbeans.EndpointService;
import org.mule.management.mbeans.EndpointServiceMBean;
import org.mule.management.mbeans.ModelService;
import org.mule.management.mbeans.ModelServiceMBean;
import org.mule.management.mbeans.MuleConfigurationService;
import org.mule.management.mbeans.MuleConfigurationServiceMBean;
import org.mule.management.mbeans.MuleService;
import org.mule.management.mbeans.MuleServiceMBean;
import org.mule.management.mbeans.ServiceService;
import org.mule.management.mbeans.ServiceServiceMBean;
import org.mule.management.mbeans.StatisticsService;
import org.mule.management.support.AutoDiscoveryJmxSupportFactory;
import org.mule.management.support.JmxSupport;
import org.mule.management.support.JmxSupportFactory;
import org.mule.management.support.SimplePasswordJmxAuthenticator;
import org.mule.transport.AbstractConnector;
import org.mule.util.ClassUtils;
import org.mule.util.StringUtils;

import java.io.IOException;
import java.rmi.server.ExportException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import javax.management.InstanceAlreadyExistsException;
import javax.management.MBeanRegistrationException;
import javax.management.MBeanServer;
import javax.management.MBeanServerFactory;
import javax.management.MalformedObjectNameException;
import javax.management.NotCompliantMBeanException;
import javax.management.ObjectName;
import javax.management.remote.JMXAuthenticator;
import javax.management.remote.JMXConnectorServer;
import javax.management.remote.JMXConnectorServerFactory;
import javax.management.remote.JMXServiceURL;
import javax.management.remote.rmi.RMIConnectorServer;

import edu.emory.mathcs.backport.java.util.concurrent.atomic.AtomicBoolean;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

/**
 * <code>JmxAgent</code> registers Mule Jmx management beans with an MBean server.
 */
public class JmxAgent extends AbstractAgent
{

    public static final String DEFAULT_REMOTING_URI = "service:jmx:rmi:///jndi/rmi://localhost:1099/server";
    // populated with values below in a static initializer
    public static final Map DEFAULT_CONNECTOR_SERVER_PROPERTIES;

    /**
     * Default JMX Authenticator to use for securing remote access.
     */
    public static final String DEFAULT_JMX_AUTHENTICATOR = SimplePasswordJmxAuthenticator.class.getName();

    /**
     * Logger used by this class
     */
    protected static final Log logger = LogFactory.getLog(JmxAgent.class);

    /**
     * Should MBeanServer be discovered.
     */
    protected boolean locateServer = true;

    private boolean createServer = true;
    private String connectorServerUrl;
    private MBeanServer mBeanServer;
    private JMXConnectorServer connectorServer;
    private Map connectorServerProperties = null;
    private boolean enableStatistics = true;
    private List registeredMBeans = new ArrayList();
    private final AtomicBoolean serverCreated = new AtomicBoolean(false);
    private final AtomicBoolean initialized = new AtomicBoolean(false);

    private JmxSupportFactory jmxSupportFactory = AutoDiscoveryJmxSupportFactory.getInstance();
    private JmxSupport jmxSupport = jmxSupportFactory.getJmxSupport();


    /**
     * Username/password combinations for JMX Remoting authentication.
     */
    private Map credentials = new HashMap();

    static
    {
        Map props = new HashMap(1);
        props.put(RMIConnectorServer.JNDI_REBIND_ATTRIBUTE, "true");
        DEFAULT_CONNECTOR_SERVER_PROPERTIES = Collections.unmodifiableMap(props);
    }

    public JmxAgent()
    {
        super("jmx-agent");
        connectorServerProperties = new HashMap(DEFAULT_CONNECTOR_SERVER_PROPERTIES);
    }

    /**
     * {@inheritDoc}
     *
     * @see org.mule.api.agent.Agent#getDescription()
     */
    public String getDescription()
    {
        if (connectorServerUrl != null)
        {
            return name + ": " + connectorServerUrl;
        }
        else
        {
            return "JMX Agent";
        }
    }

    /**
     * {@inheritDoc}
     *
     */
    public LifecycleTransitionResult initialise() throws InitialisationException
    {
        if (initialized.get())
        {
            return LifecycleTransitionResult.OK;
        }
        if (mBeanServer == null && !locateServer && !createServer)
        {
            throw new InitialisationException(ManagementMessages.createOrLocateShouldBeSet(), this);
        }
        if (mBeanServer == null && locateServer)
        {
            List l = MBeanServerFactory.findMBeanServer(null);
            if (l != null && l.size() > 0)
            {
                mBeanServer = (MBeanServer) l.get(0);
            }
        }
        if (mBeanServer == null && createServer)
        {
            mBeanServer = MBeanServerFactory.createMBeanServer();
            serverCreated.set(true);
        }
        if (mBeanServer == null)
        {
            throw new InitialisationException(ManagementMessages.cannotLocateOrCreateServer(), this);
        }
        if (connectorServerUrl != null)
        {
            try
            {
                JMXServiceURL url = new JMXServiceURL(connectorServerUrl);
                if (connectorServerProperties == null)
                {
                    connectorServerProperties = new HashMap(DEFAULT_CONNECTOR_SERVER_PROPERTIES);
                }
                // TODO custom authenticator may have its own security config, refactor
                if (!credentials.isEmpty())
                {
                    JMXAuthenticator jmxAuthenticator = (JMXAuthenticator) ClassUtils.instanciateClass(
                            DEFAULT_JMX_AUTHENTICATOR, ClassUtils.NO_ARGS);
                    // TODO support for custom authenticators
                    ((SimplePasswordJmxAuthenticator) jmxAuthenticator).setCredentials(credentials);
                    connectorServerProperties.put(JMXConnectorServer.AUTHENTICATOR, jmxAuthenticator);
                }
                connectorServer = JMXConnectorServerFactory.newJMXConnectorServer(url, connectorServerProperties, mBeanServer);
            }
            catch (Exception e)
            {
                throw new InitialisationException(CoreMessages.failedToCreate("Jmx Connector"), e, this);
            }
        }

        // We need to register all the services once the server has initialised
        ManagerNotificationListener l = new ManagerNotificationListener()
        {
            public void onNotification(ServerNotification notification)
            {
                if (notification.getAction() == ManagerNotification.MANAGER_STARTED_MODELS)
                {
                    try
                    {
                        registerWrapperService();
                        registerStatisticsService();
                        registerMuleService();
                        registerConfigurationService();
                        registerModelServices();
                        registerServiceServices();
                        registerEndpointServices();
                        registerConnectorServices();
                    }
                    catch (Exception e)
                    {
                        throw new MuleRuntimeException(CoreMessages.objectFailedToInitialise("MBeans"), e);
                    }
                }
            }
        };

        if (StringUtils.isBlank(muleContext.getConfiguration().getId()))
        {
            // TODO i18n the message properly
            throw new IllegalArgumentException(
                    "Manager ID is mandatory when running with JmxAgent. Give your Mule configuration a valid ID.");
        }

        try
        {
            muleContext.registerListener(l);
        } catch (NotificationException e) {
            throw new InitialisationException(e, this);
        }
        initialized.compareAndSet(false, true);
        return LifecycleTransitionResult.OK;
    }

    /**
     * {@inheritDoc} (non-Javadoc)
     *
     * @see org.mule.api.lifecycle.Startable#start()
     */
    public LifecycleTransitionResult start() throws MuleException
    {        
        if (connectorServer != null)
        {
            try
            {
                // detect without starting - more complex (need to configure rmi address)
                // and doesn't currently offer any advantage
//                Socket socket = new Socket("localhost", 1099);
//                socket.close();
                logger.info("Starting JMX agent connector Server");
                connectorServer.start();
            }
            catch (ExportException e)
            {
                throw new JmxManagementException(CoreMessages.failedToStart("Jmx Agent"), e);
            }
            catch (IOException e)
            {
                // this probably means that the RMI server isn't started so we request a retry
                return LifecycleTransitionResult.retry(e, this);
            }
            catch (Exception e)
            {
                throw new JmxManagementException(CoreMessages.failedToStart("Jmx Agent"), e);
            }
        }
        return LifecycleTransitionResult.OK;
    }

    /**
     * {@inheritDoc} (non-Javadoc)
     *
     * @see org.mule.api.lifecycle.Stoppable#stop()
     */
    public LifecycleTransitionResult stop() throws MuleException
    {
        if (connectorServer != null)
        {
            try
            {
                connectorServer.stop();
            }
            catch (Exception e)
            {
                throw new JmxManagementException(CoreMessages.failedToStop("Jmx Connector"), e);
            }
        }
        return LifecycleTransitionResult.OK;
    }

    /**
     * {@inheritDoc} (non-Javadoc)
     *
     * @see org.mule.api.lifecycle.Disposable#dispose()
     */
    public void dispose()
    {
        if (mBeanServer != null)
        {
            for (Iterator iterator = registeredMBeans.iterator(); iterator.hasNext();)
            {
                ObjectName objectName = (ObjectName) iterator.next();
                try
                {
                    mBeanServer.unregisterMBean(objectName);
                }
                catch (Exception e)
                {
                    logger.warn("Failed to unregister MBean: " + objectName + ". Error is: " + e.getMessage());
                }
            }
            if (serverCreated.get())
            {
                MBeanServerFactory.releaseMBeanServer(mBeanServer);
            }
            mBeanServer = null;
        }

        initialized.set(false);
    }

    /** {@inheritDoc}
     * (non-Javadoc)
     *
     * @see org.mule.api.agent.Agent#registered()
     */
    public void registered()
    {
        // nothing to do
    }

    /** {@inheritDoc}
     * (non-Javadoc)
     *
     * @see org.mule.api.agent.Agent#unregistered()
     */
    public void unregistered()
    {
        // nothing to do
    }

    /**
     * Register a Java Service Wrapper agent.
     * @throws MuleException if registration failed
     */
    protected void registerWrapperService() throws MuleException
    {
        // WrapperManager to support restarts
        final WrapperManagerAgent wmAgent = new WrapperManagerAgent();
        if (muleContext.getRegistry().lookupAgent(wmAgent.getName()) == null)
        {
           muleContext.getRegistry().registerAgent(wmAgent);
        }
    }


    protected void registerStatisticsService() throws NotCompliantMBeanException, MBeanRegistrationException,
                                                      InstanceAlreadyExistsException, MalformedObjectNameException
    {
        ObjectName on = jmxSupport.getObjectName(jmxSupport.getDomainName(muleContext) + ":type=org.mule.Statistics,name=AllStatistics");
        StatisticsService mBean = new StatisticsService();
        mBean.setMuleContext(muleContext);
        mBean.setEnabled(isEnableStatistics());
        logger.debug("Registering statistics with name: " + on);
        mBeanServer.registerMBean(mBean, on);
        registeredMBeans.add(on);
    }

    protected void registerModelServices() throws NotCompliantMBeanException, MBeanRegistrationException,
                                                  InstanceAlreadyExistsException, MalformedObjectNameException
    {
        for (Iterator iterator = muleContext.getRegistry().lookupObjects(Model.class).iterator(); iterator.hasNext();)
        {
            Model model = (Model) iterator.next();
            ModelServiceMBean serviceMBean = new ModelService(model);
            String rawName = serviceMBean.getName() + "(" + serviceMBean.getType() + ")";
            String name = jmxSupport.escape(rawName);
            ObjectName on = jmxSupport.getObjectName(jmxSupport.getDomainName(muleContext) + ":type=org.mule.Model,name=" + name);
            logger.debug("Registering model with name: " + on);
            mBeanServer.registerMBean(serviceMBean, on);
            registeredMBeans.add(on);
        }
    }

    protected void registerMuleService() throws NotCompliantMBeanException, MBeanRegistrationException,
                                                InstanceAlreadyExistsException, MalformedObjectNameException
    {
        ObjectName on = jmxSupport.getObjectName(jmxSupport.getDomainName(muleContext) + ":type=org.mule.MuleContext,name=MuleServerInfo");
        MuleServiceMBean serviceMBean = new MuleService(muleContext);
        logger.debug("Registering mule with name: " + on);
        mBeanServer.registerMBean(serviceMBean, on);
        registeredMBeans.add(on);
    }

    protected void registerConfigurationService() throws NotCompliantMBeanException, MBeanRegistrationException,
                                                         InstanceAlreadyExistsException, MalformedObjectNameException
    {
        ObjectName on = jmxSupport.getObjectName(jmxSupport.getDomainName(muleContext) + ":type=org.mule.Configuration,name=GlobalConfiguration");
        MuleConfigurationServiceMBean serviceMBean = new MuleConfigurationService(muleContext.getConfiguration());
        logger.debug("Registering configuration with name: " + on);
        mBeanServer.registerMBean(serviceMBean, on);
        registeredMBeans.add(on);
    }

    protected void registerServiceServices() throws NotCompliantMBeanException, MBeanRegistrationException,
            InstanceAlreadyExistsException, MalformedObjectNameException
    {
        String rawName;
        for (Iterator iterator = muleContext.getRegistry().lookupObjects(Service.class).iterator(); iterator.hasNext();)
        {
            rawName = ((Service) iterator.next()).getName();
            final String name = jmxSupport.escape(rawName);
            ObjectName on = jmxSupport.getObjectName(jmxSupport.getDomainName(muleContext) + ":type=org.mule.Service,name=" + name);
            ServiceServiceMBean serviceMBean = new ServiceService(rawName);
            logger.debug("Registering service with name: " + on);
            mBeanServer.registerMBean(serviceMBean, on);
            registeredMBeans.add(on);
        }

    }

    protected void registerEndpointServices() throws NotCompliantMBeanException, MBeanRegistrationException,
            InstanceAlreadyExistsException, MalformedObjectNameException
    {
        Iterator iter = muleContext.getRegistry().lookupObjects(Connector.class).iterator();
        Connector connector;
        while (iter.hasNext())
        {
            connector = (Connector) iter.next();
            if (connector instanceof AbstractConnector)
            {
                for (Iterator iterator = ((AbstractConnector) connector).getReceivers().values().iterator(); iterator.hasNext();)
                {
                    EndpointServiceMBean mBean = new EndpointService((MessageReceiver) iterator.next());
                    final String rawName = mBean.getName();
                    final String name = jmxSupport.escape(rawName);
                    if (logger.isInfoEnabled()) {
                        logger.info("Attempting to register service with name: " + jmxSupport.getDomainName(muleContext) +
                                                    ":type=org.mule.Endpoint,service=" +
                                                    jmxSupport.escape(mBean.getComponentName()) +
                                                    ",name=" + name);
                    }
                    ObjectName on = jmxSupport.getObjectName(
                                                    jmxSupport.getDomainName(muleContext) +
                                                    ":type=org.mule.Endpoint,service=" +
                                                    jmxSupport.escape(mBean.getComponentName()) +
                                                    ",name=" + name);
                    mBeanServer.registerMBean(mBean, on);
                    registeredMBeans.add(on);
                    logger.info("Registered Endpoint Service with name: " + on);
                }
            }
            else
            {
                logger.warn("Connector: " + connector
                            + " is not an istance of AbstractConnector, cannot obtain Endpoint MBeans from it");
            }

        }
    }

    protected void registerConnectorServices() throws
                                                MalformedObjectNameException,
                                                NotCompliantMBeanException,
                                                MBeanRegistrationException,
                                                InstanceAlreadyExistsException
    {
        Iterator iter = muleContext.getRegistry().lookupObjects(Connector.class).iterator();
        while (iter.hasNext())
        {
            Connector connector = (Connector) iter.next();
            ConnectorServiceMBean mBean = new ConnectorService(connector);
            final String rawName = mBean.getName();
            final String name = jmxSupport.escape(rawName);
            final String stringName = jmxSupport.getDomainName(muleContext) + ":type=org.mule.Connector,name=" + name;
            if (logger.isDebugEnabled())
            {
                logger.debug("Attempting to register service with name: " + stringName);
            }
            ObjectName oName = jmxSupport.getObjectName(stringName);
            mBeanServer.registerMBean(mBean, oName);
            registeredMBeans.add(oName);
            logger.info("Registered Connector Service with name " + oName);
        }
    }

    /**
     * @return Returns the createServer.
     */
    public boolean isCreateServer()
    {
        return createServer;
    }

    /**
     * @param createServer The createServer to set.
     */
    public void setCreateServer(boolean createServer)
    {
        this.createServer = createServer;
    }

    /**
     * @return Returns the locateServer.
     */
    public boolean isLocateServer()
    {
        return locateServer;
    }

    /**
     * @param locateServer The locateServer to set.
     */
    public void setLocateServer(boolean locateServer)
    {
        this.locateServer = locateServer;
    }

    /**
     * @return Returns the connectorServerUrl.
     */
    public String getConnectorServerUrl()
    {
        return connectorServerUrl;
    }

    /**
     * @param connectorServerUrl The connectorServerUrl to set.
     */
    public void setConnectorServerUrl(String connectorServerUrl)
    {
        this.connectorServerUrl = connectorServerUrl;
    }

    /**
     * @return Returns the enableStatistics.
     */
    public boolean isEnableStatistics()
    {
        return enableStatistics;
    }

    /**
     * @param enableStatistics The enableStatistics to set.
     */
    public void setEnableStatistics(boolean enableStatistics)
    {
        this.enableStatistics = enableStatistics;
    }

    /**
     * @return Returns the mBeanServer.
     */
    public MBeanServer getMBeanServer()
    {
        return mBeanServer;
    }

    /**
     * @param mBeanServer The mBeanServer to set.
     */
    public void setMBeanServer(MBeanServer mBeanServer)
    {
        this.mBeanServer = mBeanServer;
    }

    /**
     * Getter for property 'connectorServerProperties'.
     *
     * @return Value for property 'connectorServerProperties'.
     */
    public Map getConnectorServerProperties()
    {
        return connectorServerProperties;
    }

    /**
     * Setter for property 'connectorServerProperties'. Set to {@code null} to use defaults ({@link
     * #DEFAULT_CONNECTOR_SERVER_PROPERTIES}). Pass in an empty map to use no parameters. Passing a non-empty map will
     * replace defaults.
     *
     * @param connectorServerProperties Value to set for property 'connectorServerProperties'.
     */
    public void setConnectorServerProperties(Map connectorServerProperties)
    {
        this.connectorServerProperties = connectorServerProperties;
    }


    /**
     * Getter for property 'jmxSupportFactory'.
     *
     * @return Value for property 'jmxSupportFactory'.
     */
    public JmxSupportFactory getJmxSupportFactory()
    {
        return jmxSupportFactory;
    }

    /**
     * Setter for property 'jmxSupportFactory'.
     *
     * @param jmxSupportFactory Value to set for property 'jmxSupportFactory'.
     */
    public void setJmxSupportFactory(JmxSupportFactory jmxSupportFactory)
    {
        this.jmxSupportFactory = jmxSupportFactory;
    }


    /**
     * Setter for property 'credentials'.
     *
     * @param newCredentials Value to set for property 'credentials'.
     */
    public void setCredentials(final Map newCredentials)
    {
        this.credentials.clear();
        if (newCredentials != null && !newCredentials.isEmpty())
        {
            this.credentials.putAll(newCredentials);
        }
    }
    
}
