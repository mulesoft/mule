/*
 * $Id$
 * --------------------------------------------------------------------------------------
 * Copyright (c) MuleSource, Inc.  All rights reserved.  http://www.mulesource.com
 *
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.agent;

import org.mule.AbstractAgent;
import org.mule.RegistryContext;
import org.mule.api.MuleException;
import org.mule.api.component.Component;
import org.mule.api.endpoint.EndpointBuilder;
import org.mule.api.endpoint.EndpointURI;
import org.mule.api.lifecycle.InitialisationException;
import org.mule.api.transformer.wire.WireFormat;
import org.mule.api.transport.Connector;
import org.mule.endpoint.EndpointURIEndpointBuilder;
import org.mule.endpoint.MuleEndpointURI;
import org.mule.lifecycle.AlreadyInitialisedException;
import org.mule.transformer.wire.SerializationWireFormat;
import org.mule.transport.service.TransportFactory;
import org.mule.util.StringUtils;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

/**
 * <code>MuleAdminAgent</code> manages the server endpoint that receives Admin and
 * remote client requests
 */
public class MuleAdminAgent extends AbstractAgent
{
    public static final String DEFAULT_MANAGER_ENDPOINT = "_muleManagerEndpoint";

    public static final String AGENT_NAME = "MuleAdmin";

    /**
     * logger used by this class
     */
    protected static final Log logger = LogFactory.getLog(MuleAdminAgent.class);

    private WireFormat wireFormat;

    private String serverUri;


    public MuleAdminAgent()
    {
        super(AGENT_NAME);
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

    public void start() throws MuleException
    {
        // nothing to do (yet?)
    }

    public void stop() throws MuleException
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

        try
        {
            if (StringUtils.isEmpty(serverUri))
            {
                // no serverUrl specified, warn a user
                logger.warn("No serverUriUrl specified, MuleAdminAgent will not start. E.g. use "
                            + "<mule:admin-agent serverUri=\"tcp://example.com:60504\"/> ");

                // abort the agent registration process
               muleContext.getRegistry().unregisterAgent(this.getName());

                return;
            }

            // Check for override
            if (muleContext.getRegistry().lookupComponent(MuleManagerComponent.MANAGER_COMPONENT_NAME) != null)
            {
                logger.info("Mule manager component has already been initialised, ignoring server url");
            }
            else
            {
                if (muleContext.getRegistry().lookupConnector(DEFAULT_MANAGER_ENDPOINT) != null)
                {
                    throw new AlreadyInitialisedException("Server Components", this);
                }

                // Check to see if we have an endpoint identifier
                EndpointBuilder endpointBuilder = muleContext.getRegistry().lookupEndpointBuilder(serverUri);
                // Check to see if we have an endpoint identifier
                if (endpointBuilder == null)
                {
                    EndpointURI uri = new MuleEndpointURI(serverUri);
                    endpointBuilder = new EndpointURIEndpointBuilder(uri, muleContext);
                    endpointBuilder.setSynchronous(true);

                    // TODO DF: Doesn't the EndpointBuilder do this?
                    Connector connector = TransportFactory.getOrCreateConnectorByProtocol(uri, muleContext);
                    // If this connector has already been initialised i.e. it's a
                    // pre-existing connector don't reinit
                    if (muleContext.getRegistry().lookupConnector(connector.getName()) == null)
                    {
                        connector.setName(DEFAULT_MANAGER_ENDPOINT);
                        //connector.initialise();
                        muleContext.getRegistry().registerConnector(connector);
                    }
                    endpointBuilder.setConnector(connector);
                }
                logger.info("Registering Admin listener on: " + serverUri);
                Component component = MuleManagerComponent.getComponent(endpointBuilder, wireFormat,
                    RegistryContext.getConfiguration().getDefaultEncoding(), RegistryContext.getConfiguration()
                        .getDefaultSynchronousEventTimeout(), muleContext);
                muleContext.getRegistry().registerComponent(component);
            }
        }
        catch (MuleException e)
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
