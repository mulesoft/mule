/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.endpoint;

import org.mule.api.MuleContext;
import org.mule.api.MuleException;
import org.mule.api.endpoint.EndpointBuilder;
import org.mule.api.endpoint.EndpointException;
import org.mule.api.endpoint.EndpointFactory;
import org.mule.api.endpoint.EndpointURI;
import org.mule.api.endpoint.ImmutableEndpoint;
import org.mule.api.endpoint.InboundEndpoint;
import org.mule.api.endpoint.OutboundEndpoint;
import org.mule.api.registry.RegistrationException;
import org.mule.api.registry.ServiceType;
import org.mule.config.i18n.CoreMessages;
import org.mule.transport.service.TransportServiceDescriptor;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

public class DefaultEndpointFactory implements EndpointFactory
{
    /**
     * logger used by this class
     */
    protected static final Log logger = LogFactory.getLog(DefaultEndpointFactory.class);

    public static final String ENDPOINT_REGISTRY_PREFIX = "endpoint:";

    protected MuleContext muleContext;

    public InboundEndpoint getInboundEndpoint(String uri)
            throws MuleException
    {
        logger.debug("DefaultEndpointFactory request for inbound endpoint for uri: " + uri);
        EndpointBuilder endpointBuilder = lookupEndpointBuilder(uri);
        if (endpointBuilder == null)
        {
            logger.debug("Named EndpointBuilder not found, creating endpoint from uri");
            endpointBuilder = new EndpointURIEndpointBuilder(uri, muleContext);
        }
        return getInboundEndpoint(endpointBuilder);
    }

    public OutboundEndpoint getOutboundEndpoint(String uri)
            throws MuleException
    {
        logger.debug("DefaultEndpointFactory request for outbound endpoint for uri: " + uri);
        EndpointBuilder endpointBuilder = lookupEndpointBuilder(uri);
        if (endpointBuilder == null)
        {
            logger.debug("Named EndpointBuilder not found, creating endpoint from uri");
            endpointBuilder = new EndpointURIEndpointBuilder(uri, muleContext);

        }
        return getOutboundEndpoint(endpointBuilder);
    }

    protected EndpointBuilder lookupEndpointBuilder(String endpointName)
    {
        logger.debug("Looking up EndpointBuilder with name:" + endpointName + " in registry");
        // TODO DF: Do some simple parsing of endpointName to not lookup endpoint builder if endpointName is
        // obviously a uri and not a substituted name ??
        EndpointBuilder endpointBuilder = muleContext.getRegistry().lookupEndpointBuilder(endpointName);
        if (endpointBuilder != null)
        {
            logger.debug("EndpointBuilder with name:" + endpointName + " FOUND");
        }
        return endpointBuilder;
    }

    public InboundEndpoint getInboundEndpoint(EndpointBuilder builder) throws MuleException
    {
        InboundEndpoint endpoint = builder.buildInboundEndpoint();
        // Only continue to cache inbound endpoints because another project uses this.
        return (InboundEndpoint) registerEndpoint(endpoint);
    }

    public OutboundEndpoint getOutboundEndpoint(EndpointBuilder builder) throws MuleException
    {
        return builder.buildOutboundEndpoint();
    }

    /**
     * @param endpoint
     * @throws RegistrationException
     */
    protected ImmutableEndpoint registerEndpoint(ImmutableEndpoint endpoint) throws RegistrationException
    {
        ImmutableEndpoint registryEndpoint = (ImmutableEndpoint) muleContext.getRegistry().lookupObject(
                ENDPOINT_REGISTRY_PREFIX + endpoint.hashCode());
        if (registryEndpoint == null)
        {
            muleContext.getRegistry().registerObject(ENDPOINT_REGISTRY_PREFIX + endpoint.hashCode(), endpoint);
            registryEndpoint = endpoint;
        }
        return registryEndpoint;
    }

    public EndpointBuilder getEndpointBuilder(String uri)
            throws MuleException
    {
        logger.debug("DefaultEndpointFactory request for endpoint builder for uri: " + uri);
        EndpointBuilder endpointBuilder = lookupEndpointBuilder(uri);
        if (endpointBuilder != null)
        {
            try
            {
                endpointBuilder = (EndpointBuilder) endpointBuilder.clone();
            }
            catch (Exception e)
            {
                throw new EndpointException(CoreMessages.failedToClone("global endpoint EndpointBuilder"), e);
            }
        }
        else
        {
            logger.debug("Named EndpointBuilder not found, creating endpoint builder for uri");
            EndpointURI epURI = new MuleEndpointURI(uri, muleContext);
            TransportServiceDescriptor tsd = (TransportServiceDescriptor) muleContext.getRegistry().lookupServiceDescriptor(ServiceType.TRANSPORT, epURI.getFullScheme(), null);
            endpointBuilder = tsd.createEndpointBuilder(uri, muleContext);
        }
        return endpointBuilder;
    }

    public void setMuleContext(MuleContext context)
    {
        this.muleContext = context;
    }

    public OutboundEndpoint getOutboundEndpoint(EndpointURI uri) throws MuleException
    {
        return (OutboundEndpoint) getEndpoint(uri, new EndpointSource()
        {
            public ImmutableEndpoint getEndpoint(EndpointBuilder builder) throws MuleException
            {
                return getOutboundEndpoint(builder);
            }
        });
    }

    protected ImmutableEndpoint getEndpoint(EndpointURI uri, EndpointSource source) throws MuleException
    {
        logger.debug("DefaultEndpointFactory request for endpoint for: " + uri);
        EndpointBuilder endpointBuilder = null;
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
            endpointBuilder = new EndpointURIEndpointBuilder(uri);
        }
        return source.getEndpoint(endpointBuilder);
    }

    private interface EndpointSource
    {
        ImmutableEndpoint getEndpoint(EndpointBuilder endpointBuilder) throws MuleException;
    }

}
