/*
 * $Id$
 * --------------------------------------------------------------------------------------
 * Copyright (c) MuleSource, Inc.  All rights reserved.  http://www.mulesource.com
 *
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.endpoint;

import org.mule.api.MuleContext;
import org.mule.api.MuleEvent;
import org.mule.api.MuleMessage;
import org.mule.api.endpoint.EndpointURI;
import org.mule.api.endpoint.ImmutableEndpoint;
import org.mule.api.lifecycle.InitialisationException;
import org.mule.api.routing.filter.Filter;
import org.mule.api.security.EndpointSecurityFilter;
import org.mule.api.transaction.TransactionConfig;
import org.mule.api.transformer.Transformer;
import org.mule.api.transport.ConnectionStrategy;
import org.mule.api.transport.Connector;
import org.mule.api.transport.DispatchException;
import org.mule.config.MuleManifest;
import org.mule.transformer.TransformerUtils;
import org.mule.util.ClassUtils;

import java.net.URI;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.LinkedList;
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
public abstract class ImmutableMuleEndpoint implements ImmutableEndpoint
{

    private static final long serialVersionUID = -1650380871293160973L;

    /**
     * logger used by this class
     */
    protected static final Log logger = LogFactory.getLog(ImmutableMuleEndpoint.class);

    /**
     * The endpoint used to communicate with the external system
     */
    protected Connector connector = null;

    /**
     * The endpointUri on which to send or receive information
     */
    protected EndpointURI endpointUri = null;

    /**
     * The transformers used to transform the incoming or outgoing data
     */
    protected AtomicReference transformers = new AtomicReference(new LinkedList());

    /**
     * The transformers used to transform the incoming or outgoing data
     */
    protected AtomicReference responseTransformers = new AtomicReference(new LinkedList());

    /**
     * The name for the endpoint
     */
    protected String name = null;

    /**
     * Any additional properties for the endpoint
     */
    protected Map properties = new HashMap();

    /**
     * The transaction configuration for this endpoint
     */
    protected TransactionConfig transactionConfig = null;

    /**
     * event filter for this endpoint
     */
    protected Filter filter = null;

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
    protected EndpointSecurityFilter securityFilter = null;

    /**
     * whether events received by this endpoint should execute in a single thread
     */
    protected boolean synchronous;

    /**
     * Determines whether a synchronous call should block to obtain a response from a remote server (if the
     * transport supports it). For example for Jms endpoints, setting remote sync will cause a temporary
     * destination to be set up as a replyTo destination and will send the message a wait for a response on
     * the replyTo destination. If the JMSReplyTo is already set on the message that destination will be used
     * instead.
     */
    protected boolean remoteSync;

    /**
     * How long to block when performing a remote synchronisation to a remote host. This property is optional
     * and will be set to the default Synchonous MuleEvent time out value if not set
     */
    protected Integer remoteSyncTimeout = null;
    
    /**
     * The state that the endpoint is initialised in such as started or stopped
     */
    protected String initialState = INITIAL_STATE_STARTED;

    protected String endpointEncoding;

    protected String registryId = null;

    protected MuleContext muleContext;

    protected ConnectionStrategy connectionStrategy;

    /**
     * Default constructor.
     */
    protected ImmutableMuleEndpoint()
    {
        super();
    }

    public EndpointURI getEndpointURI()
    {
        return endpointUri;
    }

    public String getEncoding()
    {
        return endpointEncoding;
    }

    public Connector getConnector()
    {
        return connector;
    }

    public String getName()
    {
        return name;
    }

    public List getTransformers()
    {
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
               + connector + ", transformer=" + transformers.get() + ", name='" + name + "'"
               + ", isInbound=" + isInbound() + ", isOutbound=" + isOutbound()
               + ", properties=" + properties + ", transactionConfig=" + transactionConfig + ", filter=" + filter
               + ", deleteUnacceptedMessages=" + deleteUnacceptedMessages + ", initialised=" + initialised
               + ", securityFilter=" + securityFilter + ", synchronous=" + synchronous + ", initialState="
               + initialState + ", remoteSync=" + remoteSync
               + ", remoteSyncTimeout=" + remoteSyncTimeout + ", endpointEncoding=" + endpointEncoding + "}";
    }

    public String getProtocol()
    {
        return connector.getProtocol();
    }

    public TransactionConfig getTransactionConfig()
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

        final ImmutableMuleEndpoint other = (ImmutableMuleEndpoint) o;

        if (!connector.getName().equals(other.connector.getName()))
        {
            return false;
        }

        if (endpointUri != null && other.endpointUri != null
                        ? !endpointUri.getAddress().equals(other.endpointUri.getAddress())
                        : other.endpointUri != null)
        {
            return false;
        }

        if (!name.equals(other.name))
        {
            return false;
        }

        // MULE-1551 - transformer excluded from comparison here
        return isInbound() == other.isInbound() && isOutbound() == other.isOutbound();
    }

    public int hashCode()
    {
        // MULE-1551 - transformer excluded from hash here
        return ClassUtils.hash(new Object[]{connector, endpointUri, name,
                Boolean.valueOf(isInbound()), Boolean.valueOf(isOutbound())});
    }

    public Filter getFilter()
    {
        return filter;
    }

    public boolean isDeleteUnacceptedMessages()
    {
        return deleteUnacceptedMessages;
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
                ((Transformer) transformer.next()).setEndpoint(this);
            }
        }
    }

    /**
     * Returns an EndpointSecurityFilter for this endpoint. If one is not set, there will be no
     * authentication on events sent via this endpoint
     * 
     * @return EndpointSecurityFilter responsible for authenticating message flow via this endpoint.
     * @see org.mule.api.security.EndpointSecurityFilter
     */
    public EndpointSecurityFilter getSecurityFilter()
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
        return synchronous;
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
        return remoteSync;
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

    public void dispatch(MuleEvent event) throws DispatchException
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

    public MuleMessage request(long timeout) throws Exception
    {
        if (connector != null)
        {
            return connector.request(this, timeout);
        }
        else
        {
            // TODO Either remove because this should never happen or i18n the message
            throw new IllegalStateException("The connector on the endpoint: " + toString()
                                            + " is null. Please contact " + MuleManifest.getDevListEmail());
        }
    }

    public MuleMessage send(MuleEvent event) throws DispatchException
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

    public MuleContext getMuleContext()
    {
        return muleContext;
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
