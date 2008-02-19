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
import org.mule.RegistryContext;
import org.mule.api.MuleException;
import org.mule.api.agent.Agent;
import org.mule.api.lifecycle.InitialisationException;
import org.mule.api.lifecycle.LifecycleTransitionResult;
import org.mule.util.StringUtils;

import java.rmi.server.RMIClientSocketFactory;
import java.text.MessageFormat;
import java.util.HashMap;
import java.util.Map;

import javax.management.remote.rmi.RMIConnectorServer;


public class DefaultJmxSupportAgent extends AbstractAgent
{

    public static final String DEFAULT_HOST = "localhost";
    public static final String DEFAULT_PORT = "1099";

    private boolean loadJdmkAgent = false;
    private boolean loadMx4jAgent = false;
    private boolean loadProfilerAgent = false;
    private String port;
    private String host;

    public DefaultJmxSupportAgent()
    {
        super("Default Jmx");
    }

    /**
     * Username/password combinations for JMX Remoting
     * authentication.
     */
    private Map credentials = new HashMap();


    /**
     * Should be a 1 line description of the agent
     *
     * @return agent description
     */
    public String getDescription()
    {
        return "Default Jmx Agent Support";
    }

    /** {@inheritDoc} */
    public void registered()
    {
        // nothing to do
    }

    /** {@inheritDoc} */
    public void unregistered()
    {
        // nothing to do
    }

    /** {@inheritDoc} */
    public LifecycleTransitionResult start() throws MuleException
    {
        return LifecycleTransitionResult.OK;
    }

    /** {@inheritDoc} */
    public LifecycleTransitionResult stop() throws MuleException
    {
        return LifecycleTransitionResult.OK;
    }

    /**
     * A lifecycle method where implementor should free up any resources. If an
     * exception is thrown it should just be logged and processing should continue.
     * This method should not throw Runtime exceptions.
     */
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
     * @throws org.mule.api.lifecycle.InitialisationException
     *          if a fatal error occurs
     *          causing the Mule instance to shutdown
     */
    public LifecycleTransitionResult initialise() throws InitialisationException
    {
        try
        {
            Agent agent = createRmiAgent();
            if (!isAgentRegistered(agent))
            {
                RegistryContext.getRegistry().registerAgent(agent);
            }
            agent = createJmxAgent();
            if (!isAgentRegistered(agent))
            {
                RegistryContext.getRegistry().registerAgent(agent);
            }
            agent = createLog4jAgent();
            if (!isAgentRegistered(agent))
            {
                RegistryContext.getRegistry().registerAgent(agent);
            }
            agent = createJmxNotificationAgent();
            if (!isAgentRegistered(agent))
            {
                RegistryContext.getRegistry().registerAgent(agent);
            }
            if (loadJdmkAgent)
            {
                agent = createJdmkAgent();
                if (!isAgentRegistered(agent))
                {
                    RegistryContext.getRegistry().registerAgent(agent);
                }
            }

            if (loadMx4jAgent)
            {
                agent = createMx4jAgent();
                if (!isAgentRegistered(agent))
                {
                    RegistryContext.getRegistry().registerAgent(agent);
                }
            }

            if (loadProfilerAgent)
            {
                agent = createProfilerAgent();
                if (!isAgentRegistered(agent))
                {
                    RegistryContext.getRegistry().registerAgent(agent);
                }
            }

            // remove this agent once it has registered the other agents
            //TODO RM* this currently does nothing!!!
            muleContext.getRegistry().unregisterAgent(name);
        }
        catch (MuleException e)
        {
            throw new InitialisationException(e, this);
        }

        return LifecycleTransitionResult.OK;
    }

    protected JmxAgent createJmxAgent()
    {
        JmxAgent agent = new JmxAgent();
        String remotingUri = null;
        if (StringUtils.isBlank(host) && StringUtils.isBlank(port))
        {
            remotingUri = JmxAgent.DEFAULT_REMOTING_URI;
        }
        else if (StringUtils.isNotBlank(host))
        {
            // enable support for multi-NIC servers by configuring
            // a custom RMIClientSocketFactory
            Map props = agent.getConnectorServerProperties();
            Map mergedProps = new HashMap(props.size() + 1);
            mergedProps.putAll(props);
            RMIClientSocketFactory factory = new FixedHostRmiClientSocketFactory(host);
            mergedProps.put(RMIConnectorServer.RMI_CLIENT_SOCKET_FACTORY_ATTRIBUTE,
                    factory);
            agent.setConnectorServerProperties(mergedProps);
        }

        // if defaults haven't been used
        if (StringUtils.isBlank(remotingUri))
        {
            remotingUri =
                    MessageFormat.format("service:jmx:rmi:///jndi/rmi://{0}:{1}/server",
                            new Object[]{StringUtils.defaultString(host, DEFAULT_HOST),
                                    StringUtils.defaultString(port, DEFAULT_PORT)});
        }

        if (credentials != null && !credentials.isEmpty())
        {
            agent.setCredentials(credentials);
        }
        agent.setConnectorServerUrl(remotingUri);
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

    /**
     * Getter for property 'loadJdmkAgent'.
     *
     * @return Value for property 'loadJdmkAgent'.
     */
    public boolean isLoadJdmkAgent()
    {
        return loadJdmkAgent;
    }

    /**
     * Setter for property 'loadJdmkAgent'.
     *
     * @param loadJdmkAgent Value to set for property 'loadJdmkAgent'.
     */
    public void setLoadJdmkAgent(boolean loadJdmkAgent)
    {
        this.loadJdmkAgent = loadJdmkAgent;
    }

    /**
     * Getter for property 'loadMx4jAgent'.
     *
     * @return Value for property 'loadMx4jAgent'.
     */
    public boolean isLoadMx4jAgent()
    {
        return loadMx4jAgent;
    }

    /**
     * Setter for property 'loadMx4jAgent'.
     *
     * @param loadMx4jAgent Value to set for property 'loadMx4jAgent'.
     */
    public void setLoadMx4jAgent(boolean loadMx4jAgent)
    {
        this.loadMx4jAgent = loadMx4jAgent;
    }

    /**
     * Getter for property 'loadProfilerAgent'.
     * @return Value for property 'loadProfilerAgent'.
     */
    public boolean isLoadProfilerAgent()
    {
        return loadProfilerAgent;
    }

    /**
     * Setter for property 'loadProfilerAgent'.
     * @param loadProfilerAgent Value to set for property 'loadProfilerAgent'.
     */
    public void setLoadProfilerAgent(boolean loadProfilerAgent)
    {
        this.loadProfilerAgent = loadProfilerAgent;
    }

    /**
     * Getter for property 'port'.
     *
     * @return Value for property 'port'.
     */
    public String getPort()
    {
        return port;
    }

    /**
     * Setter for property 'port'.
     *
     * @param port Value to set for property 'port'.
     */
    public void setPort(final String port)
    {
        this.port = port;
    }

    /**
     * Getter for property 'host'.
     *
     * @return Value for property 'host'.
     */
    public String getHost()
    {
        return host;
    }

    /**
     * Setter for property 'host'.
     *
     * @param host Value to set for property 'host'.
     */
    public void setHost(final String host)
    {
        this.host = host;
    }


    /**
     * Setter for property 'credentials'.
     *
     * @param credentials Value to set for property 'credentials'.
     */
    public void setCredentials(final Map credentials)
    {
        this.credentials = credentials;
    }
}
