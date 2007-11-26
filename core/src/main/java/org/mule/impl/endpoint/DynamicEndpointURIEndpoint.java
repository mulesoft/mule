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

import org.mule.providers.ConnectionStrategy;
import org.mule.umo.UMOEvent;
import org.mule.umo.UMOFilter;
import org.mule.umo.UMOManagementContext;
import org.mule.umo.UMOMessage;
import org.mule.umo.UMOTransactionConfig;
import org.mule.umo.endpoint.UMOEndpointURI;
import org.mule.umo.endpoint.UMOImmutableEndpoint;
import org.mule.umo.lifecycle.InitialisationException;
import org.mule.umo.provider.DispatchException;
import org.mule.umo.provider.UMOConnector;
import org.mule.umo.security.UMOEndpointSecurityFilter;

import java.util.List;
import java.util.Map;

/**
 * Allow's EndpointURI to be set and changed dynamically by wrapping up an immutable endpoint instance.
 */
public class DynamicEndpointURIEndpoint implements UMOImmutableEndpoint
{

    private static final long serialVersionUID = -2814979100270307813L;

    private UMOImmutableEndpoint endpoint;
    private UMOEndpointURI dynamicEndpointURI;

    public DynamicEndpointURIEndpoint(UMOImmutableEndpoint endpoint)
    {
        this.endpoint = endpoint;
    }

    public DynamicEndpointURIEndpoint(UMOImmutableEndpoint endpoint, UMOEndpointURI dynamicEndpointURI)
    {
        this.endpoint = endpoint;
        setEndpointURI(dynamicEndpointURI);
    }

    public UMOEndpointURI getEndpointURI()
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

    public void setEndpointURI(UMOEndpointURI dynamicEndpointURI)
    {
        this.dynamicEndpointURI = dynamicEndpointURI;
    }

    public boolean canReceive()
    {
        return endpoint.canReceive();
    }

    public boolean canSend()
    {
        return endpoint.canSend();
    }

    public void dispatch(UMOEvent event) throws DispatchException
    {
        endpoint.dispatch(event);
    }

    public ConnectionStrategy getConnectionStrategy()
    {
        return endpoint.getConnectionStrategy();
    }

    public UMOConnector getConnector()
    {
        return endpoint.getConnector();
    }

    public int getCreateConnector()
    {
        return endpoint.getCreateConnector();
    }

    public String getEncoding()
    {
        return endpoint.getEncoding();
    }

    public UMOFilter getFilter()
    {
        return endpoint.getFilter();
    }

    public String getInitialState()
    {
        return endpoint.getInitialState();
    }

    public UMOManagementContext getManagementContext()
    {
        return endpoint.getManagementContext();
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

    public UMOEndpointSecurityFilter getSecurityFilter()
    {
        return endpoint.getSecurityFilter();
    }

    public UMOTransactionConfig getTransactionConfig()
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

    public UMOMessage receive(long timeout) throws Exception
    {
        return endpoint.receive(timeout);
    }

    public UMOMessage request(long timeout) throws Exception
    {
        return endpoint.request(timeout);
    }

    public UMOMessage send(UMOEvent event) throws DispatchException
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
