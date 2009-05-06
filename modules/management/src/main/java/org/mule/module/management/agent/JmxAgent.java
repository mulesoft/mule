/*
 * $Id$
 * --------------------------------------------------------------------------------------
 * Copyright (c) MuleSource, Inc.  All rights reserved.  http://www.mulesource.com
 *
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.module.management.agent;

import org.mule.AbstractAgent;
import org.mule.api.MuleException;
import org.mule.api.MuleRuntimeException;
import org.mule.api.context.notification.MuleContextNotificationListener;
import org.mule.api.context.notification.ServerNotification;
import org.mule.api.lifecycle.InitialisationException;
import org.mule.api.model.Model;
import org.mule.api.service.Service;
import org.mule.api.transport.Connector;
import org.mule.api.transport.MessageReceiver;
import org.mule.config.i18n.CoreMessages;
import org.mule.context.notification.MuleContextNotification;
import org.mule.context.notification.NotificationException;
import org.mule.module.management.i18n.ManagementMessages;
import org.mule.module.management.mbean.ConnectorService;
import org.mule.module.management.mbean.ConnectorServiceMBean;
import org.mule.module.management.mbean.EndpointService;
import org.mule.module.management.mbean.EndpointServiceMBean;
import org.mule.module.management.mbean.ModelService;
import org.mule.module.management.mbean.ModelServiceMBean;
import org.mule.module.management.mbean.MuleConfigurationService;
import org.mule.module.management.mbean.MuleConfigurationServiceMBean;
import org.mule.module.management.mbean.MuleService;
import org.mule.module.management.mbean.MuleServiceMBean;
import org.mule.module.management.mbean.ServiceService;
import org.mule.module.management.mbean.ServiceServiceMBean;
import org.mule.module.management.mbean.StatisticsService;
import org.mule.module.management.support.AutoDiscoveryJmxSupportFactory;
import org.mule.module.management.support.JmxSupport;
import org.mule.module.management.support.JmxSupportFactory;
import org.mule.module.management.support.SimplePasswordJmxAuthenticator;
import org.mule.transport.AbstractConnector;
import org.mule.util.ClassUtils;
import org.mule.util.StringUtils;

import java.rmi.server.ExportException;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

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
     * The JmxAgent needs a RmiRegistryAgent to be started before it can properly work.
     */    
    public List getDependentAgents()
    {
        return Arrays.asList(new Class[] { RmiRegistryAgent.class });
    }

    /**
     * {@inheritDoc}
     *
     */
    public void initialise() throws InitialisationException
    {
        if (initialized.get())
        {
            return;
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

        if (StringUtils.isBlank(muleContext.getConfiguration().getId()))
        {
            // TODO i18n the message properly
            throw new IllegalArgumentException(
                    "Manager ID is mandatory when running with JmxAgent. Give your Mule configuration a valid ID.");
        }

        try
        {
            // We need to register all the services once the server has initialised
            muleContext.registerListener(new MuleContextStartedListener());
            // and unregister once context stopped
            muleContext.registerListener(new MuleContextStoppedListener());
        } catch (NotificationException e) {
            throw new InitialisationException(e, this);
        }
        initialized.compareAndSet(false, true);
    }

    /**
     * {@inheritDoc} (non-Javadoc)
     *
     * @see org.mule.api.lifecycle.Startable#start()
     */
    public void start() throws MuleException
    {
        try
        {
            logger.info("Creating and starting JMX agent connector Server");
            if (connectorServerUrl != null)
            {
                JMXServiceURL url = new JMXServiceURL(connectorServerUrl);
                if (connectorServerProperties == null)
                {
                    connectorServerProperties = new HashMap(DEFAULT_CONNECTOR_SERVER_PROPERTIES);
                }
                // TODO custom authenticator may have its own security config,
                // refactor
                if (!credentials.isEmpty())
                {
                    JMXAuthenticator jmxAuthenticator = (JMXAuthenticator)ClassUtils.instanciateClass(DEFAULT_JMX_AUTHENTICATOR);
                    // TODO support for custom authenticators
                    ((SimplePasswordJmxAuthenticator)jmxAuthenticator).setCredentials(credentials);
                    connectorServerProperties.put(JMXConnectorServer.AUTHENTICATOR, jmxAuthenticator);
                }
                connectorServer = JMXConnectorServerFactory.newJMXConnectorServer(url,
                    connectorServerProperties, mBeanServer);
                connectorServer.start();
            }
        }
        catch (ExportException e)
        {
            throw new JmxManagementException(CoreMessages.failedToStart("Jmx Agent"), e);
        }
        catch (Exception e)
        {
            throw new JmxManagementException(CoreMessages.failedToStart("Jmx Agent"), e);
        }
    }

    public void stop() throws MuleException
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
    }

    /**
     * {@inheritDoc}
     */
    public void dispose()
    {
        unregisterMBeansIfNecessary();
        if (serverCreated.get())
        {
            MBeanServerFactory.releaseMBeanServer(mBeanServer);
        }
        mBeanServer = null;
        serverCreated.compareAndSet(true, false);
        initialized.set(false);
    }

    /**
     * {@inheritDoc}
     */
    public void registered()
    {
        // nothing to do
    }

    /** 
     * {@inheritDoc}
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
        }
    }

    protected void registerMuleService() throws NotCompliantMBeanException, MBeanRegistrationException,
                                                InstanceAlreadyExistsException, MalformedObjectNameException
    {
        ObjectName on = jmxSupport.getObjectName(jmxSupport.getDomainName(muleContext) + ":type=org.mule.MuleContext,name=MuleServerInfo");
        MuleServiceMBean serviceMBean = new MuleService(muleContext);
        logger.debug("Registering mule with name: " + on);
        mBeanServer.registerMBean(serviceMBean, on);
    }

    protected void registerConfigurationService() throws NotCompliantMBeanException, MBeanRegistrationException,
                                                         InstanceAlreadyExistsException, MalformedObjectNameException
    {
        ObjectName on = jmxSupport.getObjectName(jmxSupport.getDomainName(muleContext) + ":type=org.mule.Configuration,name=GlobalConfiguration");
        MuleConfigurationServiceMBean serviceMBean = new MuleConfigurationService(muleContext.getConfiguration());
        logger.debug("Registering configuration with name: " + on);
        mBeanServer.registerMBean(serviceMBean, on);
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

    protected void unregisterMBeansIfNecessary()
    {
        if (mBeanServer == null)
        {
            return;
        }

        try
        {
            ObjectName query = jmxSupport.getObjectName(jmxSupport.getDomainName(muleContext) + ":*");
            Set mbeans = mBeanServer.queryNames(query, null);
            while (!mbeans.isEmpty())
            {
                ObjectName name = (ObjectName) mbeans.iterator().next();
                try
                {
                    mBeanServer.unregisterMBean(name);
                }
                catch (Exception e)
                {
                    logger.warn(String.format("Failed to unregister MBean: %s. Error is: %s", name, e.getMessage()));
                }

                // query mbeans again, as some mbeans have cascaded unregister operations,
                // this prevents duplicate unregister attempts
                mbeans = mBeanServer.queryNames(query, null);
            }
        }
        catch (MalformedObjectNameException e)
        {
            logger.warn("Failed to create ObjectName query", e);
        }
    }

    protected class MuleContextStartedListener implements MuleContextNotificationListener
    {

        public void onNotification(ServerNotification notification)
        {
            if (notification.getAction() == MuleContextNotification.CONTEXT_STARTED)
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
    }

    protected class MuleContextStoppedListener implements MuleContextNotificationListener
    {

        public void onNotification(ServerNotification notification)
        {
            if (notification.getAction() == MuleContextNotification.CONTEXT_STOPPED)
            {
                unregisterMBeansIfNecessary();
            }
        }
    }
}
