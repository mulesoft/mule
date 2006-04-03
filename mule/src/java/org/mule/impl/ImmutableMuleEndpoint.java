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

package org.mule.impl;

import edu.emory.mathcs.backport.java.util.concurrent.ConcurrentHashMap;
import edu.emory.mathcs.backport.java.util.concurrent.atomic.AtomicBoolean;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.mule.MuleException;
import org.mule.MuleManager;
import org.mule.config.i18n.Message;
import org.mule.config.i18n.Messages;
import org.mule.impl.endpoint.MuleEndpoint;
import org.mule.impl.endpoint.MuleEndpointURI;
import org.mule.providers.AbstractConnector;
import org.mule.providers.service.ConnectorFactory;
import org.mule.providers.service.ConnectorFactoryException;
import org.mule.umo.UMOException;
import org.mule.umo.UMOFilter;
import org.mule.umo.UMOTransactionConfig;
import org.mule.umo.endpoint.UMOEndpoint;
import org.mule.umo.endpoint.UMOEndpointURI;
import org.mule.umo.endpoint.UMOImmutableEndpoint;
import org.mule.umo.lifecycle.InitialisationException;
import org.mule.umo.provider.UMOConnector;
import org.mule.umo.security.UMOEndpointSecurityFilter;
import org.mule.umo.transformer.UMOTransformer;
import org.mule.util.ClassHelper;
import org.mule.util.MuleObjectHelper;

import java.util.Collections;
import java.util.Map;

