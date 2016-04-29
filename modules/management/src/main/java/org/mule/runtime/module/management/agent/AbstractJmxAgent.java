/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.module.management.agent;

import org.mule.runtime.core.AbstractAgent;
import org.mule.runtime.core.api.MuleException;
import org.mule.runtime.core.api.MuleRuntimeException;
import org.mule.runtime.core.api.context.notification.MuleContextNotificationListener;
import org.mule.runtime.core.api.lifecycle.InitialisationException;
import org.mule.runtime.core.api.transport.Connector;
import org.mule.runtime.core.api.transport.MessageReceiver;
import org.mule.runtime.core.config.i18n.CoreMessages;
import org.mule.runtime.core.construct.AbstractFlowConstruct;
import org.mule.runtime.core.context.notification.MuleContextNotification;
import org.mule.runtime.core.context.notification.NotificationException;
import org.mule.runtime.core.management.stats.FlowConstructStatistics;
import org.mule.runtime.core.transport.AbstractConnector;
import org.mule.runtime.core.util.StringUtils;
import org.mule.runtime.module.management.i18n.ManagementMessages;
import org.mule.runtime.module.management.mbean.ApplicationService;
import org.mule.runtime.module.management.mbean.ConnectorService;
import org.mule.runtime.module.management.mbean.ConnectorServiceMBean;
import org.mule.runtime.module.management.mbean.EndpointService;
import org.mule.runtime.module.management.mbean.EndpointServiceMBean;
import org.mule.runtime.module.management.mbean.FlowConstructService;
import org.mule.runtime.module.management.mbean.FlowConstructServiceMBean;
import org.mule.runtime.module.management.mbean.MuleConfigurationService;
import org.mule.runtime.module.management.mbean.MuleConfigurationServiceMBean;
import org.mule.runtime.module.management.mbean.MuleService;
import org.mule.runtime.module.management.mbean.MuleServiceMBean;
import org.mule.runtime.module.management.mbean.StatisticsService;
import org.mule.runtime.module.management.mbean.StatisticsServiceMBean;
import org.mule.runtime.module.management.support.AutoDiscoveryJmxSupportFactory;
import org.mule.runtime.module.management.support.JmxSupport;
import org.mule.runtime.module.management.support.JmxSupportFactory;
import org.mule.runtime.module.management.support.SimplePasswordJmxAuthenticator;

import java.lang.management.ManagementFactory;
import java.net.URI;
import java.rmi.RemoteException;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.rmi.server.ExportException;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.atomic.AtomicBoolean;

import javax.management.InstanceAlreadyExistsException;
import javax.management.MBeanRegistrationException;
import javax.management.MBeanServer;
import javax.management.MBeanServerFactory;
import javax.management.MalformedObjectNameException;
import javax.management.NotCompliantMBeanException;
import javax.management.ObjectName;
import javax.management.remote.JMXConnectorServer;
import javax.management.remote.JMXConnectorServerFactory;
import javax.management.remote.JMXServiceURL;
import javax.management.remote.rmi.RMIConnectorServer;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

/**
 * <code>AbstractJmxAgent</code> registers Mule Jmx management beans with an MBean server.
 */
public abstract class AbstractJmxAgent extends AbstractAgent
{
    public static final String NAME = "jmx-agent";

    public static final String DEFAULT_REMOTING_URI = "service:jmx:rmi:///jndi/rmi://localhost:1099/server";

    // populated with values below in a static initializer
    public static final Map<String, String> DEFAULT_CONNECTOR_SERVER_PROPERTIES;

    /**
     * Default JMX Authenticator to use for securing remote access.
     */
    public static final String DEFAULT_JMX_AUTHENTICATOR = SimplePasswordJmxAuthenticator.class.getName();

    /**
     * Logger used by this class
     */
    protected static final Log logger = LogFactory.getLog(AbstractJmxAgent.class);

