/* 
 * $Header$
 * $Revision$
 * $Date$
 * ------------------------------------------------------------------------------------------------------
 * 
 * Copyright (c) Cubis Limited. All rights reserved.
 * http://www.cubis.co.uk 
 * 
 * The software in this package is published under the terms of the BSD
 * style license a copy of which has been included with this distribution in
 * the LICENSE.txt file. 
 *
 */

package org.mule.impl;


import EDU.oswego.cs.dl.util.concurrent.SynchronizedBoolean;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.mule.InitialisationException;
import org.mule.MuleException;
import org.mule.MuleManager;
import org.mule.impl.endpoint.MuleEndpoint;
import org.mule.impl.endpoint.MuleEndpointURI;
import org.mule.providers.AbstractConnector;
import org.mule.providers.service.ConnectorFactory;
import org.mule.umo.UMOException;
import org.mule.umo.UMOFilter;
import org.mule.umo.UMOTransactionConfig;
import org.mule.umo.endpoint.UMOEndpoint;
import org.mule.umo.endpoint.UMOEndpointURI;
import org.mule.umo.endpoint.UMOImmutableEndpoint;
import org.mule.umo.provider.UMOConnector;
import org.mule.umo.security.UMOEndpointSecurityFilter;
import org.mule.umo.transformer.UMOTransformer;
import org.mule.util.MuleObjectHelper;

import java.util.HashMap;
import java.util.Map;

/**
 * <code>ImmutableMuleEndpoint</code> describes a Provider in the Mule Server. A endpoint is
 * a grouping of an endpoint, an endpointUri and a transformer.
 *
 * @author <a href="mailto:ross.mason@cubis.co.uk">Ross Mason</a>
 * @version $Revision$
 */
public class ImmutableMuleEndpoint implements UMOImmutableEndpoint
{
    /**
     * logger used by this class
     */
    protected static transient Log logger = LogFactory.getLog(ImmutableMuleEndpoint.class);

    /**
     * The endpoint used to communicate with the external system
     */
    protected UMOConnector connector = null;

    /**
     * The endpointUri on which to send or receive information
     */
    protected UMOEndpointURI endpointUri = null;

    /**
     * The transformer used to transform the incoming or outgoing data
     */
    protected UMOTransformer transformer = null;

    /**
     * The name for the endpoint
     */
    protected String name = null;

    /**
     * Determines whether the endpoint is a receiver or sender or both
     */
    protected String type = ENDPOINT_TYPE_SENDER_AND_RECEIVER;

    /**
     * Any additional properties for the endpoint
     */
    protected Map properties = new HashMap();

    /**
     * The transaction configuration for this endpoint
     */
    protected UMOTransactionConfig transactionConfig = null;

    /**
     * event filter for this endpoint
     */
    protected UMOFilter filter = null;

    /**
     * determines whether unaccepted filtered events should be
     * removed from the source.  If they are not removed its up to
     * the Message receiver to handle recieving the same message again
     */
    protected boolean deleteUnacceptedMessages = false;

    /**
     * has this endpoint been initialised
     */
    protected SynchronizedBoolean initialised = new SynchronizedBoolean(false);

    /**
     * The security filter to apply to this endpoint
     */
    protected UMOEndpointSecurityFilter securityFilter = null;

    /**
     * whether events received by this endpoint should execute in a single
     * thread
     */
    protected Boolean synchronous = null;
    /**
     * Default ctor
     */
    private ImmutableMuleEndpoint() {
    }

    public ImmutableMuleEndpoint(String endpoint, String type) throws UMOException
    {
        UMOEndpoint p = getOrCreateEndpointForUri(new MuleEndpointURI(endpoint), type);
        initFromDescriptor(p);
    }
    /**
     * Default constructor
     */
    public ImmutableMuleEndpoint(String name, UMOEndpointURI endpointUri, UMOConnector connector,
                                           UMOTransformer transformer, String type,
                                           Map properties)
    {
        this.name = name;
        this.endpointUri = endpointUri;
        this.connector = connector;
        this.transformer = transformer;
        if(transformer != null) {
            getTransformer().setEndpoint(this);
        }

        this.type = type;
        this.properties = properties;

        //Create a default transaction config
        transactionConfig = new MuleTransactionConfig();
        if(properties!=null && endpointUri!=null) {
            properties.putAll(endpointUri.getParams());
        }
    }

    public ImmutableMuleEndpoint(UMOImmutableEndpoint source)
    {
        this();
        initFromDescriptor(source);
    }

    protected void initFromDescriptor(UMOImmutableEndpoint source) {
        if(this.name==null) this.name = source.getName();
        if(this.endpointUri==null) this.endpointUri = source.getEndpointURI();
        if(this.connector==null) this.connector = source.getConnector();
        if(this.transformer==null)this.transformer = source.getTransformer();
        if(transformer != null) {
            getTransformer().setEndpoint(this);
        }

        this.type = source.getType();
        this.properties = source.getProperties();
        this.transactionConfig = source.getTransactionConfig();
        if(properties!=null && endpointUri!=null) {
            properties.putAll(endpointUri.getParams());
        }

    }

