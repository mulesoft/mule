/*
 * $Id$
 * --------------------------------------------------------------------------------------
 * Copyright (c) MuleSource, Inc.  All rights reserved.  http://www.mulesource.com
 *
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.impl.endpoint;

import org.mule.config.i18n.CoreMessages;
import org.mule.impl.ImmutableMuleEndpoint;
import org.mule.impl.ManagementContextAware;
import org.mule.providers.ConnectionStrategy;
import org.mule.providers.service.TransportFactory;
import org.mule.umo.UMOException;
import org.mule.umo.UMOFilter;
import org.mule.umo.UMOManagementContext;
import org.mule.umo.UMOTransactionConfig;
import org.mule.umo.endpoint.EndpointException;
import org.mule.umo.endpoint.UMOEndpoint;
import org.mule.umo.endpoint.UMOEndpointURI;
import org.mule.umo.endpoint.UMOImmutableEndpoint;
import org.mule.umo.lifecycle.InitialisationException;
import org.mule.umo.provider.UMOConnector;
import org.mule.umo.security.UMOEndpointSecurityFilter;
import org.mule.umo.transformer.UMOTransformer;

import java.util.Map;

/**
 * <code>MuleEndpoint</code> describes a Provider in the Mule Server. A endpoint is
 * a grouping of an endpoint, an endpointUri and a transformer.
 */
public class MuleEndpoint extends ImmutableMuleEndpoint implements UMOEndpoint, ManagementContextAware
{
    /**
     * Serial version
     */
    private static final long serialVersionUID = 2028442057178326047L;

    public static final String ALWAYS_CREATE_STRING = "ALWAYS_CREATE";
    public static final String NEVER_CREATE_STRING = "NEVER_CREATE";

    /**
     * Default constructor This is required right now for the Mule digester to set
     * the properties through the classes mutators
     */
    public MuleEndpoint()
    {
        super(null, null, null, null, ENDPOINT_TYPE_SENDER_AND_RECEIVER, 0, null, null);
    }

    public MuleEndpoint(String name,
                        UMOEndpointURI endpointUri,
                        UMOConnector connector,
                        UMOTransformer transformer,
                        String type,
                        int createConnector,
                        String endpointEncoding,
                        Map props)
    {
        super(name, endpointUri, connector, transformer, type, createConnector, endpointEncoding, props);
    }

    public MuleEndpoint(UMOImmutableEndpoint endpoint) throws UMOException
    {
        super(endpoint);
        this.setManagementContext(endpoint.getManagementContext());
    }

    public MuleEndpoint(String uri, boolean receiver) throws UMOException
    {
        super(uri, receiver);
    }

    public void setEndpointURI(UMOEndpointURI endpointUri) throws EndpointException
    {
        if (connector != null && endpointUri != null
            && !connector.supportsProtocol(endpointUri.getFullScheme()))
        {
            throw new IllegalArgumentException(
                CoreMessages.connectorSchemeIncompatibleWithEndpointScheme(connector.getProtocol(),
                endpointUri).getMessage());
        }
        if(endpointUri==null)
        {
            throw new NullPointerException(CoreMessages.objectIsNull("endpointURI").getMessage());
        }

        this.endpointUri = endpointUri;
        if (endpointUri != null)
        {
            properties.putAll(endpointUri.getParams());
        }
        if(initialised.get())
        {
            try
            {
                endpointUri.initialise();
            }
            catch (InitialisationException e)
            {
                throw new EndpointException(e);
            }
        }
    }

    public void setEncoding(String endpointEncoding)
    {
        this.endpointEncoding = endpointEncoding;
    }

    public void setType(String type)
    {
        this.type = type;
    }

    public void setConnector(UMOConnector connector)
    {
        if (connector != null && endpointUri != null
            && !connector.supportsProtocol(endpointUri.getFullScheme()))
        {
            throw new IllegalArgumentException(
                CoreMessages.connectorSchemeIncompatibleWithEndpointScheme(connector.getProtocol(),
                endpointUri).getMessage());
        }
        this.connector = connector;
    }

    public void setName(String name)
    {
        this.name = name;
    }

    public void setTransformer(UMOTransformer trans)
    {
        transformer.set(trans);
        updateTransformerEndpoint();
    }

