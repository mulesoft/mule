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
import org.mule.umo.UMOException;
import org.mule.umo.UMOManagementContext;
import org.mule.umo.endpoint.EndpointException;
import org.mule.umo.endpoint.UMOEndpointBuilder;
import org.mule.umo.endpoint.UMOEndpointFactory;
import org.mule.umo.endpoint.UMOEndpointURI;
import org.mule.umo.endpoint.UMOImmutableEndpoint;
import org.mule.umo.lifecycle.InitialisationException;

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

    public UMOImmutableEndpoint createInboundEndpoint(String uri, UMOManagementContext managementContext)
            throws UMOException
    {
        UMOImmutableEndpoint globalEndpoint = lookupEndpoint(uri);
        UMOImmutableEndpoint endpoint = null;
        if (globalEndpoint != null)
        {
            // Copy for now.  Once global endpoints are builders we will invoke builder here
            endpoint = new InboundEndpoint(globalEndpoint);
        }
        else
        {
            UMOEndpointBuilder endpointBuilder = new EndpointURIEndpointBuilder(new MuleEndpointURI(uri), managementContext);
            return endpointBuilder.buildInboundEndpoint();
        }
        return endpoint;
    }

    public UMOImmutableEndpoint createOutboundEndpoint(String uri, UMOManagementContext managementContext)
            throws UMOException
    {
        UMOImmutableEndpoint globalEndpoint = lookupEndpoint(uri);
        UMOImmutableEndpoint endpoint = null;
        if (globalEndpoint != null)
        {
            // Copy for now.  Once global endpoints are builders we will invoke builder here
            endpoint = new OutboundEndpoint(globalEndpoint);
        }
        else
        {
            UMOEndpointBuilder endpointBuilder = new EndpointURIEndpointBuilder(new MuleEndpointURI(uri), managementContext);
            return endpointBuilder.buildOutboundEndpoint();
        }
        return endpoint;
    }

    public UMOImmutableEndpoint createResponseEndpoint(String uri, UMOManagementContext managementContext)
            throws UMOException
    {
        UMOImmutableEndpoint globalEndpoint = lookupEndpoint(uri);
        UMOImmutableEndpoint endpoint = null;
        if (globalEndpoint != null)
        {
            // Copy for now.  Once global endpoints are builders we will invoke builder here
            endpoint = new ResponseEndpoint(globalEndpoint);
        }
        else
        {
            UMOEndpointBuilder endpointBuilder = new EndpointURIEndpointBuilder(new MuleEndpointURI(uri), managementContext);
            return endpointBuilder.buildResponseEndpoint();
        }
        return endpoint;
    }

    /** @deprecated  */
    public UMOImmutableEndpoint createEndpoint(UMOEndpointURI uri,
                                               String type,
                                               UMOManagementContext managementContext) throws UMOException
    {
        UMOImmutableEndpoint endpoint = null;
        if (uri.getEndpointName() != null)
        {
            endpoint = lookupEndpoint(uri.getEndpointName());
        }
        if (endpoint == null)
        {
            endpoint = buidNewEndpoint(uri, type, managementContext);
        }
        return endpoint;
    }

    protected UMOImmutableEndpoint lookupEndpoint(String poiendpointNamentName)
    {
        return RegistryContext.getRegistry().lookupEndpoint(poiendpointNamentName);
    }

    protected UMOImmutableEndpoint buidNewEndpoint(UMOEndpointURI uri,
                                                   String type,
                                                   UMOManagementContext managementContext)
            throws InitialisationException, EndpointException
    {
        UMOEndpointBuilder endpointBuilder = new EndpointURIEndpointBuilder(uri, managementContext);
        if (UMOImmutableEndpoint.ENDPOINT_TYPE_RECEIVER.equals(type))
        {
            return endpointBuilder.buildInboundEndpoint();
        }
        else if (UMOImmutableEndpoint.ENDPOINT_TYPE_SENDER.equals(type))
        {
            return endpointBuilder.buildOutboundEndpoint();
        }
        else if (UMOImmutableEndpoint.ENDPOINT_TYPE_RESPONSE.equals(type))
        {
            return endpointBuilder.buildResponseEndpoint();
        }
        else
        {
            throw new IllegalArgumentException("The endpoint type: " + type + "is not recognized.");

        }
    }

    protected UMOImmutableEndpoint buidNewInboundEndpoint(UMOEndpointURI uri,
                                                          String type,
                                                          UMOManagementContext managementContext)
            throws InitialisationException, EndpointException
    {
        UMOEndpointBuilder endpointBuilder = new EndpointURIEndpointBuilder(uri, managementContext);
        return endpointBuilder.buildInboundEndpoint();
    }

    protected UMOImmutableEndpoint buidNewOutboundEndpoint(UMOEndpointURI uri,
                                                           String type,
                                                           UMOManagementContext managementContext)
            throws InitialisationException, EndpointException
    {
        UMOEndpointBuilder endpointBuilder = new EndpointURIEndpointBuilder(uri, managementContext);
        return endpointBuilder.buildOutboundEndpoint();
    }

    protected UMOImmutableEndpoint buidNewResponeEndpoint(UMOEndpointURI uri,
                                                          String type,
                                                          UMOManagementContext managementContext)
            throws InitialisationException, EndpointException
    {
        UMOEndpointBuilder endpointBuilder = new EndpointURIEndpointBuilder(uri, managementContext);
        return endpointBuilder.buildResponseEndpoint();
    }

}
