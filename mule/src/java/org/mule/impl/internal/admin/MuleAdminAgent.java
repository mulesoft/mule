/*
 * $Header$
 * $Revision$
 * $Date$
 * ------------------------------------------------------------------------------------------------------
 *
 * Copyright (c) SymphonySoft Limited. All rights reserved.
 * http://www.symphonysoft.com
 *
 * The software in this package is published under the terms of the BSD
 * style license a copy of which has been included with this distribution in
 * the LICENSE.txt file.
 */
package org.mule.impl.internal.admin;

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
    public static final String DEFAULT_MANAGER_PROVIDER = "_muleManagerProvider";

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
        return "Mule Admin";
    }

    /**
     * Sets the name of this agent
     * 
     * @param name the name of the agent
     */
    public void setName(String name)
    {
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
    }

    public void stop() throws UMOException
    {
    }

    public void dispose()
    {
    }

    public void registered()
    {
    }

    public void unregistered()
    {
    }

    public void initialise() throws InitialisationException
    {
        serverEndpoint = MuleManager.getConfiguration().getServerUrl();
        UMOManager manager = MuleManager.getInstance();

        try {

            if ("".equals(serverEndpoint)) {
                // no serverUrl specified, warn a user
                logger.warn("No serverEndpointUrl specified, MuleAdminAgent will not start. E.g. use " +
                        "<mule-environment-properties serverUrl=\"tcp://example.com:60504\"/> ");

                // abort the agent registration process
                manager.removeAgent(this.getName());

                return;
            }

            // Check for override
            if (manager.getModel().isComponentRegistered(MuleManagerComponent.MANAGER_COMPONENT_NAME)) {
                logger.info("Mule manager component has already been initialised, ignoring server url");
            } else {
                if (manager.lookupConnector(DEFAULT_MANAGER_PROVIDER) != null) {
                    throw new AlreadyInitialisedException("Server Components", this);
                }

                UMOEndpointURI endpointUri = new MuleEndpointURI(serverEndpoint);
                UMOConnector connector = ConnectorFactory.getOrCreateConnectorByProtocol(endpointUri);
                // If this connector has already been initialised i.e. it's a
                // pre-existing connector not not reinit
                if (manager.lookupConnector(connector.getName()) == null) {
                    connector.setName(DEFAULT_MANAGER_PROVIDER);
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