    public void setProperties(Map props)
    {
        properties.clear();
        properties.putAll(props);
    }

    public boolean isReadOnly()
    {
        return false;
    }

    public void setTransactionConfig(UMOTransactionConfig config)
    {
        transactionConfig = config;
    }

    public void setFilter(UMOFilter filter)
    {
        this.filter = filter;
    }

    /**
     * If a filter is configured on this endpoint, this property will determine if
     * message that are not excepted by the filter are deleted
     * 
     * @param delete if message should be deleted, false otherwise
     */
    public void setDeleteUnacceptedMessages(boolean delete)
    {
        deleteUnacceptedMessages = delete;
    }

    /**
     * Sets an UMOEndpointSecurityFilter for this endpoint. If a filter is set all
     * traffice via this endpoint with be subject to authentication.
     * 
     * @param filter the UMOSecurityFilter responsible for authenticating message
     *            flow via this endpoint.
     * @see org.mule.umo.security.UMOEndpointSecurityFilter
     */
    public void setSecurityFilter(UMOEndpointSecurityFilter filter)
    {
        securityFilter = filter;
        if (securityFilter != null)
        {
            securityFilter.setEndpoint(this);
        }
    }

    /**
     * Determines if requests originating from this endpoint should be synchronous
     * i.e. execute in a single thread and possibly return an result. This property
     * is only used when the endpoint is of type 'receiver'.
     * 
     * @param synchronous whether requests on this endpoint should execute in a
     *            single thread. This property is only used when the endpoint is of
     *            type 'receiver'
     */
    public void setSynchronous(boolean synchronous)
    {
        this.synchronous = Boolean.valueOf(synchronous);
    }

    public void setCreateConnector(int action)
    {
        createConnector = action;
    }

    public void setCreateConnectorAsString(String action)
    {
        if (ALWAYS_CREATE_STRING.equals(action))
        {
            createConnector = TransportFactory.ALWAYS_CREATE_CONNECTOR;
        }
        else if (NEVER_CREATE_STRING.equals(action))
        {
            createConnector = TransportFactory.NEVER_CREATE_CONNECTOR;
        }
        else
        {
            createConnector = TransportFactory.GET_OR_CREATE_CONNECTOR;
        }
    }

    /**
     * For certain providers that support the notion of a backchannel such as sockets
     * (outputStream) or Jms (ReplyTo) Mule can automatically wait for a response
     * from a backchannel when dispatching over these protocols. This is different
     * for synchronous as synchronous behavior only applies to in
     * 
     * @param value whether the endpoint should perfrom sync receives
     */
    public void setRemoteSync(boolean value)
    {
        this.remoteSync = Boolean.valueOf(value);
        if (value)
        {
            this.synchronous = Boolean.TRUE;
        }
    }

    /**
     * The timeout value for remoteSync invocations
     * 
     * @param timeout the timeout in milliseconds
     */
    public void setRemoteSyncTimeout(int timeout)
    {
        this.remoteSyncTimeout = new Integer(timeout);
    }

    /**
     * Sets the state the endpoint will be loaded in. The States are 'stopped' and
     * 'started' (default)
     * 
     * @param state
     */
    public void setInitialState(String state)
    {
        this.initialState = state;
    }

    public void setResponseTransformer(UMOTransformer trans)
    {
        responseTransformer = trans;
    }

    /**
     * Determines whether the endpoint should deal with requests as streams
     * 
     * @param stream true if the request should be streamed
     */
    public void setStreaming(boolean stream)
    {
        this.streaming = stream;
    }

    /**
     * Sets a property on the endpoint
     * 
     * @param key the property key
     * @param value the value of the property
     */
    public void setProperty(String key, Object value)
    {
        properties.put(key, value);
    }


    /**
     * Setter for property 'connectionStrategy'.
     *
     * @param connectionStrategy Value to set for property 'connectionStrategy'.
     */
    public void setConnectionStrategy(ConnectionStrategy connectionStrategy)
    {
        this.connectionStrategy = connectionStrategy;
    }


    public void setManagementContext(UMOManagementContext context)
    {
        this.managementContext = context;
    }
}
