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
    public static final Map DEFAULT_CONNECTOR_SERVER_PROPERTIES;

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
    private Map connectorServerProperties = null;
    private boolean enableStatistics = true;

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

    public JmxAgentConfigurer()
    {
        connectorServerProperties = new HashMap(DEFAULT_CONNECTOR_SERVER_PROPERTIES);
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

    public void setMuleContext(MuleContext context)
    {
        this.muleContext = context;
        try
        {
            // by the time mule contextis injected, other attributes will have been set already
            JmxAgent agent = (JmxAgent) muleContext.getRegistry().lookupObject(JmxAgent.class);
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
