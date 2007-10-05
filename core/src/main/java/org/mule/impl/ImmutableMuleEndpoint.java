/*
 * $Id$
 * --------------------------------------------------------------------------------------
 * Copyright (c) MuleSource, Inc.  All rights reserved.  http://www.mulesource.com
 *
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.impl;

import org.mule.RegistryContext;
import org.mule.config.MuleManifest;
import org.mule.providers.AbstractConnector;
import org.mule.providers.ConnectionStrategy;
import org.mule.providers.service.TransportFactory;
import org.mule.transformers.TransformerUtils;
import org.mule.umo.UMOEvent;
import org.mule.umo.UMOFilter;
import org.mule.umo.UMOManagementContext;
import org.mule.umo.UMOMessage;
import org.mule.umo.UMOTransactionConfig;
import org.mule.umo.endpoint.UMOEndpoint;
import org.mule.umo.endpoint.UMOEndpointURI;
import org.mule.umo.endpoint.UMOImmutableEndpoint;
import org.mule.umo.lifecycle.InitialisationException;
import org.mule.umo.provider.DispatchException;
import org.mule.umo.provider.UMOConnector;
import org.mule.umo.security.UMOEndpointSecurityFilter;
import org.mule.umo.transformer.UMOTransformer;
import org.mule.util.ClassUtils;

import java.net.URI;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import edu.emory.mathcs.backport.java.util.concurrent.atomic.AtomicBoolean;
import edu.emory.mathcs.backport.java.util.concurrent.atomic.AtomicReference;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

/**
 * <code>ImmutableMuleEndpoint</code> describes a Provider in the Mule Server. A endpoint is a grouping of
 * an endpoint, an endpointUri and a transformer.
 */
public class ImmutableMuleEndpoint implements UMOImmutableEndpoint
{
    private static final long serialVersionUID = -1650380871293160973L;

    /**
     * logger used by this class
     */
    protected static final Log logger = LogFactory.getLog(ImmutableMuleEndpoint.class);

    /**
     * The endpoint used to communicate with the external system
     */
    protected UMOConnector connector = null;

    /**
     * The endpointUri on which to send or receive information
     */
    protected UMOEndpointURI endpointUri = null;

    /**
     * The transformers used to transform the incoming or outgoing data
     */
    protected AtomicReference transformers = new AtomicReference(TransformerUtils.UNDEFINED);

    /**
     * The transformers used to transform the incoming or outgoing data
     */
    protected AtomicReference responseTransformers = new AtomicReference(TransformerUtils.UNDEFINED);

    /**
     * The name for the endpoint
     */
    protected String name = null;

    // TODO Remove MULE-2266
    protected static String ENDPOINT_TYPE_SENDER_AND_RECEIVER = "senderAndReceiver";

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
     * determines whether unaccepted filtered events should be removed from the source. If they are not
     * removed its up to the Message receiver to handle recieving the same message again
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
     * whether events received by this endpoint should execute in a single thread
     */
    protected Boolean synchronous = null;

    /**
     * Determines whether a synchronous call should block to obtain a response from a remote server (if the
     * transport supports it). For example for Jms endpoints, setting remote sync will cause a temporary
     * destination to be set up as a replyTo destination and will send the message a wait for a response on
     * the replyTo destination. If the JMSReplyTo is already set on the message that destination will be used
     * instead.
     */
    protected Boolean remoteSync = null;

    /**
     * How long to block when performing a remote synchronisation to a remote host. This property is optional
     * and will be set to the default Synchonous Event time out value if not set
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

    protected String endpointEncoding;

    /**
     * determines if a new connector should be created for this endpoint
     */
    protected int createConnector = TransportFactory.GET_OR_CREATE_CONNECTOR;

    protected String registryId = null;

    protected UMOManagementContext managementContext;

    protected ConnectionStrategy connectionStrategy;

    /**
     * Default constructor.
     */
    protected ImmutableMuleEndpoint()
    {
        super();
    }

    public UMOEndpointURI getEndpointURI()
    {
        return endpointUri;
    }

    public String getEncoding()
    {
        return endpointEncoding;
    }

    public String getType()
    {
        return type;
    }

    public UMOConnector getConnector()
    {
        return connector;
    }

    public String getName()
    {
        return name;
    }

    public List getTransformers()
    {
        lazyInitTransformers();
        return (List) transformers.get();
    }

    public Map getProperties()
    {
        return properties;
    }

    public boolean isReadOnly()
    {
        return true;
    }

