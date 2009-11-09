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
import org.mule.api.config.MuleProperties;
import org.mule.api.endpoint.EndpointURI;
import org.mule.api.endpoint.OutboundEndpoint;
import org.mule.api.retry.RetryPolicyTemplate;
import org.mule.api.routing.filter.Filter;
import org.mule.api.security.EndpointSecurityFilter;
import org.mule.api.transaction.TransactionConfig;
import org.mule.api.transport.Connector;
import org.mule.api.transport.DispatchException;
import org.mule.config.MuleManifest;
import org.mule.util.StringUtils;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class DefaultOutboundEndpoint extends AbstractEndpoint implements OutboundEndpoint
{

    private static final long serialVersionUID = 8860985949279708638L;

    private List<String> responseProperties;

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
                                   int responseTimeout,
                                   String initialState,
                                   String endpointEncoding,
                                   String endpointBuilderName,
                                   MuleContext muleContext,
                                   RetryPolicyTemplate retryPolicyTemplate,
                                   String responsePropertiesList)
    {
        super(connector, endpointUri, transformers, responseTransformers, name, properties, transactionConfig, filter,
            deleteUnacceptedMessage, securityFilter, synchronous, responseTimeout, initialState,
            endpointEncoding, endpointBuilderName, muleContext, retryPolicyTemplate);
        
        responseProperties = new ArrayList<String>();
        // Propagate the Correlation-related properties from the previous message by default (see EE-1613).
        responseProperties.add(MuleProperties.MULE_CORRELATION_ID_PROPERTY);
        responseProperties.add(MuleProperties.MULE_CORRELATION_GROUP_SIZE_PROPERTY);
        responseProperties.add(MuleProperties.MULE_CORRELATION_SEQUENCE_PROPERTY);
        // Add any additional properties specified by the user.
        String[] props = StringUtils.splitAndTrim(responsePropertiesList, ",");
        if (props != null)
        {
            for (int i=0; i < props.length; ++i)
            {
                responseProperties.add(props[i]);
            }
        }
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
    
    public List<String> getResponseProperties()
    {
        return responseProperties;
    }
}
