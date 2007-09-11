/*
 * $Id$
 * --------------------------------------------------------------------------------------
 * Copyright (c) MuleSource, Inc.  All rights reserved.  http://www.mulesource.com
 *
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.umo.endpoint;

import org.mule.registry.Registry;
import org.mule.umo.UMOException;
import org.mule.umo.UMOManagementContext;

/**
 * Endpoint factory creates immutable instances of {@link UMOImmutableEndpoint}.
 * These endpoints may be <br/>
 * <li> Endpoints created by uri string of the type requested.
 * <li> Endpoints of the type requested created based on a given global endpoint identified by global endpoint
 * name.
 * <li> Endpoints of the type requested based on an existing configured endpoint instance that has been given
 * a name in configuration. <br/> <br/> This factory always returns new unique endpoint
 * instances.  The {@link Registry} should be used to lookup/create endpoints.
 */
public interface UMOEndpointFactory
{

    /**
     * Creates an endpoint with the "INBOUND" role. <br/><br/> The uri parameter
     * can either be a uri, or a (global) endpoint identifier or name. <br/><br/> The
     * {@link UMOImmutableEndpoint} interface is currently used as the return type but this will be replaces
     * by and more specific interface. SEE MULE-2292
     * 
     * @param uri endpoint identifier or uri
     * @param managementContext
     * @return
     * @throws UMOException
     */
    public abstract UMOImmutableEndpoint createInboundEndpoint(String uri,
                                                               UMOManagementContext managementContext)
        throws UMOException;

    /**
     * Creates an endpoint with the "OUTBOUND" role. <br/><br/> The uri parameter
     * can either be a uri, or a (global) endpoint identifier or name. <br/><br/> The
     * {@link UMOImmutableEndpoint} interface is currently used as the return type but this will be replaces
     * by and more specific interface. SEE MULE-2292
     * 
     * @param uri endpoint identifier or uri
     * @param managementContext
     * @return
     * @throws UMOException
     */
    public abstract UMOImmutableEndpoint createOutboundEndpoint(String uri,
                                                                UMOManagementContext managementContext)
        throws UMOException;

    /**
     * Creates an endpoint with the "RESPONSE" role. <br/><br/> The uri parameter
     * can either be a uri, or a (global) endpoint identifier or name. <br/><br/> The
     * {@link UMOImmutableEndpoint} interface is currently used as the return type but this will be replaces
     * by and more specific interface. SEE MULE-2292<br/><br/> Also see MULE-2293.
     * 
     * @param uri endpoint identifier or uri.<br/><br/>
     * @param managementContext
     * @return
     * @throws UMOException
     */
    public abstract UMOImmutableEndpoint createResponseEndpoint(String uri,
                                                                UMOManagementContext managementContext)
        throws UMOException;

    /**
     * @param endpointUri
     * @param endpointType
     * @param managementContext
     * @return
     * @throws EndpointException
     * @throws UMOException
     */
    public abstract UMOImmutableEndpoint createEndpoint(UMOEndpointURI endpointUri,
                                                        String endpointType,
                                                        UMOManagementContext managementContext)
        throws EndpointException, UMOException;

}
