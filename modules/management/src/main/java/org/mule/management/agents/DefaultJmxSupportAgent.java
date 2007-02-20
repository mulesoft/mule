/*
 * $Id$
 * --------------------------------------------------------------------------------------
 * Copyright (c) MuleSource, Inc.  All rights reserved.  http://www.mulesource.com
 *
 * The software in this package is published under the terms of the MuleSource MPL
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.management.agents;

import org.mule.impl.AbstractAgent;
import org.mule.umo.UMOException;
import org.mule.umo.UMOManagementContext;
import org.mule.umo.lifecycle.InitialisationException;
import org.mule.umo.manager.UMOAgent;

/**
 * TODO document.
 */
public class DefaultJmxSupportAgent extends AbstractAgent
{
    private boolean loadJdmkAgent = false;
    private boolean loadMx4jAgent = false;


    public DefaultJmxSupportAgent()
    {
        super("Default Jmx Config");
    }

    /**
     * Should be a 1 line description of the agent
     * 
     * @return
     */
    public String getDescription()
    {
        return "Default Jmx Agent Support";
    }

    public void registered()
    {
        // nothing to do
    }

    public void unregistered()
    {
        // nothing to do
    }

    public void start() throws UMOException
    {
        // nothing to do
    }

    public void stop() throws UMOException
    {
        // nothing to do
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
     * @throws org.mule.umo.lifecycle.InitialisationException if a fatal error occurs
     *             causing the Mule instance to shutdown
     * @throws org.mule.umo.lifecycle.RecoverableException if an error occurs that
     *             can be recovered from
     * @param managementContext
     */
    public void doInitialise(UMOManagementContext managementContext) throws InitialisationException {

        try
        {
            UMOAgent agent = createRmiAgent();
            if (!isAgentRegistered(agent))
            {
                managementContext.getRegistry().registerAgent(agent);
            }
            agent = createJmxAgent();
            if (!isAgentRegistered(agent))
            {
                managementContext.getRegistry().registerAgent(agent);
            }
            agent = createLog4jAgent();
            if (!isAgentRegistered(agent))
            {
                managementContext.getRegistry().registerAgent(agent);
            }
            agent = createJmxNotificationAgent();
            if (!isAgentRegistered(agent))
            {
                managementContext.getRegistry().registerAgent(agent);
            }
            if (loadJdmkAgent)
            {
                agent = createJdmkAgent();
                if (!isAgentRegistered(agent))
                {
                    managementContext.getRegistry().registerAgent(agent);
                }
            }

            if (loadMx4jAgent)
            {
                agent = createMx4jAgent();
                if (!isAgentRegistered(agent))
                {
                    managementContext.getRegistry().registerAgent(agent);
                }
            }

            // remove this agent once t has registered the other agents
            managementContext.getRegistry().unregisterAgent(name);
        }
        catch (UMOException e)
        {
            throw new InitialisationException(e, this);
        }
    }

    protected JmxAgent createJmxAgent()
    {
        JmxAgent agent = new JmxAgent();
        agent.setConnectorServerUrl(JmxAgent.DEFAULT_REMOTING_URI);
        return agent;
    }

    protected Log4jAgent createLog4jAgent()
    {
        return new Log4jAgent();
    }

    protected RmiRegistryAgent createRmiAgent()
    {
        return new RmiRegistryAgent();
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

    protected boolean isAgentRegistered(UMOAgent agent)
    {
        return managementContext.getRegistry().lookupAgent(agent.getName()) != null;
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
}
