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
import org.mule.api.transport.ConnectionStrategy;
import org.mule.api.transport.Connector;
import org.mule.api.transport.DispatchException;

import java.util.List;
import java.util.Map;

/**
 * Allow's EndpointURI to be set and changed dynamically by wrapping up an immutable endpoint instance.
 */
public class DynamicEndpointURIEndpoint implements ImmutableEndpoint
{

    private static final long serialVersionUID = -2814979100270307813L;

    private ImmutableEndpoint endpoint;
    private EndpointURI dynamicEndpointURI;

    public DynamicEndpointURIEndpoint(ImmutableEndpoint endpoint)
    {
        this.endpoint = endpoint;
    }

    public DynamicEndpointURIEndpoint(ImmutableEndpoint endpoint, EndpointURI dynamicEndpointURI)
    {
        this.endpoint = endpoint;
        setEndpointURI(dynamicEndpointURI);
    }

    public EndpointURI getEndpointURI()
    {
        if (dynamicEndpointURI != null)
        {
            return dynamicEndpointURI;
        }
        else
        {
            return endpoint.getEndpointURI();
        }
    }

    public void setEndpointURI(EndpointURI dynamicEndpointURI)
    {
        this.dynamicEndpointURI = dynamicEndpointURI;
    }

    public boolean canRequest()
    {
        return endpoint.canRequest();
    }

    public boolean canSend()
    {
        return endpoint.canSend();
    }

    public void dispatch(MuleEvent event) throws DispatchException
    {
        endpoint.dispatch(event);
    }

    public ConnectionStrategy getConnectionStrategy()
    {
        return endpoint.getConnectionStrategy();
    }

    public Connector getConnector()
    {
        return endpoint.getConnector();
    }

    public String getEncoding()
    {
        return endpoint.getEncoding();
    }

    public Filter getFilter()
    {
        return endpoint.getFilter();
    }

    public String getInitialState()
    {
        return endpoint.getInitialState();
    }

    public MuleContext getMuleContext()
    {
        return endpoint.getMuleContext();
    }

    public String getName()
    {
        return endpoint.getName();
    }

    public Map getProperties()
    {
        return endpoint.getProperties();
    }

    public Object getProperty(Object key)
    {
        return endpoint.getProperty(key);
    }

    public String getProtocol()
    {
        return endpoint.getProtocol();
    }

    public int getRemoteSyncTimeout()
    {
        return endpoint.getRemoteSyncTimeout();
    }

    public List getResponseTransformers()
    {
        return endpoint.getResponseTransformers();
    }

    public EndpointSecurityFilter getSecurityFilter()
    {
        return endpoint.getSecurityFilter();
    }

    public TransactionConfig getTransactionConfig()
    {
        return endpoint.getTransactionConfig();
    }

    public List getTransformers()
    {
        return endpoint.getTransformers();
    }

    public String getType()
    {
        return endpoint.getType();
    }

    public void initialise() throws InitialisationException
    {
        endpoint.initialise();
    }

    public boolean isDeleteUnacceptedMessages()
    {
        return endpoint.isDeleteUnacceptedMessages();
    }

    public boolean isReadOnly()
    {
        return endpoint.isReadOnly();
    }

    public boolean isRemoteSync()
    {
        return endpoint.isRemoteSync();
    }

    public boolean isSynchronous()
    {
        return endpoint.isSynchronous();
    }

    public MuleMessage request(long timeout) throws Exception
    {
        return endpoint.request(timeout);
    }

    public MuleMessage send(MuleEvent event) throws DispatchException
    {
        return endpoint.send(event);
    }

    public int hashCode()
    {
        final int prime = 31;
        int result = 1;
        result = prime * result + ((dynamicEndpointURI == null) ? 0 : dynamicEndpointURI.hashCode());
        result = prime * result + ((endpoint == null) ? 0 : endpoint.hashCode());
        return result;
    }

    public boolean equals(Object obj)
    {
        if (this == obj) return true;
        if (obj == null) return false;
        if (getClass() != obj.getClass()) return false;
        final DynamicEndpointURIEndpoint other = (DynamicEndpointURIEndpoint) obj;
        if (dynamicEndpointURI == null)
        {
            if (other.dynamicEndpointURI != null) return false;
        }
        else if (!dynamicEndpointURI.equals(other.dynamicEndpointURI)) return false;
        if (endpoint == null)
        {
            if (other.endpoint != null) return false;
        }
        else if (!endpoint.equals(other.endpoint)) return false;
        return true;
    }

}
