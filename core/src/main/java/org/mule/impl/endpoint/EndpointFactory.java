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

import org.mule.RegistryContext;
import org.mule.config.i18n.CoreMessages;
import org.mule.umo.UMOException;
import org.mule.umo.UMOManagementContext;
import org.mule.umo.endpoint.EndpointException;
import org.mule.umo.endpoint.UMOEndpointBuilder;
import org.mule.umo.endpoint.UMOEndpointFactory;
import org.mule.umo.endpoint.UMOEndpointURI;
import org.mule.umo.endpoint.UMOImmutableEndpoint;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

public class EndpointFactory implements UMOEndpointFactory
{
    /** logger used by this class */
    protected static final Log logger = LogFactory.getLog(EndpointFactory.class);

    public static final int GET_OR_CREATE_CONNECTOR = 0;
    public static final int ALWAYS_CREATE_CONNECTOR = 1;
    public static final int NEVER_CREATE_CONNECTOR = 2;
    public static final int USE_CONNECTOR = 3;

    protected UMOManagementContext managementContext;
    
    public UMOImmutableEndpoint getInboundEndpoint(String uri)
        throws UMOException
    {
        logger.debug("EndpointFactory request for inbound endpoint for uri: " + uri);
        UMOEndpointBuilder endpointBuilder = lookupEndpointBuilder(uri);
        if (endpointBuilder == null)
        {
            logger.debug("Named EndpointBuilder not found, creating endpoint from uri");
            endpointBuilder = new EndpointURIEndpointBuilder(uri, managementContext);
        }
        return getInboundEndpoint(endpointBuilder);
    }

    public UMOImmutableEndpoint getOutboundEndpoint(String uri)
        throws UMOException
    {
        logger.debug("EndpointFactory request for outbound endpoint for uri: " + uri);
        UMOEndpointBuilder endpointBuilder = lookupEndpointBuilder(uri);
        if (endpointBuilder == null)
        {
            logger.debug("Named EndpointBuilder not found, creating endpoint from uri");
            endpointBuilder = new EndpointURIEndpointBuilder(uri, managementContext);
            
        }
        return getOutboundEndpoint(endpointBuilder);
    }

    public UMOImmutableEndpoint getResponseEndpoint(String uri)
        throws UMOException
    {
        logger.debug("EndpointFactory request for response endpoint for uri: " + uri);
        UMOEndpointBuilder endpointBuilder = lookupEndpointBuilder(uri);
        if (endpointBuilder == null)
        {
            logger.debug("Named EndpointBuilder not found, creating endpoint from uri");
            endpointBuilder = new EndpointURIEndpointBuilder(uri, managementContext);
        }
        return getResponseEndpoint(endpointBuilder);
    }

    /** @deprecated */
    public UMOImmutableEndpoint getEndpoint(UMOEndpointURI uri, String type)
        throws UMOException
    {
        logger.debug("EndpointFactory request for endpoint of type: " + type + ", for uri: " + uri);
        UMOEndpointBuilder endpointBuilder = null;
        // IF EndpointURI has a name lookup
        if (uri.getEndpointName() != null)
        {
            endpointBuilder = lookupEndpointBuilder(uri.getEndpointName());
            if (endpointBuilder == null)
            {
                throw new IllegalArgumentException("The endpoint with name: " + uri.getEndpointName()
                                                   + "was not found.");
            }
        }
        else
        {
            logger.debug("Named EndpointBuilder not found, creating endpoint from uri");
            endpointBuilder = new EndpointURIEndpointBuilder(uri, managementContext);
        }
        if (UMOImmutableEndpoint.ENDPOINT_TYPE_RECEIVER.equals(type))
        {
            return getInboundEndpoint(endpointBuilder);
        }
        else if (UMOImmutableEndpoint.ENDPOINT_TYPE_SENDER.equals(type))
        {
            return getOutboundEndpoint(endpointBuilder);
        }
        else if (UMOImmutableEndpoint.ENDPOINT_TYPE_RESPONSE.equals(type))
        {
            return getResponseEndpoint(endpointBuilder);
        }
        else
        {
            throw new IllegalArgumentException("The endpoint type: " + type + "is not recognized.");

        }
    }

    protected UMOEndpointBuilder lookupEndpointBuilder(String endpointName)
    {
        logger.debug("Looking up EndpointBuilder with name:" + endpointName + " in registry");
        // TODO DF: Do some simple parsing of endpointName to not lookup endpoint builder if endpointName is
        // obviously a uri and not a substituted name ??
        UMOEndpointBuilder endpointBuilder = RegistryContext.getRegistry().lookupEndpointBuilder(endpointName);
        if (endpointBuilder != null)
        {
            logger.debug("EndpointBuilder with name:" + endpointName + " FOUND");
        }
        return endpointBuilder;
    }

    public UMOImmutableEndpoint getInboundEndpoint(UMOEndpointBuilder builder) throws UMOException
    {
        // TODO 1) Store in repo, 2) Register in registry, 3) Lifecycle ?
        return builder.buildInboundEndpoint();
    }

    public UMOImmutableEndpoint getOutboundEndpoint(UMOEndpointBuilder builder)
        throws UMOException
    {
        // TODO 1) Store in repo, 2) Register in registry, 3) Lifecycle ?
        return builder.buildOutboundEndpoint();
    }

    public UMOImmutableEndpoint getResponseEndpoint(UMOEndpointBuilder builder)
        throws UMOException
    {
        // TODO 1) Store in repo, 2) Register in registry, 3) Lifecycle ?
        return builder.buildResponseEndpoint();
    }

    public UMOEndpointBuilder getEndpointBuilder(String uri)
        throws UMOException
    {
        logger.debug("EndpointFactory request for endpoint builder for uri: " + uri);
        UMOEndpointBuilder endpointBuilder = lookupEndpointBuilder(uri);
        if (endpointBuilder != null)
        {
            try
            {
                endpointBuilder = (UMOEndpointBuilder) endpointBuilder.clone();
            }
            catch (Exception e)
            {
                throw new EndpointException(CoreMessages.failedToClone("global endpoint EndpointBuilder"), e);
            }
        }
        else
        {
            logger.debug("Named EndpointBuilder not found, creating endpoint builder for uri");
            endpointBuilder = new EndpointURIEndpointBuilder(uri, managementContext);
        }
        return endpointBuilder;
    }
    
    public void setManagementContext(UMOManagementContext context)
    {
        this.managementContext = context;
    }
}
