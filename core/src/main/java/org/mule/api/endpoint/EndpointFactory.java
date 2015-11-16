/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.api.endpoint;

import org.mule.api.MuleException;
import org.mule.api.context.MuleContextAware;
import org.mule.api.registry.Registry;
import org.mule.endpoint.EndpointURIEndpointBuilder;

/**
 * Endpoint factory creates immutable instances of {@link ImmutableEndpoint}. These endpoints may be <br/>
 * <li> Endpoints created by uri string of the type requested.
 * <li> Endpoints of the type requested created based on a given global endpoint identified by global endpoint
 * name.
 * <li> Endpoints of the type requested based on an existing configured endpoint instance that has been given
 * a name in configuration. <br/> <br/> This factory always returns new unique endpoint instances. The
 * {@link Registry} should be used to lookup/create endpoints.
 */
public interface EndpointFactory extends MuleContextAware
{

    /**
     * Creates an endpoint with the "INBOUND" role. <br/><br/> The uri parameter can either be a uri, or a
     * (global) endpoint identifier or name. <br/><br/> The {@link InboundEndpoint} interface is
     * currently used as the return type but this will be replaces by and more specific interface. SEE
     * MULE-2292
     * 
     * @param uri endpoint identifier or uri
     * @throws MuleException
     */
    InboundEndpoint getInboundEndpoint(String uri) throws MuleException;

    /**
     * Creates an endpoint with the "OUTBOUND" role. <br/><br/> The uri parameter can either be a uri, or a
     * (global) endpoint identifier or name. <br/><br/> The {@link OutboundEndpoint} interface is
     * currently used as the return type but this will be replaces by and more specific interface. SEE
     * MULE-2292
     *
     *  To add an outbound endpoint in a pipeline an honor the execution context of an endpoint a {@link org.mule.api.endpoint.OutboundEndpointExecutorFactory} must
     * be used to wrap the outbound endpoint.
     * 
     * @param uri endpoint identifier or uri
     * @throws MuleException
     */
    OutboundEndpoint getOutboundEndpoint(String uri) throws MuleException;

    /**
     * Creates an endpoint with the "INBOUND" role using the builder provided.
     * 
     * @param builder
     * @throws MuleException
     */
    InboundEndpoint getInboundEndpoint(EndpointBuilder builder) throws MuleException;

    /**
     * Creates an endpoint with the "OUTBOUND" role using the builder provided.
     *
     * To add an outbound endpoint in a pipeline an {@link org.mule.api.endpoint.OutboundEndpointExecutorFactory} must
     * be used to wrap the outbound endpoint.
     * 
     * @param builder
     * @throws MuleException
     */
    OutboundEndpoint getOutboundEndpoint(EndpointBuilder builder) throws MuleException;

    /**
     * Used to retrieve the an EndpointBuilder equal to the one would be used to create an endpoint.<br/><br/>
     *  This is
     * useful if you need to customize a builder before creation of an endpoint as you can use this method to
     * obtain the endpoint builder, custommize it and then call the factory methods that take a
     * EndpointBuilder rather than a String. <br/><br/><i>(Of course if you know the uri is a uri rather than a global
     * endpoint identifier you could create your own EndpointURIEndpointBuilder locally, this method allows
     * the uri to be substituted with a global endpoint name and returns it's builder if this is the case.
     * allow the uri parameter to be either a uri or a global endpoint identifier you need this method.</i> <br/><br/>
     * Each and every call to this method, even if it is for the same uri/global endpoint name will return a new EndpoointBuilder instance.
     * 
     * @param uri endpoint identifier or uri
     * @throws MuleException
     * @see EndpointBuilder
     * @see EndpointURIEndpointBuilder
     */
    EndpointBuilder getEndpointBuilder(String uri) throws MuleException;

}