/**
 * <code>ImmutableMuleEndpoint</code> describes a Provider in the Mule Server.
 * A endpoint is a grouping of an endpoint, an endpointUri and a transformer.
 * 
 * @author <a href="mailto:ross.mason@symphonysoft.com">Ross Mason</a>
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
     * The transformer used to transform the incoming or outgoing data
     */
    protected UMOTransformer responseTransformer = null;

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
    protected Map properties = null;

    /**
     * The transaction configuration for this endpoint
     */
    protected UMOTransactionConfig transactionConfig = null;

    /**
     * event filter for this endpoint
     */
    protected UMOFilter filter = null;

    /**
     * determines whether unaccepted filtered events should be removed from the
     * source. If they are not removed its up to the Message receiver to handle
     * recieving the same message again
     */
    protected boolean deleteUnacceptedMessages = false;

    /**
     * has this endpoint been initialised
     */
    protected AtomicBoolean initialised = new AtomicBoolean(false);

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
     * Determines whether a synchronous call should block to obtain a response
     * from a remote server (if the transport supports it). For example for Jms
     * endpoints, setting remote sync will cause a temporary destination to be
     * set up as a replyTo destination and will send the message a wait for a
     * response on the replyTo destination. If the JMSReplyTo is already set on
     * the message that destination will be used instead.
     */
    protected Boolean remoteSync = null;

    /**
     * How long to block when performing a remote synchronisation to a remote
     * host. This property is optional and will be set to the default Synchonous
     * Event time out value if not set
     */
    protected Integer remoteSyncTimeout = null;

    /**
     * Determines whether the endpoint should deal with requests as streams
     */
    protected boolean streaming = false;

    /**
     * The state that the endpoint is initialised in such as started or stopped
     */
    protected String initialState = INITIAL_STATE_STARTED;

    protected String endpointEncoding = null;

    /**
     * determines if a new connector should be created for this endpoint
     */
    protected int createConnector = ConnectorFactory.GET_OR_CREATE_CONNECTOR;

    /**
     * Default ctor
     */
    private ImmutableMuleEndpoint()
    {
        super();
    }

    /**
     * Default constructor
     */
    public ImmutableMuleEndpoint(String name,
                                 UMOEndpointURI endpointUri,
                                 UMOConnector connector,
                                 UMOTransformer transformer,
                                 String type,
                                 int createConnector,
                                 String endpointEncoding,
                                 Map props)
    {
        this.name = name;
        this.connector = connector;
        this.createConnector = createConnector;
        this.endpointEncoding = endpointEncoding;
        this.type = type;

        if (endpointUri != null) {
            this.endpointUri = new MuleEndpointURI(endpointUri);
        }

        if (transformer != null) {
            transformer.setEndpoint(this);
            this.transformer = transformer;
        }

        this.properties = new ConcurrentHashMap();

        if (props != null) {
            this.properties.putAll(props);
        }

        if (endpointUri != null) {
            properties.putAll(endpointUri.getParams());
        }

        // seal the properties if we are immutable to avoid
        // write-through aliasing problems with the exposed Map
        if (!(this instanceof MuleEndpoint)) {
            this.properties = Collections.unmodifiableMap(this.properties);
        }

        // Create a default transaction config
        transactionConfig = new MuleTransactionConfig();
    }

    public ImmutableMuleEndpoint(UMOImmutableEndpoint source)
    {
        this();
        this.initFromDescriptor(source);
    }

    public ImmutableMuleEndpoint(String endpointName, boolean receiver) throws UMOException
    {
        this();
        String type = (receiver ? UMOEndpoint.ENDPOINT_TYPE_RECEIVER : UMOEndpoint.ENDPOINT_TYPE_SENDER);
        UMOEndpoint p = getOrCreateEndpointForUri(new MuleEndpointURI(endpointName), type);
        this.initFromDescriptor(p);
    }

    protected void initFromDescriptor(UMOImmutableEndpoint source)
    {
        if (this.name == null) {
            this.name = source.getName();
        }

        if (this.endpointUri == null && source.getEndpointURI() != null) {
            this.endpointUri = new MuleEndpointURI(source.getEndpointURI());
        }

        if (this.endpointEncoding == null) {
            this.endpointEncoding = source.getEncoding();
        }

        if (this.connector == null) {
            this.connector = source.getConnector();
        }

        if (this.transformer == null) {
            this.transformer = source.getTransformer();
        }

        if (this.transformer != null) {
            this.transformer.setEndpoint(this);
        }

        if (this.responseTransformer == null) {
            this.responseTransformer = source.getResponseTransformer();
        }

        if (responseTransformer != null) {
            this.responseTransformer.setEndpoint(this);
        }

        this.properties = new ConcurrentHashMap();

        if (source.getProperties() != null) {
            this.properties.putAll(source.getProperties());
        }

        if (endpointUri != null && endpointUri.getParams() != null) {
            this.properties.putAll(endpointUri.getParams());
        }

        // seal the properties if we are immutable to avoid
        // write-through aliasing problems with the exposed Map
        if (!(this instanceof MuleEndpoint)) {
            this.properties = Collections.unmodifiableMap(this.properties);
        }

        this.type = source.getType();
        this.transactionConfig = source.getTransactionConfig();
        this.deleteUnacceptedMessages = source.isDeleteUnacceptedMessages();
        this.initialState = source.getInitialState();

        remoteSyncTimeout = new Integer(source.getRemoteSyncTimeout());
        remoteSync = Boolean.valueOf(source.isRemoteSync());

        filter = source.getFilter();
        securityFilter = source.getSecurityFilter();
    }

    /*
     * (non-Javadoc)
     * 
     * @see org.mule.umo.endpoint.UMOEndpoint#getEndpointURI()
     */
    public UMOEndpointURI getEndpointURI()
    {
        return endpointUri;
    }

    public String getEncoding()
    {
        return endpointEncoding;
    }

    /*
     * (non-Javadoc)
     * 
     * @see org.mule.umo.endpoint.UMOImmutableEndpoint#getType()
     */
    public String getType()
    {
        return type;
    }

    /*
     * (non-Javadoc)
     * 
     * @see org.mule.umo.endpoint.UMOImmutableEndpoint#getConnectorName()
     */
    public UMOConnector getConnector()
    {
        return connector;
    }

    /*
     * (non-Javadoc)
     * 
     * @see org.mule.umo.endpoint.UMOImmutableEndpoint#getName()
     */
    public String getName()
    {
        return name;
    }

    /*
     * (non-Javadoc)
     * 
     * @see org.mule.umo.endpoint.UMOEndpoint#getTransformer()
     */
    public UMOTransformer getTransformer()
    {
        return transformer;
    }

    /*
     * (non-Javadoc)
     * 
     * @see org.mule.umo.endpoint.UMOImmutableEndpoint#getParams()
     */
    public Map getProperties()
    {
        return properties;
    }

    /*
     * (non-Javadoc)
     * 
     * @see java.lang.Object#clone()
     */
    // TODO this is the 'old' implementation of the clone() method which returns
    // a MUTABLE endpoint! We need to clarify what endpoint mutability and
    // cloning actually means
    public Object clone()
    {
        UMOEndpoint clone = new MuleEndpoint(name, endpointUri, connector, transformer, type,
                createConnector, endpointEncoding, properties);

        clone.setTransactionConfig(transactionConfig);
        clone.setFilter(filter);
        clone.setSecurityFilter(securityFilter);

        if (remoteSync != null) {
            clone.setRemoteSync(isRemoteSync());
        }
        if (remoteSyncTimeout != null) {
            clone.setRemoteSyncTimeout(getRemoteSyncTimeout());
        }

        if (synchronous != null) {
            clone.setSynchronous(synchronous.booleanValue());
        }

        clone.setDeleteUnacceptedMessages(deleteUnacceptedMessages);

        clone.setInitialState(initialState);
        if (initialised.get()) {
            try {
                clone.initialise();
            }
            catch (InitialisationException e) {
                // this really should never happen as the endpoint is already
                // initialised
                logger.error(e.getMessage(), e);
            }
        }

        return clone;
    }

    /*
     * (non-Javadoc)
     * 
     * @see org.mule.umo.endpoint.UMOImmutableEndpoint#isReadOnly()
     */
    public boolean isReadOnly()
    {
        return true;
    }

    public String toString()
    {
        return ClassHelper.getClassName(this.getClass()) + "{connector=" + connector + ", endpointUri="
                + endpointUri + ", transformer=" + transformer + ", name='" + name + "'" + ", type='"
                + type + "'" + ", properties=" + properties + ", transactionConfig=" + transactionConfig
                + ", filter=" + filter + ", deleteUnacceptedMessages=" + deleteUnacceptedMessages
                + ", initialised=" + initialised + ", securityFilter=" + securityFilter
                + ", synchronous=" + synchronous + ", initialState=" + initialState
                + ", createConnector=" + createConnector + ", remoteSync=" + remoteSync
                + ", remoteSyncTimeout=" + remoteSyncTimeout + ", endpointEncoding=" + endpointEncoding
                + "}";
    }

    /*
     * (non-Javadoc)
     * 
     * @see org.mule.umo.endpoint.UMOImmutableEndpoint#getProtocol()
     */
    public String getProtocol()
    {
        return connector.getProtocol();
    }

    /*
     * (non-Javadoc)
     * 
     * @see org.mule.umo.endpoint.UMOImmutableEndpoint#canReceive()
     */
    public boolean canReceive()
    {
        return (getType().equals(ENDPOINT_TYPE_RECEIVER) || getType().equals(
                ENDPOINT_TYPE_SENDER_AND_RECEIVER));
    }

    /*
     * (non-Javadoc)
     * 
     * @see org.mule.umo.endpoint.UMOImmutableEndpoint#canSend()
     */
    public boolean canSend()
    {
        return (getType().equals(ENDPOINT_TYPE_SENDER) || getType().equals(
                ENDPOINT_TYPE_SENDER_AND_RECEIVER));
    }

    /*
     * (non-Javadoc)
     * 
     * @see org.mule.umo.endpoint.UMOEndpoint#getTransactionConfig()
     */
    public UMOTransactionConfig getTransactionConfig()
    {
        return transactionConfig;
    }

    public boolean equals(Object o)
    {
        if (this == o) {
            return true;
        }
        if (!(o instanceof ImmutableMuleEndpoint)) {
            return false;
        }

        final ImmutableMuleEndpoint immutableMuleProviderDescriptor = (ImmutableMuleEndpoint)o;

        if (!connector.getName().equals(immutableMuleProviderDescriptor.connector.getName())) {
            return false;
        }
        if (endpointUri != null && immutableMuleProviderDescriptor.endpointUri != null
                ? !endpointUri.getAddress().equals(
                        immutableMuleProviderDescriptor.endpointUri.getAddress())
                : immutableMuleProviderDescriptor.endpointUri != null) {
            return false;
        }
        if (!name.equals(immutableMuleProviderDescriptor.name)) {
            return false;
        }
        if (transformer != null
                ? !transformer.equals(immutableMuleProviderDescriptor.transformer)
                : immutableMuleProviderDescriptor.transformer != null) {
            return false;
        }
        if (!type.equals(immutableMuleProviderDescriptor.type)) {
            return false;
        }

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
        if (uri.getEndpointName() != null) {
            endpoint.setName(uri.getEndpointName());
        }
        return endpoint;
    }

    public static UMOEndpoint getEndpointFromUri(String uri)
    {
        UMOEndpoint endpoint = null;
        if (uri != null) {
            String endpointString = MuleManager.getInstance().lookupEndpointIdentifier(uri, uri);
            endpoint = MuleManager.getInstance().lookupEndpoint(endpointString);
        }
        return endpoint;
    }

    public static UMOEndpoint getEndpointFromUri(UMOEndpointURI uri) throws UMOException
    {
        UMOEndpoint endpoint = null;
        if (uri.getEndpointName() != null) {
            String endpointString = MuleManager.getInstance().lookupEndpointIdentifier(
                    uri.getEndpointName(), uri.getEndpointName());
            endpoint = MuleManager.getInstance().lookupEndpoint(endpointString);
            if (endpoint != null) {
                if (uri.getAddress() != null && uri.getAddress().length() > 0) {
                    endpoint.setEndpointURI(uri);
                }
            }

        }
        return endpoint;
    }

    public static UMOEndpoint getOrCreateEndpointForUri(String uriIdentifier, String type)
            throws UMOException
    {
        UMOEndpoint endpoint = getEndpointFromUri(uriIdentifier);
        if (endpoint == null) {
            endpoint = createEndpointFromUri(new MuleEndpointURI(uriIdentifier), type);
        }
        else {
            if (endpoint.getType().equals(UMOEndpoint.ENDPOINT_TYPE_SENDER_AND_RECEIVER)) {
                endpoint.setType(type);
            }
            else if (!endpoint.getType().equals(type)) {
                throw new IllegalArgumentException("Endpoint matching: " + uriIdentifier
                        + " is not of type: " + type + ". It is of type: " + endpoint.getType());

            }
        }
        return endpoint;
    }

    public static UMOEndpoint getOrCreateEndpointForUri(UMOEndpointURI uri, String type)
            throws UMOException
    {
        UMOEndpoint endpoint = getEndpointFromUri(uri);
        if (endpoint == null) {
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
        if (initialised.get()) {
            logger.debug("Already initialised: " + toString());
            return;
        }
        if (connector == null) {
            if (endpointUri.getConnectorName() != null) {
                connector = MuleManager.getInstance().lookupConnector(endpointUri.getConnectorName());
                if (connector == null) {
                    throw new IllegalArgumentException("Connector not found: "
                            + endpointUri.getConnectorName());
                }
            }
            else {
                try {
                    connector = ConnectorFactory.getOrCreateConnectorByProtocol(this);
                    if (connector == null) {
                        throw new InitialisationException(new Message(
                                Messages.CONNECTOR_WITH_PROTOCOL_X_NOT_REGISTERED, endpointUri
                                        .getScheme()), this);
                    }
                }
                catch (ConnectorFactoryException e) {
                    throw new InitialisationException(new Message(
                            Messages.FAILED_TO_CREATE_CONNECTOR_FROM_URI_X, endpointUri), e, this);
                }
            }

            if (endpointUri.getEndpointName() != null && name == null) {
                name = endpointUri.getEndpointName();
            }
        }
        if (name == null) {
            name = "_" + endpointUri.getScheme() + "Endpoint#" + hashCode();
            endpointUri.setEndpointName(name);
        }
        String sync = endpointUri.getParams().getProperty("synchronous", null);
        if (sync != null) {
            synchronous = Boolean.valueOf(sync);
        }
        if (properties != null && endpointUri.getParams() != null) {
            properties.putAll(endpointUri.getParams());
        }

        if (endpointUri.getTransformers() != null) {
            try {
                transformer = MuleObjectHelper.getTransformer(endpointUri.getTransformers(), ",");
            }
            catch (MuleException e) {
                throw new InitialisationException(e, this);
            }
        }

        if (transformer == null) {
            if (connector instanceof AbstractConnector) {
                if (UMOEndpoint.ENDPOINT_TYPE_SENDER.equals(type)) {
                    transformer = ((AbstractConnector)connector).getDefaultOutboundTransformer();
                }
                else {
                    transformer = ((AbstractConnector)connector).getDefaultInboundTransformer();
                }
            }
        }
        if (transformer != null) {
            transformer.setEndpoint(this);
        }

        if (endpointUri.getResponseTransformers() != null) {
            try {
                responseTransformer = MuleObjectHelper.getTransformer(endpointUri
                        .getResponseTransformers(), ",");
            }
            catch (MuleException e) {
                throw new InitialisationException(e, this);
            }
        }
        if (responseTransformer == null) {
            if (connector instanceof AbstractConnector) {
                responseTransformer = ((AbstractConnector)connector).getDefaultResponseTransformer();
            }
        }
        if (responseTransformer != null) {
            responseTransformer.setEndpoint(this);
        }

        if (securityFilter != null) {
            securityFilter.setEndpoint(this);
            securityFilter.initialise();
        }

        // Allow remote sync values to be set as params on the endpoint URI
        String rs = (String)endpointUri.getParams().remove("remoteSync");
        if (rs != null) {
            remoteSync = Boolean.valueOf(rs);
        }

        String rsTimeout = (String)endpointUri.getParams().remove("remoteSyncTimeout");
        if (rsTimeout != null) {
            remoteSyncTimeout = Integer.valueOf(rsTimeout);
        }

        initialised.set(true);
    }

    /**
     * Returns an UMOEndpointSecurityFilter for this endpoint. If one is not
     * set, there will be no authentication on events sent via this endpoint
     * 
     * @return UMOEndpointSecurityFilter responsible for authenticating message
     *         flow via this endpoint.
     * @see org.mule.umo.security.UMOEndpointSecurityFilter
     */
    public UMOEndpointSecurityFilter getSecurityFilter()
    {
        return securityFilter;
    }

    /**
     * Determines if requests originating from this endpoint should be
     * synchronous i.e. execute in a single thread and possibly return an
     * result. This property is only used when the endpoint is of type
     * 'receiver'
     * 
     * @return whether requests on this endpoint should execute in a single
     *         thread. This property is only used when the endpoint is of type
     *         'receiver'
     */
    public boolean isSynchronous()
    {
        if (synchronous == null) {
            return MuleManager.getConfiguration().isSynchronous();
        }
        return synchronous.booleanValue();
    }

    public boolean isSynchronousSet()
    {
        return (synchronous != null);
    }

    public int getCreateConnector()
    {
        return createConnector;
    }

    /**
     * For certain providers that support the notion of a backchannel such as
     * sockets (outputStream) or Jms (ReplyTo) Mule can automatically wait for a
     * response from a backchannel when dispatching over these protocols. This
     * is different for synchronous as synchronous behavior only applies to in
     * 
     * @return
     */
    public boolean isRemoteSync()
    {
        if (remoteSync == null) {
            if (connector == null || connector.isRemoteSyncEnabled()) {
                remoteSync = Boolean.valueOf(MuleManager.getConfiguration().isRemoteSync());
            }
            else {
                remoteSync = Boolean.FALSE;
            }
        }
        return remoteSync.booleanValue();
    }

    /**
     * The timeout value for remoteSync invocations
     * 
     * @return the timeout in milliseconds
     */
    public int getRemoteSyncTimeout()
    {
        if (remoteSyncTimeout == null) {
            remoteSyncTimeout = new Integer(MuleManager.getConfiguration().getSynchronousEventTimeout());
        }
        return remoteSyncTimeout.intValue();
    }

    /**
     * Sets the state the endpoint will be loaded in. The States are 'stopped'
     * and 'started' (default)
     * 
     * @return the endpoint starting state
     */
    public String getInitialState()
    {
        return initialState;
    }

    public UMOTransformer getResponseTransformer()
    {
        return responseTransformer;
    }

    /**
     * Determines whether the endpoint should deal with requests as streams
     * 
     * @return true if the request should be streamed
     */
    public boolean isStreaming()
    {
        return streaming;
    }

    public Object getProperty(Object key) {
        Object value =  properties.get(key);
        if(value==null) {
            value = endpointUri.getParams().get(key);
        }
        return value;
    }
}
