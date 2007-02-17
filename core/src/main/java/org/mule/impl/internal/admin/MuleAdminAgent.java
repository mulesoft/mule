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

import org.mule.MuleManager;
import org.mule.impl.AlreadyInitialisedException;
import org.mule.impl.endpoint.MuleEndpointURI;
import org.mule.providers.service.TransportFactory;
import org.mule.registry.UMORegistry;
import org.mule.transformers.wire.SerializationWireFormat;
import org.mule.transformers.wire.WireFormat;
import org.mule.umo.UMODescriptor;
import org.mule.umo.UMOException;
import org.mule.umo.endpoint.UMOEndpointURI;
import org.mule.umo.lifecycle.InitialisationException;
import org.mule.umo.manager.UMOAgent;
import org.mule.umo.provider.UMOConnector;

import org.apache.commons.lang.StringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

/**
 * <code>MuleAdminAgent</code> manages the server endpoint that receives Admin and
 * remote client requests
 */
public class MuleAdminAgent implements UMOAgent
{
    public static final String DEFAULT_MANAGER_ENDPOINT = "_muleManagerEndpoint";

    public static final String AGENT_NAME = "Mule Admin";

    /**
     * logger used by this class
     */
    protected static final Log logger = LogFactory.getLog(MuleAdminAgent.class);

    private WireFormat wireFormat;

    private String serverUri;

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
        return getName() + ": accepting connections on " + serverUri;
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
        if (wireFormat == null)
        {
            wireFormat = new SerializationWireFormat();
        }
        //TODO RM* serverUri = MuleManager.getConfiguration().getServerUrl();
        UMORegistry registry = MuleManager.getRegistry();

        try
        {
            if (StringUtils.isEmpty(serverUri))
            {
                // no serverUrl specified, warn a user
                logger.warn("No serverUriUrl specified, MuleAdminAgent will not start. E.g. use "
                            + "<mule:admin-agent serverUri=\"tcp://example.com:60504\"/> ");

                // abort the agent registration process
                registry.unregisterAgent(this.getName());

                return;
            }

            // Check for override
            if (MuleManager.getRegistry().lookupComponent(MuleManagerComponent.MANAGER_COMPONENT_NAME) != null)
            {
                logger.info("Mule manager component has already been initialised, ignoring server url");
            }
            else
            {
                if (registry.lookupConnector(DEFAULT_MANAGER_ENDPOINT) != null)
                {
                    throw new AlreadyInitialisedException("Server Components", this);
                }

                // Check to see if we have an endpoint identifier
                serverUri = registry.lookupEndpointIdentifier(serverUri,
                    serverUri);
                UMOEndpointURI endpointUri = new MuleEndpointURI(serverUri);
                UMOConnector connector = TransportFactory.getOrCreateConnectorByProtocol(endpointUri);
                // If this connector has already been initialised i.e. it's a
                // pre-existing connector not not reinit
                if (registry.lookupConnector(connector.getName()) == null)
                {
                    connector.setName(DEFAULT_MANAGER_ENDPOINT);
                    connector.initialise();
                    registry.registerConnector(connector);
                }

                logger.info("Registering Admin listener on: " + serverUri);
                UMODescriptor descriptor = MuleManagerComponent.getDescriptor(connector, endpointUri,
                    wireFormat);
                MuleManager.getRegistry().registerSystemComponent(descriptor);
            }
        }
        catch (UMOException e)
        {
            throw new InitialisationException(e, this);
        }
    }

    public String toString()
    {
        return "MuleAdminAgent{" + "serverUri='" + serverUri + "'" + "}";
    }

    public WireFormat getWireFormat()
    {
        return wireFormat;
    }

    public void setWireFormat(WireFormat wireFormat)
    {
        this.wireFormat = wireFormat;
    }


    public String getServerUri()
    {
        return serverUri;
    }

    public void setServerUri(String serverUri)
    {
        this.serverUri = serverUri;
    }
}