    public String toString()
    {
        // Use the interface to retrieve the string and set
        // the endpoint uri to a default value
        String sanitizedEndPointUri = null;
        URI uri = null;
        if (endpointUri != null)
        {
            sanitizedEndPointUri = endpointUri.toString();
            uri = endpointUri.getUri();
        }
        // The following will further sanitize the endpointuri by removing
        // the embedded password. This will only remove the password if the
        // uri contains all the necessary information to successfully rebuild the url
        if (uri != null && (uri.getRawUserInfo() != null) && (uri.getScheme() != null) && (uri.getHost() != null)
            && (uri.getRawPath() != null))
        {
            // build a pattern up that matches what we need tp strip out the password
            Pattern sanitizerPattern = Pattern.compile("(.*):.*");
            Matcher sanitizerMatcher = sanitizerPattern.matcher(uri.getRawUserInfo());
            if (sanitizerMatcher.matches())
            {
                sanitizedEndPointUri = new StringBuffer(uri.getScheme()).append("://")
                    .append(sanitizerMatcher.group(1))
                    .append(":<password>")
                    .append("@")
                    .append(uri.getHost())
                    .append(uri.getRawPath())
                    .toString();
            }
            if (uri.getRawQuery() != null)
            {
                sanitizedEndPointUri = sanitizedEndPointUri + "?" + uri.getRawQuery();
            }

        }

        return ClassUtils.getClassName(getClass()) + "{endpointUri=" + sanitizedEndPointUri + ", connector="
               + connector + ", transformer=" + transformers.get() + ", name='" + name + "'" + ", type='" + type + "'"
               + ", properties=" + properties + ", transactionConfig=" + transactionConfig + ", filter=" + filter
               + ", deleteUnacceptedMessages=" + deleteUnacceptedMessages + ", initialised=" + initialised
               + ", securityFilter=" + securityFilter + ", synchronous=" + synchronous + ", initialState="
               + initialState + ", createConnector=" + createConnector + ", remoteSync=" + remoteSync
               + ", remoteSyncTimeout=" + remoteSyncTimeout + ", endpointEncoding=" + endpointEncoding + "}";
    }

    public String getProtocol()
    {
        return connector.getProtocol();
    }

    public boolean canReceive()
    {
        return getType().equals(ENDPOINT_TYPE_RECEIVER);
    }

    public boolean canSend()
    {
        return getType().equals(ENDPOINT_TYPE_SENDER);
    }

    public UMOTransactionConfig getTransactionConfig()
    {
        return transactionConfig;
    }

    public boolean equals(Object o)
    {
        if (this == o)
        {
            return true;
        }
        if (!(o instanceof ImmutableMuleEndpoint))
        {
            return false;
        }

        final ImmutableMuleEndpoint immutableMuleProviderDescriptor = (ImmutableMuleEndpoint) o;

        if (!connector.getName().equals(immutableMuleProviderDescriptor.connector.getName()))
        {
            return false;
        }
        if (endpointUri != null && immutableMuleProviderDescriptor.endpointUri != null
                                                                                      ? !endpointUri.getAddress()
                                                                                          .equals(
                                                                                              immutableMuleProviderDescriptor.endpointUri.getAddress())
                                                                                      : immutableMuleProviderDescriptor.endpointUri != null)
        {
            return false;
        }
        if (!name.equals(immutableMuleProviderDescriptor.name))
        {
            return false;
        }
        // MULE-1551 - transformer excluded from comparison here
        return getType().equals(immutableMuleProviderDescriptor.getType());
    }

    public int hashCode()
    {
        int result = appendHash(0, connector);
        result = appendHash(result, endpointUri);
        // MULE-1551 - transformer excluded from hash here
        result = appendHash(result, name);
        result = appendHash(result, getType());
        return result;
    }

    private int appendHash(int hash, Object component)
    {
        int delta = component != null ? component.hashCode() : 0;
        return 29 * hash + delta;
    }

    public UMOFilter getFilter()
    {
        return filter;
    }

    public boolean isDeleteUnacceptedMessages()
    {
        return deleteUnacceptedMessages;
    }

    // TODO 
    protected void lazyInitTransformers()
    {
        // for efficiency
        if (TransformerUtils.isUndefined((List) transformers.get()))
        {
            List newTransformers;
            if (connector instanceof AbstractConnector)
            {
                if (UMOEndpoint.ENDPOINT_TYPE_SENDER.equals(type))
                {
                    newTransformers = ((AbstractConnector) connector).getDefaultOutboundTransformers();
                }
                else
                {
                    newTransformers = ((AbstractConnector) connector).getDefaultInboundTransformers();
                }
                if (logger.isDebugEnabled())
                {
                    logger.debug("Creating new transformer " + newTransformers + " for endpoint " + this + " of type "
                                 + type);
                }
            }
            else
            {
                newTransformers = TransformerUtils.UNDEFINED;
                // Why would a connector not inherit AbstractConnector?
                logger.warn("Connector " + connector.getName() + " does not inherit AbstractConnector");
            }
            setTransformersIfUndefined(transformers, newTransformers);
        }
    }