    /* (non-Javadoc)
     * @see org.mule.umo.endpoint.UMOEndpoint#getEndpointURI()
     */
    public UMOEndpointURI getEndpointURI()
    {
        return endpointUri;
    }

    /* (non-Javadoc)
     * @see org.mule.umo.endpoint.UMOImmutableEndpoint#getType()
     */
    public String getType()
    {
        return type;
    }

    /* (non-Javadoc)
     * @see org.mule.umo.endpoint.UMOImmutableEndpoint#getConnectorName()
     */
    public UMOConnector getConnector()
    {
        return connector;
    }


    /* (non-Javadoc)
     * @see org.mule.umo.endpoint.UMOImmutableEndpoint#getName()
     */
    public String getName()
    {
        return name;
    }


    /* (non-Javadoc)
     * @see org.mule.umo.endpoint.UMOEndpoint#getTransformer()
     */
    public UMOTransformer getTransformer()
    {
        return transformer;
    }

    /* (non-Javadoc)
     * @see org.mule.umo.endpoint.UMOImmutableEndpoint#getParams()
     */
    public Map getProperties()
    {
        return properties;
    }

    /* (non-Javadoc)
     * @see java.lang.Object#clone()
     */
    public Object clone()// throws CloneNotSupportedException
    {
        UMOEndpoint clone = new MuleEndpoint(name, endpointUri, connector, transformer, type, (properties==null ? null : new HashMap(properties)));
        clone.setTransactionConfig(transactionConfig);
        clone.setFilter(filter);
        return clone;

    }

    /* (non-Javadoc)
     * @see org.mule.umo.endpoint.UMOImmutableEndpoint#isReadOnly()
     */
    public boolean isReadOnly()
    {
        return true;
    }

        public String toString()
        {
            StringBuffer buf = new StringBuffer();
            buf.append("Provider: ").append(name);
            buf.append(", endpointUri=").append(endpointUri);
            if(connector != null) {
                buf.append(", connector=").append(connector.getName()).append(" (").append(connector.getProtocol()).append(")");
            } else {
                 buf.append(", connector=not set");
            }
            buf.append(", type=").append(type);
            return buf.toString();
        }

    /* (non-Javadoc)
     * @see org.mule.umo.endpoint.UMOImmutableEndpoint#getProtocol()
     */
    public String getProtocol()
    {
        return connector.getProtocol();
    }

    /* (non-Javadoc)
     * @see org.mule.umo.endpoint.UMOImmutableEndpoint#canReceive()
     */
    public boolean canReceive()
    {
        return (getType().equals(ENDPOINT_TYPE_RECEIVER) || getType().equals(ENDPOINT_TYPE_SENDER_AND_RECEIVER));
    }

    /* (non-Javadoc)
     * @see org.mule.umo.endpoint.UMOImmutableEndpoint#canSend()
     */
    public boolean canSend()
    {
        return (getType().equals(ENDPOINT_TYPE_SENDER) || getType().equals(ENDPOINT_TYPE_SENDER_AND_RECEIVER));
    }

    /* (non-Javadoc)
     * @see org.mule.umo.endpoint.UMOEndpoint#getTransactionConfig()
     */
    public UMOTransactionConfig getTransactionConfig()
    {
        return transactionConfig;
    }

    public boolean equals(Object o)
    {
        if (this == o) return true;
        if (!(o instanceof ImmutableMuleEndpoint)) return false;

        final ImmutableMuleEndpoint immutableMuleProviderDescriptor = (ImmutableMuleEndpoint) o;

        if (!connector.getName().equals(immutableMuleProviderDescriptor.connector.getName())) return false;
        if (endpointUri != null && immutableMuleProviderDescriptor.endpointUri != null ? !endpointUri.getAddress().equals(immutableMuleProviderDescriptor.endpointUri.getAddress()) : immutableMuleProviderDescriptor.endpointUri != null) return false;
        if (!name.equals(immutableMuleProviderDescriptor.name)) return false;
        if (transformer != null ? !transformer.equals(immutableMuleProviderDescriptor.transformer) : immutableMuleProviderDescriptor.transformer != null) return false;
        if (!type.equals(immutableMuleProviderDescriptor.type)) return false;

        return true;
    }

    public int hashCode()
    {
        int result;
        result = (connector != null ? connector.hashCode() : 0);
        result = 29 * result + (endpointUri != null ? endpointUri.hashCode() : 0);
        result = 29 * result + (transformer != null ? transformer.hashCode() : 0);
        result = 29 * result + (name != null ? name.hashCode() : 0);
        result = 29 * result + (type != null ? type.hashCode() : 0);
        return result;
    }

    public UMOFilter getFilter()
    {
        return filter;
    }

    public static UMOEndpoint createEndpointFromUri(UMOEndpointURI uri, String type) throws UMOException
    {
        UMOEndpoint endpoint = ConnectorFactory.createEndpoint(uri, type);
        if(uri.getEndpointName()!=null) endpoint.setName(uri.getEndpointName());
        return endpoint;
    }