    /**
     * Should MBeanServer be discovered.
     */
    protected boolean locateServer = true;

    protected boolean containerMode = true;

    // don't create mbean server by default, use a platform mbean server
    private boolean createServer = false;
    private String connectorServerUrl;
    private MBeanServer mBeanServer;
    private JMXConnectorServer connectorServer;
    private Map<String, Object> connectorServerProperties = null;
    private boolean enableStatistics = true;
    private final AtomicBoolean serverCreated = new AtomicBoolean(false);
    private final AtomicBoolean initialized = new AtomicBoolean(false);

    private JmxSupportFactory jmxSupportFactory = AutoDiscoveryJmxSupportFactory.getInstance();
    private JmxSupport jmxSupport = jmxSupportFactory.getJmxSupport();
    private ConfigurableJMXAuthenticator jmxAuthenticator;

    //Used is RMI is being used
    private Registry rmiRegistry;
    private boolean createRmiRegistry = true;

    /**
     * Username/password combinations for JMX Remoting authentication.
     */
    private Map<String, String> credentials = new HashMap<String, String>();

    private AbstractJmxAgent.MuleContextStartedListener muleContextStartedListener;
    private AbstractJmxAgent.MuleContextStoppedListener muleContextStoppedListener;

    static
    {
        Map<String, String> props = new HashMap<String, String>(1);
        props.put(RMIConnectorServer.JNDI_REBIND_ATTRIBUTE, "true");
        DEFAULT_CONNECTOR_SERVER_PROPERTIES = Collections.unmodifiableMap(props);
    }

    public AbstractJmxAgent()
    {
        super(NAME);
        connectorServerProperties = new HashMap<String, Object>(DEFAULT_CONNECTOR_SERVER_PROPERTIES);
    }

    @Override
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
     */
    @Override
    public void initialise() throws InitialisationException
    {
        if (initialized.get())
        {
            return;
        }

        this.containerMode = muleContext.getConfiguration().isContainerMode();

        try
        {
            Object agent = muleContext.getRegistry().lookupObject(this.getClass());
            // if we find ourselves, but not initialized yet - proceed with init, otherwise return
            if (agent == this && this.initialized.get())
            {
                if (logger.isDebugEnabled())
                {
                    logger.debug("Found an existing JMX agent in the registry, we're done here.");
                }
                return;
            }
        }
        catch (Exception e)
        {
            throw new InitialisationException(e, this);
        }


        if (mBeanServer == null && createServer)
        {
            // here we create a new mbean server, not using a platform one
            mBeanServer = MBeanServerFactory.createMBeanServer();
            serverCreated.set(true);
        }

        if (mBeanServer == null && locateServer)
        {
            mBeanServer = ManagementFactory.getPlatformMBeanServer();
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
            muleContextStartedListener = new MuleContextStartedListener();
            muleContext.registerListener(muleContextStartedListener);
            // and unregister once context stopped
            muleContextStoppedListener = new MuleContextStoppedListener();
            muleContext.registerListener(muleContextStoppedListener);
        }
        catch (NotificationException e)
        {
            throw new InitialisationException(e, this);
        }
        initialized.compareAndSet(false, true);
    }

    protected void initRMI() throws Exception
    {
        String connectUri = (connectorServerUrl != null ? connectorServerUrl : StringUtils.EMPTY);
        if (connectUri.contains("jmx:rmi"))
        {
            int i = connectUri.lastIndexOf("rmi://");
            URI uri = new URI(connectUri.substring(i));
            if (rmiRegistry == null)
            {
                try
                {
                    if (isCreateRmiRegistry())
                    {
                        try
                        {
                            rmiRegistry = LocateRegistry.createRegistry(uri.getPort());
                        }
                        catch (ExportException e)
                        {
                            logger.info("Registry on " + uri  + " already bound. Attempting to use that instead");
                            rmiRegistry = LocateRegistry.getRegistry(uri.getHost(), uri.getPort());
                        }
                    }
                    else
                    {
                        rmiRegistry = LocateRegistry.getRegistry(uri.getHost(), uri.getPort());
                    }
                }
                catch (RemoteException e)
                {
                    throw new InitialisationException(e, this);
                }
            }
        }
    }

