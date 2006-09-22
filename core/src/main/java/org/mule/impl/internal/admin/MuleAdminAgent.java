/*
 * $Id$
 * --------------------------------------------------------------------------------------
 * Copyright (c) MuleSource, Inc.  All rights reserved.  http://www.mulesource.com
 *
 * The software in this package is published under the terms of the MuleSource MPL
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.impl.internal.admin;

import org.apache.commons.lang.StringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.mule.MuleManager;
import org.mule.impl.AlreadyInitialisedException;
import org.mule.impl.endpoint.MuleEndpointURI;
import org.mule.providers.service.ConnectorFactory;
import org.mule.umo.UMODescriptor;
import org.mule.umo.UMOException;
import org.mule.umo.endpoint.UMOEndpointURI;
import org.mule.umo.lifecycle.InitialisationException;
import org.mule.umo.manager.UMOAgent;
import org.mule.umo.manager.UMOManager;
import org.mule.umo.provider.UMOConnector;

/**
 * <code>MuleAdminAgent</code> manages the server endpoint that receives Admin
 * and remote client requests
 * 
 * @author <a href="mailto:ross.mason@symphonysoft.com">Ross Mason</a>
 * @version $Revision$
 */
public class MuleAdminAgent implements UMOAgent
{
    public static final String DEFAULT_MANAGER_ENDPOINT = "_muleManagerEndpoint";

    public static final String AGENT_NAME = "Mule Admin";

    /**
     * logger used by this class
     */
    protected static transient Log logger = LogFactory.getLog(MuleAdminAgent.class);

    private String serverEndpoint;

    /**
     * Gets the name of this agent
     * 
     * @return the agent name
     */
    public String getName()
    {
        return AGENT_NAME;
    }

    /**
     * Sets the name of this agent
     * 
     * @param name the name of the agent
     */
    public void setName(String name)
    {
        // not allowed
    }

    /**
     * Should be a 1 line description of the agent
     * 
     * @return
     */
    public String getDescription()
    {
        return getName() + ": accepting connections on " + serverEndpoint;
    }

    public void start() throws UMOException
    {
        // nothing to do (yet?)
    }

    public void stop() throws UMOException
    {
        // nothing to do (yet?)
    }

    public void dispose()
    {
        // nothing to do (yet?)
    }

    public void registered()
    {
        // nothing to do (yet?)
    }

    public void unregistered()
    {
        // nothing to do (yet?)
    }

    public void initialise() throws InitialisationException
    {
        serverEndpoint = MuleManager.getConfiguration().getServerUrl();
        UMOManager manager = MuleManager.getInstance();

        try {
            if (StringUtils.isEmpty(serverEndpoint)) {
                // no serverUrl specified, warn a user
                logger.warn("No serverEndpointUrl specified, MuleAdminAgent will not start. E.g. use " +
                        "<mule-environment-properties serverUrl=\"tcp://example.com:60504\"/> ");

                // abort the agent registration process
                manager.unregisterAgent(this.getName());

                return;
            }

            // Check for override
            if (manager.getModel().isComponentRegistered(MuleManagerComponent.MANAGER_COMPONENT_NAME)) {
                logger.info("Mule manager component has already been initialised, ignoring server url");
            } else {
                if (manager.lookupConnector(DEFAULT_MANAGER_ENDPOINT) != null) {
                    throw new AlreadyInitialisedException("Server Components", this);
                }

                UMOEndpointURI endpointUri = new MuleEndpointURI(serverEndpoint);
                UMOConnector connector = ConnectorFactory.getOrCreateConnectorByProtocol(endpointUri);
                // If this connector has already been initialised i.e. it's a
                // pre-existing connector not not reinit
                if (manager.lookupConnector(connector.getName()) == null) {
                    connector.setName(DEFAULT_MANAGER_ENDPOINT);
                    connector.initialise();
                    manager.registerConnector(connector);
                }

                logger.info("Registering Admin listener on: " + serverEndpoint);
                UMODescriptor descriptor = MuleManagerComponent.getDescriptor(connector, endpointUri);
                manager.getModel().registerComponent(descriptor);
            }
        } catch (UMOException e) {
            throw new InitialisationException(e, this);
        }
    }

    public String toString()
    {
        return "MuleAdminAgent{" + "serverEndpoint='" + serverEndpoint + "'" + "}";
    }
}