     public static UMOEndpoint getEndpointFromUri(String uri)
    {
        UMOEndpoint endpoint = null;
        if(uri!=null) {
            String endpointString = MuleManager.getInstance().lookupEndpointIdentifier(uri, uri);
            endpoint = MuleManager.getInstance().lookupEndpoint(endpointString);
        }
        return endpoint;
    }

    public static UMOEndpoint getEndpointFromUri(UMOEndpointURI uri) throws UMOException
    {
        UMOEndpoint endpoint = null;
        if(uri.getEndpointName()!=null) {
            String endpointString = MuleManager.getInstance().lookupEndpointIdentifier(uri.getEndpointName(), uri.getEndpointName());
            endpoint = MuleManager.getInstance().lookupEndpoint(endpointString);
            if(endpoint!=null) {
                if(uri.getAddress()!=null && uri.getAddress().length() > 0)
                {
                    endpoint.setEndpointURI(uri);
                }
            }

        }
        return endpoint;
    }

    public static UMOEndpoint getOrCreateEndpointForUri(String uriIdentifier, String type) throws UMOException
    {
        UMOEndpoint endpoint = getEndpointFromUri(uriIdentifier);
        if(endpoint==null) {
            endpoint = createEndpointFromUri(new MuleEndpointURI(uriIdentifier), type);
        } else {

            if(endpoint.getType().equals(UMOEndpoint.ENDPOINT_TYPE_SENDER_AND_RECEIVER))
            {
                endpoint.setType(type);
            } else if(!endpoint.getType().equals(type)) {
                throw new IllegalArgumentException("Endpoint matching: " + uriIdentifier + " is not of type: " + type +
                        ". It is of type: " + endpoint.getType());

            }
        }
        return endpoint;
    }

    public static UMOEndpoint getOrCreateEndpointForUri(UMOEndpointURI uri, String type) throws UMOException
    {
        UMOEndpoint endpoint = getEndpointFromUri(uri);
        if(endpoint==null) {
            endpoint = createEndpointFromUri(uri, type);
        }
        return endpoint;
    }

    public boolean isDeleteUnacceptedMessages()
    {
        return deleteUnacceptedMessages;
    }

    public void initialise() throws InitialisationException
    {
        if(connector==null) {
            if(endpointUri.getConnectorName()!=null) {
                connector = MuleManager.getInstance().lookupConnector(endpointUri.getConnectorName());
                if(connector==null) throw new IllegalArgumentException("Connector not found: " + endpointUri.getConnectorName());
            } else {
                try
                {
                    connector = MuleObjectHelper.getOrCreateConnectorByProtocol(endpointUri);

                } catch (UMOException e)
                {
                    throw new InitialisationException("Failed to create connector from Uri: " + e.getMessage(), e);
                }
            }

            if(endpointUri.getEndpointName() != null && name==null) {
                name = endpointUri.getEndpointName();
            }
        }
        if(name==null) {
            name = "_" + endpointUri.getScheme() + "Endpoint#" + hashCode();
        }

        if(endpointUri.getTransformers()!=null) {
            try
            {
                transformer = MuleObjectHelper.getTransformer(endpointUri.getTransformers(), ",");
            } catch (MuleException e)
            {
                throw new InitialisationException(e.getMessage(), e);
            }
        }

        if(transformer==null) {
            if(connector instanceof AbstractConnector) {
                if(UMOEndpoint.ENDPOINT_TYPE_RECEIVER.equals(type)) {
                    transformer = ((AbstractConnector)connector).getDefaultInboundTransformer();
                } else {
                    transformer = ((AbstractConnector)connector).getDefaultOutboundTransformer();
                }
            }
        }
        if(transformer!=null) {
            transformer.setEndpoint(this);
        }

        if(securityFilter!=null) {
            securityFilter.setEndpoint(this);
            securityFilter.initialise();
        }

        String sync = endpointUri.getParams().getProperty("synchronous", null);
        if(sync!=null) {
            synchronous = Boolean.valueOf(sync);
        }
    }

    /**
     * Returns an UMOEndpointSecurityFilter for this endpoint.  If one is
     * not set, there will be no authentication
     * on events sent via this endpoint
     *
     * @return UMOEndpointSecurityFilter responsible for authenticating
     *         message flow via this endpoint.
     * @see org.mule.umo.security.UMOEndpointSecurityFilter
     */
    public UMOEndpointSecurityFilter getSecurityFilter()
    {
        return securityFilter;
    }

    /**
     * Determines if requests originating from this endpoint should be synchronous
     * i.e. execute in a single thread and possibly return an result.  This property
     * is only used when the endpoint is of type 'receiver'
     *
     * @return whether requests on this endpoint should execute in a single
     *         thread. This property is only used when the endpoint is of type 'receiver'
     */
    public boolean isSynchronous()
    {
        if(synchronous==null) {
            return MuleManager.getConfiguration().isSynchronous();
        }
        return synchronous.booleanValue();
    }

    public boolean isSynchronousExplicitlySet() {
        return synchronous != null;
    }
}