    @Override
    public void start() throws MuleException
    {
        try
        {
            // TODO cleanup rmi registry creation too
            initRMI();
            if (connectorServerUrl == null)
            {
                return;
            }

            logger.info("Creating and starting JMX agent connector Server");
            JMXServiceURL url = new JMXServiceURL(connectorServerUrl);
            if (connectorServerProperties == null)
            {
                connectorServerProperties = new HashMap<String, Object>(DEFAULT_CONNECTOR_SERVER_PROPERTIES);
            }
            if (!credentials.isEmpty())
            {
                connectorServerProperties.put(JMXConnectorServer.AUTHENTICATOR,
                        this.getJmxAuthenticator());
            }
            connectorServer = JMXConnectorServerFactory.newJMXConnectorServer(url,
                                                                              connectorServerProperties,
                                                                              mBeanServer);
            connectorServer.start();
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

    @Override
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
    @Override
    public void dispose()
    {
        unregisterMBeansIfNecessary();
        unregisterListeners();
        if (serverCreated.get())
        {
            MBeanServerFactory.releaseMBeanServer(mBeanServer);
        }
        mBeanServer = null;
        serverCreated.compareAndSet(true, false);
        initialized.set(false);
    }

    private void unregisterListeners()
    {
        muleContext.unregisterListener(muleContextStartedListener);
        muleContext.unregisterListener(muleContextStoppedListener);
    }

    /**
     * Register a Java Service Wrapper agent.
     *
     * @throws org.mule.runtime.core.api.MuleException if registration failed
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
        ObjectName on = jmxSupport.getObjectName(String.format("%s:%s", jmxSupport.getDomainName(muleContext, !containerMode), StatisticsServiceMBean.DEFAULT_JMX_NAME));
        StatisticsService service = new StatisticsService();
        service.setMuleContext(muleContext);
        service.setEnabled(isEnableStatistics());
        ClassloaderSwitchingMBeanWrapper mBean = new ClassloaderSwitchingMBeanWrapper(service, StatisticsServiceMBean.class, muleContext.getExecutionClassLoader());
        logger.debug("Registering statistics with name: " + on);
        mBeanServer.registerMBean(mBean, on);
    }

    protected void registerMuleService() throws NotCompliantMBeanException, MBeanRegistrationException,
        InstanceAlreadyExistsException, MalformedObjectNameException
    {
        ObjectName on = jmxSupport.getObjectName(String.format("%s:%s", jmxSupport.getDomainName(muleContext, !containerMode), MuleServiceMBean.DEFAULT_JMX_NAME));
        if (muleContext.getConfiguration().isContainerMode() && mBeanServer.isRegistered(on))
        {
            // while in container mode, a previous stop() action leaves MuleContext MBean behind for remote start() operation
            return;
        }
        MuleService service = new MuleService(muleContext);
        ClassloaderSwitchingMBeanWrapper serviceMBean = new ClassloaderSwitchingMBeanWrapper(service, MuleServiceMBean.class, muleContext.getExecutionClassLoader());
        logger.debug("Registering mule with name: " + on);
        mBeanServer.registerMBean(serviceMBean, on);
    }

    protected void registerConfigurationService() throws NotCompliantMBeanException, MBeanRegistrationException,
            InstanceAlreadyExistsException, MalformedObjectNameException
    {
        ObjectName on = jmxSupport.getObjectName(String.format("%s:%s", jmxSupport.getDomainName(muleContext, !containerMode), MuleConfigurationServiceMBean.DEFAULT_JMX_NAME));
        MuleConfigurationServiceMBean service = new MuleConfigurationService(muleContext.getConfiguration());
        ClassloaderSwitchingMBeanWrapper mBean = new ClassloaderSwitchingMBeanWrapper(service, MuleConfigurationServiceMBean.class, muleContext.getExecutionClassLoader());
        logger.debug("Registering configuration with name: " + on);
        mBeanServer.registerMBean(mBean, on);
    }

    protected void registerFlowConstructServices() throws NotCompliantMBeanException, MBeanRegistrationException,
        InstanceAlreadyExistsException, MalformedObjectNameException
    {
        for (AbstractFlowConstruct flowConstruct : muleContext.getRegistry().lookupObjects(AbstractFlowConstruct.class))
        {
            final String rawName = flowConstruct.getName();
            final String name = jmxSupport.escape(rawName);
            final String jmxName = String.format("%s:type=%s,name=%s", jmxSupport.getDomainName(muleContext, !containerMode), flowConstruct.getConstructType(), name);
            ObjectName on = jmxSupport.getObjectName(jmxName);
            FlowConstructServiceMBean fcMBean = new FlowConstructService(flowConstruct.getConstructType(), rawName, muleContext, flowConstruct.getStatistics());
            ClassloaderSwitchingMBeanWrapper wrapper = new ClassloaderSwitchingMBeanWrapper(fcMBean, FlowConstructServiceMBean.class, muleContext.getExecutionClassLoader());
            logger.debug("Registering service with name: " + on);
            mBeanServer.registerMBean(wrapper, on);
        }
    }

    protected void registerApplicationServices() throws NotCompliantMBeanException, MBeanRegistrationException,
        InstanceAlreadyExistsException, MalformedObjectNameException
    {
        FlowConstructStatistics appStats = muleContext.getStatistics().getApplicationStatistics();
        if (appStats != null)
        {
            final String rawName = appStats.getName();
            final String name = jmxSupport.escape(rawName);
            final String jmxName = String.format("%s:type=%s,name=%s", jmxSupport.getDomainName(muleContext, !containerMode), appStats.getFlowConstructType(), name);
            ObjectName on = jmxSupport.getObjectName(jmxName);
            FlowConstructServiceMBean fcMBean = new ApplicationService(appStats.getFlowConstructType(), rawName, muleContext,appStats);
            ClassloaderSwitchingMBeanWrapper wrapper = new ClassloaderSwitchingMBeanWrapper(fcMBean, FlowConstructServiceMBean.class, muleContext.getExecutionClassLoader());
            logger.debug("Registering application statistics with name: " + on);
            mBeanServer.registerMBean(wrapper, on);
        }
    }

    protected void registerEndpointServices() throws NotCompliantMBeanException, MBeanRegistrationException,
            InstanceAlreadyExistsException, MalformedObjectNameException
    {
        for (Connector connector : muleContext.getRegistry().lookupObjects(Connector.class))
        {
            if (connector instanceof AbstractConnector)
            {
                for (MessageReceiver messageReceiver : ((AbstractConnector) connector).getReceivers().values())
                {
                    if (muleContext.equals(messageReceiver.getFlowConstruct().getMuleContext()))
                    {
                        EndpointServiceMBean service = new EndpointService(messageReceiver);

                        String fullName = buildFullyQualifiedEndpointName(service, connector);
                        if (logger.isInfoEnabled())
                        {
                            logger.info("Attempting to register service with name: " + fullName);
                        }

                        ObjectName on = jmxSupport.getObjectName(fullName);
                        ClassloaderSwitchingMBeanWrapper mBean = new ClassloaderSwitchingMBeanWrapper(service, EndpointServiceMBean.class, muleContext.getExecutionClassLoader());
                        mBeanServer.registerMBean(mBean, on);
                        if (logger.isInfoEnabled())
                        {
                            logger.info("Registered Endpoint Service with name: " + on);
                        }
                    }
                }
            }
            else
            {
                logger.warn("Connector: " + connector
                            + " is not an istance of AbstractConnector, cannot obtain Endpoint MBeans from it");
            }
        }
    }

    protected String buildFullyQualifiedEndpointName(EndpointServiceMBean mBean, Connector connector)
    {
        String rawName = jmxSupport.escape(mBean.getName());

        StringBuilder fullName = new StringBuilder(128);
        fullName.append(jmxSupport.getDomainName(muleContext, !containerMode));
        fullName.append(":type=Endpoint,service=");
        fullName.append(jmxSupport.escape(mBean.getComponentName()));
        fullName.append(",connector=");
        fullName.append(connector.getName());
        fullName.append(",name=");
        fullName.append(rawName);
        return fullName.toString();
    }

    protected void registerConnectorServices() throws MalformedObjectNameException,
            NotCompliantMBeanException, MBeanRegistrationException, InstanceAlreadyExistsException
    {
        for (Connector connector : muleContext.getRegistry().lookupLocalObjects(Connector.class))
        {
            ConnectorServiceMBean service = new ConnectorService(connector);
            final String rawName = service.getName();
            final String name = jmxSupport.escape(rawName);
            final String jmxName = String.format("%s:%s%s", jmxSupport.getDomainName(muleContext, !containerMode), ConnectorServiceMBean.DEFAULT_JMX_NAME_PREFIX, name);
            if (logger.isDebugEnabled())
            {
                logger.debug("Attempting to register service with name: " + jmxName);
            }
            ObjectName oName = jmxSupport.getObjectName(jmxName);
            ClassloaderSwitchingMBeanWrapper mBean = new ClassloaderSwitchingMBeanWrapper(service, ConnectorServiceMBean.class, muleContext.getExecutionClassLoader());
            mBeanServer.registerMBean(mBean, oName);
            logger.info("Registered Connector Service with name " + oName);
        }
    }


    public boolean isCreateServer()
    {
        return createServer;
    }

    public void setCreateServer(boolean createServer)
    {
        this.createServer = createServer;
    }

    public boolean isLocateServer()
    {
        return locateServer;
    }

    public void setLocateServer(boolean locateServer)
    {
        this.locateServer = locateServer;
    }

    public String getConnectorServerUrl()
    {
        return connectorServerUrl;
    }

    public void setConnectorServerUrl(String connectorServerUrl)
    {
        this.connectorServerUrl = connectorServerUrl;
    }

    public boolean isEnableStatistics()
    {
        return enableStatistics;
    }

    public void setEnableStatistics(boolean enableStatistics)
    {
        this.enableStatistics = enableStatistics;
    }

    public MBeanServer getMBeanServer()
    {
        return mBeanServer;
    }

    public void setMBeanServer(MBeanServer mBeanServer)
    {
        this.mBeanServer = mBeanServer;
    }

    public Map<String, Object> getConnectorServerProperties()
    {
        return connectorServerProperties;
    }

    /**
     * Setter for property 'connectorServerProperties'. Set to {@code null} to use defaults ({@link
     * #DEFAULT_CONNECTOR_SERVER_PROPERTIES}). Pass in an empty map to use no parameters.
     * Passing a non-empty map will replace defaults.
     *
     * @param connectorServerProperties Value to set for property 'connectorServerProperties'.
     */
    public void setConnectorServerProperties(Map<String, Object> connectorServerProperties)
    {
        this.connectorServerProperties = connectorServerProperties;
    }

    public JmxSupportFactory getJmxSupportFactory()
    {
        return jmxSupportFactory;
    }

    public void setJmxSupportFactory(JmxSupportFactory jmxSupportFactory)
    {
        this.jmxSupportFactory = jmxSupportFactory;
    }


    /**
     * Setter for property 'credentials'.
     *
     * @param newCredentials Value to set for property 'credentials'.
     */
    public void setCredentials(final Map<String, String> newCredentials)
    {
        this.credentials.clear();
        if (newCredentials != null && !newCredentials.isEmpty())
        {
            this.credentials.putAll(newCredentials);
        }
    }

    protected void unregisterMBeansIfNecessary()
    {
        unregisterMBeansIfNecessary(false);
    }

    /**
     * @param containerMode when true, MuleContext will still be exposed to enable the 'start' operation
     */
    protected void unregisterMBeansIfNecessary(boolean containerMode)
    {
        if (mBeanServer == null)
        {
            return;
        }

        try
        {
            // note that we don't try to resolve a domain name clash here.
            // e.g. when stopping an app via jmx, we want to obtain current domain only,
            // but the execution thread is different, and doesn't have the resolved domain info
            final String domain = jmxSupport.getDomainName(muleContext, false);
            ObjectName query = jmxSupport.getObjectName(domain + ":*");
            Set<ObjectName> mbeans = mBeanServer.queryNames(query, null);
            while (!mbeans.isEmpty())
            {
                ObjectName name = mbeans.iterator().next();
                try
                {
                    if (!(containerMode && MuleServiceMBean.DEFAULT_JMX_NAME.equals(name.getCanonicalKeyPropertyListString())))
                    {
                        mBeanServer.unregisterMBean(name);
                    }
                }
                catch (Exception e)
                {
                    logger.warn(String.format("Failed to unregister MBean: %s. Error is: %s", name, e.getMessage()));
                }

                // query mbeans again, as some mbeans have cascaded unregister operations,
                // this prevents duplicate unregister attempts
                mbeans = mBeanServer.queryNames(query, null);

                if (containerMode)
                {
                    // filter out MuleContext MBean to avoid an endless loop
                    mbeans.remove(jmxSupport.getObjectName(String.format("%s:%s", domain, MuleServiceMBean.DEFAULT_JMX_NAME)));
                }
            }
        }
        catch (MalformedObjectNameException e)
        {
            logger.warn("Failed to create ObjectName query", e);
        }
    }

    public Registry getRmiRegistry()
    {
        return rmiRegistry;
    }

    public void setRmiRegistry(Registry rmiRegistry)
    {
        this.rmiRegistry = rmiRegistry;
    }

    public boolean isCreateRmiRegistry()
    {
        return createRmiRegistry;
    }

    public void setCreateRmiRegistry(boolean createRmiRegistry)
    {
        this.createRmiRegistry = createRmiRegistry;
    }

    protected class MuleContextStartedListener implements MuleContextNotificationListener<MuleContextNotification>
    {

        @Override
        public void onNotification(MuleContextNotification notification)
        {
            if (notification.getAction() == MuleContextNotification.CONTEXT_STARTED)
            {
                try
                {
                    registerServices();
                }
                catch (Exception e)
                {
                    throw new MuleRuntimeException(CoreMessages.objectFailedToInitialise("MBeans"), e);
                }
            }
        }
    }

    protected abstract void registerServices() throws MuleException, NotCompliantMBeanException, MBeanRegistrationException, InstanceAlreadyExistsException, MalformedObjectNameException;

    protected class MuleContextStoppedListener implements MuleContextNotificationListener<MuleContextNotification>
    {

        @Override
        public void onNotification(MuleContextNotification notification)
        {
            if (notification.getAction() == MuleContextNotification.CONTEXT_STOPPED)
            {
                boolean containerMode = notification.getMuleContext().getConfiguration().isContainerMode();
                unregisterMBeansIfNecessary(containerMode);
            }
        }
    }

    public ConfigurableJMXAuthenticator getJmxAuthenticator()
    {
        if (this.jmxAuthenticator == null)
        {
            this.jmxAuthenticator = new SimplePasswordJmxAuthenticator();
            this.jmxAuthenticator.configure(credentials);
        }
        return jmxAuthenticator;
    }

    public void setJmxAuthenticator(ConfigurableJMXAuthenticator jmxAuthenticator)
    {
        this.jmxAuthenticator = jmxAuthenticator;
    }
}
