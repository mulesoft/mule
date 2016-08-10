/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.compatibility.core.endpoint;

import static org.mule.compatibility.core.registry.MuleRegistryTransportHelper.lookupServiceDescriptor;
import org.mule.compatibility.core.api.endpoint.EndpointBuilder;
import org.mule.compatibility.core.api.endpoint.EndpointException;
import org.mule.compatibility.core.api.endpoint.EndpointFactory;
import org.mule.compatibility.core.api.endpoint.EndpointURI;
import org.mule.compatibility.core.api.endpoint.ImmutableEndpoint;
import org.mule.compatibility.core.api.endpoint.InboundEndpoint;
import org.mule.compatibility.core.api.endpoint.OutboundEndpoint;
import org.mule.compatibility.core.api.registry.LegacyServiceType;
import org.mule.compatibility.core.registry.MuleRegistryTransportHelper;
import org.mule.compatibility.core.transport.service.TransportServiceDescriptor;
import org.mule.runtime.core.api.MuleContext;
import org.mule.runtime.core.api.MuleException;
import org.mule.runtime.core.api.registry.RegistrationException;
import org.mule.runtime.core.config.i18n.CoreMessages;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class DefaultEndpointFactory implements EndpointFactory
{
    /**
     * logger used by this class
     */
    protected static final Logger logger = LoggerFactory.getLogger(DefaultEndpointFactory.class);

    public static final String ENDPOINT_REGISTRY_PREFIX = "endpoint:";

    protected MuleContext muleContext;

    @Override
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

    @Override
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
        EndpointBuilder endpointBuilder = MuleRegistryTransportHelper.lookupEndpointBuilder(muleContext.getRegistry(), endpointName);
        if (endpointBuilder != null)
        {
            logger.debug("EndpointBuilder with name:" + endpointName + " FOUND");
        }
        return endpointBuilder;
    }

    @Override
    public InboundEndpoint getInboundEndpoint(EndpointBuilder builder) throws MuleException
    {
        InboundEndpoint endpoint = builder.buildInboundEndpoint();
        // Only continue to cache inbound endpoints because another project uses this.
        return (InboundEndpoint) registerEndpoint(endpoint);
    }

    @Override
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

    @Override
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
            TransportServiceDescriptor tsd = (TransportServiceDescriptor) lookupServiceDescriptor(muleContext.getRegistry(), LegacyServiceType.TRANSPORT,
                    epURI.getFullScheme(), null);
            endpointBuilder = tsd.createEndpointBuilder(uri, muleContext);
        }
        return endpointBuilder;
    }

    @Override
    public void setMuleContext(MuleContext context)
    {
        this.muleContext = context;
    }

    public OutboundEndpoint getOutboundEndpoint(EndpointURI uri) throws MuleException
    {
        return (OutboundEndpoint) getEndpoint(uri, new EndpointSource()
        {
            @Override
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
