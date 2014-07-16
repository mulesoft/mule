/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.module.management.agent;

import org.mule.api.MuleContext;
import org.mule.api.context.MuleContextAware;
import org.mule.api.registry.RegistrationException;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import javax.management.MBeanServer;
import javax.management.remote.rmi.RMIConnectorServer;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

/**
 * Mule now binds to a platform mbeanserver by default and jmx agent is always registered via a
 * bootstrap process. Thus a namespace handler creates this configurer class instead which propagates
 * user settings to a jmx agent in the registry (instead of trying to register a duplicate jmx agent).
 */
public class JmxAgentConfigurer implements MuleContextAware
{
    // populated with values below in a static initializer
    public static final Map<String, Object> DEFAULT_CONNECTOR_SERVER_PROPERTIES;

    /**
     * Logger used by this class
     */
    protected static final Log logger = LogFactory.getLog(JmxAgentConfigurer.class);

    protected MuleContext muleContext;

    /**
     * Should MBeanServer be discovered.
     */
    protected boolean locateServer = true;
    // don't create mbean server by default, use a platform mbean server
    private boolean createServer = false;
    private String connectorServerUrl;
    private MBeanServer mBeanServer;
    private Map<String, Object> connectorServerProperties = null;
    private boolean enableStatistics = true;
    private boolean createRmiRegistry = true;

    /**
     * Username/password combinations for JMX Remoting authentication.
     */
    private Map<String, String> credentials = new HashMap<String, String>();

    static
    {
        Map<String, Object> props = new HashMap<String, Object>(1);
        props.put(RMIConnectorServer.JNDI_REBIND_ATTRIBUTE, "true");
        DEFAULT_CONNECTOR_SERVER_PROPERTIES = Collections.unmodifiableMap(props);
    }

    public JmxAgentConfigurer()
    {
        connectorServerProperties = new HashMap<String, Object>(DEFAULT_CONNECTOR_SERVER_PROPERTIES);
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
     * Setter for property 'connectorServerProperties'. Set to {@code null} to use
     * defaults ({@link #DEFAULT_CONNECTOR_SERVER_PROPERTIES}). Pass in an empty map
     * to use no parameters. Passing a non-empty map will replace defaults.
     *
     * @param connectorServerProperties Value to set for property
     *            'connectorServerProperties'.
     */
    public void setConnectorServerProperties(Map<String, Object> connectorServerProperties)
    {
        this.connectorServerProperties = connectorServerProperties;
    }

    public void setCredentials(final Map<String, String> newCredentials)
    {
        this.credentials.clear();
        if (newCredentials != null && !newCredentials.isEmpty())
        {
            this.credentials.putAll(newCredentials);
        }
    }

    public boolean isCreateRmiRegistry()
    {
        return createRmiRegistry;
    }

    public void setCreateRmiRegistry(boolean createRmiRegistry)
    {
        this.createRmiRegistry = createRmiRegistry;
    }

    public void setMuleContext(MuleContext context)
    {
        this.muleContext = context;
        try
        {
            // by the time mule context is injected, other attributes will have been set already
            AbstractJmxAgent agent = muleContext.getRegistry().lookupObject(AbstractJmxAgent.class);
            // in case it is injected, otherwise will follow the init logic
            if (getMBeanServer() != null)
            {
                agent.setMBeanServer(getMBeanServer());
            }
            if (getConnectorServerUrl() != null)
            {
                agent.setConnectorServerUrl(getConnectorServerUrl());
            }
            if (getConnectorServerProperties() != null && !getConnectorServerProperties().isEmpty())
            {
                agent.setConnectorServerProperties(getConnectorServerProperties());
            }
            // these can be copied as is
            agent.setCreateServer(isCreateServer());
            agent.setLocateServer(isLocateServer());
            agent.setEnableStatistics(isEnableStatistics());
            agent.setCreateRmiRegistry(isCreateRmiRegistry());
            agent.setCredentials(credentials);
        }
        catch (RegistrationException e)
        {
            throw new RuntimeException(e);
        }
    }

    public void setName(String name)
    {
        // ignore the name, spring wants it
    }
}
