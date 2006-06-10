/*
 * $Id$
 * ------------------------------------------------------------------------------------------------------
 *
 * Copyright (c) SymphonySoft Limited. All rights reserved.
 * http://www.symphonysoft.com
 *
 * The software in this package is published under the terms of the BSD
 * style license a copy of which has been included with this distribution in
 * the LICENSE.txt file.
 */
package org.mule.management.agents;

import org.mule.MuleManager;
import org.mule.umo.UMOException;
import org.mule.umo.lifecycle.InitialisationException;
import org.mule.umo.lifecycle.RecoverableException;
import org.mule.umo.manager.UMOAgent;

import java.util.HashMap;
import java.util.Map;

/**
 * todo document
 *
 * @author <a href="mailto:ross.mason@symphonysoft.com">Ross Mason</a>
 * @version $Revision$
 */
public class DefaultJmxSupportAgent implements UMOAgent {

    public static final String DEFAULT_REMOTING_URI = "service:jmx:rmi:///jndi/rmi://localhost:1099/server";

    private String name = "Default Jmx";
    private boolean loadJmdkAgent = false;
    private boolean loadMX4JAgent = false;

    /**
     * Gets the name of this agent
     *
     * @return the agent name
     */
    public String getName() {
        return name;
    }

    /**
     * Sets the name of this agent
     *
     * @param name the name of the agent
     */
    public void setName(String name) {
        this.name = name;
    }

    /**
     * Should be a 1 line description of the agent
     *
     * @return
     */
    public String getDescription() {
        return "Default Jmx Agent Support";
    }

    public void registered() {
        // nothing to do
    }

    public void unregistered() {
        // nothing to do
    }

    public void start() throws UMOException {
        // nothing to do
    }

    public void stop() throws UMOException {
        // nothing to do
    }

    /**
     * A lifecycle method where implementor should free up any resources. If an
     * exception is thrown it should just be logged and processing should
     * continue. This method should not throw Runtime exceptions.
     */
    public void dispose() {
        // nothing to do
    }

    /**
     * Method used to perform any initialisation work. If a fatal error occurs
     * during initialisation an <code>InitialisationException</code> should be
     * thrown, causing the Mule instance to shutdown. If the error is
     * recoverable, say by retrying to connect, a
     * <code>RecoverableException</code> should be thrown. There is no
     * guarantee that by throwing a Recoverable exception that the Mule instance
     * will not shut down.
     *
     * @throws org.mule.umo.lifecycle.InitialisationException
     *          if a fatal error occurs causing the Mule
     *          instance to shutdown
     * @throws org.mule.umo.lifecycle.RecoverableException
     *          if an error occurs that can be recovered
     *          from
     */
    public void initialise() throws InitialisationException, RecoverableException {

        try {
            UMOAgent agent = createRmiAgent();
            if (!isAgentRegistered(agent)) {
                MuleManager.getInstance().registerAgent(agent);
            }
            agent = createJmxAgent();
            if (!isAgentRegistered(agent)) {
                MuleManager.getInstance().registerAgent(agent);
            }
            agent = createLog4jAgent();
            if (!isAgentRegistered(agent)) {
                MuleManager.getInstance().registerAgent(agent);
            }
            agent = createJmxNotificationAgent();
            if (!isAgentRegistered(agent)) {
                MuleManager.getInstance().registerAgent(agent);
            }
            if (loadJmdkAgent) {
                agent = createJdmkAgent();
                if (!isAgentRegistered(agent)) {
                    MuleManager.getInstance().registerAgent(agent);
                }
            }

            if (loadMX4JAgent) {
                agent = createMx4jAgent();
                if (!isAgentRegistered(agent)) {
                    MuleManager.getInstance().registerAgent(agent);
                }
            }

            //remove this agent once t has registered the other agents
            MuleManager.getInstance().unregisterAgent(name);
        } catch (UMOException e) {
            throw new InitialisationException(e, this);
        }
    }

    protected JmxAgent createJmxAgent() {
        JmxAgent agent = new JmxAgent();
        agent.setConnectorServerUrl(DEFAULT_REMOTING_URI);
        Map props = new HashMap();
        props.put("jmx.remote.jndi.rebind", "true");
        agent.setConnectorServerProperties(props);
        return agent;
    }

    protected Log4jAgent createLog4jAgent() {
        return new Log4jAgent();
    }

    protected RmiRegistryAgent createRmiAgent() {
        return new RmiRegistryAgent();
    }

    protected JmxServerNotificationAgent createJmxNotificationAgent() {
        return new JmxServerNotificationAgent();
    }

    protected Mx4jAgent createMx4jAgent() {
        return new Mx4jAgent();
    }

    protected JdmkAgent createJdmkAgent() {
        return new JdmkAgent();
    }

    protected boolean isAgentRegistered(UMOAgent agent) {
        return MuleManager.getInstance().lookupAgent(agent.getName()) != null;
    }

    public boolean isLoadJmdkAgent() {
        return loadJmdkAgent;
    }

    public void setLoadJmdkAgent(boolean loadJmdkAgent) {
        this.loadJmdkAgent = loadJmdkAgent;
    }

    public boolean isLoadMX4JAgent() {
        return loadMX4JAgent;
    }

    public void setLoadMX4JAgent(boolean loadMX4JAgent) {
        this.loadMX4JAgent = loadMX4JAgent;
    }
}
