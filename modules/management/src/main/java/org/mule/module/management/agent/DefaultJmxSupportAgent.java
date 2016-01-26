/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.module.management.agent;

import static org.mule.config.bootstrap.ArtifactType.APP;
import static org.mule.config.bootstrap.ArtifactType.DOMAIN;

import org.mule.AbstractAgent;
import org.mule.api.MuleException;
import org.mule.api.agent.Agent;
import org.mule.api.lifecycle.InitialisationException;
import org.mule.api.registry.MuleRegistry;
import org.mule.api.registry.RegistrationException;
import org.mule.config.bootstrap.ArtifactType;
import org.mule.util.StringUtils;

import java.rmi.server.RMIClientSocketFactory;
import java.text.MessageFormat;
import java.util.HashMap;
import java.util.Map;

import javax.management.remote.rmi.RMIConnectorServer;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class DefaultJmxSupportAgent extends AbstractAgent
{

    private static final Logger logger = LoggerFactory.getLogger(DefaultJmxSupportAgent.class);

    public static final String DEFAULT_HOST = "localhost";
    public static final String DEFAULT_PORT = "1099";

    private final boolean loadLog4jAgent = false;
    private boolean loadJdmkAgent = false;
    private boolean loadMx4jAgent = false;
    private boolean loadProfilerAgent = false;
    private String port;
    private String host;

    private ConfigurableJMXAuthenticator jmxAuthenticator;

    public DefaultJmxSupportAgent()
    {
        super("jmx-default-config");
    }

    /**
     * Username/password combinations for JMX Remoting
     * authentication.
     */
    private Map<String, String> credentials = new HashMap<String, String>();

    /**
     * Should be a 1 line description of the agent
     *
     * @return agent description
     */
    @Override
    public String getDescription()
    {
        return "Default Jmx Support Agent";
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void start() throws MuleException
    {
        // nothing to do
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void stop() throws MuleException
    {
        // nothing to do
    }

    /**
     * A lifecycle method where implementor should free up any resources. If an
     * exception is thrown it should just be logged and processing should continue.
     * This method should not throw Runtime exceptions.
     */
    @Override
    public void dispose()
    {
        // nothing to do
    }

    /**
     * Method used to perform any initialisation work. If a fatal error occurs during
     * initialisation an <code>InitialisationException</code> should be thrown,
     * causing the Mule instance to shutdown. If the error is recoverable, say by
     * retrying to connect, a <code>RecoverableException</code> should be thrown.
     * There is no guarantee that by throwing a Recoverable exception that the Mule
     * instance will not shut down.
     *
     * @throws org.mule.api.lifecycle.InitialisationException if a fatal error occurs
     *                                                        causing the Mule instance to shutdown
     */
    @Override
    public void initialise() throws InitialisationException
    {
        try
        {
            Agent agent = createRmiAgent();
            final MuleRegistry registry = muleContext.getRegistry();
            if (!isAgentRegistered(agent))
            {
                agent.initialise();
                registerAgent(agent, registry);
            }

            // any existing jmx agent will be modified with remote connector settings
            agent = createJmxAgent();
            if (lookupJmxAgent(registry) == null)
            {
                registerAgent(agent, registry);
            }

            agent = createJmxNotificationAgent();
            if (!isAgentRegistered(agent))
            {
                registerAgent(agent, registry);
            }

            if (loadJdmkAgent)
            {
                agent = createJdmkAgent();
                if (!isAgentRegistered(agent))
                {
                    registerAgent(agent, registry);
                }
            }

            if (loadMx4jAgent)
            {
                agent = createMx4jAgent();
                if (!isAgentRegistered(agent))
                {
                    registerAgent(agent, registry);
                }
            }

            if (loadProfilerAgent)
            {
                agent = createProfilerAgent();
                if (!isAgentRegistered(agent))
                {
                    registerAgent(agent, registry);
                }
            }
        }
        catch (MuleException e)
        {
            throw new InitialisationException(e, this);
        }
    }

    protected AbstractJmxAgent lookupJmxAgent(final MuleRegistry registry) throws RegistrationException
    {
        // there must be only one jmx agent, so lookup by type instead
        ArtifactType artifactType = muleContext.getArtifactType();
        if (APP.equals(artifactType))
        {
            return registry.lookupObject(JmxApplicationAgent.class);
        }
        else if (DOMAIN.equals(artifactType))
        {
            return registry.lookupObject(JmxDomainAgent.class);
        }
        else
        {
            return registry.lookupObject(AbstractJmxAgent.class);
        }
    }

    protected void registerAgent(Agent agent, MuleRegistry registry) throws MuleException
    {
        registry.registerAgent(agent);
    }

    public AbstractJmxAgent createJmxAgent()
    {
        AbstractJmxAgent agent;
        try
        {
            agent = lookupJmxAgent(muleContext.getRegistry());
        }
        catch (RegistrationException e)
        {
            throw new RuntimeException(e);
        }

        // otherwise, just augment an existing jmx agent with remote connector

        String remotingUri = null;
        if (StringUtils.isBlank(host) && StringUtils.isBlank(port))
        {
            remotingUri = AbstractJmxAgent.DEFAULT_REMOTING_URI;
        }
        else if (StringUtils.isNotBlank(host))
        {
            // enable support for multi-NIC servers by configuring
            // a custom RMIClientSocketFactory
            Map<String, Object> props = agent.getConnectorServerProperties();
            Map<String, Object> mergedProps = new HashMap<String, Object>(props.size() + 1);
            mergedProps.putAll(props);

            RMIClientSocketFactory factory = new FixedHostRmiClientSocketFactory(host);
            mergedProps.put(RMIConnectorServer.RMI_CLIENT_SOCKET_FACTORY_ATTRIBUTE,
                            factory);
            agent.setConnectorServerProperties(mergedProps);
        }

        // if defaults haven't been used
        if (StringUtils.isBlank(remotingUri))
        {
            remotingUri = MessageFormat.format("service:jmx:rmi:///jndi/rmi://{0}:{1}/server",
                                               StringUtils.defaultString(host, DEFAULT_HOST),
                                               StringUtils.defaultString(port, DEFAULT_PORT));
        }

        if (credentials != null && !credentials.isEmpty())
        {
            agent.setCredentials(credentials);
        }
        agent.setConnectorServerUrl(remotingUri);
        agent.setJmxAuthenticator(this.jmxAuthenticator);
        return agent;
    }

    protected Log4jAgent createLog4jAgent()
    {
        return new Log4jAgent();
    }

    protected RmiRegistryAgent createRmiAgent()
    {
        final RmiRegistryAgent agent = new RmiRegistryAgent();
        agent.setHost(StringUtils.defaultString(host, DEFAULT_HOST));
        agent.setPort(StringUtils.defaultString(port, DEFAULT_PORT));
        return agent;
    }

    protected JmxServerNotificationAgent createJmxNotificationAgent()
    {
        return new JmxServerNotificationAgent();
    }

    protected Mx4jAgent createMx4jAgent()
    {
        return new Mx4jAgent();
    }

    protected JdmkAgent createJdmkAgent()
    {
        return new JdmkAgent();
    }

    protected YourKitProfilerAgent createProfilerAgent()
    {
        return new YourKitProfilerAgent();
    }

    protected boolean isAgentRegistered(Agent agent)
    {
        return muleContext.getRegistry().lookupAgent(agent.getName()) != null;
    }

    public boolean isLoadLog4jAgent()
    {
        return loadLog4jAgent;
    }

    public void setLoadLog4jAgent(boolean loadLog4jAgent)
    {
        if (loadLog4jAgent)
        {
            logger.warn("Log4jAgent is deprecated since Mule 3.6.0 because log4j2 already supports JMX OOTB. " +
                        "Thus, enabling it here will have no effect. Check migration guide for more information");
        }
    }

    public boolean isLoadJdmkAgent()
    {
        return loadJdmkAgent;
    }

    public void setLoadJdmkAgent(boolean loadJdmkAgent)
    {
        this.loadJdmkAgent = loadJdmkAgent;
    }

    public boolean isLoadMx4jAgent()
    {
        return loadMx4jAgent;
    }

    public void setLoadMx4jAgent(boolean loadMx4jAgent)
    {
        this.loadMx4jAgent = loadMx4jAgent;
    }

    public boolean isLoadProfilerAgent()
    {
        return loadProfilerAgent;
    }

    public void setLoadProfilerAgent(boolean loadProfilerAgent)
    {
        this.loadProfilerAgent = loadProfilerAgent;
    }

    public String getPort()
    {
        return port;
    }

    public void setPort(final String port)
    {
        this.port = port;
    }

    @Deprecated
    public String getHost()
    {
        return host;
    }

    @Deprecated
    public void setHost(final String host)
    {
        logger.warn("The host attribute for jmx-default-config is deprecated, for multi-homed hosts consider using " +
                    "instead the Java system property java.rmi.server.hostname");
        this.host = host;
    }

    public void setCredentials(Map<String, String> credentials)
    {
        this.credentials = credentials;
    }

    public ConfigurableJMXAuthenticator getJmxAuthenticator()
    {
        return jmxAuthenticator;
    }

    public void setJmxAuthenticator(ConfigurableJMXAuthenticator jmxAuthenticator)
    {
        this.jmxAuthenticator = jmxAuthenticator;
    }
}