    protected void setTransformersIfUndefined(AtomicReference reference, List transformers)
    {
        TransformerUtils.discourageNullTransformers(transformers);
        reference.compareAndSet(TransformerUtils.UNDEFINED, transformers);
        updateTransformerEndpoints(reference);
    }

    // TODO - remove (or fix)
    protected void updateTransformerEndpoints(AtomicReference reference)
    {
        List transformers = (List) reference.get();
        if (TransformerUtils.isDefined(transformers))
        {
            Iterator transformer = transformers.iterator();
            while (transformer.hasNext())
            {
                ((UMOTransformer) transformer.next()).setEndpoint(this);
            }
        }
    }

    /**
     * Returns an UMOEndpointSecurityFilter for this endpoint. If one is not set, there will be no
     * authentication on events sent via this endpoint
     * 
     * @return UMOEndpointSecurityFilter responsible for authenticating message flow via this endpoint.
     * @see org.mule.umo.security.UMOEndpointSecurityFilter
     */
    public UMOEndpointSecurityFilter getSecurityFilter()
    {
        return securityFilter;
    }

    /**
     * Determines if requests originating from this endpoint should be synchronous i.e. execute in a single
     * thread and possibly return an result. This property is only used when the endpoint is of type
     * 'receiver'
     * 
     * @return whether requests on this endpoint should execute in a single thread. This property is only used
     *         when the endpoint is of type 'receiver'
     */
    public boolean isSynchronous()
    {
        if (synchronous == null)
        {
            return RegistryContext.getConfiguration().isDefaultSynchronousEndpoints();
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
     * For certain providers that support the notion of a backchannel such as sockets (outputStream) or Jms
     * (ReplyTo) Mule can automatically wait for a response from a backchannel when dispatching over these
     * protocols. This is different for synchronous as synchronous behavior only applies to in
     * 
     * @return
     */
    public boolean isRemoteSync()
    {
        if (remoteSync == null)
        {
            // what is this for?!
            if (connector == null || connector.isRemoteSyncEnabled())
            {
                remoteSync = Boolean.FALSE;
            }
            else
            {
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
        if (remoteSyncTimeout == null)
        {
            remoteSyncTimeout = new Integer(0);
        }
        return remoteSyncTimeout.intValue();
    }

    /**
     * Sets the state the endpoint will be loaded in. The States are 'stopped' and 'started' (default)
     * 
     * @return the endpoint starting state
     */
    public String getInitialState()
    {
        return initialState;
    }

    public List getResponseTransformers()
    {
        return (List) responseTransformers.get();
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

    public Object getProperty(Object key)
    {
        Object value = properties.get(key);
        if (value == null)
        {
            value = endpointUri.getParams().get(key);
        }
        return value;
    }

    // TODO the following methods should most likely be lifecycle-enabled

    public void dispatch(UMOEvent event) throws DispatchException
    {
        if (connector != null)
        {
            connector.dispatch(this, event);
        }
        else
        {
            // TODO Either remove because this should never happen or i18n the message
            throw new IllegalStateException("The connector on the endpoint: " + toString()
                                            + " is null. Please contact " + MuleManifest.getDevListEmail());
        }
    }

    public UMOMessage receive(long timeout) throws Exception
    {
        if (connector != null)
        {
            return connector.receive(this, timeout);
        }
        else
        {
            // TODO Either remove because this should never happen or i18n the message
            throw new IllegalStateException("The connector on the endpoint: " + toString()
                                            + " is null. Please contact " + MuleManifest.getDevListEmail());
        }
    }

    public UMOMessage send(UMOEvent event) throws DispatchException
    {
        if (connector != null)
        {
            return connector.send(this, event);
        }
        else
        {
            // TODO Either remove because this should never happen or i18n the message
            throw new IllegalStateException("The connector on the endpoint: " + toString()
                                            + " is null. Please contact " + MuleManifest.getDevListEmail());
        }
    }

    public UMOManagementContext getManagementContext()
    {
        return managementContext;
    }

    /**
     * Getter for property 'connectionStrategy'.
     * 
     * @return Value for property 'connectionStrategy'.
     */
    public ConnectionStrategy getConnectionStrategy()
    {
        return connectionStrategy;
    }

    public void initialise() throws InitialisationException
    {
        // Nothing to initialise currently
    }

}
