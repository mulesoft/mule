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
import org.mule.api.endpoint.OutboundEndpoint;
import org.mule.api.retry.RetryPolicyTemplate;
import org.mule.api.routing.filter.Filter;
import org.mule.api.security.EndpointSecurityFilter;
import org.mule.api.transaction.TransactionConfig;
import org.mule.api.transport.Connector;
import org.mule.api.transport.DispatchException;
import org.mule.config.MuleManifest;

import java.util.List;
import java.util.Map;

public class DefaultOutboundEndpoint extends AbstractEndpoint implements OutboundEndpoint
{

    private static final long serialVersionUID = 8860985949279708638L;

    public DefaultOutboundEndpoint(Connector connector,
                                   EndpointURI endpointUri,
                                   List transformers,
                                   List responseTransformers,
                                   String name,
                                   Map properties,
                                   TransactionConfig transactionConfig,
                                   Filter filter,
                                   boolean deleteUnacceptedMessage,
                                   EndpointSecurityFilter securityFilter,
                                   boolean synchronous,
                                   boolean remoteSync,
                                   int remoteSyncTimeout,
                                   String initialState,
                                   String endpointEncoding,
                                   MuleContext muleContext,
                                   RetryPolicyTemplate retryPolicyTemplate)
    {
        super(connector, endpointUri, transformers, responseTransformers, name, properties, transactionConfig, filter,
            deleteUnacceptedMessage, securityFilter, synchronous, remoteSync, remoteSyncTimeout, initialState,
            endpointEncoding, muleContext, retryPolicyTemplate);
    }

    public void dispatch(MuleEvent event) throws DispatchException
    {
        if (getConnector() != null)
        {
            getConnector().dispatch(this, event);
        }
        else
        {
            // TODO Either remove because this should never happen or i18n the
            // message
            throw new IllegalStateException("The connector on the endpoint: " + toString()
                                            + " is null. Please contact " + MuleManifest.getDevListEmail());
        }
    }

    public MuleMessage send(MuleEvent event) throws DispatchException
    {
        if (getConnector() != null)
        {
            return getConnector().send(this, event);
        }
        else
        {
            // TODO Either remove because this should never happen or i18n the
            // message
            throw new IllegalStateException("The connector on the endpoint: " + toString()
                                            + " is null. Please contact " + MuleManifest.getDevListEmail());
        }
    }
}
