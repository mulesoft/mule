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
 *
 */
package org.mule.impl.endpoint;

import org.mule.impl.ImmutableMuleEndpoint;
import org.mule.providers.service.ConnectorFactory;
import org.mule.umo.UMOException;
import org.mule.umo.UMOFilter;
import org.mule.umo.UMOTransactionConfig;
import org.mule.umo.endpoint.EndpointException;
import org.mule.umo.endpoint.UMOEndpoint;
import org.mule.umo.endpoint.UMOEndpointURI;
import org.mule.umo.endpoint.UMOImmutableEndpoint;
import org.mule.umo.provider.UMOConnector;
import org.mule.umo.security.UMOEndpointSecurityFilter;
import org.mule.umo.transformer.UMOTransformer;

import java.util.HashMap;
import java.util.Map;

/**
 * <code>MuleEndpoint</code> describes a Provider in the Mule Server. A endpoint is
 * a grouping of an endpoint, an endpointUri and a transformer.
 *
 * @author <a href="mailto:ross.mason@symphonysoft.com">Ross Mason</a>
 * @version $Revision$
 */
public class MuleEndpoint extends ImmutableMuleEndpoint implements UMOEndpoint
{
    /**
     * Default constructor
     * This is required right now for the Mule digester to set the properties through
     * the classes mutators
     */
    public MuleEndpoint()
    {
        super(null, null, null, null, ENDPOINT_TYPE_SENDER_AND_RECEIVER, 0, new HashMap());
    }

    public MuleEndpoint(String name, UMOEndpointURI endpointUri, UMOConnector connector,
                                  UMOTransformer transformer, String type, int createConnector,
                                  Map properties)
    {
        super(name, endpointUri, connector, transformer, type, createConnector, properties);
    }

    public MuleEndpoint(UMOImmutableEndpoint endpoint)
    {
        super(endpoint);
    }


    public MuleEndpoint(String endpoint, boolean receiver)
            throws UMOException
    {
        super(endpoint, (receiver ? UMOEndpoint.ENDPOINT_TYPE_RECEIVER : UMOEndpoint.ENDPOINT_TYPE_SENDER));
    }

    /* (non-Javadoc)
     * @see org.mule.umo.endpoint.UMOEndpoint#setEndpointURI(java.lang.String)
     */
    public void setEndpointURI(UMOEndpointURI endpointUri) throws EndpointException
    {
        if(connector!=null  && endpointUri != null && !
            endpointUri.getFullScheme().toLowerCase().startsWith(connector.getProtocol().toLowerCase())) {
                throw new IllegalArgumentException("Endpoint scheme must be compatible with the connector scheme. Connector is: "
                        + connector.getProtocol() + ", endpoint is: " + endpointUri);
            }
        this.endpointUri = endpointUri;
    }

    /* (non-Javadoc)
     * @see org.mule.umo.endpoint.UMOEndpoint#setType(String)
     */
    public void setType(String type)
    {
        this.type = type;
    }

    /* (non-Javadoc)
     * @see org.mule.umo.endpoint.UMOEndpoint#setConnector(org.mule.umo.provider.UMOConnector)
     */
    public void setConnector(UMOConnector connector)
    {
        if(connector!=null  && endpointUri != null && !
            endpointUri.getFullScheme().toLowerCase().startsWith(connector.getProtocol().toLowerCase())) {
                throw new IllegalArgumentException("Endpoint scheme must be compatible with the connector scheme. Connector is: "
                        + connector.getProtocol() + ", endpoint is: " + endpointUri);
            }
        this.connector = connector;        
    }

    /* (non-Javadoc)
     * @see org.mule.umo.endpoint.UMOEndpoint#setName(java.lang.String)
     */
    public void setName(String name)
    {
        this.name = name;
    }

    /* (non-Javadoc)
     * @see org.mule.umo.endpoint.UMOEndpoint#setTransformer(org.mule.umo.transformer.UMOTransformer)
     */
    public void setTransformer(UMOTransformer trans)
    {
        transformer = trans;
        if(transformer!=null) {
            transformer.setEndpoint(this);
        }
    }

    /* (non-Javadoc)
     * @see org.mule.umo.endpoint.UMOEndpoint#getPropertiesForURI(java.util.Map)
     */
    public void setProperties(Map props)
    {
        properties = props;
    }

    /* (non-Javadoc)
     * @see org.mule.umo.endpoint.UMOEndpoint#isReadOnly()
     */
    public boolean isReadOnly()
    {
        return false;
    }

    /**
     * Creates a read-only copy of the endpoint
     *
     * @return read-only copy
     */
    public UMOImmutableEndpoint getImmutableProvider()
    {
        UMOImmutableEndpoint result = new ImmutableMuleEndpoint(name, endpointUri, connector, transformer, type, createConnector, properties);

        return result;
    }


    /* (non-Javadoc)
     * @see org.mule.umo.endpoint.UMOEndpoint#setTransactionConfig(org.mule.umo.UMOTransactionConfig)
     */
    public void setTransactionConfig(UMOTransactionConfig config)
    {
        transactionConfig = config;
    }

    public void setFilter(UMOFilter filter) {
        this.filter = filter;
    }

    /**
     * If a filter is configured on this endpoint, this property will
     * determine if message that are not excepted by the filter are deleted
     *
     * @param delete if message should be deleted, false otherwise
     */
    public void setDeleteUnacceptedMessages(boolean delete)
    {
        deleteUnacceptedMessages = delete;
    }

    /**
     * Sets an UMOEndpointSecurityFilter for this endpoint.  If a filter is
     * set all traffice via this endpoint with be subject to authentication.
     * @param filter the UMOSecurityFilter responsible for authenticating
     * message flow via this endpoint.
     * @see org.mule.umo.security.UMOEndpointSecurityFilter
     */
    public void setSecurityFilter(UMOEndpointSecurityFilter filter)
    {
        securityFilter = filter;
        if(securityFilter!=null) {
            securityFilter.setEndpoint(this);
        }
    }

    /**
     * Determines if requests originating from this endpoint should be synchronous
     * i.e. execute in a single thread and possibly return an result.  This property
     * is only used when the endpoint is of type 'receiver'.
     *
     * @param synchronous whether requests on this endpoint should execute in a single
     *                    thread. This property is only used when the endpoint is of type 'receiver'
     */
    public void setSynchronous(boolean synchronous)
    {
        this.synchronous = new Boolean(synchronous);
    }

    public void setCreateConnector(int action) {
        createConnector = action;
    }

    public void setCreateConnectorAsString(String action) {
        if("ALWAYS_CREATE".equals(action)) {
            createConnector = ConnectorFactory.ALWAYS_CREATE_CONNECTOR;
        } else if("NEVER_CREATE".equals(action)) {
            createConnector = ConnectorFactory.NEVER_CREATE_CONNECTOR;
        } else {
            createConnector = ConnectorFactory.GET_OR_CREATE_CONNECTOR;
        }
    }
}